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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gt.edu.url.so.proyectosistemasoperativos.common.PixelArtRenderer;
import gt.edu.url.so.proyectosistemasoperativos.common.PostProcessingPipeline;

import static gt.edu.url.so.proyectosistemasoperativos.common.PixelArtRenderer.*;

public class MenuScreen extends ScreenAdapter {

    private final SOGame game;
    private PixelArtRenderer renderer;
    private PostProcessingPipeline postFx;
    private Stage stage;

    private int animFrame = 0;
    private float frameTimer = 0;

    // Fonts & skin
    private BitmapFont titleFont;
    private BitmapFont buttonFont;
    private BitmapFont smallFont;
    private Skin skin;

    private static final int CW = 1100, CH = 750;

    public MenuScreen(SOGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        renderer = new PixelArtRenderer(CW, CH, 3);
        postFx = new PostProcessingPipeline(CW, CH);

        // HD-2D: cinematic menu with bloom on lights
        postFx.setFocusCenter(0.45f);
        postFx.setFocusRange(0.35f);
        postFx.setDofStrength(0.30f);   // more DoF in menu is fine - cinematic
        postFx.setDofRadius(2.5f);
        postFx.setBloomIntensity(0.35f);
        postFx.setBloomThreshold(0.42f);
        postFx.setBloomRadius(2.0f);
        postFx.setWarmth(1.25f);
        postFx.setContrast(1.12f);
        postFx.setSaturation(1.20f);
        postFx.setVignetteRadius(0.65f); // strong vignette for cinematic
        postFx.setVignetteSoftness(0.40f);

        // Generate fonts - much larger for readability
        FreeTypeFontGenerator gen = null;
        try {
            gen = new FreeTypeFontGenerator(Gdx.files.classpath(
                    "gt/edu/url/so/proyectosistemasoperativos/common/fonts/PressStart2P-Regular.ttf"));
        } catch (Exception e) {
            gen = new FreeTypeFontGenerator(Gdx.files.internal(
                    "gt/edu/url/so/proyectosistemasoperativos/common/fonts/PressStart2P-Regular.ttf"));
        }

        // Title font - large with thick border
        FreeTypeFontGenerator.FreeTypeFontParameter tp = new FreeTypeFontGenerator.FreeTypeFontParameter();
        tp.size = 40;
        tp.color = new Color(1f, 0.85f, 0.5f, 1f);
        tp.borderWidth = 3;
        tp.borderColor = new Color(0.2f, 0.1f, 0.0f, 1f);
        tp.shadowOffsetX = 2;
        tp.shadowOffsetY = 2;
        tp.shadowColor = new Color(0, 0, 0, 0.6f);
        titleFont = gen.generateFont(tp);

        // Button font - clear with border
        FreeTypeFontGenerator.FreeTypeFontParameter bp = new FreeTypeFontGenerator.FreeTypeFontParameter();
        bp.size = 18;
        bp.color = Color.WHITE;
        bp.borderWidth = 2;
        bp.borderColor = new Color(0.15f, 0.08f, 0.0f, 1f);
        buttonFont = gen.generateFont(bp);

        // Small font with border for readability
        FreeTypeFontGenerator.FreeTypeFontParameter sp = new FreeTypeFontGenerator.FreeTypeFontParameter();
        sp.size = 13;
        sp.color = new Color(1f, 0.9f, 0.7f, 1f);
        sp.borderWidth = 1.5f;
        sp.borderColor = new Color(0.15f, 0.08f, 0.0f, 1f);
        smallFont = gen.generateFont(sp);

        gen.dispose();

        // Build skin programmatically
        skin = new Skin();
        skin.add("default-font", buttonFont);
        skin.add("title-font", titleFont);
        skin.add("small-font", smallFont);

        // Pixmap textures for backgrounds
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);

        // Dark backdrop for the menu panel
        pm.setColor(new Color(0.08f, 0.05f, 0.02f, 0.75f));
        pm.fill();
        skin.add("dark-bg", new Texture(pm));

