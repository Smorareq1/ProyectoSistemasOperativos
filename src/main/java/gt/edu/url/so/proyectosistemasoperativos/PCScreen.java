package gt.edu.url.so.proyectosistemasoperativos;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gt.edu.url.so.proyectosistemasoperativos.common.GameLogger;
import gt.edu.url.so.proyectosistemasoperativos.common.PixelArtRenderer;
import gt.edu.url.so.proyectosistemasoperativos.common.PostProcessingPipeline;
import gt.edu.url.so.proyectosistemasoperativos.producerconsumer.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static gt.edu.url.so.proyectosistemasoperativos.common.PixelArtRenderer.*;

public class PCScreen extends ScreenAdapter {

    private static final Color COLOR_PAR = TEAL;
    private static final Color COLOR_IMPAR = GREEN;
    private static final Color COLOR_PRIMO = ORANGE;

    private final SOGame game;
    private PixelArtRenderer renderer;
    private PostProcessingPipeline postFx;
    private Stage stage;

    private int animFrame = 0;
    private float frameTimer = 0;

    // Simulation
    private PCController controller;
    private int minerState = 0;
    private int[] robotStates = {0, 0, 0};
    private int lastMinedNumber = -1;
    private boolean paused = false;

    // UI
    private BitmapFont uiFont;
    private BitmapFont smallFont;
    private BitmapFont logFont;
    private Skin skin;
    private Label minerStatusLabel;
    private Label minerNumberLabel;
    private Label bufferCountLabel;
    private Label[] robotScoreLabels = new Label[3];
    private Label[] robotStatusLabels = new Label[3];
    private final List<String> logMessages = new ArrayList<>();
    private Label logLabel;
    private ScrollPane logScrollPane;
    private static final int MAX_LOG = 50;
    private static final int BUFFER_SIZE = 12;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Scale 3 = 366x250 pixel art space (more detail)
    private static final int CW = 1100, CH = 750;
    private static final int SCALE = 3;

    private final GameLogger logger = new GameLogger() {
        @Override
        public void log(String message) {
            String entry = "[" + LocalTime.now().format(TIME_FMT) + "] " + message;
            synchronized (logMessages) {
                logMessages.add(entry);
                if (logMessages.size() > MAX_LOG) logMessages.remove(0);
            }
        }
        @Override
        public void clear() {
            synchronized (logMessages) { logMessages.clear(); }
        }
    };

    public PCScreen(SOGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        renderer = new PixelArtRenderer(CW, CH, SCALE);
        postFx = new PostProcessingPipeline(CW, CH);

        // HD-2D: dark industrial scene + bloom on sparks/lights
        postFx.setFocusCenter(0.45f);
        postFx.setFocusRange(0.40f);
        postFx.setDofStrength(0.22f);
        postFx.setDofRadius(2.0f);
        postFx.setBloomIntensity(0.35f); // sparks and ore glow in dark
        postFx.setBloomThreshold(0.40f);
        postFx.setBloomRadius(2.0f);
        postFx.setWarmth(1.18f);
        postFx.setContrast(1.15f);      // deep industrial contrast
        postFx.setSaturation(1.18f);
        postFx.setVignetteRadius(0.68f);
        postFx.setVignetteSoftness(0.42f);

        FreeTypeFontGenerator gen = loadFontGen();
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = 18;
        p.color = Color.WHITE;
        p.borderWidth = 2;
        p.borderColor = new Color(0.1f, 0.05f, 0.0f, 1f);
        uiFont = gen.generateFont(p);
        p.size = 14;
        p.color = new Color(1f, 0.95f, 0.85f, 1f);
        p.borderWidth = 1.5f;
        p.borderColor = new Color(0.1f, 0.05f, 0.0f, 1f);
        smallFont = gen.generateFont(p);

        // Smaller font for logs — fits more text
        p.size = 10;
        p.color = new Color(0.9f, 0.88f, 0.78f, 1f);
        p.borderWidth = 1f;
        p.borderColor = new Color(0.08f, 0.04f, 0.0f, 1f);
        logFont = gen.generateFont(p);

        gen.dispose();

        controller = new PCController(logger);
        setupCallbacks();
        buildSkin();
        buildUI();
    }

