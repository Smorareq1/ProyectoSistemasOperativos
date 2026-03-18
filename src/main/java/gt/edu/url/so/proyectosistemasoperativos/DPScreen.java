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
import gt.edu.url.so.proyectosistemasoperativos.philosophers.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static gt.edu.url.so.proyectosistemasoperativos.common.PixelArtRenderer.*;

public class DPScreen extends ScreenAdapter {

    private static final Color[] ROBE_COLORS = {
            web("#4060c0"), web("#c04040"), web("#40a040"),
            web("#c08040"), web("#8040a0"), web("#40a0a0"),
            web("#a06040"), web("#6080c0"), web("#c06080"),
            web("#80a040")
    };
    private static final Color[] ROBE_DARK = {
            web("#304890"), web("#903030"), web("#307830"),
            web("#906030"), web("#603078"), web("#307878"),
            web("#784830"), web("#486090"), web("#904860"),
            web("#607830")
    };
    private static final Color[] HAIR_COLORS = {
            DARK_BROWN, web("#202020"), web("#c89060"),
            web("#909090"), web("#402820"), DARK_BROWN,
            web("#202020"), web("#c89060"), web("#402820"),
            web("#909090")
    };

    private final SOGame game;
    private PixelArtRenderer renderer;
    private PostProcessingPipeline postFx;
    private Stage stage;

    private int animFrame = 0;
    private float frameTimer = 0;

    // Simulation
    private PhilosopherConfig config;
    private DPController controller;
    private Philosopher[] philosophers;
    private boolean running = false;
    private boolean paused = false;

    // UI
    private BitmapFont uiFont;
    private BitmapFont smallFont;
    private BitmapFont logFont;
    private BitmapFont sidebarFont;
    private Skin skin;
    private Label[] stateLabels;
    private Label[] forkLabels;
    private Table stateContainer;
    private Label nLabel;
    private final List<String> logMessages = new ArrayList<>();
    private Label logLabel;
    private ScrollPane logScrollPane;
    private Table leftSidebar;
    private Table rightSidebar;
    private Table bottomBar;
    private static final int MAX_LOG = 100;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Scale 3 = 366x250 pixel art space (more detail than scale 4)
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

    public DPScreen(SOGame game) {
        this.game = game;
        this.config = new PhilosopherConfig();
    }

    @Override
    public void show() {
        renderer = new PixelArtRenderer(CW, CH, SCALE);
        postFx = new PostProcessingPipeline(CW, CH);

        // HD-2D: dark scene + bloom on lights + gentle edge DoF
        postFx.setFocusCenter(0.45f);
        postFx.setFocusRange(0.40f);   // wide sharp center
        postFx.setDofStrength(0.25f);   // gentle tilt-shift at edges only
        postFx.setDofRadius(2.0f);
        postFx.setBloomIntensity(0.50f); // strong bloom for torch/fire glow
        postFx.setBloomThreshold(0.30f); // lower threshold for more light pickup
        postFx.setBloomRadius(2.5f);
        postFx.setBloomPasses(3);        // wider, softer glow
        postFx.setWarmth(1.30f);        // warm candlelit feel
        postFx.setContrast(1.15f);      // deeper shadows
        postFx.setSaturation(1.22f);    // rich colors
        postFx.setVignetteRadius(0.68f); // noticeable vignette for mood
        postFx.setVignetteSoftness(0.48f);

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

        // Sidebar font (pixel-art style, larger to fill boxes)
        p.size = 15;
        p.color = new Color(0.9f, 0.88f, 0.78f, 1f);
        p.borderWidth = 1f;
        p.borderColor = new Color(0.08f, 0.04f, 0.0f, 1f);
        sidebarFont = gen.generateFont(p);

        // Log font (pixel-art style, slightly smaller for dense text)
        p.size = 12;
        p.color = new Color(0.85f, 0.82f, 0.72f, 1f);
        p.borderWidth = 0.8f;
        p.borderColor = new Color(0.06f, 0.03f, 0.0f, 1f);
        logFont = gen.generateFont(p);

        gen.dispose();

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

    private void buildSkin() {
        skin = new Skin();
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);

        // Button backgrounds
        pm.setColor(new Color(0.18f, 0.12f, 0.06f, 0.85f));
        pm.fill();
        Texture brownTex = new Texture(pm);
        pm.setColor(new Color(0.30f, 0.18f, 0.08f, 0.90f));
        pm.fill();
        Texture overTex = new Texture(pm);
        pm.setColor(new Color(0.45f, 0.28f, 0.12f, 0.95f));
        pm.fill();
        Texture downTex = new Texture(pm);

        // Panel background (dark, semi-transparent)
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

        // Sidebar label style
        Label.LabelStyle sidebarStyle = new Label.LabelStyle(sidebarFont, new Color(0.9f, 0.88f, 0.78f, 1f));
        skin.add("sidebar", sidebarStyle);

        // Log font: pixel-art PressStart2P, compact
        Label.LabelStyle logStyle = new Label.LabelStyle(logFont, new Color(0.85f, 0.82f, 0.72f, 1f));
        skin.add("log", logStyle);

        ScrollPane.ScrollPaneStyle sps = new ScrollPane.ScrollPaneStyle();
        sps.background = new TextureRegionDrawable(new TextureRegion(darkTex));
        skin.add("default", sps);

        // Lighter panel for sidebar
        Pixmap pm2 = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm2.setColor(new Color(0.10f, 0.08f, 0.04f, 0.88f));
        pm2.fill();
        Texture sidebarTex = new Texture(pm2);
        pm2.dispose();

        skin.add("panel-bg", new TextureRegionDrawable(new TextureRegion(darkTex)), TextureRegionDrawable.class);
        skin.add("sidebar-bg", new TextureRegionDrawable(new TextureRegion(sidebarTex)), TextureRegionDrawable.class);
    }

