package gt.edu.url.so.proyectosistemasoperativos.common;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LibGDX pixel art rendering engine — equivalent of PixelGameCanvas.
 * Renders to a FrameBuffer using ShapeRenderer (fill/dot) and SpriteBatch (text).
 * Each "pixel art pixel" = S x S screen pixels.
 */
public class PixelArtRenderer implements Disposable {

    private final int S; // scale factor
    private final int width, height; // total pixel dimensions of the framebuffer

    private final FrameBuffer frameBuffer;
    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch spriteBatch;
    private final OrthographicCamera camera;

    // Font management
    private FreeTypeFontGenerator fontGenerator;
    private final Map<Integer, BitmapFont> fontCache = new HashMap<>();
    private static final String FONT_PATH = "gt/edu/url/so/proyectosistemasoperativos/common/fonts/PressStart2P-Regular.ttf";

    // Buffered text entries (drawn after ShapeRenderer ends)
    private final List<TextEntry> textBuffer = new ArrayList<>();

    private static class TextEntry {
        final String text;
        final float x, y;
        final Color color;
        final int fontSize;

        TextEntry(String text, float x, float y, Color color, int fontSize) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = new Color(color);
            this.fontSize = fontSize;
        }
    }

    // ═══════════════════════════════════════
    //  COLOR PALETTE
    // ═══════════════════════════════════════
    public static final Color BLACK       = hexColor("#1a1018");
    public static final Color DARK_BROWN  = hexColor("#4a3020");
    public static final Color BROWN       = hexColor("#6b4c38");
    public static final Color MED_BROWN   = hexColor("#8b7355");
    public static final Color TAN         = hexColor("#c4a882");
    public static final Color CREAM       = hexColor("#f0dcc0");
    public static final Color BG_CREAM    = hexColor("#f5e6c8");
    public static final Color WHITE       = hexColor("#fff8e8");

    public static final Color ORANGE      = hexColor("#e8682a");
    public static final Color DK_ORANGE   = hexColor("#c45020");
    public static final Color GOLD        = hexColor("#f0c040");
    public static final Color DK_GOLD     = hexColor("#c89828");

    public static final Color RED         = hexColor("#c83830");
    public static final Color DK_RED      = hexColor("#8b2018");
    public static final Color BRIGHT_RED  = hexColor("#e84040");

    public static final Color GREEN       = hexColor("#68b030");
    public static final Color DK_GREEN    = hexColor("#4a8820");
    public static final Color LT_GREEN    = hexColor("#90d858");

    public static final Color TEAL        = hexColor("#3898b8");
    public static final Color DK_TEAL     = hexColor("#2870a0");
    public static final Color LT_TEAL     = hexColor("#60c0e0");

    public static final Color SKIN        = hexColor("#e8b888");
    public static final Color SKIN_DK     = hexColor("#c89868");
    public static final Color SKIN_LT     = hexColor("#f0d0a8");

    public static final Color GRAY        = hexColor("#808080");
    public static final Color DK_GRAY     = hexColor("#505050");
    public static final Color LT_GRAY     = hexColor("#a0a0a0");
    public static final Color VLT_GRAY    = hexColor("#c8c8c8");

    public static final Color PLUM        = hexColor("#6858a0");
    public static final Color DK_PLUM     = hexColor("#483870");
    public static final Color LT_PLUM     = hexColor("#9080c0");

    public static final Color BRICK1      = hexColor("#b85830");
    public static final Color BRICK2      = hexColor("#a04828");
    public static final Color BRICK_LT    = hexColor("#d07040");
    public static final Color MORTAR      = hexColor("#d0b898");

    public static final Color WOOD1       = hexColor("#b08050");
    public static final Color WOOD2       = hexColor("#986838");
    public static final Color WOOD_LT     = hexColor("#c89868");
    public static final Color WOOD_DK     = hexColor("#704828");

    public static final Color SKY_BLUE    = hexColor("#88c8e8");
    public static final Color SKY_LT      = hexColor("#a8d8f0");

    // ═══════════════════════════════════════
    //  CONSTRUCTOR
    // ═══════════════════════════════════════
    public PixelArtRenderer(int width, int height, int pixelScale) {
        this.S = pixelScale;
        this.width = width;
        this.height = height;

        frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
        shapeRenderer = new ShapeRenderer();
        spriteBatch = new SpriteBatch();

        camera = new OrthographicCamera();
        camera.setToOrtho(true, width, height); // Y-flipped so (0,0) is top-left
        camera.update();

        shapeRenderer.setProjectionMatrix(camera.combined);
        spriteBatch.setProjectionMatrix(camera.combined);

        // Load font generator
        try {
            fontGenerator = new FreeTypeFontGenerator(Gdx.files.classpath(FONT_PATH));
        } catch (Exception e) {
            // Fallback: try internal path
            try {
                fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal(FONT_PATH));
            } catch (Exception e2) {
                fontGenerator = null;
            }
        }
    }

    public PixelArtRenderer(int width, int height) {
        this(width, height, 3);
    }

    // ═══════════════════════════════════════
    //  FRAME LIFECYCLE
    // ═══════════════════════════════════════

    /** Begin rendering a frame. Binds the framebuffer and starts ShapeRenderer. */
    public void beginFrame() {
        textBuffer.clear();
        frameBuffer.begin();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
    }

    /** End rendering a frame. Flushes ShapeRenderer, draws buffered text, unbinds framebuffer. */
    public void endFrame() {
        shapeRenderer.end();

        // Draw all buffered text entries
        if (!textBuffer.isEmpty()) {
            spriteBatch.setProjectionMatrix(camera.combined);
            spriteBatch.begin();
            for (TextEntry entry : textBuffer) {
                BitmapFont font = getFont(entry.fontSize);
                font.setColor(entry.color);
                font.draw(spriteBatch, entry.text, entry.x, entry.y);
            }
            spriteBatch.end();
        }

        frameBuffer.end();
    }

    /** Returns the framebuffer texture for post-processing. */
    public com.badlogic.gdx.graphics.Texture getFrameBufferTexture() {
        return frameBuffer.getColorBufferTexture();
    }

    /** Returns the FrameBuffer itself. */
    public FrameBuffer getFrameBuffer() {
        return frameBuffer;
    }

    // ═══════════════════════════════════════
    //  CORE DRAWING
    // ═══════════════════════════════════════

    /** Fill a pixel-grid rectangle */
    public void fill(double px, double py, int pw, int ph, Color c) {
        shapeRenderer.setColor(c);
        shapeRenderer.rect((float)(px * S), (float)(py * S), (float)(pw * S), (float)(ph * S));
    }

    /** Fill a single pixel */
    public void dot(double px, double py, Color c) {
        fill(px, py, 1, 1, c);
    }

    /** Clear entire canvas */
    public void clear(Color bg) {
        Gdx.gl.glClearColor(bg.r, bg.g, bg.b, bg.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    public int pxW() { return width / S; }
    public int pxH() { return height / S; }
    public int scale() { return S; }

    // ═══════════════════════════════════════
    //  TEXT RENDERING
    // ═══════════════════════════════════════

    /** Draw pixel text — buffers for later rendering with SpriteBatch.
     *  fontSize is in pixel-art units and gets scaled by S for the framebuffer. */
    public void drawText(String text, double px, double py, Color color, double fontSize) {
        int size = Math.max(1, (int)(fontSize * S));
        float x = (float)(px * S);
        float y = (float)(py * S + fontSize * S);
        textBuffer.add(new TextEntry(text, x, y, color, size));
    }

    private BitmapFont getFont(int size) {
        BitmapFont font = fontCache.get(size);
        if (font == null) {
            if (fontGenerator != null) {
                FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
                param.size = size;
                param.mono = true;
                param.flip = true; // We use a Y-flipped camera
                font = fontGenerator.generateFont(param);
            } else {
                font = new BitmapFont(true); // default font, flipped
            }
            fontCache.put(size, font);
        }
        return font;
    }

    // ═══════════════════════════════════════
    //  COLOR UTILITY METHODS
    // ═══════════════════════════════════════

    /** Convert hex string "#RRGGBB" to LibGDX Color */
    private static Color hexColor(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        float r = Integer.parseInt(h.substring(0, 2), 16) / 255f;
        float g = Integer.parseInt(h.substring(2, 4), 16) / 255f;
        float b = Integer.parseInt(h.substring(4, 6), 16) / 255f;
        return new Color(r, g, b, 1f);
    }

    /** JavaFX Color.web("#RRGGBB") equivalent */
    public static Color web(String hex) {
        return hexColor(hex);
    }

    /** JavaFX Color.rgb(r, g, b) where r,g,b are 0-255 */
    public static Color rgb(int r, int g, int b) {
        return new Color(r / 255f, g / 255f, b / 255f, 1f);
    }

    /** JavaFX Color.rgb(r, g, b, a) where r,g,b are 0-255 and a is 0-1 */
    public static Color rgb(int r, int g, int b, double a) {
        return new Color(r / 255f, g / 255f, b / 255f, (float) a);
    }

    /** JavaFX Color.deriveColor(hShift, satFactor, brightFactor, opacityFactor) */
    public static Color deriveColor(Color c, double hShift, double satFactor, double brightFactor, double opacityFactor) {
        float[] hsv = new float[3];
        rgbToHsv(c.r, c.g, c.b, hsv);
        hsv[0] = (hsv[0] + (float) hShift) % 360f;
        if (hsv[0] < 0) hsv[0] += 360f;
        hsv[1] = clampF((float)(hsv[1] * satFactor));
        hsv[2] = clampF((float)(hsv[2] * brightFactor));
        float[] rgb = hsvToRgb(hsv[0], hsv[1], hsv[2]);
        return new Color(rgb[0], rgb[1], rgb[2], clampF((float)(c.a * opacityFactor)));
    }

    /** JavaFX Color.darker() — multiply r,g,b by 0.7 */
    public static Color darker(Color c) {
        return new Color(c.r * 0.7f, c.g * 0.7f, c.b * 0.7f, c.a);
    }

    /** JavaFX Color.brighter() — multiply r,g,b by 1/0.7, clamped to 1 */
    public static Color brighter(Color c) {
        float factor = 1f / 0.7f;
        float r = Math.min(c.r * factor, 1f);
        float g = Math.min(c.g * factor, 1f);
        float b = Math.min(c.b * factor, 1f);
        // If color is black (or near black), bump to a small value first
        if (c.r == 0 && c.g == 0 && c.b == 0) {
            return new Color(1f / 255f / 0.7f, 1f / 255f / 0.7f, 1f / 255f / 0.7f, c.a);
        }
        return new Color(r, g, b, c.a);
    }

    /** JavaFX Color.desaturate() — move r,g,b toward their average */
    public static Color desaturate(Color c) {
        float avg = (c.r + c.g + c.b) / 3f;
        float factor = 0.7f; // how much to keep of original
        float r = c.r * factor + avg * (1f - factor);
        float g = c.g * factor + avg * (1f - factor);
        float b = c.b * factor + avg * (1f - factor);
        return new Color(r, g, b, c.a);
    }

    /** JavaFX Color.interpolate(other, t) — linear interpolation */
    public static Color interpolate(Color a, Color b, double t) {
        float f = (float) t;
        return new Color(
            a.r + (b.r - a.r) * f,
            a.g + (b.g - a.g) * f,
            a.b + (b.b - a.b) * f,
            a.a + (b.a - a.a) * f
        );
    }

    private static float clampF(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private static void rgbToHsv(float r, float g, float b, float[] hsv) {
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        hsv[2] = max; // value
        hsv[1] = (max == 0) ? 0 : d / max; // saturation
        if (d == 0) {
            hsv[0] = 0;
        } else if (max == r) {
            hsv[0] = 60f * (((g - b) / d) % 6f);
        } else if (max == g) {
            hsv[0] = 60f * (((b - r) / d) + 2f);
        } else {
            hsv[0] = 60f * (((r - g) / d) + 4f);
        }
        if (hsv[0] < 0) hsv[0] += 360f;
    }

    private static float[] hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1f - Math.abs((h / 60f) % 2f - 1f));
        float m = v - c;
        float r, g, b;
        if (h < 60) { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }
        return new float[]{r + m, g + m, b + m};
    }

    // ═══════════════════════════════════════
    //  BACKGROUND TILES
    // ═══════════════════════════════════════

    /** Brick wall pattern — HD-2D with mortar depth, wear, and ambient gradient */
    public void drawBrickWall(int sx, int sy, int w, int h) {
        // Mortar base with vertical ambient gradient (lighter at top = torch light)
        for (int y = sy; y < sy + h; y++) {
            double t = (double)(y - sy) / Math.max(h, 1);
            int r = (int)(0xd0 - t * 0x28), g = (int)(0xb8 - t * 0x30), b = (int)(0x98 - t * 0x28);
            fill(sx, y, w, 1, rgb(clamp(r), clamp(g), clamp(b)));
        }
        // Dark grout shadow lines
        int bW = 7, bH = 3;
        int row = 0;
        for (int y = sy; y < sy + h; y += bH + 1) {
            fill(sx, y + bH, w, 1, web("#6a5038"));
            int off = (row % 2 == 0) ? 0 : 4;
            for (int x = sx - bW + off; x < sx + w; x += bW + 1) {
                int dx = Math.max(x, sx);
                int dw = Math.min(x + bW, sx + w) - dx;
                int dh = Math.min(bH, sy + h - y);
                if (dw > 0 && dh > 0) {
                    // Varied brick colors via pseudo-random hash
                    int hash = ((row * 7 + x * 13) & 0xFF);
                    Color bc;
                    if (hash < 70) bc = BRICK2;
                    else if (hash < 190) bc = BRICK1;
                    else bc = BRICK_LT;
                    fill(dx, y, dw, dh, bc);
                    // Top edge highlight
                    fill(dx, y, dw, 1, deriveColor(bc, 0, 0.9, 1.25, 1.0));
                    // Bottom edge shadow
                    fill(dx, y + dh - 1, dw, 1, darker(bc));
                    // Left edge micro-highlight
                    dot(dx, y, deriveColor(bc, 0, 0.8, 1.35, 1.0));
                    // Right edge shadow
                    if (dw > 1) dot(dx + dw - 1, y + dh - 1, darker(darker(bc)));
                    // Occasional wear/damage
                    if (hash > 225 && dw > 3) {
                        dot(dx + 2, y + 1, desaturate(darker(bc)));
                        dot(dx + 1, y + 1, darker(bc));
                    }
                    // Occasional mortar stain
                    if (hash > 200 && hash <= 225 && dw > 4) {
                        dot(dx + 3, y + 1, darker(MORTAR));
                    }
                }
            }
            row++;
        }
    }

    /** Stone floor tiles — HD-2D with specular highlights, grout shadows, and wear */
    public void drawStoneFloor(int sx, int sy, int w, int h) {
        Color[] sc = {TAN, MED_BROWN, web("#b09870"), web("#a08060"),
                       web("#c0a880"), web("#908068")};
        int tW = 6, tH = 5;
        for (int y = sy; y < sy + h; y += tH) {
            int off = ((y - sy) / tH % 2) * 3;
            for (int x = sx + off; x < sx + w; x += tW) {
                int idx = Math.abs((x * 3 + y * 7)) % sc.length;
                int dw = Math.min(tW - 1, sx + w - x);
                int dh = Math.min(tH - 1, sy + h - y);
                if (dw > 0 && dh > 0) {
                    Color base = sc[idx];
                    fill(x, y, dw, dh, base);
                    // Top-left specular highlight
                    dot(x, y, deriveColor(base, 0, 0.8, 1.3, 1.0));
                    if (dw > 1) dot(x + 1, y, deriveColor(base, 0, 0.9, 1.15, 1.0));
                    // Bottom-right shadow
                    if (dh > 1) fill(x, y + dh - 1, dw, 1, darker(base));
                    if (dw > 1) dot(x + dw - 1, y + dh - 1, darker(darker(base)));
                    // Occasional surface crack
                    int hash = Math.abs(x * 11 + y * 17) & 0xFF;
                    if (hash > 230 && dw > 2 && dh > 2) {
                        dot(x + 1, y + 1, darker(base));
                        dot(x + 2, y + 2, darker(base));
                    }
                }
            }
        }
        // Dark grout lines between rows
        for (int y = sy; y < sy + h; y += tH) {
            fill(sx, y, w, 1, web("#5a4030"));
        }
        // Vertical grout accents
        for (int y = sy; y < sy + h; y += tH) {
            int off = ((y - sy) / tH % 2) * 3;
            for (int x = sx + off; x < sx + w; x += tW) {
                if (x > sx) dot(x - 1, y + 1, web("#6a5040"));
            }
        }
    }

    /** Wood plank floor — HD-2D with grain, knots, and plank shadow gaps */
    public void drawWoodFloor(int sx, int sy, int w, int h) {
        int pH = 5;
        Color[] planks = {WOOD1, WOOD2, web("#a07848"), web("#c09060")};
        for (int y = sy; y < sy + h; y += pH) {
            int plankIdx = ((y - sy) / pH) % planks.length;
            Color pc = planks[plankIdx];
            int dh = Math.min(pH, sy + h - y);
            fill(sx, y, w, dh, pc);
            // Top edge highlight (light catching the plank edge)
            fill(sx, y, w, 1, deriveColor(pc, 0, 0.85, 1.2, 1.0));
            // Bottom gap shadow between planks
            fill(sx, y + dh - 1, w, 1, darker(darker(pc)));
            // Wood grain streaks (lighter lines along the plank)
            for (int gx = sx + (plankIdx * 3); gx < sx + w; gx += 8) {
                int gw = Math.min(3, sx + w - gx);
                fill(gx, y + 2, gw, 1, deriveColor(pc, 0, 0.7, 1.15, 1.0));
            }
            // Knot marks
            int knotOff = 10 + ((y / pH) % 3) * 7;
            for (int x = sx + knotOff; x < sx + w; x += 22) {
                dot(x, y + 2, DARK_BROWN);
                dot(x + 1, y + 2, darker(pc));
                dot(x, y + 3, darker(pc));
            }
            // Nail heads on plank ends
            for (int x = sx + 2; x < sx + w; x += 30) {
                dot(x, y + 1, DK_GRAY);
            }
        }
    }

    /** Animated conveyor belt — HD-2D with metallic rollers, rivets, and shine */
    public void drawConveyor(int sx, int sy, int w, int frame) {
        // Side frame plates (metallic)
        fill(sx - 2, sy - 1, 2, 10, DK_GRAY);
        fill(sx + w, sy - 1, 2, 10, DK_GRAY);
        dot(sx - 2, sy - 1, LT_GRAY);
        dot(sx + w, sy - 1, LT_GRAY);
        // Top rail with metallic sheen
        fill(sx, sy, w, 1, web("#606060"));
        fill(sx, sy, w, 1, DK_GRAY);
        // Bottom rail
        fill(sx, sy + 7, w, 1, web("#505050"));
        // Belt surface with depth
        fill(sx, sy + 1, w, 6, web("#454545"));
        fill(sx, sy + 2, w, 4, GRAY);
        fill(sx, sy + 2, w, 1, LT_GRAY); // top shine on belt surface
        // Animated stripes (belt motion)
        int off = frame % 4;
        for (int x = sx + off; x < sx + w; x += 4) {
            int dw = Math.min(2, sx + w - x);
            fill(x, sy + 2, dw, 4, LT_GRAY);
            fill(x, sy + 2, dw, 1, VLT_GRAY); // stripe highlight
        }
        // Rivets along top and bottom rails
        for (int x = sx + 3; x < sx + w - 1; x += 8) {
            dot(x, sy, LT_GRAY);
            dot(x, sy + 7, LT_GRAY);
        }
        // Roller drums at ends
        fill(sx, sy + 1, 2, 6, web("#707070"));
        fill(sx, sy + 2, 2, 1, VLT_GRAY); // roller shine
        fill(sx + w - 2, sy + 1, 2, 6, web("#707070"));
        fill(sx + w - 2, sy + 2, 2, 1, VLT_GRAY);
        // Support legs with reinforced base
        for (int x = sx + 6; x < sx + w - 2; x += 18) {
            fill(x, sy + 8, 2, 4, DK_GRAY);
            fill(x, sy + 8, 2, 1, LT_GRAY); // top shine
            fill(x - 1, sy + 11, 4, 1, web("#404040"));
            fill(x - 1, sy + 12, 4, 1, web("#353535"));
            // Bolts on legs
            dot(x, sy + 9, LT_GRAY);
        }
    }

    // ═══════════════════════════════════════
    //  CHARACTER SPRITES
    // ═══════════════════════════════════════

    /**
     * Draw a miner character (16w x 24h pixels) — HD-2D detailed style.
     * frame: 0=idle, 1=mine-up, 2=mine-down, 3=blocked, 4=done
     */
    public void drawMiner(int x, int y, int frame) {
        Color HAT_HI = web("#ffe878");
        Color HAT    = GOLD;
        Color HAT_MD = DK_GOLD;
        Color HAT_DK = web("#a07818");
        Color LAMP   = web("#e0f0ff");

        // ── Hard Hat ──
        fill(x + 4, y, 8, 1, HAT_HI);
        fill(x + 3, y + 1, 10, 1, HAT);
        fill(x + 3, y + 2, 10, 1, HAT_MD);
        fill(x + 2, y + 3, 12, 1, HAT_DK);
        // Lamp on hat
        fill(x + 7, y - 1, 2, 1, LT_GRAY);
        dot(x + 7, y - 2, LAMP);
        dot(x + 8, y - 2, LAMP);
        // Hat highlight
        dot(x + 5, y, web("#fff8c0"));
        dot(x + 6, y, web("#fff8c0"));

        // ── Face ──
        fill(x + 4, y + 4, 8, 5, SKIN);
        fill(x + 4, y + 4, 8, 1, SKIN_LT);
        fill(x + 4, y + 8, 8, 1, SKIN_DK);
        // Eyebrows
        fill(x + 5, y + 4, 2, 1, DARK_BROWN);
        fill(x + 9, y + 4, 2, 1, DARK_BROWN);
        // Eyes
        dot(x + 5, y + 5, BLACK);
        dot(x + 6, y + 5, web("#302018"));
        dot(x + 9, y + 5, web("#302018"));
        dot(x + 10, y + 5, BLACK);
        // Eye whites
        dot(x + 6, y + 5, WHITE);
        dot(x + 9, y + 5, WHITE);
        // Nose
        dot(x + 7, y + 6, SKIN_DK);
        dot(x + 8, y + 6, SKIN_DK);
        // Ears
        dot(x + 3, y + 5, SKIN_DK);
        dot(x + 12, y + 5, SKIN_DK);

        if (frame == 3) {
            fill(x + 6, y + 8, 4, 1, RED);
            dot(x + 7, y + 7, SKIN_DK);
        } else if (frame == 4) {
            fill(x + 6, y + 8, 4, 1, SKIN_DK);
            dot(x + 7, y + 8, SKIN);
            dot(x + 8, y + 8, SKIN);
        } else {
            fill(x + 7, y + 8, 2, 1, SKIN_DK);
        }

        // ── Body (overalls) ──
        fill(x + 3, y + 9, 10, 6, ORANGE);
        fill(x + 3, y + 9, 1, 6, DK_ORANGE);
        fill(x + 12, y + 9, 1, 6, DK_ORANGE);
        fill(x + 3, y + 9, 10, 1, web("#f08038"));
        // Suspender straps
        fill(x + 5, y + 9, 1, 3, WHITE);
        fill(x + 10, y + 9, 1, 3, WHITE);
        // Belt
        fill(x + 3, y + 14, 10, 1, BROWN);
        fill(x + 7, y + 14, 2, 1, GOLD);
        // Pocket detail
        fill(x + 4, y + 12, 3, 2, DK_ORANGE);
        fill(x + 9, y + 12, 3, 2, DK_ORANGE);

        // ── Arms ──
        if (frame == 1) {
            fill(x + 13, y + 8, 2, 4, SKIN);
            dot(x + 13, y + 8, SKIN_LT);
            fill(x + 14, y + 2, 1, 7, BROWN);
            fill(x + 13, y + 1, 3, 2, GRAY);
            fill(x + 15, y + 3, 2, 1, GRAY);
            dot(x + 16, y + 2, LT_GRAY);
            fill(x + 1, y + 9, 2, 4, SKIN);
        } else if (frame == 2) {
            fill(x + 13, y + 11, 2, 4, SKIN);
            fill(x + 14, y + 11, 1, 7, BROWN);
            fill(x + 13, y + 17, 3, 2, GRAY);
            dot(x + 15, y + 16, GRAY);
            fill(x + 1, y + 9, 2, 4, SKIN);
        } else if (frame == 3) {
            fill(x + 1, y + 7, 2, 5, SKIN);
            fill(x + 13, y + 7, 2, 5, SKIN);
        } else {
            fill(x + 1, y + 9, 2, 4, SKIN);
            dot(x + 1, y + 12, SKIN_DK);
            fill(x + 13, y + 9, 2, 4, SKIN);
            dot(x + 14, y + 12, SKIN_DK);
            if (frame == 0) {
                fill(x + 14, y + 4, 1, 10, BROWN);
                fill(x + 13, y + 3, 3, 2, GRAY);
                dot(x + 15, y + 5, GRAY);
                dot(x + 13, y + 3, LT_GRAY);
            }
        }

        // ── Legs ──
        int legA = (frame == 1) ? 1 : 0;
        fill(x + 4, y + 15, 4, 4 + legA, DARK_BROWN);
        fill(x + 8, y + 15, 4, 4 - legA, DARK_BROWN);
        fill(x + 4, y + 15, 4, 1, BROWN);
        fill(x + 8, y + 15, 4, 1, BROWN);
        // Boots
        fill(x + 3, y + 19 + legA, 5, 2, BLACK);
        fill(x + 7, y + 19 - legA, 5, 2, BLACK);
        fill(x + 3, y + 19 + legA, 5, 1, web("#383030"));
        fill(x + 7, y + 19 - legA, 5, 1, web("#383030"));
        // Boot sole highlight
        fill(x + 3, y + 20 + legA, 5, 1, web("#282020"));
        fill(x + 7, y + 20 - legA, 5, 1, web("#282020"));

        // ── Status indicators ──
        if (frame == 3) {
            fill(x + 6, y - 5, 4, 3, RED);
            fill(x + 7, y - 4, 2, 1, BRIGHT_RED);
            dot(x + 7, y - 2, new Color(0, 0, 0, 0)); // TRANSPARENT
        }
        if (frame == 4) {
            dot(x + 5, y - 2, GREEN);
            dot(x + 6, y - 3, LT_GREEN);
            dot(x + 7, y - 4, GREEN);
            dot(x + 8, y - 4, LT_GREEN);
            dot(x + 9, y - 3, GREEN);
            dot(x + 10, y - 2, LT_GREEN);
        }
    }

    /**
     * Draw a robot character (10w x 14h pixels).
     * frame: 0=idle, 1=processing, 2=searching, 3=done
     */
    public void drawRobot(int x, int y, int frame, Color main, Color dark, Color light) {
        Color METAL_HI = web("#e0e0e0");
        Color METAL    = LT_GRAY;
        Color METAL_MD = GRAY;
        Color METAL_DK = DK_GRAY;

        // ── Antenna ──
        fill(x + 6, y, 2, 1, METAL_MD);
        fill(x + 6, y + 1, 2, 1, METAL);
        dot(x + 6, y - 1, main);
        dot(x + 7, y - 1, light);
        // Antenna glow
        if (frame == 1) {
            dot(x + 5, y - 2, deriveColor(light, 0, 1, 1, 0.5));
            dot(x + 8, y - 2, deriveColor(light, 0, 1, 1, 0.5));
        }

        // ── Head ──
        fill(x + 3, y + 2, 8, 5, METAL);
        fill(x + 3, y + 2, 8, 1, METAL_HI);
        fill(x + 3, y + 6, 8, 1, METAL_MD);
        fill(x + 3, y + 2, 1, 5, METAL_HI);
        fill(x + 10, y + 2, 1, 5, METAL_DK);
        // Visor
        fill(x + 4, y + 3, 6, 3, BLACK);
        fill(x + 4, y + 3, 6, 1, web("#181828"));
        // Eyes
        if (frame == 3) {
            dot(x + 5, y + 4, METAL_DK);
            dot(x + 8, y + 4, METAL_DK);
        } else if (frame == 2) {
            dot(x + 5, y + 5, main);
            dot(x + 8, y + 4, main);
            dot(x + 6, y + 4, deriveColor(light, 0, 1, 1, 0.4));
        } else {
            dot(x + 5, y + 4, main);
            dot(x + 8, y + 4, main);
            if (frame == 1) {
                dot(x + 5, y + 5, light);
                dot(x + 8, y + 5, light);
                dot(x + 6, y + 4, deriveColor(main, 0, 0.5, 1.3, 1));
                dot(x + 7, y + 4, deriveColor(main, 0, 0.5, 1.3, 1));
            }
        }
        // Mouth grille
        fill(x + 5, y + 5, 4, 1, METAL_DK);
        dot(x + 6, y + 5, METAL_MD);
        dot(x + 7, y + 5, METAL_MD);

        // ── Body ──
        fill(x + 2, y + 7, 10, 7, main);
        fill(x + 2, y + 7, 1, 7, dark);
        fill(x + 11, y + 7, 1, 7, dark);
        fill(x + 2, y + 7, 10, 1, light);
        fill(x + 2, y + 13, 10, 1, dark);
        // Rivets
        dot(x + 3, y + 8, light);
        dot(x + 10, y + 8, dark);
        dot(x + 3, y + 12, light);
        dot(x + 10, y + 12, dark);
        // Chest display panel
        fill(x + 4, y + 8, 6, 4, METAL_DK);
        fill(x + 4, y + 8, 6, 1, BLACK);
        if (frame == 1) {
            fill(x + 5, y + 9, 2, 1, GREEN);
            fill(x + 8, y + 9, 1, 1, GREEN);
            dot(x + 5, y + 10, LT_GREEN);
        } else if (frame == 3) {
            fill(x + 5, y + 9, 2, 1, RED);
            dot(x + 8, y + 9, RED);
        } else {
            fill(x + 5, y + 9, 2, 1, main);
            fill(x + 8, y + 9, 1, 1, light);
            dot(x + 5, y + 10, darker(main));
        }
        // Belt / waist
        fill(x + 2, y + 13, 10, 1, METAL_MD);

        // ── Arms ──
        if (frame == 1) {
            // Processing - arms up with sparks
            fill(x + 0, y + 5, 2, 7, main);
            fill(x + 0, y + 5, 2, 1, light);
            fill(x + 12, y + 5, 2, 7, main);
            fill(x + 12, y + 5, 2, 1, light);
            dot(x + 0, y + 11, dark);
            dot(x + 13, y + 11, dark);
            // Claw hands
            dot(x + 0, y + 4, METAL);
            dot(x + 13, y + 4, METAL);
        } else if (frame == 2) {
            fill(x + 0, y + 8, 2, 5, main);
            fill(x + 0, y + 8, 2, 1, light);
            fill(x + 12, y + 7, 2, 6, main);
            fill(x + 12, y + 7, 2, 1, light);
        } else {
            fill(x + 0, y + 8, 2, 5, main);
            fill(x + 0, y + 8, 2, 1, light);
            fill(x + 12, y + 8, 2, 5, main);
            fill(x + 12, y + 8, 2, 1, light);
            dot(x + 0, y + 12, dark);
            dot(x + 13, y + 12, dark);
        }

        // ── Legs ──
        fill(x + 3, y + 14, 3, 4, METAL_DK);
        fill(x + 8, y + 14, 3, 4, METAL_DK);
        fill(x + 3, y + 14, 3, 1, METAL_MD);
        fill(x + 8, y + 14, 3, 1, METAL_MD);
        // Piston joints
        dot(x + 4, y + 16, METAL);
        dot(x + 9, y + 16, METAL);
        // Feet
        fill(x + 2, y + 18, 4, 2, METAL_MD);
        fill(x + 8, y + 18, 4, 2, METAL_MD);
        fill(x + 2, y + 18, 4, 1, METAL);
        fill(x + 8, y + 18, 4, 1, METAL);

        // ── Status effects ──
        if (frame == 1) {
            dot(x + 2, y - 1, GOLD);
            dot(x + 11, y, GOLD);
            dot(x + 7, y - 2, light);
        }
        if (frame == 2) {
            fill(x + 5, y - 4, 4, 1, GOLD);
            fill(x + 6, y - 3, 2, 1, GOLD);
            dot(x + 6, y - 2, main);
        }
    }

    /**
     * Draw a philosopher character seated (14w x 18h pixels).
     * HD-2D style with detailed robe folds, expressive face, ornate chair.
     * frame: 0=thinking, 1=eating, 2=waiting
     * bobY: vertical bob for idle animation
     */
    public void drawPhilosopher(int x, int y, int frame, Color robe, Color robeDk, Color hair, int bobY) {
        int by = y + bobY;
        Color hairHi = brighter(hair);
        Color robeHi = brighter(robe);
        Color SASH = GOLD;

        // ── Hair ──
        fill(x + 4, by, 6, 1, hair);
        fill(x + 3, by + 1, 8, 2, hair);
        // Hair highlight
        dot(x + 5, by, brighter(hair));
        dot(x + 6, by + 1, brighter(hair));
        // Side hair / sideburns
        dot(x + 3, by + 3, hair);
        dot(x + 10, by + 3, hair);

        // ── Face ──
        fill(x + 4, by + 3, 6, 5, SKIN);
        fill(x + 4, by + 3, 6, 1, SKIN_LT);  // forehead highlight
        // Eyebrows
        fill(x + 4, by + 4, 2, 1, darker(hair));
        fill(x + 8, by + 4, 2, 1, darker(hair));
        // Eyes
        dot(x + 5, by + 5, BLACK);
        dot(x + 8, by + 5, BLACK);
        // Eye whites
        dot(x + 4, by + 5, WHITE);
        dot(x + 9, by + 5, WHITE);
        // Nose
        dot(x + 7, by + 6, SKIN_DK);
        // Mouth
        if (frame == 1) {
            // Eating - open mouth
            fill(x + 6, by + 7, 2, 1, DK_ORANGE);
        } else if (frame == 2) {
            // Waiting - frown
            fill(x + 6, by + 7, 2, 1, SKIN_DK);
            dot(x + 5, by + 7, SKIN_DK);
        } else {
            // Thinking - neutral
            fill(x + 6, by + 7, 2, 1, SKIN_DK);
        }
        // Cheek blush
        dot(x + 4, by + 6, rgb(220, 160, 130, 0.6));
        dot(x + 9, by + 6, rgb(220, 160, 130, 0.6));
        // Beard stubble
        dot(x + 5, by + 7, SKIN_DK);
        dot(x + 8, by + 7, SKIN_DK);

        // ── Body (robe) ──
        fill(x + 3, by + 8, 8, 5, robe);
        // Robe shading — left dark, right light
        fill(x + 3, by + 8, 1, 5, robeDk);
        fill(x + 10, by + 8, 1, 5, robeDk);
        // Collar / neckline
        fill(x + 5, by + 8, 4, 1, brighter(robe));
        dot(x + 6, by + 8, brighter(brighter(robe)));
        dot(x + 7, by + 8, brighter(brighter(robe)));
        // Robe fold lines
        dot(x + 5, by + 10, robeDk);
        dot(x + 8, by + 10, robeDk);
        dot(x + 6, by + 11, robeDk);
        dot(x + 7, by + 12, robeDk);
        // Belt / sash
        fill(x + 3, by + 12, 8, 1, darker(robeDk));
        dot(x + 6, by + 12, GOLD);  // belt buckle

        // ── Arms ──
        if (frame == 0) {
            // Thinking — right arm raised, hand on chin
            fill(x + 1, by + 8, 2, 4, robe);
            dot(x + 1, by + 8, robeDk);
            fill(x + 11, by + 6, 2, 5, robe);
            dot(x + 11, by + 6, robeDk);
            // Hand on chin
            fill(x + 10, by + 5, 2, 2, SKIN);
            dot(x + 10, by + 5, SKIN_LT);
        } else if (frame == 1) {
            // Eating — right arm reaching with fork
            fill(x + 1, by + 8, 2, 4, robe);
            dot(x + 1, by + 8, robeDk);
            fill(x + 11, by + 8, 2, 3, robe);
            fill(x + 12, by + 8, 1, 2, SKIN);
            // Fork in hand
            fill(x + 12, by + 5, 1, 4, LT_GRAY);
            dot(x + 11, by + 5, LT_GRAY);
            dot(x + 12, by + 4, LT_GRAY);
            dot(x + 13, by + 5, LT_GRAY);
        } else {
            // Waiting — arms crossed/fidgeting
            fill(x + 1, by + 7, 2, 4, robe);
            fill(x + 11, by + 7, 2, 4, robe);
            dot(x + 1, by + 7, robeDk);
            dot(x + 12, by + 7, robeDk);
            // Clenched hands
            dot(x + 1, by + 10, SKIN);
            dot(x + 12, by + 10, SKIN);
        }

        // ── Legs (seated) ──
        fill(x + 4, by + 13, 3, 2, DARK_BROWN);
        fill(x + 7, by + 13, 3, 2, DARK_BROWN);
        // Shoes
        fill(x + 4, by + 15, 3, 1, BLACK);
        fill(x + 7, by + 15, 3, 1, BLACK);

        // ── Chair — ornate with armrests ──
        // Seat
        fill(x + 2, by + 12, 10, 1, WOOD1);
        fill(x + 2, by + 12, 10, 1, WOOD_LT);  // seat highlight
        // Chair back
        fill(x + 2, by + 7, 1, 6, WOOD2);
        fill(x + 11, by + 7, 1, 6, WOOD2);
        // Chair back top finial
        dot(x + 2, by + 7, WOOD_LT);
        dot(x + 11, by + 7, WOOD_LT);
        // Chair legs
        fill(x + 2, by + 13, 1, 4, WOOD2);
        fill(x + 11, by + 13, 1, 4, WOOD2);
        fill(x + 2, by + 16, 1, 1, WOOD_DK);
        fill(x + 11, by + 16, 1, 1, WOOD_DK);
        // Front chair legs
        fill(x + 4, by + 15, 1, 2, WOOD2);
        fill(x + 9, by + 15, 1, 2, WOOD2);

        // ── Status particles ──
        if (frame == 0) {
            // Thought bubbles
            dot(x + 1, by - 1, PLUM);
            dot(x + 0, by - 2, LT_PLUM);
            dot(x - 1, by - 3, LT_PLUM);
            dot(x + 2, by - 3, PLUM);
        }
        if (frame == 2) {
            // ! exclamation for waiting
            fill(x + 5, by - 4, 3, 2, ORANGE);
            fill(x + 5, by - 2, 3, 1, ORANGE);
            dot(x + 6, by - 4, GOLD);
        }
    }

    // ═══════════════════════════════════════
    //  ITEMS
    // ═══════════════════════════════════════

    /** Ore block (7x7) — HD-2D style with bevel and crystal highlights */
    public void drawOreBlock(int x, int y, Color color) {
        fill(x, y, 7, 7, color);
        // Top and left highlight bevel
        fill(x, y, 7, 1, brighter(color));
        fill(x, y, 1, 7, brighter(color));
        fill(x + 1, y + 1, 5, 1, brighter(brighter(color)));
        // Bottom and right shadow bevel
        fill(x + 6, y, 1, 7, darker(color));
        fill(x, y + 6, 7, 1, darker(color));
        fill(x + 1, y + 5, 5, 1, darker(darker(color)));
        // Crystal sparkle highlights
        dot(x + 2, y + 2, WHITE);
        dot(x + 3, y + 1, brighter(color));
        dot(x + 4, y + 3, brighter(brighter(color)));
        // Crack detail
        dot(x + 3, y + 4, darker(color));
        dot(x + 4, y + 5, darker(color));
    }

    /** Plate (8x4) — HD-2D style with ceramic shading */
    public void drawPlate(int x, int y, boolean hasFood) {
        fill(x + 1, y, 6, 1, VLT_GRAY);
        fill(x, y + 1, 8, 2, WHITE);
        fill(x, y + 1, 8, 1, web("#f0f0f0"));
        fill(x + 1, y + 3, 6, 1, VLT_GRAY);
        // Rim highlight
        dot(x + 1, y, web("#fafafa"));
        if (hasFood) {
            fill(x + 2, y + 1, 4, 2, GOLD);
            fill(x + 2, y + 1, 4, 1, web("#d0a030"));
            dot(x + 3, y + 1, RED);
            dot(x + 5, y + 1, DK_GREEN);
        }
    }

    /** Fork item (2x7) — HD-2D style with tines */
    public void drawForkItem(int x, int y, boolean visible) {
        if (!visible) return;
        // Handle
        fill(x, y + 3, 2, 4, LT_GRAY);
        fill(x, y + 3, 2, 1, VLT_GRAY);
        dot(x, y + 6, GRAY);
        // Tines
        dot(x - 1, y, LT_GRAY);
        dot(x, y, LT_GRAY);
        dot(x + 1, y, LT_GRAY);
        dot(x + 2, y, LT_GRAY);
        fill(x - 1, y + 1, 1, 2, LT_GRAY);
        fill(x + 2, y + 1, 1, 2, LT_GRAY);
        fill(x, y + 1, 2, 2, VLT_GRAY);
    }

    /** Round table (top-down view) — HD-2D with 3D bevel, textured cloth, shadow */
    public void drawRoundTable(int cx, int cy, int radius) {
        // Drop shadow beneath table
        int sR = radius + 2;
        for (int dy = -sR; dy <= sR; dy++) {
            int dx = (int) (Math.sqrt(sR * sR - dy * dy));
            fill(cx - dx + 1, cy + dy + 2, dx * 2 + 1, 1, rgb(40, 30, 20, 0.35));
        }
        // Main table surface
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) (Math.sqrt(radius * radius - dy * dy));
            // Subtle vertical gradient on wood (lighter at top)
            double t = (double)(dy + radius) / (2.0 * radius);
            Color wood = interpolate(WOOD1, WOOD2, t);
            fill(cx - dx, cy + dy, dx * 2 + 1, 1, wood);
        }
        // Outer bevel ring — dark edge
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) (Math.sqrt(radius * radius - dy * dy));
            dot(cx - dx, cy + dy, WOOD_DK);
            dot(cx + dx, cy + dy, WOOD_DK);
        }
        for (int dx2 = -radius; dx2 <= radius; dx2++) {
            int dy2 = (int) (Math.sqrt(radius * radius - dx2 * dx2));
            dot(cx + dx2, cy - dy2, WOOD_LT);  // top edge catches light
            dot(cx + dx2, cy + dy2, WOOD_DK);   // bottom edge in shadow
        }
        // Inner bevel ring
        int midR = radius - 2;
        for (int dx2 = -midR; dx2 <= midR; dx2++) {
            int dy2 = (int) (Math.sqrt(midR * midR - dx2 * dx2));
            dot(cx + dx2, cy - dy2, WOOD_LT);
            dot(cx + dx2, cy + dy2, web("#604028"));
        }
        // Tablecloth center with texture
        int inner = radius - 4;
        for (int dy = -inner; dy <= inner; dy++) {
            int dx = (int) (Math.sqrt(inner * inner - dy * dy));
            Color cloth = ((dy + inner) % 3 == 0) ? CREAM : web("#e8d4b0");
            fill(cx - dx, cy + dy, dx * 2 + 1, 1, cloth);
        }
        // Cloth border embroidery
        for (int dx2 = -inner; dx2 <= inner; dx2++) {
            int dy2 = (int) (Math.sqrt(inner * inner - dx2 * dx2));
            dot(cx + dx2, cy - dy2, DK_RED);
            dot(cx + dx2, cy + dy2, DK_RED);
        }
        // Center decorative plate
        fill(cx - 3, cy - 3, 7, 7, WHITE);
        fill(cx - 2, cy - 2, 5, 5, VLT_GRAY);
        fill(cx - 2, cy - 2, 5, 1, WHITE); // plate shine
        fill(cx - 1, cy - 1, 3, 3, GOLD);
        dot(cx, cy, RED);
        dot(cx - 1, cy - 1, DK_GOLD); // plate shadow
    }

    // ═══════════════════════════════════════
    //  DECORATIONS
    // ═══════════════════════════════════════

    /** Torch on wall — HD-2D with multi-tone flame, glow, and embers */
    public void drawTorch(int x, int y, int frame) {
        // Wall bracket (metallic)
        fill(x, y + 7, 3, 1, DK_GRAY);
        dot(x + 1, y + 7, LT_GRAY);
        // Torch handle
        fill(x + 1, y + 4, 1, 3, DARK_BROWN);
        dot(x + 1, y + 4, BROWN);
        // Ambient glow on wall behind flame
        dot(x - 1, y + 2, rgb(255, 160, 40, 0.15));
        dot(x + 3, y + 2, rgb(255, 160, 40, 0.15));
        dot(x, y - 1, rgb(255, 200, 80, 0.1));
        dot(x + 2, y - 1, rgb(255, 200, 80, 0.1));
        // Multi-tone flame animation
        if (frame % 4 < 2) {
            // Flame shape 1 — tall
            fill(x, y + 2, 3, 2, ORANGE);          // outer flame
            fill(x, y + 1, 3, 1, DK_ORANGE);        // mid flame
            fill(x + 1, y, 1, 3, GOLD);             // inner flame
            dot(x + 1, y + 1, web("#fff0c0")); // white-hot core
            dot(x + 1, y, web("#ffe880"));      // bright tip
            // Ember sparks
            dot(x + 2, y - 1, rgb(255, 100, 30, 0.7));
            dot(x - 1, y, rgb(255, 140, 40, 0.5));
        } else {
            // Flame shape 2 — wide
            fill(x, y + 2, 3, 2, ORANGE);
            fill(x, y + 1, 3, 2, DK_ORANGE);
            fill(x + 1, y + 1, 1, 2, GOLD);
            dot(x + 1, y + 1, web("#fff0c0")); // white-hot core
            dot(x, y, GOLD);
            dot(x + 2, y, RED);
            // Ember sparks (different positions)
            dot(x, y - 1, rgb(255, 120, 30, 0.6));
            dot(x + 3, y, rgb(255, 80, 20, 0.4));
        }
    }

    /** Window on wall — HD-2D with night sky, stars, and glass refraction */
    public void drawWindow(int x, int y) {
        // Outer frame with depth
        fill(x, y, 10, 10, DARK_BROWN);
        fill(x + 1, y, 8, 1, WOOD_LT);   // top frame highlight
        fill(x, y, 1, 10, WOOD_LT);       // left frame highlight
        fill(x + 9, y, 1, 10, WOOD_DK);   // right frame shadow
        fill(x, y + 9, 10, 1, WOOD_DK);   // bottom frame shadow
        // Night sky background
        fill(x + 1, y + 1, 8, 8, web("#1a1a3a"));
        fill(x + 1, y + 1, 8, 3, web("#202050")); // lighter near horizon
        fill(x + 1, y + 6, 8, 3, web("#151530")); // darker at bottom
        // Stars
        dot(x + 2, y + 2, web("#ffffcc"));
        dot(x + 7, y + 1, web("#ccccff"));
        dot(x + 4, y + 3, web("#eeeedd"));
        dot(x + 8, y + 4, web("#ddddff"));
        dot(x + 3, y + 6, web("#ffffdd"));
        // Moon crescent
        dot(x + 6, y + 2, web("#f0e8c0"));
        dot(x + 7, y + 2, web("#e8e0b0"));
        dot(x + 6, y + 3, web("#e8e0b0"));
        // Window dividers
        fill(x + 5, y + 1, 1, 8, BROWN);
        fill(x + 1, y + 5, 8, 1, BROWN);
        // Glass refraction / warm interior light reflection
        dot(x + 2, y + 1, rgb(255, 200, 120, 0.3));
        dot(x + 3, y + 2, rgb(255, 200, 120, 0.2));
        dot(x + 7, y + 6, rgb(255, 180, 100, 0.25));
    }

    /** Painting/picture on wall — HD-2D with gilded frame, shadow, and art detail */
    public void drawPainting(int x, int y, Color frameColor, Color art1, Color art2) {
        // Drop shadow behind frame
        fill(x + 1, y + 1, 8, 6, rgb(30, 20, 10, 0.4));
        // Ornate frame
        fill(x, y, 8, 6, frameColor);
        fill(x, y, 8, 1, brighter(frameColor));    // top highlight
        fill(x, y, 1, 6, brighter(frameColor));    // left highlight
        fill(x + 7, y, 1, 6, darker(frameColor));  // right shadow
        fill(x, y + 5, 8, 1, darker(frameColor));  // bottom shadow
        // Gold corner accents
        dot(x, y, DK_GOLD);
        dot(x + 7, y, DK_GOLD);
        dot(x, y + 5, DK_GOLD);
        dot(x + 7, y + 5, DK_GOLD);
        // Art canvas
        fill(x + 1, y + 1, 6, 4, art1);
        fill(x + 2, y + 3, 4, 2, art2);
        fill(x + 3, y + 2, 2, 1, brighter(art2));
        // Highlight shimmer on canvas
        dot(x + 1, y + 1, deriveColor(art1, 0, 0.7, 1.3, 1.0));
    }

    /** Barrel — HD-2D with stave detail, metallic bands, and shadow */
    public void drawBarrel(int x, int y) {
        // Drop shadow
        fill(x + 1, y + 7, 5, 1, rgb(30, 20, 10, 0.4));
        // Top rim
        fill(x + 1, y, 4, 1, WOOD_LT);
        dot(x + 2, y, brighter(WOOD1));
        // Staves
        fill(x, y + 1, 6, 5, WOOD1);
        fill(x, y + 1, 1, 5, WOOD_DK);      // left shadow stave
        fill(x + 5, y + 1, 1, 5, WOOD_DK);  // right shadow stave
        fill(x + 1, y + 1, 1, 5, deriveColor(WOOD1, 0, 0.9, 1.1, 1.0)); // highlight stave
        // Wood grain on center stave
        dot(x + 3, y + 2, WOOD_LT);
        dot(x + 3, y + 4, WOOD_LT);
        // Metal band with shine
        fill(x, y + 3, 6, 1, DK_GRAY);
        dot(x + 2, y + 3, LT_GRAY); // metallic highlight
        dot(x + 4, y + 3, LT_GRAY);
        // Second band
        fill(x, y + 1, 6, 1, web("#585858"));
        dot(x + 3, y + 1, LT_GRAY);
        // Bottom rim
        fill(x + 1, y + 6, 4, 1, WOOD_DK);
    }

    /** Crate — HD-2D with planked texture, nails, and cast shadow */
    public void drawCrate(int x, int y) {
        // Cast shadow
        fill(x + 1, y + 6, 6, 1, rgb(30, 20, 10, 0.35));
        // Main body
        fill(x, y, 6, 6, WOOD2);
        // Plank lines
        fill(x + 2, y, 1, 6, darker(WOOD2));
        fill(x + 4, y, 1, 6, darker(WOOD2));
        // 3D bevel edges
        fill(x, y, 6, 1, WOOD_LT);       // top highlight
        fill(x, y, 1, 6, WOOD_LT);       // left highlight
        fill(x + 5, y, 1, 6, WOOD_DK);   // right shadow
        fill(x, y + 5, 6, 1, WOOD_DK);   // bottom shadow
        // Corner reinforcement (metallic)
        dot(x, y, DK_GRAY);
        dot(x + 5, y, DK_GRAY);
        dot(x, y + 5, DK_GRAY);
        dot(x + 5, y + 5, DK_GRAY);
        // X brace pattern
        dot(x + 1, y + 1, WOOD_DK);
        dot(x + 4, y + 1, WOOD_DK);
        dot(x + 2, y + 2, WOOD_DK);
        dot(x + 3, y + 2, WOOD_DK);
        dot(x + 2, y + 3, WOOD_DK);
        dot(x + 3, y + 3, WOOD_DK);
        dot(x + 1, y + 4, WOOD_DK);
        dot(x + 4, y + 4, WOOD_DK);
        // Nail heads
        dot(x + 1, y + 1, LT_GRAY);
        dot(x + 4, y + 4, LT_GRAY);
    }

    /** Warning stripes (factory) — HD-2D with embossed look */
    public void drawWarningStripes(int x, int y, int w) {
        fill(x, y, w, 1, DK_GOLD);  // top shadow line
        fill(x, y + 1, w, 2, GOLD);
        fill(x, y + 3, w, 1, web("#a08020")); // bottom shadow
        for (int i = 0; i < w; i += 4) {
            int dw = Math.min(2, w - i);
            fill(x + i, y, dw, 3, BLACK);
            dot(x + i, y, web("#303030")); // stripe highlight
        }
    }

    /** Pipe (horizontal) — HD-2D with metallic sheen and bolted joints */
    public void drawPipeH(int x, int y, int w) {
        fill(x, y, w, 1, web("#404040"));     // top shadow
        fill(x, y + 1, w, 1, VLT_GRAY);              // top shine
        fill(x, y + 2, w, 1, LT_GRAY);               // body
        fill(x, y + 3, w, 1, web("#404040"));  // bottom shadow
        // Joints with bolts
        for (int jx = x + 8; jx < x + w - 2; jx += 16) {
            fill(jx, y, 3, 4, GRAY);
            fill(jx, y, 3, 1, LT_GRAY);  // joint highlight
            // Bolts
            dot(jx, y + 1, DK_GRAY);
            dot(jx + 2, y + 2, DK_GRAY);
        }
        // End caps
        fill(x, y, 1, 4, GRAY);
        fill(x + w - 1, y, 1, 4, GRAY);
    }

    /** Pipe (vertical) — HD-2D with metallic sheen */
    public void drawPipeV(int x, int y, int h) {
        fill(x, y, 1, h, web("#404040"));      // left shadow
        fill(x + 1, y, 1, h, VLT_GRAY);               // left shine
        fill(x + 2, y, 1, h, LT_GRAY);                // body
        fill(x + 3, y, 1, h, web("#404040"));   // right shadow
        // Joint rings
        for (int jy = y + 6; jy < y + h - 2; jy += 12) {
            fill(x, jy, 4, 1, GRAY);
            dot(x + 1, jy, LT_GRAY);
        }
    }

    /** Gauge/meter on wall — HD-2D with chrome bezel and LED indicator */
    public void drawGauge(int x, int y, double value) {
        // Chrome bezel
        fill(x, y, 6, 6, DK_GRAY);
        fill(x, y, 6, 1, LT_GRAY);   // top shine
        fill(x, y, 1, 6, LT_GRAY);   // left shine
        fill(x + 5, y, 1, 6, web("#303030")); // right shadow
        fill(x, y + 5, 6, 1, web("#303030")); // bottom shadow
        // Dial face
        fill(x + 1, y + 1, 4, 4, BLACK);
        // Scale markings
        dot(x + 1, y + 3, DK_GRAY);
        dot(x + 2, y + 3, DK_GRAY);
        dot(x + 3, y + 3, DK_GRAY);
        dot(x + 4, y + 3, DK_GRAY);
        // Needle
        int needleX = x + 1 + (int)(value * 3);
        fill(needleX, y + 1, 1, 3, RED);
        dot(needleX, y + 1, web("#ff6060"));
        // Status LED
        dot(x + 1, y + 4, value > 0.5 ? RED : GREEN);
        dot(x + 4, y + 4, GOLD);
    }

    /** Mine entrance — HD-2D with depth, lantern light, and reinforced beams */
    public void drawMineEntrance(int x, int y) {
        // Shadow around entrance
        fill(x - 1, y + 1, 22, 20, rgb(20, 15, 10, 0.3));
        // Support beams with wood grain
        fill(x, y, 2, 20, WOOD2);
        fill(x + 18, y, 2, 20, WOOD2);
        fill(x, y, 1, 20, WOOD_LT);        // left beam highlight
        fill(x + 19, y, 1, 20, WOOD_DK);   // right beam shadow
        // Top beam with highlight
        fill(x, y, 20, 2, WOOD1);
        fill(x, y, 20, 1, WOOD_LT);
        // Cross beam
        fill(x + 2, y + 2, 16, 1, WOOD_DK);
        // Metal reinforcement brackets
        fill(x + 1, y + 2, 2, 2, DK_GRAY);
        fill(x + 17, y + 2, 2, 2, DK_GRAY);
        dot(x + 1, y + 2, LT_GRAY);
        dot(x + 17, y + 2, LT_GRAY);
        // Dark interior with depth gradient
        fill(x + 2, y + 3, 16, 17, BLACK);
        fill(x + 3, y + 3, 14, 2, web("#2a2028"));
        fill(x + 4, y + 5, 12, 2, web("#1a1018"));
        // Lantern glow inside
        dot(x + 4, y + 5, rgb(255, 180, 80, 0.4));
        dot(x + 5, y + 6, rgb(255, 160, 60, 0.3));
        // Rail tracks with metallic shine
        fill(x + 6, y + 18, 8, 2, GRAY);
        fill(x + 6, y + 18, 8, 1, LT_GRAY); // rail shine
        fill(x + 7, y + 17, 1, 3, BROWN);
        fill(x + 12, y + 17, 1, 3, BROWN);
        // Wooden rail ties
        fill(x + 6, y + 19, 8, 1, WOOD_DK);
    }

    /** Shelf with bottles — HD-2D with wood brackets, bottle shine, and shadow */
    public void drawShelf(int x, int y) {
        // Wall brackets
        fill(x + 1, y + 4, 1, 2, WOOD_DK);
        fill(x + 10, y + 4, 1, 2, WOOD_DK);
        // Shelf plank with depth
        fill(x, y + 4, 12, 1, WOOD1);
        fill(x, y + 4, 12, 1, WOOD_LT); // top surface highlight
        fill(x, y + 5, 12, 1, WOOD_DK); // underside shadow
        // Bottles with shine
        Color[] bottleColors = {GREEN, RED, TEAL, GOLD};
        for (int i = 0; i < 4; i++) {
            Color bc = bottleColors[i];
            fill(x + 1 + i * 3, y + 1, 2, 3, bc);
            dot(x + 1 + i * 3, y, darker(bc)); // cork/cap
            // Glass shine
            dot(x + 1 + i * 3, y + 1, brighter(bc));
            // Label on bottle
            dot(x + 1 + i * 3, y + 2, brighter(desaturate(bc)));
            dot(x + 2 + i * 3, y + 2, brighter(desaturate(bc)));
        }
        // Shadow beneath bottles on shelf
        for (int i = 0; i < 4; i++) {
            dot(x + 1 + i * 3, y + 4, WOOD_DK);
        }
    }

    /** Fireplace — HD-2D with multi-tone fire, embers, white core, brick detail */
    public void drawFireplace(int x, int y, int frame) {
        // Brick mantle with depth
        fill(x, y, 16, 2, BRICK2);
        fill(x, y, 16, 1, BRICK_LT);  // top highlight
        fill(x + 1, y + 2, 14, 12, BRICK1);
        // Brick detail on sides
        fill(x + 1, y + 2, 2, 12, BRICK2);
        fill(x + 13, y + 2, 2, 12, BRICK2);
        dot(x + 1, y + 3, BRICK_LT);
        dot(x + 14, y + 5, BRICK_LT);
        dot(x + 1, y + 7, BRICK_LT);
        dot(x + 14, y + 9, BRICK_LT);
        // Firebox interior
        fill(x + 3, y + 4, 10, 10, BLACK);
        fill(x + 3, y + 4, 10, 1, web("#1a1018")); // soot line
        // Ambient glow on interior walls
        fill(x + 3, y + 8, 1, 6, web("#3a1808"));
        fill(x + 12, y + 8, 1, 6, web("#3a1808"));
        // Fire animation with multi-tone flames
        if (frame % 3 == 0) {
            fill(x + 5, y + 8, 6, 6, ORANGE);            // outer
            fill(x + 6, y + 7, 4, 5, GOLD);               // mid
            fill(x + 7, y + 6, 2, 3, web("#fff0b0")); // white-hot core
            dot(x + 7, y + 5, web("#ffe8a0"));       // flame tip
            dot(x + 5, y + 8, RED);                        // ember
            dot(x + 10, y + 9, DK_ORANGE);
        } else if (frame % 3 == 1) {
            fill(x + 4, y + 9, 8, 5, ORANGE);
            fill(x + 5, y + 7, 6, 5, GOLD);
            fill(x + 6, y + 7, 4, 3, web("#fff0b0"));
            dot(x + 8, y + 6, web("#ffe8a0"));
            dot(x + 4, y + 10, RED);
            dot(x + 11, y + 10, DK_ORANGE);
        } else {
            fill(x + 5, y + 8, 6, 6, ORANGE);
            fill(x + 6, y + 7, 4, 4, GOLD);
            fill(x + 7, y + 7, 2, 2, web("#fff0b0"));
            dot(x + 6, y + 6, web("#ffe8a0"));
            dot(x + 9, y + 7, RED);
            dot(x + 5, y + 9, DK_ORANGE);
        }
        // Floating ember sparks above fire
        int emX = x + 6 + (frame % 5);
        int emY = y + 4 - (frame % 3);
        if (emY >= y + 4) dot(emX, emY, rgb(255, 120, 30, 0.6));
        // Logs with detail
        fill(x + 4, y + 12, 8, 2, WOOD_DK);
        fill(x + 5, y + 11, 6, 1, WOOD2);
        dot(x + 5, y + 12, WOOD2);        // log end grain
        dot(x + 11, y + 12, WOOD2);
        // Glow on hearth floor
        fill(x + 5, y + 13, 6, 1, rgb(200, 100, 20, 0.3));
    }

    /** Draw a simple particle/spark */
    public void drawSpark(int x, int y, int frame, Color color) {
        int spread = frame % 6;
        if (spread < 3) {
            dot(x - spread, y - spread, color);
            dot(x + spread, y - spread - 1, brighter(color));
            dot(x + spread + 1, y, color);
        }
    }

    /** Draw smoke puff */
    public void drawSmoke(int x, int y, int frame) {
        int rise = (frame % 12);
        Color smokeC = web("#c0c0c0");
        if (rise < 4) {
            dot(x, y - rise, smokeC);
            dot(x + 1, y - rise, smokeC);
        } else if (rise < 8) {
            dot(x - 1, y - rise, deriveColor(smokeC, 0, 1, 1, 0.6));
            dot(x + 1, y - rise, deriveColor(smokeC, 0, 1, 1, 0.6));
        }
    }

    // ═══════════════════════════════════════
    //  ATMOSPHERIC EFFECTS (HD-2D)
    // ═══════════════════════════════════════

    /**
     * Draw floating dust particles for atmospheric depth.
     * Creates the characteristic HD-2D "lived-in" atmosphere.
     */
    public void drawDustParticles(int pw, int ph, int frame, int count) {
        for (int i = 0; i < count; i++) {
            // Deterministic pseudo-random positions based on index
            double seed = i * 137.5 + 42.7;
            double t = (frame * 0.3 + i * 31.7) % 300.0;
            double px = (Math.sin(seed + t * 0.008) * 0.5 + 0.5) * pw;
            double py = (t / 300.0) * ph;
            // Gentle horizontal drift
            px += Math.sin(t * 0.03 + i * 2.1) * 3;

            float alpha = (float)(Math.sin(t * 0.04 + i * 1.3) * 0.25 + 0.35);
            if (alpha <= 0) continue;

            // Warm-tinted particles (like dust in sunbeam)
            float r = 1.0f, g = 0.95f, b = 0.8f;
            // Some particles are slightly brighter/larger
            if (i % 5 == 0) {
                dot((int)px, (int)py, new Color(r, g, b, alpha * 0.8f));
                dot((int)px + 1, (int)py, new Color(r, g, b, alpha * 0.4f));
                dot((int)px, (int)py + 1, new Color(r, g, b, alpha * 0.4f));
            } else {
                dot((int)px, (int)py, new Color(r, g, b, alpha * 0.5f));
            }
        }
    }

    /**
     * Draw a soft light glow around a point (for torches, lamps).
     * The glow is drawn as concentric semi-transparent rectangles that
     * the bloom post-processing will pick up and spread beautifully.
     */
    public void drawLightGlow(int cx, int cy, int radius, Color lightColor, float intensity) {
        for (int r = 1; r <= radius; r += 2) {
            float falloff = 1.0f - (float)r / radius;
            float alpha = falloff * falloff * intensity; // quadratic falloff
            if (alpha < 0.01f) continue;
            Color c = new Color(lightColor.r, lightColor.g, lightColor.b, alpha);
            fill(cx - r, cy - r, r * 2, r * 2, c);
        }
    }

    /**
     * Draw a horizontal gradient fog/haze band for atmospheric depth.
     * Great for separating foreground from background layers.
     */
    public void drawFogBand(int y, int height, int pw, Color fogColor, float maxAlpha) {
        for (int row = 0; row < height; row++) {
            float t = (float)row / height;
            // Bell curve: strongest in middle
            float alpha = (float)(Math.sin(t * Math.PI)) * maxAlpha;
            if (alpha < 0.01f) continue;
            fill(0, y + row, pw, 1, new Color(fogColor.r, fogColor.g, fogColor.b, alpha));
        }
    }

    /**
     * Draw ambient light rays streaming from a point (like from a window).
     * Creates volumetric light shaft appearance.
     */
    public void drawLightRays(int sx, int sy, int length, int spread, int frame, Color lightColor) {
        float flicker = (float)(Math.sin(frame * 0.15) * 0.1 + 0.9);
        for (int ray = 0; ray < spread; ray++) {
            double angle = (ray - spread / 2.0) * 0.08 + Math.PI / 2;
            for (int d = 0; d < length; d++) {
                int rx = sx + (int)(Math.cos(angle) * d);
                int ry = sy + (int)(Math.sin(angle) * d);
                float alpha = (1.0f - (float)d / length) * 0.08f * flicker;
                if (alpha < 0.005f) continue;
                fill(rx, ry, 1, 1, new Color(lightColor.r, lightColor.g, lightColor.b, alpha));
            }
        }
    }

    /**
     * Draw a subtle ambient occlusion shadow along a horizontal edge.
     * Darkens the area below a wall/ceiling transition.
     */
    public void drawAmbientOcclusion(int y, int pw, int depth, float intensity) {
        for (int d = 0; d < depth; d++) {
            float alpha = (1.0f - (float)d / depth) * intensity;
            fill(0, y + d, pw, 1, new Color(0, 0, 0, alpha));
        }
    }

    // ═══════════════════════════════════════
    //  DISPOSE
    // ═══════════════════════════════════════

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        spriteBatch.dispose();
        frameBuffer.dispose();
        for (BitmapFont font : fontCache.values()) {
            font.dispose();
        }
        fontCache.clear();
        if (fontGenerator != null) {
            fontGenerator.dispose();
        }
    }
}