    private FreeTypeFontGenerator loadFontGen() {
        try {
            return new FreeTypeFontGenerator(Gdx.files.classpath(
                    "gt/edu/url/so/proyectosistemasoperativos/common/fonts/PressStart2P-Regular.ttf"));
        } catch (Exception e) {
            return new FreeTypeFontGenerator(Gdx.files.internal(
                    "gt/edu/url/so/proyectosistemasoperativos/common/fonts/PressStart2P-Regular.ttf"));
        }
    }

    private void setupCallbacks() {
        controller.setProducerCallbacks(
                estado -> {
                    switch (estado) {
                        case "ACTIVO" -> minerState = 1;
                        case "BLOQUEADO" -> minerState = 3;
                        case "TERMINADO" -> minerState = 4;
                    }
                },
                numero -> lastMinedNumber = numero
        );
        controller.setConsumerParCallbacks(
                estado -> updateRobotState(0, estado),
                (num, suma) -> Gdx.app.postRunnable(() -> robotScoreLabels[0].setText(String.valueOf(suma)))
        );
        controller.setConsumerImparCallbacks(
                estado -> updateRobotState(1, estado),
                (num, suma) -> Gdx.app.postRunnable(() -> robotScoreLabels[1].setText(String.valueOf(suma)))
        );
        controller.setConsumerPrimoCallbacks(
                estado -> updateRobotState(2, estado),
                (num, suma) -> Gdx.app.postRunnable(() -> robotScoreLabels[2].setText(String.valueOf(suma)))
        );
    }

    private void updateRobotState(int idx, String estado) {
        switch (estado) {
            case "ACTIVO" -> robotStates[idx] = 1;
            case "BLOQUEADO" -> robotStates[idx] = 2;
            case "TERMINADO" -> robotStates[idx] = 3;
        }
    }

    private void buildSkin() {
        skin = new Skin();
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);

        pm.setColor(new Color(0.18f, 0.12f, 0.06f, 0.85f));
        pm.fill();
        Texture brownTex = new Texture(pm);
        pm.setColor(new Color(0.30f, 0.18f, 0.08f, 0.90f));
        pm.fill();
        Texture overTex = new Texture(pm);
        pm.setColor(new Color(0.45f, 0.28f, 0.12f, 0.95f));
        pm.fill();
        Texture downTex = new Texture(pm);
        pm.setColor(new Color(0.08f, 0.06f, 0.03f, 0.78f));
        pm.fill();
        Texture darkTex = new Texture(pm);
        pm.dispose();

        TextButton.TextButtonStyle tbs = new TextButton.TextButtonStyle();
        tbs.font = uiFont;
        tbs.fontColor = new Color(1f, 0.95f, 0.8f, 1f);
        tbs.overFontColor = new Color(1f, 0.8f, 0.3f, 1f);
        tbs.downFontColor = Color.WHITE;
        tbs.up = new TextureRegionDrawable(new TextureRegion(brownTex));
        tbs.over = new TextureRegionDrawable(new TextureRegion(overTex));
        tbs.down = new TextureRegionDrawable(new TextureRegion(downTex));
        skin.add("default", tbs);

        Label.LabelStyle ls = new Label.LabelStyle(smallFont, smallFont.getColor());
        skin.add("default", ls);

        Label.LabelStyle lsUI = new Label.LabelStyle(uiFont, Color.WHITE);
        skin.add("ui", lsUI);

        // Compact readable font for logs (default LibGDX font - much more compact than PressStart2P)
        BitmapFont defaultFont = new BitmapFont();
        defaultFont.setColor(new Color(0.9f, 0.88f, 0.78f, 1f));
        defaultFont.getData().setScale(1.1f);
        Label.LabelStyle logStyle = new Label.LabelStyle(defaultFont, new Color(0.9f, 0.88f, 0.78f, 1f));
        skin.add("log", logStyle);

        ScrollPane.ScrollPaneStyle sps = new ScrollPane.ScrollPaneStyle();
        sps.background = new TextureRegionDrawable(new TextureRegion(darkTex));
        skin.add("default", sps);

