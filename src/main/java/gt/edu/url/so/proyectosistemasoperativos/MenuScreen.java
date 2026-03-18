package gt.edu.url.so.proyectosistemasoperativos;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gt.edu.url.so.proyectosistemasoperativos.common.AudioManager;
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

    // Hover state for glow effect
    private boolean pcHovered = false;
    private boolean dpHovered = false;

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
        postFx.setDofStrength(0.35f);   // stronger DoF in menu — cinematic
        postFx.setDofRadius(2.5f);
        postFx.setBloomIntensity(0.50f); // stronger bloom for visible glow
        postFx.setBloomThreshold(0.32f); // catch more light sources
        postFx.setBloomRadius(2.5f);
        postFx.setBloomPasses(3);
        postFx.setWarmth(0.85f); // cooler blue/green tone for tech theme
        postFx.setContrast(1.25f);
        postFx.setSaturation(1.30f);
        postFx.setVignetteRadius(0.62f); // strong vignette for cinematic
        postFx.setVignetteSoftness(0.42f);

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
        tp.color = new Color(0.4f, 0.85f, 1.0f, 1f);
        tp.borderWidth = 3;
        tp.borderColor = new Color(0.0f, 0.05f, 0.15f, 1f);
        tp.shadowOffsetX = 2;
        tp.shadowOffsetY = 2;
        tp.shadowColor = new Color(0, 0, 0, 0.6f);
        titleFont = gen.generateFont(tp);

        // Button font - clear with border
        FreeTypeFontGenerator.FreeTypeFontParameter bp = new FreeTypeFontGenerator.FreeTypeFontParameter();
        bp.size = 18;
        bp.color = Color.WHITE;
        bp.borderWidth = 2;
        bp.borderColor = new Color(0.0f, 0.05f, 0.15f, 1f);
        buttonFont = gen.generateFont(bp);

        // Small font with border for readability
        FreeTypeFontGenerator.FreeTypeFontParameter sp = new FreeTypeFontGenerator.FreeTypeFontParameter();
        sp.size = 13;
        sp.color = new Color(0.7f, 0.9f, 1f, 1f);
        sp.borderWidth = 1.5f;
        sp.borderColor = new Color(0.0f, 0.05f, 0.15f, 1f);
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
        pm.setColor(new Color(0.05f, 0.05f, 0.12f, 0.85f));
        pm.fill();
        skin.add("dark-bg", new Texture(pm));
        pm.dispose();

        // Pixel Art Button Skins (NinePatch)
        Color bdDark = new Color(0.05f, 0.08f, 0.15f, 1f); // Dark border
        
        NinePatchDrawable upSkin = createPixelButtonSkin(bdDark, 
                new Color(0.15f, 0.20f, 0.35f, 0.95f), // Base blue
                new Color(0.25f, 0.35f, 0.55f, 1f),    // High
                new Color(0.10f, 0.15f, 0.25f, 1f));   // Shadow

        NinePatchDrawable overSkin = createPixelButtonSkin(bdDark, 
                new Color(0.25f, 0.35f, 0.55f, 1f),    // Lighter blue base
                new Color(0.40f, 0.55f, 0.75f, 1f),    // High
                new Color(0.15f, 0.20f, 0.35f, 1f));   // Shadow

        NinePatchDrawable downSkin = createPixelButtonSkin(bdDark, 
                new Color(0.10f, 0.15f, 0.25f, 1f),    // Dark pressed base
                new Color(0.08f, 0.10f, 0.20f, 1f),    // High
                new Color(0.05f, 0.10f, 0.15f, 1f));   // Shadow

        // TextButton style
        TextButton.TextButtonStyle tbs = new TextButton.TextButtonStyle();
        tbs.font = buttonFont;
        tbs.fontColor = new Color(0.8f, 0.95f, 1f, 1f);
        tbs.overFontColor = new Color(0.4f, 1f, 0.8f, 1f);
        tbs.downFontColor = Color.WHITE;
        tbs.up = upSkin;
        tbs.over = overSkin;
        tbs.down = downSkin;
        skin.add("default", tbs);

        // Label styles
        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, new Color(0.4f, 0.85f, 1.0f, 1f));
        skin.add("title", titleStyle);
        Label.LabelStyle smallStyle = new Label.LabelStyle(smallFont, new Color(0.7f, 0.9f, 1.0f, 1f));
        skin.add("small", smallStyle);

        // Stage
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Audio
        AudioManager.getInstance().playMusic("menu_music.mp3", true);

        // ── Title panel (top center) ──
        Table titlePanel = new Table();
        titlePanel.setBackground(new TextureRegionDrawable(new TextureRegion(skin.get("dark-bg", Texture.class))));
        titlePanel.pad(25, 45, 20, 45);

        Label title = new Label("PROYECTO SO", skin, "title");
        Label subtitle = new Label("SIMULADOR DE CONCURRENCIA", skin, "small");
        Label credits = new Label("Universidad Rafael Landivar", skin, "small");
        Label credits2 = new Label("Sistemas Operativos", skin, "small");

        titlePanel.add(title).padBottom(10).row();
        titlePanel.add(subtitle).padBottom(6).row();
        titlePanel.add(credits).padBottom(2).row();
        titlePanel.add(credits2).row();

        // ── Buttons with hover tracking ──
        TextButton pcBtn = new TextButton("  PRODUCTOR - CONSUMIDOR  ", skin);
        pcBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                game.showProducerConsumer();
            }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                pcHovered = true;
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                pcHovered = false;
            }
        });

        TextButton dpBtn = new TextButton("  FILOSOFOS COMENSALES  ", skin);
        dpBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                game.showDiningPhilosophers();
            }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                dpHovered = true;
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                dpHovered = false;
            }
        });

        // ── Root layout ──
        Table root = new Table();
        root.setFillParent(true);

        // Title at top. expandY().top() pushes everything else down to the bottom
        root.add(titlePanel).colspan(2).expandY().top().padTop(25).row();

        // Buttons side by side at the very bottom, precisely aligned under scenes
        // Buttons now auto-size to their text (no width() constraint) and are pushed closer to center
        root.add(pcBtn).height(55).expandX().left().padLeft(160).padBottom(45);
        
        // DP button aligned right with padding to center under the round table
        root.add(dpBtn).height(55).expandX().right().padRight(160).padBottom(45);

        stage.addActor(root);
    }

    /** Generates a nice pixel art ninepatch skin programmatically */
    private NinePatchDrawable createPixelButtonSkin(Color bd, Color bg, Color hl, Color sh) {
        int w = 24, h = 24, s = 2; // Block scale
        int tw = w * s, th = h * s;
        Pixmap pm = new Pixmap(tw, th, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        
        // Background
        pm.setColor(bg);
        pm.fill();
        
        // Outer border
        pm.setColor(bd);
        pm.fillRectangle(0, 0, tw, s);            // top
        pm.fillRectangle(0, th - s, tw, s);       // bottom
        pm.fillRectangle(0, 0, s, th);            // left
        pm.fillRectangle(tw - s, 0, s, th);       // right
        
        // Corner cutouts for that octagonal pixel art look
        pm.setColor(Color.CLEAR);
        pm.fillRectangle(0, 0, s, s);
        pm.fillRectangle(tw - s, 0, s, s);
        pm.fillRectangle(0, th - s, s, s);
        pm.fillRectangle(tw - s, th - s, s, s);
        
        // Re-draw border immediately adjacent to cutouts
        pm.setColor(bd);
        pm.fillRectangle(s, s, s, s); // top-left inner
        pm.fillRectangle(tw - s*2, s, s, s); // top-right inner
        pm.fillRectangle(s, th - s*2, s, s); // bot-left inner
        pm.fillRectangle(tw - s*2, th - s*2, s, s); // bot-right inner
        
        // Inner Highlight
        pm.setColor(hl);
        pm.fillRectangle(s*2, s, tw - s*4, s); // top highlight
        pm.fillRectangle(s, s*2, s, th - s*4); // left highlight
        
        // Inner Shadow
        pm.setColor(sh);
        pm.fillRectangle(s*2, th - s*2, tw - s*4, s); // bottom shadow
        pm.fillRectangle(tw - s*2, s*2, s, th - s*4); // right shadow
        
        Texture tex = new Texture(pm);
        pm.dispose();
        
        NinePatch patch = new NinePatch(tex, s*3, s*3, s*3, s*3);
        return new NinePatchDrawable(patch);
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
        drawMenuBackground(renderer, animFrame, pcHovered, dpHovered);
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

    private void drawMenuBackground(PixelArtRenderer c, int frame, boolean pcHover, boolean dpHover) {
        int pw = c.pxW();  // 366
        int ph = c.pxH();  // 250

        // Dark atmospheric background
        c.clear(web("#0a0a0e"));

        // Tech wall (top ~55%)
        int wallEnd = ph * 55 / 100;
        c.drawTechWall(0, 0, pw, wallEnd);

        // Server floor (bottom ~45%)
        c.drawServerFloor(0, wallEnd, pw, ph - wallEnd);
        c.fill(0, wallEnd - 1, pw, 2, BLACK); // separator divider

        // Wall decorations - Monitors & Wires
        c.drawMonitor(20, wallEnd - 40, frame);
        c.drawMonitor(pw - 30, wallEnd - 40, frame + 15);
        
        // Horizontal glowing strip in wall (TechWall has it, but let's add some visual markers)
        c.drawGauge(pw / 2 - 20, wallEnd - 20, (frame % 20) / 20.0);
        c.drawGauge(pw / 2 + 10, wallEnd - 20, 0.85);

        // Server racks standing on floor (background decor)
        c.drawServerRack(12, wallEnd - 15, frame, GREEN);
        c.drawServerRack(pw - 28, wallEnd - 15, frame + 10, TEAL);
        c.drawServerRack(pw / 4, wallEnd - 5, frame + 5, ORANGE);
        c.drawServerRack(pw * 3 / 4, wallEnd - 5, frame + 8, LT_TEAL);

        // ==========================================
        // LEFT SIDE: Producer - Consumer Concept
        // ==========================================
        int pcX = 30;
        int pcY = wallEnd + 25;
        
        // El "Buffer" es la cinta transportadora
        c.drawConveyor(pcX + 25, pcY + 5, 60, frame * 2);
        
        // Productor (Robot Teal)
        c.drawRobot(pcX, pcY, (frame % 8 < 4) ? 0 : 1, TEAL, DK_TEAL, LT_TEAL);
        
        // Consumidor (Robot Orange)
        c.drawRobot(pcX + 90, pcY, (frame % 8 < 4) ? 1 : 0, ORANGE, DK_ORANGE, GOLD);
        
        // "Productos" (Bloques de datos) moviendose en el buffer
        int prodOffset = (frame * 2) % 30;
        c.drawOreBlock(pcX + 30 + prodOffset, pcY - 2, LT_GREEN);
        c.drawOreBlock(pcX + 50 + prodOffset, pcY - 2, LT_GREEN);

        // ==========================================
        // RIGHT SIDE: Dining Philosophers Concept
        // ==========================================
        int dpX = pw - 120;
        int dpY = wallEnd + 15;
        
        // La mesa compartida
        c.drawRoundTable(dpX + 45, dpY + 15, 25);
        
        // Filósofo 1: Pensando (Azul)
        c.drawPhilosopher(dpX + 20, dpY - 5, 0, web("#4060c0"), web("#304890"), DARK_BROWN, frame % 4 < 2 ? 0 : -1);
        
        // Filósofo 2: Comiendo (Rojo)
        c.drawPhilosopher(dpX + 58, dpY - 5, 1, web("#c04040"), web("#903030"), web("#202020"), 0);
        
        // Filósofo 3: Esperando (Verde)
        c.drawPhilosopher(dpX + 40, dpY + 10, 2, web("#40a060"), web("#308040"), GOLD, 0);

        // Platos y Recursos (Tenedores/Palillos)
        c.drawPlate(dpX + 30, dpY + 8, true);  // Plato del pensador (lleno pero no come)
        c.drawPlate(dpX + 50, dpY + 8, false); // Plato del que está comiendo (vacío/comiendo)
        c.drawPlate(dpX + 40, dpY + 18, true); // Plato del que espera

        // === LIGHT EFFECTS - the heart of HD-2D ===
        Color neonBlue = new Color(0.2f, 0.6f, 1.0f, 1f);
        Color neonGreen = new Color(0.2f, 1.0f, 0.4f, 1f);

        // Server rack glow pools 
        c.drawLightGlow(20, wallEnd, 40, neonGreen, 0.15f);
        c.drawLightGlow(pw - 20, wallEnd, 40, neonBlue, 0.15f);
        c.drawLightGlow(pw / 4 + 8, wallEnd + 5, 40, new Color(1f, 0.6f, 0.2f, 1f), 0.15f);
        c.drawLightGlow(pw * 3 / 4 + 8, wallEnd + 5, 40, neonBlue, 0.15f);

        // Screen monitors glow
        c.drawLightRays(25, wallEnd - 30, 30, 10, frame, neonGreen);
        c.drawLightRays(pw - 25, wallEnd - 30, 30, 10, frame + 5, neonGreen);

        // Ambient occlusion
        c.drawAmbientOcclusion(wallEnd, pw, 7, 0.2f);

        // Cool fog
        c.drawFogBand(wallEnd - 5, 15, pw, new Color(0.2f, 0.4f, 0.8f, 1f), 0.08f);

        // Digital particles
        c.drawDustParticles(pw, ph, frame, 30);

        // === HOVER SPOTLIGHT EFFECT ===
        // When the user hovers a button, a spotlight cone beams down from the top
        if (pcHover) {
            // Spotlight cone: starts narrow at top (y=0), widens to reach PC scene
            int spotCenterX = pcX + 50;
            c.drawSpotlight(spotCenterX, 6, 100, 0, pcY + 20, new Color(0.3f, 1.0f, 0.9f, 1f), 0.22f);
            // Bright source dot at the very top
            c.drawLightGlow(spotCenterX, 2, 12, new Color(0.6f, 1f, 0.95f, 1f), 0.6f);
            // Floor pool of light where cone lands
            c.drawLightGlow(spotCenterX, pcY + 15, 50, new Color(0.3f, 1.0f, 0.9f, 1f), 0.2f);
        }
        if (dpHover) {
            // Spotlight cone for Dining Philosophers scene
            int spotCenterX = dpX + 45;
            c.drawSpotlight(spotCenterX, 6, 90, 0, dpY + 25, new Color(1.0f, 0.85f, 0.4f, 1f), 0.22f);
            // Bright source dot at the very top
            c.drawLightGlow(spotCenterX, 2, 12, new Color(1f, 0.9f, 0.5f, 1f), 0.6f);
            // Floor pool of light where cone lands
            c.drawLightGlow(spotCenterX, dpY + 20, 50, new Color(1.0f, 0.85f, 0.4f, 1f), 0.2f);
        }

        // === DYNAMIC POINT LIGHTS (box2dLight) ===
        Color neonBlueDyn = new Color(0.1f, 0.4f, 0.9f, 1f);
        Color neonGreenDyn = new Color(0.1f, 0.9f, 0.3f, 1f);

        // Server rack lights
        c.addFlickeringLight(20, wallEnd, 50, neonGreenDyn, 0.6f, frame);
        c.addFlickeringLight(pw - 20, wallEnd, 50, neonBlueDyn, 0.6f, frame + 10);
        
        // Central console light (cool blue)
        c.addPointLight(pw / 2f, ph / 2f, 90, new Color(0.3f, 0.6f, 1.0f, 1f), 0.4f);

        // Hover dynamic lights — spotlight source at top + pool at scene
        if (pcHover) {
            c.addPointLight(pcX + 50, 5, 60, new Color(0.3f, 1.0f, 0.9f, 1f), 0.8f);
            c.addPointLight(pcX + 50, pcY + 10, 80, new Color(0.2f, 1.0f, 0.8f, 1f), 0.5f);
        }
        if (dpHover) {
            c.addPointLight(dpX + 45, 5, 60, new Color(1.0f, 0.9f, 0.5f, 1f), 0.8f);
            c.addPointLight(dpX + 45, dpY + 15, 80, new Color(1.0f, 0.8f, 0.3f, 1f), 0.5f);
        }
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