    private Label speedLabel;
    private static final float[] SPEED_MULTIPLIERS = {0.25f, 0.5f, 1f, 2f, 4f};
    private static final String[] SPEED_LABELS = {"x0.25", "x0.5", "x1", "x2", "x4"};
    private int currentSpeedIdx = 2;

    private void buildUI() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        // (Textures for button backgrounds are created inside buildSkin)

        // === Bottom control bar (buttons + title) ===
        TextButton backBtn = new TextButton("  MENU  ", skin);
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                cleanup();
                game.showMenu();
            }
        });

        Label title = new Label("  CENA DE FILOSOFOS  ", skin, "ui");

        TextButton playBtn = new TextButton("  PLAY  ", skin);
        TextButton pauseBtn = new TextButton("  PAUSA  ", skin);
        TextButton stopBtn = new TextButton("  STOP  ", skin);

        // Speed controls
        TextButton speedDown = new TextButton(" - ", skin);
        TextButton speedUp = new TextButton(" + ", skin);
        speedLabel = new Label("x1", skin);

        playBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                if (!running) iniciarSimulacion();
                else if (paused) reanudarSimulacion();
            }
        });
        pauseBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                if (running && !paused) pausarSimulacion();
            }
        });
        stopBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                detenerSimulacion();
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
                currentSpeedIdx = Math.min(SPEED_MULTIPLIERS.length - 1, currentSpeedIdx + 1);
                applySpeed();
            }
        });

        bottomBar = new Table();
        // No background — pixel-art frame drawn by PixelArtRenderer
        bottomBar.center();
        bottomBar.add(backBtn).pad(4).padLeft(8);
        bottomBar.add(title).expandX().center().pad(4);
        bottomBar.add(speedDown).pad(3);
        bottomBar.add(speedLabel).pad(3);
        bottomBar.add(speedUp).pad(3);
        bottomBar.add(playBtn).pad(4);
        bottomBar.add(pauseBtn).pad(4);
        bottomBar.add(stopBtn).pad(4).padRight(8);

        // Empty root (just an anchor for the stage)
        Table root = new Table();
        root.setFillParent(true);

        // === LEFT SIDEBAR ===
        leftSidebar = new Table();
        // Background removed; rendering by PixelArtRenderer
        leftSidebar.top().pad(12);

        // --- 1. N philosopher selector ---
        Table nRow = new Table();
        Label nTitle = new Label("FILOSOFOS", skin, "sidebar");
        nTitle.setColor(GOLD);
        TextButton nDown = new TextButton(" - ", skin);
        TextButton nUp = new TextButton(" + ", skin);
        nLabel = new Label(String.valueOf(config.getNumFilosofos()), skin, "sidebar");
        nLabel.setColor(Color.WHITE);

        nDown.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                if (!running) {
                    config.setNumFilosofos(config.getNumFilosofos() - 1);
                    nLabel.setText(String.valueOf(config.getNumFilosofos()));
                    rebuildSidebarStates();
                }
            }
        });
        nUp.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                if (!running) {
                    config.setNumFilosofos(config.getNumFilosofos() + 1);
                    nLabel.setText(String.valueOf(config.getNumFilosofos()));
                    rebuildSidebarStates();
                }
            }
        });

        Table nControls = new Table();
        nControls.add(nDown).size(28, 24).padRight(6);
        nControls.add(nLabel).center().padLeft(4).padRight(10);
        nControls.add(nUp).size(28, 24);
        
        nRow.add(nTitle).center().padBottom(4).row();
        nRow.add(nControls).center();
        leftSidebar.add(nRow).fillX().padBottom(4).row();

        addSeparator(leftSidebar);

        // --- 2. State labels container ---
        stateContainer = new Table();
        stateContainer.top().center();
        rebuildSidebarStates();

        ScrollPane stateScroll = new ScrollPane(stateContainer);
        stateScroll.setFadeScrollBars(true);
        stateScroll.setScrollingDisabled(true, false);
        leftSidebar.add(stateScroll).expand().fill().padTop(0).row();

        // === RIGHT SIDEBAR (LOG) ===
        rightSidebar = new Table();
        // Background removed; rendering by PixelArtRenderer
        rightSidebar.top().pad(18);

        Label logTitle = new Label("LOG", skin, "sidebar");
        logTitle.setColor(GOLD);
        rightSidebar.add(logTitle).expandX().center().padBottom(6).row();

        logLabel = new Label("", skin, "log");
        logLabel.setWrap(true);
        logScrollPane = new ScrollPane(logLabel, skin);
        logScrollPane.setFadeScrollBars(false);
        logScrollPane.setScrollingDisabled(true, false);
        rightSidebar.add(logScrollPane).expand().fill().row();

        stage.addActor(root);
        stage.addActor(leftSidebar);
        stage.addActor(rightSidebar);
        stage.addActor(bottomBar);

        // Position sidebars and bottom bar to match pixel-art UI boxes
        repositionUI(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    /**
     * Dynamically position the left and right sidebars to match the pixel-art UI boxes.
     * Pixel-art box coords (in pixel-art space, Y-down):
     *   Left:  x=10, y=84, w=100, h=92
     *   Right: x=pw-100-10=256, y=84, w=100, h=92
     * Converting to screen coords: multiply by SCALE, flip Y.
     */
    // Pixel-art sidebar constants (in pixel-art coords)
    private static final int UI_BOX_W = 70;
    private static final int UI_BOX_H = 92;
    private static final int UI_BOX_Y = 84;
    private static final int UI_BOX_MARGIN = 4;
    // Bottom bar constants (in pixel-art coords)
    private static final int BAR_W = 220;
    private static final int BAR_H = 22;
    private static final int BAR_X = (366 - 220) / 2; // Centered
    private static final int BAR_Y = 250 - 22 - 2; // bottom margin of 2px

    private void repositionUI(int screenW, int screenH) {
        float canvasW = CW;  // 1100
        float canvasH = CH;  // 750
        float sx = screenW / canvasW;
        float sy = screenH / canvasH;

        int pw = CW / SCALE;  // 366

        // Left sidebar
        float leftX = UI_BOX_MARGIN * SCALE * sx;
        float leftW = UI_BOX_W * SCALE * sx;
        float leftH = UI_BOX_H * SCALE * sy;
        float leftY = screenH - (UI_BOX_Y + UI_BOX_H) * SCALE * sy;
        leftSidebar.setPosition(leftX, leftY);
        leftSidebar.setSize(leftW, leftH);

        // Right sidebar
        float rightX = (pw - UI_BOX_W - UI_BOX_MARGIN) * SCALE * sx;
        float rightW = UI_BOX_W * SCALE * sx;
        float rightH = UI_BOX_H * SCALE * sy;
        float rightY = screenH - (UI_BOX_Y + UI_BOX_H) * SCALE * sy;
        rightSidebar.setPosition(rightX, rightY);
        rightSidebar.setSize(rightW, rightH);

        // Bottom bar
        float barX = BAR_X * SCALE * sx;
        float barW = BAR_W * SCALE * sx;
        float barH = BAR_H * SCALE * sy;
        float barY = screenH - (BAR_Y + BAR_H) * SCALE * sy;
        bottomBar.setPosition(barX, barY);
        bottomBar.setSize(barW, barH);
    }

    private void addSeparator(Table parent) {
        Pixmap linePm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        linePm.setColor(new Color(0.35f, 0.28f, 0.15f, 0.6f));
        linePm.fill();
        Texture lineTex = new Texture(linePm);
        linePm.dispose();
        Table sep = new Table();
        sep.setBackground(new TextureRegionDrawable(new TextureRegion(lineTex)));
        parent.add(sep).fillX().height(1).padTop(4).padBottom(4).row();
    }

    private void rebuildSidebarStates() {
        int N = config.getNumFilosofos();
        stateLabels = new Label[N];
        forkLabels = new Label[N];

        if (stateContainer != null) {
            stateContainer.clear();

            // Philosopher header
            Label phTitle = new Label("Filosofo", skin, "sidebar");
            phTitle.setColor(new Color(0.7f, 0.65f, 0.55f, 1f));
            
            // Fork header
            Label fkTitle = new Label("Tenedor", skin, "sidebar");
            fkTitle.setColor(new Color(0.7f, 0.65f, 0.55f, 1f));
            
            // Layout in 2 columns, centered
            stateContainer.add(phTitle).center().padRight(6).padBottom(4);
            stateContainer.add(fkTitle).center().padBottom(4).row();
            
            for (int i = 0; i < N; i++) {
                stateLabels[i] = new Label("F" + i + ": PENSANDO", skin, "sidebar");
                stateLabels[i].setColor(TEAL);
                forkLabels[i] = new Label("T" + i + ": libre", skin, "sidebar");
                forkLabels[i].setColor(LT_GREEN);
                
                stateContainer.add(stateLabels[i]).center().padRight(6).padBottom(2);
                stateContainer.add(forkLabels[i]).center().padBottom(2).row();
            }
        }
    }

    private void applySpeed() {
        speedLabel.setText(SPEED_LABELS[currentSpeedIdx]);
        float mult = SPEED_MULTIPLIERS[currentSpeedIdx];
        // Adjust philosopher config timings based on speed multiplier
        int baseMinPensar = 500, baseMaxPensar = 3000;
        int baseMinComer = 500, baseMaxComer = 2000;
        config.setTiempoMinPensar((int)(baseMinPensar / mult));
        config.setTiempoMaxPensar((int)(baseMaxPensar / mult));
        config.setTiempoMinComer((int)(baseMinComer / mult));
        config.setTiempoMaxComer((int)(baseMaxComer / mult));
    }

    private void rebuildStateLabels() {
        rebuildSidebarStates();
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

        if (running && controller != null) {
            updateStateFromController();
        }

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

        // Dark background — everything is a dimly lit tavern at night
        Color bgDark = web("#1a1018");
        renderer.clear(bgDark);

        // === LAYOUT ===
        int wallH = ph * 28 / 100;       // ~70 px — taller wall for more decoration space
        int floorEnd = ph * 85 / 100;    // ~212 px wood floor ends
        int stoneH = ph - floorEnd;       // ~38 px stone cellar floor

        // === DARK BRICK WALL (top) ===
        renderer.drawBrickWall(0, 0, pw, wallH);
        // Darken the wall with a semi-transparent overlay for moody atmosphere
        renderer.fill(0, 0, pw, wallH, new Color(0, 0, 0, 0.35f));
        renderer.fill(0, wallH, pw, 2, BLACK);

        // === DARK WOOD FLOOR ===
        renderer.drawWoodFloor(0, wallH + 2, pw, floorEnd - wallH - 2);
        // Darken the floor significantly
        renderer.fill(0, wallH + 2, pw, floorEnd - wallH - 2, new Color(0, 0, 0, 0.40f));

        // === DARK STONE CELLAR FLOOR ===
        renderer.drawStoneFloor(0, floorEnd, pw, stoneH);
        renderer.fill(0, floorEnd, pw, stoneH, new Color(0, 0, 0, 0.50f));
        renderer.fill(0, floorEnd - 1, pw, 2, BLACK);

        // === WALL DECORATIONS (drawn on top of darkened wall) ===
        // Torches — these are the primary light sources
        renderer.drawTorch(20, wallH - 30, animFrame);
        renderer.drawTorch(pw / 2 - 50, wallH - 28, animFrame);
        renderer.drawTorch(pw / 2 + 45, wallH - 28, animFrame);
        renderer.drawTorch(pw - 25, wallH - 30, animFrame);

        // Windows with moonlight
        renderer.drawWindow(60, wallH - 36);
        renderer.drawWindow(pw - 72, wallH - 36);

        // Decorations
        renderer.drawPainting(pw / 4 + 10, wallH - 32, BROWN, web("#4080a0"), web("#60a060"));
        renderer.drawPainting(pw * 3 / 4 - 10, wallH - 30, WOOD_DK, ORANGE, GOLD);
        renderer.drawShelf(pw / 3, wallH - 24);
        renderer.drawShelf(pw * 2 / 3, wallH - 24);

        // === TAVERNA SIGN — large, centered, well-positioned ===
        int signW = 110;
        int signX = pw / 2 - signW / 2;
        renderer.fill(signX, 4, signW, 18, WOOD2);
        renderer.fill(signX + 1, 5, signW - 2, 16, WOOD1);
        renderer.fill(signX + 2, 6, signW - 4, 14, DK_RED);
        renderer.fill(signX + 2, 6, signW - 4, 4, RED);
        renderer.drawText("TAVERNA", signX + 26.0, 4.5, GOLD, 8);

        // === FIREPLACE — central warm light source ===
        renderer.drawFireplace(pw / 2 - 10, wallH - 18, animFrame);

        // === LIGHT EFFECTS — BEFORE characters so they appear lit ===
        Color torchLight = new Color(1f, 0.75f, 0.35f, 1f);
        Color fireLight = new Color(1f, 0.55f, 0.15f, 1f);
        Color moonLight = new Color(0.6f, 0.7f, 1f, 1f);

        // Torch glow pools (large, warm) — these illuminate the scene
        renderer.drawLightGlow(20, wallH - 24, 35, torchLight, 0.12f);
        renderer.drawLightGlow(pw / 2 - 50, wallH - 22, 30, torchLight, 0.10f);
        renderer.drawLightGlow(pw / 2 + 45, wallH - 22, 30, torchLight, 0.10f);
        renderer.drawLightGlow(pw - 25, wallH - 24, 35, torchLight, 0.12f);

        // Fireplace — big warm glow
        renderer.drawLightGlow(pw / 2, wallH - 10, 50, fireLight, 0.15f);
        // Firelight on the floor area
        renderer.drawLightGlow(pw / 2, wallH + 20, 60, fireLight, 0.08f);

        // Moonlight from windows
        renderer.drawLightRays(66, wallH - 30, 50, 10, animFrame, moonLight);
        renderer.drawLightRays(pw - 66, wallH - 30, 50, 10, animFrame, moonLight);

        // === TABLE (centered in lit floor area) ===
        int cx = pw / 2;
        int cy = wallH + (floorEnd - wallH) * 42 / 100;
        int tableR = 34;  // bigger table for scale 3
        // Light pool on the table from above
        renderer.drawLightGlow(cx, cy, tableR + 15, torchLight, 0.06f);
        renderer.drawRoundTable(cx, cy, tableR);

        // === PIXEL ART UI SIDEBARS ===
        renderer.drawUIBox(UI_BOX_MARGIN, UI_BOX_Y, UI_BOX_W, UI_BOX_H);
        renderer.drawUIBox(pw - UI_BOX_W - UI_BOX_MARGIN, UI_BOX_Y, UI_BOX_W, UI_BOX_H);

        // === PIXEL ART BOTTOM BAR ===
        renderer.drawUIBox(BAR_X, BAR_Y, BAR_W, BAR_H);

        // === PHILOSOPHERS ===
        int N = config.getNumFilosofos();
        double philR = tableR + 38;  // more space from table

        for (int i = 0; i < N; i++) {
            double angle = 2 * Math.PI * i / N - Math.PI / 2;
            int px = cx + (int) (philR * Math.cos(angle)) - 7;
            int py = cy + (int) (philR * Math.sin(angle)) - 9;

            int philFrame = 0;
            if (running && controller != null) {
                int[] snapshot = controller.getSnapshot();
                if (snapshot != null && i < N) {
                    EstadoFilosofo estado = EstadoFilosofo.values()[snapshot[i]];
                    philFrame = switch (estado) {
                        case PENSANDO -> 0;
                        case ESPERANDO -> 2;
                        case COMIENDO -> 1;
                    };
                }
            }

            int bobY = (philFrame == 2) ? (animFrame % 4 < 2 ? 0 : -1) : 0;

            // Drop shadow
            renderer.fill(px - 1, py + 17, 16, 4, new Color(0, 0, 0, 0.4f));

            // Small light pool around each philosopher
            renderer.drawLightGlow(px + 7, py + 8, 16, torchLight, 0.04f);

            renderer.drawPhilosopher(px, py, philFrame,
                    ROBE_COLORS[i % ROBE_COLORS.length],
                    ROBE_DARK[i % ROBE_DARK.length],
                    HAIR_COLORS[i % HAIR_COLORS.length],
                    bobY);

            // State indicator with high contrast
            Color stateColor = switch (philFrame) {
                case 0 -> TEAL;
                case 1 -> LT_GREEN;
                case 2 -> GOLD;
                default -> WHITE;
            };
            String stateText = switch (philFrame) {
                case 1 -> "COME";
                case 2 -> "ESPERA";
                default -> "F" + i;
            };
            // Dark badge with colored text
            int badgeW = stateText.length() * 5 + 4;
            renderer.fill(px - 1, py + 21, badgeW, 8, new Color(0, 0, 0, 0.75f));
            renderer.fill(px - 1, py + 21, badgeW, 1, stateColor);
            renderer.drawText(stateText, (px + 1.0), (py + 22.0), stateColor, 4);

            // Plates
            double plateR = tableR + 16;
            int platX = cx + (int) (plateR * Math.cos(angle)) - 3;
            int platY = cy + (int) (plateR * Math.sin(angle)) - 1;
            renderer.drawPlate(platX, platY, philFrame == 1);

            // Forks
            double forkAngle = 2 * Math.PI * (i + 0.5) / N - Math.PI / 2;
            double forkR = tableR + 10;
            int fkx = cx + (int) (forkR * Math.cos(forkAngle));
            int fky = cy + (int) (forkR * Math.sin(forkAngle));

            boolean forkFree = true;
            if (running && controller != null) {
                int[] snapshot = controller.getSnapshot();
                if (snapshot != null) {
                    int forkHolder = snapshot[N + i];
                    forkFree = (forkHolder < 0);
                }
            }
            renderer.drawForkItem(fkx, fky, forkFree);
            if (!forkFree) {
                renderer.fill(fkx - 1, fky - 1, 3, 3, BRIGHT_RED);
            }
        }

        // === FLOOR DECORATIONS ===
        renderer.drawBarrel(12, floorEnd + 6);
        renderer.drawBarrel(24, floorEnd + 10);
        renderer.drawCrate(pw - 22, floorEnd + 6);
        renderer.drawCrate(pw - 34, floorEnd + 10);
        renderer.drawBarrel(pw / 3, floorEnd + 7);
        renderer.drawCrate(pw * 2 / 3, floorEnd + 6);

        // === ATMOSPHERIC EFFECTS ===
        renderer.drawAmbientOcclusion(wallH + 2, pw, 8, 0.15f);
        renderer.drawFogBand(wallH - 3, 12, pw, new Color(0.8f, 0.65f, 0.4f, 1f), 0.06f);
        renderer.drawDustParticles(pw, ph, animFrame, 35);

        // === DYNAMIC POINT LIGHTS (box2dLight) ===
        Color torchLightDyn = new Color(1f, 0.7f, 0.3f, 1f);
        Color fireLightDyn = new Color(1f, 0.45f, 0.12f, 1f);
        Color moonLightDyn = new Color(0.45f, 0.55f, 0.9f, 1f);

        // Torch lights (flickering)
        renderer.addFlickeringLight(21, wallH - 24, 42, torchLightDyn, 0.65f, animFrame);
        renderer.addFlickeringLight(pw / 2f - 49, wallH - 22, 35, torchLightDyn, 0.50f, animFrame);
        renderer.addFlickeringLight(pw / 2f + 46, wallH - 22, 35, torchLightDyn, 0.50f, animFrame);
        renderer.addFlickeringLight(pw - 24, wallH - 24, 42, torchLightDyn, 0.65f, animFrame);

        // Fireplace — large warm glow
        renderer.addFlickeringLight(pw / 2f, wallH - 8, 65, fireLightDyn, 0.85f, animFrame);
        // Firelight pooling on floor
        renderer.addPointLight(pw / 2f, wallH + 20, 55, fireLightDyn, 0.35f);

        // Moonlight from windows (cool blue, static)
        renderer.addPointLight(66, wallH - 30, 35, moonLightDyn, 0.3f);
        renderer.addPointLight(pw - 66, wallH - 30, 35, moonLightDyn, 0.3f);

        // Table center light
        renderer.addPointLight(cx, cy, tableR + 10, torchLightDyn, 0.2f);

        // Philosopher eating glow
        for (int i = 0; i < N; i++) {
            int philFrame = 0;
            if (running && controller != null) {
                int[] snap = controller.getSnapshot();
                if (snap != null && i < N) {
                    EstadoFilosofo est = EstadoFilosofo.values()[snap[i]];
                    philFrame = switch (est) {
                        case PENSANDO -> 0;
                        case ESPERANDO -> 2;
                        case COMIENDO -> 1;
                    };
                }
            }
            if (philFrame == 1) { // Eating — warm glow
                double angle = 2 * Math.PI * i / N - Math.PI / 2;
                float plx = cx + (float)(philR * Math.cos(angle));
                float ply = cy + (float)(philR * Math.sin(angle));
                renderer.addPointLight(plx, ply, 15, new Color(1f, 0.8f, 0.4f, 1f), 0.4f);
            }
        }
    }

    private void updateStateFromController() {
        if (controller == null) return;
        int N = config.getNumFilosofos();
        int[] snapshot = controller.getSnapshot();
        if (snapshot == null) return;

        Gdx.app.postRunnable(() -> {
            for (int i = 0; i < N && i < stateLabels.length; i++) {
                EstadoFilosofo estado = EstadoFilosofo.values()[snapshot[i]];
                int forkHolder = snapshot[N + i];

                switch (estado) {
                    case PENSANDO -> {
                        stateLabels[i].setText("F" + i + ": PENSANDO");
                        stateLabels[i].setColor(TEAL);
                    }
                    case ESPERANDO -> {
                        stateLabels[i].setText("F" + i + ": ESPERANDO");
                        stateLabels[i].setColor(GOLD);
                    }
                    case COMIENDO -> {
                        stateLabels[i].setText("F" + i + ": COMIENDO");
                        stateLabels[i].setColor(LT_GREEN);
                    }
                }

                if (forkHolder >= 0) {
                    forkLabels[i].setText("T" + i + ": F" + forkHolder);
                    forkLabels[i].setColor(BRIGHT_RED);
                } else {
                    forkLabels[i].setText("T" + i + ": libre");
                    forkLabels[i].setColor(LT_GREEN);
                }
            }
        });
    }

    private void iniciarSimulacion() {
        int N = config.getNumFilosofos();
        controller = new DPController(N);
        philosophers = new Philosopher[N];
        running = true;
        paused = false;

        for (int i = 0; i < N; i++) {
            philosophers[i] = new Philosopher(i, controller, logger, config);
            philosophers[i].start();
        }
        logger.log("Simulacion iniciada (N=" + N + ")");
    }

    private void pausarSimulacion() {
        paused = true;
        if (philosophers != null) for (Philosopher p : philosophers) p.pausar();
        logger.log("Simulacion pausada");
    }

    private void reanudarSimulacion() {
        paused = false;
        if (philosophers != null) for (Philosopher p : philosophers) p.reanudar();
        logger.log("Simulacion reanudada");
    }

    private void detenerSimulacion() {
        running = false;
        paused = false;
        if (philosophers != null) for (Philosopher p : philosophers) p.detener();
        logger.log("Simulacion detenida");
        rebuildStateLabels();
    }

    private void cleanup() {
        detenerSimulacion();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        repositionUI(width, height);
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