        skin.add("panel-bg", new TextureRegionDrawable(new TextureRegion(darkTex)), TextureRegionDrawable.class);
    }

    private Label speedLabel;

    private void buildUI() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        TextureRegionDrawable darkBg = skin.get("panel-bg", TextureRegionDrawable.class);

        Table root = new Table();
        root.setFillParent(true);
        root.top();

        // === Top bar ===
        TextButton backBtn = new TextButton("  MENU  ", skin);
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                cleanup();
                game.showMenu();
            }
        });

        Label title = new Label("  PIXEL FACTORY  ", skin, "ui");

        TextButton playBtn = new TextButton("  START  ", skin);
        TextButton pauseBtn = new TextButton("  PAUSE  ", skin);
        TextButton stopBtn = new TextButton("  STOP  ", skin);

        // Speed controls
        TextButton speedDown = new TextButton(" - ", skin);
        TextButton speedUp = new TextButton(" + ", skin);
        speedLabel = new Label("x1", skin);

        playBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                if (!controller.isRunning()) controller.iniciar();
                else if (paused) { controller.reanudar(); paused = false; }
            }
        });
        pauseBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                if (controller.isRunning() && !paused) { controller.pausar(); paused = true; }
            }
        });
        stopBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                controller.detener();
                paused = false;
            }
        });
        speedDown.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                currentSpeedIdx = Math.max(0, currentSpeedIdx - 1);
                applySpeed();
            }
        });
        speedUp.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                currentSpeedIdx = Math.min(SPEED_OPTIONS.length - 1, currentSpeedIdx + 1);
                applySpeed();
            }
        });

        Table topBar = new Table();
        topBar.setBackground(darkBg);
        topBar.add(backBtn).pad(6).padLeft(10);
        topBar.add(title).expandX().center().pad(6);
        topBar.add(speedDown).pad(4);
        topBar.add(speedLabel).pad(4);
        topBar.add(speedUp).pad(4);
        topBar.add(playBtn).pad(6);
        topBar.add(pauseBtn).pad(6);
        topBar.add(stopBtn).pad(6).padRight(10);
        root.add(topBar).fillX().row();

        // === Middle: fully transparent for pixel art ===
        root.add().expand().fill().row();

        // === Bottom: STATUS ROW (compact) then FULL-WIDTH LOG ===
        Table bottomSection = new Table();
        bottomSection.setBackground(darkBg);

        // --- Status row: all info in one horizontal line ---
        Table statusRow = new Table();
        statusRow.left().pad(6, 10, 4, 10);

        minerStatusLabel = new Label("IDLE", skin);
        minerNumberLabel = new Label("-", skin);
        bufferCountLabel = new Label("0/12", skin);

        statusRow.add(new Label("MINER:", skin)).left().padRight(4);
        statusRow.add(minerStatusLabel).left().padRight(12);
        statusRow.add(new Label("NUM:", skin)).left().padRight(4);
        statusRow.add(minerNumberLabel).left().padRight(12);
        statusRow.add(new Label("BUF:", skin)).left().padRight(4);
        statusRow.add(bufferCountLabel).left().padRight(20);

        // Consumer scores inline
        String[] names = {"EVEN", "ODD", "PRIME"};
        Color[] nameColors = {new Color(0.4f, 0.8f, 0.9f, 1f), new Color(0.5f, 0.9f, 0.4f, 1f), new Color(1f, 0.8f, 0.3f, 1f)};
        for (int i = 0; i < 3; i++) {
            robotScoreLabels[i] = new Label("0", skin);
            robotStatusLabels[i] = new Label("IDLE", skin);
            Label nameLabel = new Label(names[i] + ":", skin);
            nameLabel.setColor(nameColors[i]);
            statusRow.add(nameLabel).left().padRight(2);
            statusRow.add(robotScoreLabels[i]).left().padRight(4);
            statusRow.add(robotStatusLabels[i]).left().padRight(12);
        }

        bottomSection.add(statusRow).fillX().left().row();

        // --- Separator line ---
        Pixmap linePm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        linePm.setColor(new Color(0.3f, 0.25f, 0.15f, 0.6f));
        linePm.fill();
        Texture lineTex = new Texture(linePm);
        linePm.dispose();
        Table separator = new Table();
        separator.setBackground(new TextureRegionDrawable(new TextureRegion(lineTex)));
        bottomSection.add(separator).fillX().height(1).padLeft(8).padRight(8).row();

        // --- Log area: full width, scrollable ---
        logLabel = new Label("", skin, "log");
        logLabel.setWrap(true);
        logScrollPane = new ScrollPane(logLabel, skin);
        logScrollPane.setFadeScrollBars(false);
        logScrollPane.setScrollingDisabled(true, false);

        bottomSection.add(logScrollPane).expand().fill().pad(4, 8, 6, 8);

        root.add(bottomSection).fillX().height(260).pad(2);

        stage.addActor(root);
    }

    private static final float[] SPEED_OPTIONS = {0.25f, 0.5f, 1f, 2f, 4f};
    private static final String[] SPEED_LABELS = {"x0.25", "x0.5", "x1", "x2", "x4"};
    private static final int[] SPEED_DELAYS = {2000, 1000, 500, 250, 125};
    private int currentSpeedIdx = 2; // default x1

    private void applySpeed() {
        speedLabel.setText(SPEED_LABELS[currentSpeedIdx]);
        controller.setDelay(SPEED_DELAYS[currentSpeedIdx]);
    }

    @Override
    public void render(float delta) {
        frameTimer += delta;
        if (frameTimer >= 0.1f) {
            frameTimer -= 0.1f;
            animFrame++;
        }

        renderer.beginFrame();
        renderScene();
        renderer.endFrame();

        postFx.render(renderer.getFrameBufferTexture());

        // Restore GL state for Scene2D overlay
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

        // Update UI labels
        Gdx.app.postRunnable(() -> {
            String[] statusTexts = {"IDLE", "MINING...", "", "BELT FULL!", "DONE"};
            if (minerState >= 0 && minerState < statusTexts.length) {
                minerStatusLabel.setText(statusTexts[minerState]);
            }
            if (lastMinedNumber >= 0) {
                minerNumberLabel.setText(String.valueOf(lastMinedNumber));
            }
            if (controller.getBuffer() != null) {
                int count = controller.getBuffer().getSnapshot().size();
                bufferCountLabel.setText(count + "/" + BUFFER_SIZE);
            }
            String[] robotTexts = {"IDLE", "CONSUMING", "SEARCHING", "COMPLETE"};
            for (int i = 0; i < 3; i++) {
                if (robotStates[i] >= 0 && robotStates[i] < robotTexts.length) {
                    robotStatusLabels[i].setText(robotTexts[robotStates[i]]);
                }
            }
        });

        synchronized (logMessages) {
            if (!logMessages.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String msg : logMessages) {
                    sb.append(msg).append("\n");
                }
                logLabel.setText(sb.toString());
                logScrollPane.layout();
                logScrollPane.setScrollPercentY(1f);
            }
        }

        stage.act(delta);
        stage.draw();
    }

    private void renderScene() {
        int pw = renderer.pxW();  // 366
        int ph = renderer.pxH();  // 250

        // Dark industrial background
        renderer.clear(web("#12100e"));

        // === LAYOUT ===
        int wallH = ph * 28 / 100;       // ~70 px wall
        int workEnd = ph * 68 / 100;     // ~170 px work floor
        int stoneStart = workEnd;

        // === DARK BRICK WALL ===
        renderer.drawBrickWall(0, 0, pw, wallH);
        renderer.fill(0, 0, pw, wallH, new Color(0, 0, 0, 0.40f));
        renderer.fill(0, wallH, pw, 2, BLACK);
        renderer.drawWarningStripes(0, wallH - 2, pw);

        // === DARK WORK FLOOR ===
        renderer.fill(0, wallH + 2, pw, workEnd - wallH - 2, web("#3a3428"));
        // Subtle floor grid
        for (int gx = 0; gx < pw; gx += 20) {
            renderer.fill(gx, wallH + 2, 1, workEnd - wallH - 2, new Color(0, 0, 0, 0.15f));
        }

        // === DARK STONE BASEMENT ===
        renderer.drawStoneFloor(0, stoneStart, pw, ph - stoneStart);
        renderer.fill(0, stoneStart, pw, ph - stoneStart, new Color(0, 0, 0, 0.45f));
        renderer.fill(0, stoneStart - 1, pw, 2, BLACK);

        // === WALL DECORATIONS ===
        renderer.drawTorch(15, wallH - 32, animFrame);
        renderer.drawTorch(pw / 3, wallH - 30, animFrame);
        renderer.drawTorch(pw * 2 / 3, wallH - 30, animFrame);
        renderer.drawTorch(pw - 20, wallH - 32, animFrame);

        // Pipes along wall
        renderer.drawPipeH(40, wallH - 12, pw / 3);
        renderer.drawPipeH(pw / 2 + 20, wallH - 14, pw / 3);
        renderer.drawPipeV(40, wallH - 28, 16);
        renderer.drawPipeV(pw - 60, wallH - 28, 14);

        // Gauges
        renderer.drawGauge(80, wallH - 26, (animFrame % 30) / 30.0);
        renderer.drawGauge(pw / 2, wallH - 26, 0.7);
        renderer.drawGauge(pw - 80, wallH - 24, 0.4);

        // === SIGN — centered ===
        int signW = 120;
        int signX = pw / 2 - signW / 2;
        renderer.fill(signX, 4, signW, 20, WOOD2);
        renderer.fill(signX + 1, 5, signW - 2, 18, WOOD1);
        renderer.fill(signX + 2, 6, signW - 4, 16, DK_RED);
        renderer.fill(signX + 2, 6, signW - 4, 4, RED);
        renderer.drawText("PIXEL FACTORY", signX + 8.0, 8.0, GOLD, 8);

        // === LIGHT EFFECTS (before scene objects) ===
        Color torchLight = new Color(1f, 0.75f, 0.35f, 1f);
        Color sparkLight = new Color(1f, 0.6f, 0.2f, 1f);

        renderer.drawLightGlow(15, wallH - 26, 35, torchLight, 0.12f);
        renderer.drawLightGlow(pw / 3, wallH - 24, 30, torchLight, 0.10f);
        renderer.drawLightGlow(pw * 2 / 3, wallH - 24, 30, torchLight, 0.10f);
        renderer.drawLightGlow(pw - 20, wallH - 26, 35, torchLight, 0.12f);

        // === PRODUCTION LINE (centered in work area) ===
        int lineY = wallH + (workEnd - wallH) / 2;

        // Light on the work area
        renderer.drawLightGlow(pw / 2, lineY, 80, torchLight, 0.05f);

        // Mine entrance (left side)
        renderer.drawMineEntrance(10, lineY - 20);
        renderer.drawLightGlow(18, lineY - 10, 20, sparkLight, 0.08f);

        // Miner
        int mFrame = minerState;
        if (minerState == 1) mFrame = (animFrame % 4 < 2) ? 1 : 2;
        renderer.drawMiner(36, lineY - 8, mFrame);

        if (minerState == 1) {
            renderer.drawSpark(54, lineY + 2, animFrame, GOLD);
            renderer.drawSpark(56, lineY, animFrame + 2, ORANGE);
            renderer.drawSpark(52, lineY + 4, animFrame + 4, ORANGE);
        }

        // Mined ore block — with glow
        if (lastMinedNumber >= 0 && minerState != 4) {
            TipoNumero tipo = NumberClassifier.clasificar(lastMinedNumber);
            Color numColor = switch (tipo) {
                case PAR -> COLOR_PAR;
                case IMPAR -> COLOR_IMPAR;
                case PRIMO -> COLOR_PRIMO;
            };
            renderer.drawLightGlow(40, lineY - 4, 12, numColor, 0.06f);
            renderer.drawOreBlock(36, lineY - 10, numColor);
            renderer.drawText(String.valueOf(lastMinedNumber), 36, lineY - 16.0, WHITE, 6);
        }

        // Pipes (miner → conveyor)
        int pipeY = lineY + 10;
        renderer.drawPipeH(58, pipeY, 24);
        int arrowOff = animFrame % 8;
        for (int i = 0; i < 4; i++) {
            int ax = 62 + i * 6 + arrowOff;
            if (ax < 80) {
                renderer.dot(ax, pipeY + 1, GOLD);
                renderer.dot(ax + 1, pipeY + 2, GOLD);
            }
        }

        // Conveyor belt
        int convX = 85;
        int convW = pw * 40 / 100;  // 40% of width
        renderer.drawConveyor(convX, lineY, convW, animFrame);
        drawBufferOnCanvas(convX, lineY);

        // Pipes (conveyor → robots)
        int pipeX2 = convX + convW + 2;
        renderer.drawPipeH(pipeX2, pipeY, 22);
        for (int i = 0; i < 4; i++) {
            int ax = pipeX2 + 4 + i * 5 + (animFrame % 6);
            if (ax < pipeX2 + 20) {
                renderer.dot(ax, pipeY + 1, GOLD);
                renderer.dot(ax + 1, pipeY + 2, GOLD);
            }
        }

        // === ROBOT STATION (right side) ===
        int rsX = pipeX2 + 24;
        int rsW = pw - rsX - 6;
        int rsY = wallH + 6;
        int rsH = workEnd - wallH - 12;
        // Dark station background
        renderer.fill(rsX, rsY, rsW, rsH, web("#2a2420"));
        renderer.fill(rsX, rsY, rsW, 2, DK_GRAY);
        renderer.fill(rsX, rsY, 1, rsH, DK_GRAY);
        renderer.fill(rsX + rsW - 1, rsY, 1, rsH, DK_GRAY);
        renderer.fill(rsX, rsY + rsH - 1, rsW, 1, DK_GRAY);

        Color[][] robotColors = {
                {TEAL, DK_TEAL, LT_TEAL},
                {GREEN, DK_GREEN, LT_GREEN},
                {ORANGE, DK_ORANGE, GOLD}
        };
        String[] robotLabels = {"PAR", "IMP", "PRI"};
        int robotSpacing = rsH / 3;
        for (int i = 0; i < 3; i++) {
            int rx = rsX + 8, ry = rsY + 6 + i * robotSpacing;
            // Robot glow
            renderer.drawLightGlow(rx + 5, ry + 7, 14, robotColors[i][0], 0.06f);
            renderer.drawRobot(rx, ry, robotStates[i], robotColors[i][0], robotColors[i][1], robotColors[i][2]);

            // Label with dark badge
            String label = robotLabels[i];
            int badgeW = label.length() * 6 + 4;
            renderer.fill(rx + 14, ry + 1, badgeW, 8, new Color(0, 0, 0, 0.7f));
            renderer.drawText(label, rx + 16.0, ry + 2.0, robotColors[i][2], 5);

            // State bar
            Color stateColor = switch (robotStates[i]) {
                case 1 -> LT_GREEN;
                case 2 -> BRIGHT_RED;
                case 3 -> GOLD;
                default -> DK_GRAY;
            };
            renderer.fill(rx + 14, ry + 10, 12, 2, stateColor);
        }

        // === FLOOR DECORATIONS ===
        renderer.drawCrate(8, stoneStart + 6);
        renderer.drawCrate(20, stoneStart + 10);
        renderer.drawBarrel(pw - 22, stoneStart + 6);
        renderer.drawBarrel(pw - 34, stoneStart + 9);
        renderer.drawCrate(pw - 28, stoneStart + 4);

        // Smoke
        renderer.drawSmoke(pw / 3, wallH - 4, animFrame);
        renderer.drawSmoke(pw * 2 / 3, wallH - 5, animFrame + 4);
        renderer.drawSmoke(pw / 2, wallH - 6, animFrame + 8);

        // === ATMOSPHERIC EFFECTS ===
        renderer.drawAmbientOcclusion(wallH + 2, pw, 8, 0.18f);
        renderer.drawFogBand(wallH - 3, 10, pw, new Color(0.6f, 0.5f, 0.35f, 1f), 0.05f);
        renderer.drawDustParticles(pw, ph, animFrame, 30);
    }

    private void drawBufferOnCanvas(int convX, int lineY) {
        if (controller.getBuffer() == null) return;
        List<Integer> snapshot = controller.getBuffer().getSnapshot();

        for (int i = 0; i < BUFFER_SIZE && i < snapshot.size(); i++) {
            int num = snapshot.get(i);
            TipoNumero tipo = NumberClassifier.clasificar(num);
            Color color = switch (tipo) {
                case PAR -> COLOR_PAR;
                case IMPAR -> COLOR_IMPAR;
                case PRIMO -> COLOR_PRIMO;
            };
            int col = i % 6;
            int row = i / 6;
            int bx = convX + 6 + col * 18;
            int by = lineY - 8 + row * 10;
            renderer.drawOreBlock(bx, by, color);
            renderer.drawText(String.valueOf(num), bx + 1.0, by - 1.0, WHITE, 5);
        }
    }

    private void cleanup() {
        if (controller != null) controller.detener();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        cleanup();
        if (renderer != null) renderer.dispose();
        if (postFx != null) postFx.dispose();
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
    }
}