        // Button backgrounds - darker and more opaque
        pm.setColor(new Color(0.35f, 0.18f, 0.08f, 0.95f));
        pm.fill();
        skin.add("brown", new Texture(pm));
        pm.setColor(new Color(0.50f, 0.28f, 0.10f, 1f));
        pm.fill();
        skin.add("brown-over", new Texture(pm));
        pm.setColor(new Color(0.65f, 0.35f, 0.12f, 1f));
        pm.fill();
        skin.add("brown-down", new Texture(pm));
        pm.dispose();

        // TextButton style
        TextButton.TextButtonStyle tbs = new TextButton.TextButtonStyle();
        tbs.font = buttonFont;
        tbs.fontColor = new Color(1f, 0.95f, 0.8f, 1f);
        tbs.overFontColor = new Color(1f, 0.8f, 0.3f, 1f);
        tbs.downFontColor = Color.WHITE;
        tbs.up = new TextureRegionDrawable(new TextureRegion(skin.get("brown", Texture.class)));
        tbs.over = new TextureRegionDrawable(new TextureRegion(skin.get("brown-over", Texture.class)));
        tbs.down = new TextureRegionDrawable(new TextureRegion(skin.get("brown-down", Texture.class)));
        skin.add("default", tbs);

        // Label styles
        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, new Color(1f, 0.85f, 0.5f, 1f));
        skin.add("title", titleStyle);
        Label.LabelStyle smallStyle = new Label.LabelStyle(smallFont, new Color(1f, 0.9f, 0.7f, 1f));
        skin.add("small", smallStyle);

        // Stage
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Layout - centered panel with dark backdrop
        Table root = new Table();
        root.setFillParent(true);
        root.center();

        // Inner panel with dark background
        Table panel = new Table();
        panel.setBackground(new TextureRegionDrawable(new TextureRegion(skin.get("dark-bg", Texture.class))));
        panel.pad(40, 60, 40, 60);

        Label title = new Label("PROYECTO SO", skin, "title");
        Label subtitle = new Label("SIMULADOR DE CONCURRENCIA", skin, "small");
        Label credits = new Label("Universidad Rafael Landivar", skin, "small");
        Label credits2 = new Label("Sistemas Operativos", skin, "small");

        TextButton pcBtn = new TextButton("  PRODUCTOR - CONSUMIDOR  ", skin);
        pcBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.showProducerConsumer();
            }
        });

        TextButton dpBtn = new TextButton("  FILOSOFOS COMENSALES  ", skin);
        dpBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.showDiningPhilosophers();
            }
        });

        panel.add(title).padBottom(15).row();
        panel.add(subtitle).padBottom(10).row();
        panel.add(credits).padBottom(3).row();
        panel.add(credits2).padBottom(35).row();
        panel.add(pcBtn).width(480).height(60).padBottom(18).row();
        panel.add(dpBtn).width(480).height(60).padBottom(10).row();

        root.add(panel);
        stage.addActor(root);
    }

    @Override
    public void render(float delta) {
        frameTimer += delta;
        if (frameTimer >= 0.1f) {
            frameTimer -= 0.1f;
            animFrame++;
        }

        // Render pixel art scene
        renderer.beginFrame();
        drawMenuBackground(renderer, animFrame);
        renderer.endFrame();

        // Post-process and composite to screen
        postFx.render(renderer.getFrameBufferTexture());

        // Restore GL state for Scene2D overlay
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

        // Draw UI on top
        stage.act(delta);
        stage.draw();
    }

    private void drawMenuBackground(PixelArtRenderer c, int frame) {
        int pw = c.pxW();  // 366
        int ph = c.pxH();  // 250

        // Dark atmospheric background
        c.clear(web("#14100c"));

        // Brick wall (top ~35%)
        int wallEnd = ph * 35 / 100;
        c.drawBrickWall(0, 0, pw, wallEnd);
        c.fill(0, 0, pw, wallEnd, new Color(0, 0, 0, 0.35f)); // darken
        c.fill(0, wallEnd, pw, 2, BLACK);

        // Wood floor (middle ~35%)
        int floorH = ph * 35 / 100;
        c.drawWoodFloor(0, wallEnd + 2, pw, floorH);
        c.fill(0, wallEnd + 2, pw, floorH, new Color(0, 0, 0, 0.35f));

        // Stone floor (bottom ~30%)
        int stoneStart = wallEnd + 2 + floorH;
        c.drawStoneFloor(0, stoneStart, pw, ph - stoneStart);
        c.fill(0, stoneStart, pw, ph - stoneStart, new Color(0, 0, 0, 0.45f));
        c.fill(0, stoneStart - 1, pw, 2, BLACK);

        // Wall decorations - torches as primary light
        for (int tx = 20; tx < pw; tx += 60) {
            c.drawTorch(tx, wallEnd - 22, frame);
        }
        c.drawWindow(55, wallEnd - 36);
        c.drawWindow(pw - 67, wallEnd - 36);
        c.drawPainting(pw / 3, wallEnd - 32, BROWN, web("#4080a0"), web("#60a060"));
        c.drawPainting(pw * 2 / 3, wallEnd - 30, WOOD_DK, ORANGE, GOLD);
        c.drawShelf(pw / 4, wallEnd - 24);
        c.drawShelf(pw * 3 / 4, wallEnd - 24);
        c.drawPipeH(0, wallEnd - 10, pw);
        c.drawGauge(pw / 2 - 22, wallEnd - 24, (frame % 20) / 20.0);
        c.drawGauge(pw / 2 + 16, wallEnd - 24, 0.65);

        // Decorative objects
        c.drawBarrel(12, stoneStart + 6);
        c.drawBarrel(22, stoneStart + 10);
        c.drawCrate(pw - 20, stoneStart + 5);
        c.drawCrate(pw - 30, stoneStart + 9);
        c.drawBarrel(pw / 3, stoneStart + 7);
        c.drawCrate(pw * 2 / 3, stoneStart + 6);

        // Animated characters
        int minerX = (frame * 2) % (pw + 20) - 10;
        c.drawMiner(minerX, wallEnd + 14, (frame % 4 < 2) ? 1 : 2);
        c.drawRobot(pw / 4, wallEnd + 26, (frame % 8 < 4) ? 0 : 1, TEAL, DK_TEAL, LT_TEAL);
        c.drawRobot(pw / 2, wallEnd + 30, 0, GREEN, DK_GREEN, LT_GREEN);
        c.drawRobot(pw * 3 / 4, wallEnd + 24, (frame % 6 < 3) ? 1 : 0, ORANGE, DK_ORANGE, GOLD);

        int philY = stoneStart - 22;
        c.drawPhilosopher(pw / 5, philY, (frame % 12 < 4) ? 0 : (frame % 12 < 8 ? 1 : 2),
                web("#4060c0"), web("#304890"), DARK_BROWN, frame % 4 < 2 ? 0 : -1);
        c.drawPhilosopher(pw * 4 / 5, philY, (frame % 10 < 5) ? 1 : 0,
                web("#c04040"), web("#903030"), web("#202020"), 0);

        // Smoke
        c.drawSmoke(pw / 3, wallEnd - 4, frame);
        c.drawSmoke(pw * 2 / 3, wallEnd - 5, frame + 3);

        // === LIGHT EFFECTS - the heart of HD-2D ===
        Color torchLight = new Color(1f, 0.75f, 0.35f, 1f);
        Color moonLight = new Color(0.6f, 0.7f, 1f, 1f);

        // Torch glow pools - these illuminate the dark scene
        for (int tx = 20; tx < pw; tx += 60) {
            c.drawLightGlow(tx, wallEnd - 16, 30, torchLight, 0.12f);
        }

        // Moonlight from windows
        c.drawLightRays(61, wallEnd - 30, 45, 10, frame, moonLight);
        c.drawLightRays(pw - 61, wallEnd - 30, 45, 10, frame, moonLight);

        // Ambient occlusion
        c.drawAmbientOcclusion(wallEnd + 2, pw, 7, 0.15f);
        c.drawAmbientOcclusion(stoneStart, pw, 5, 0.12f);

        // Warm fog
        c.drawFogBand(wallEnd - 3, 10, pw, new Color(0.8f, 0.65f, 0.4f, 1f), 0.06f);

        // Dust particles
        c.drawDustParticles(pw, ph, frame, 40);
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
        if (renderer != null) renderer.dispose();
        if (postFx != null) postFx.dispose();
        if (stage != null) stage.dispose();
        // Fonts are disposed by skin.dispose() since they were added via skin.add()
        if (skin != null) skin.dispose();
    }
}
