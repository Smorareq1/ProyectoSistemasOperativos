package gt.edu.url.so.proyectosistemasoperativos.common;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Canvas-based pixel art rendering engine.
 * Draws backgrounds, characters, items and effects using a scaled pixel grid.
 * Each "pixel art pixel" = S x S screen pixels.
 */
public class PixelGameCanvas extends Canvas {

    private final int S; // scale factor
    private final GraphicsContext gc;

    // ═══════════════════════════════════════
    //  COLOR PALETTE
    // ═══════════════════════════════════════
    public static final Color BLACK       = Color.web("#1a1018");
    public static final Color DARK_BROWN  = Color.web("#4a3020");
    public static final Color BROWN       = Color.web("#6b4c38");
    public static final Color MED_BROWN   = Color.web("#8b7355");
    public static final Color TAN         = Color.web("#c4a882");
    public static final Color CREAM       = Color.web("#f0dcc0");
    public static final Color BG_CREAM    = Color.web("#f5e6c8");
    public static final Color WHITE       = Color.web("#fff8e8");

    public static final Color ORANGE      = Color.web("#e8682a");
    public static final Color DK_ORANGE   = Color.web("#c45020");
    public static final Color GOLD        = Color.web("#f0c040");
    public static final Color DK_GOLD     = Color.web("#c89828");

    public static final Color RED         = Color.web("#c83830");
    public static final Color DK_RED      = Color.web("#8b2018");
    public static final Color BRIGHT_RED  = Color.web("#e84040");

    public static final Color GREEN       = Color.web("#68b030");
    public static final Color DK_GREEN    = Color.web("#4a8820");
    public static final Color LT_GREEN    = Color.web("#90d858");

    public static final Color TEAL        = Color.web("#3898b8");
    public static final Color DK_TEAL     = Color.web("#2870a0");
    public static final Color LT_TEAL     = Color.web("#60c0e0");

    public static final Color SKIN        = Color.web("#e8b888");
    public static final Color SKIN_DK     = Color.web("#c89868");
    public static final Color SKIN_LT     = Color.web("#f0d0a8");

    public static final Color GRAY        = Color.web("#808080");
    public static final Color DK_GRAY     = Color.web("#505050");
    public static final Color LT_GRAY     = Color.web("#a0a0a0");
    public static final Color VLT_GRAY    = Color.web("#c8c8c8");

    public static final Color PLUM        = Color.web("#6858a0");
    public static final Color DK_PLUM     = Color.web("#483870");
    public static final Color LT_PLUM     = Color.web("#9080c0");

    public static final Color BRICK1      = Color.web("#b85830");
    public static final Color BRICK2      = Color.web("#a04828");
    public static final Color BRICK_LT    = Color.web("#d07040");
    public static final Color MORTAR      = Color.web("#d0b898");

    public static final Color WOOD1       = Color.web("#b08050");
    public static final Color WOOD2       = Color.web("#986838");
    public static final Color WOOD_LT     = Color.web("#c89868");
    public static final Color WOOD_DK     = Color.web("#704828");

    public static final Color SKY_BLUE    = Color.web("#88c8e8");
    public static final Color SKY_LT      = Color.web("#a8d8f0");

    // ═══════════════════════════════════════
    //  CONSTRUCTOR
    // ═══════════════════════════════════════
    public PixelGameCanvas(double width, double height, int pixelScale) {
        super(width, height);
        this.S = pixelScale;
        this.gc = getGraphicsContext2D();
        gc.setImageSmoothing(false);
    }

    public PixelGameCanvas(double width, double height) {
        this(width, height, 3);
    }

    // ═══════════════════════════════════════
    //  CORE DRAWING
    // ═══════════════════════════════════════
    /** Fill a pixel-grid rectangle */
    public void fill(double px, double py, int pw, int ph, Color c) {
        gc.setFill(c);
        gc.fillRect(px * S, py * S, pw * S, ph * S);
    }

    /** Fill a single pixel */
    public void dot(double px, double py, Color c) {
        fill(px, py, 1, 1, c);
    }

    /** Clear entire canvas */
    public void clear(Color bg) {
        gc.setFill(bg);
        gc.fillRect(0, 0, getWidth(), getHeight());
    }

    public int pxW() { return (int)(getWidth() / S); }
    public int pxH() { return (int)(getHeight() / S); }
    public int scale() { return S; }
    public GraphicsContext ctx() { return gc; }

    // ═══════════════════════════════════════
    //  BACKGROUND TILES
    // ═══════════════════════════════════════

    /** Brick wall pattern */
    public void drawBrickWall(int sx, int sy, int w, int h) {
        fill(sx, sy, w, h, MORTAR);
        int bW = 7, bH = 3;
        int row = 0;
        for (int y = sy; y < sy + h; y += bH + 1) {
            int off = (row % 2 == 0) ? 0 : 4;
            for (int x = sx - bW + off; x < sx + w; x += bW + 1) {
                int dx = Math.max(x, sx);
                int dw = Math.min(x + bW, sx + w) - dx;
                int dh = Math.min(bH, sy + h - y);
                if (dw > 0 && dh > 0) {
                    Color bc = ((row + x / (bW + 1)) % 3 == 0) ? BRICK2 : BRICK1;
                    fill(dx, y, dw, dh, bc);
                    if (dh >= 2) fill(dx, y, dw, 1, BRICK_LT);
                }
            }
            row++;
        }
    }

    /** Stone floor tiles */
    public void drawStoneFloor(int sx, int sy, int w, int h) {
        Color[] sc = {TAN, MED_BROWN, Color.web("#b09870"), Color.web("#a08060")};
        int tW = 6, tH = 5;
        for (int y = sy; y < sy + h; y += tH) {
            int off = ((y - sy) / tH % 2) * 3;
            for (int x = sx + off; x < sx + w; x += tW) {
                int idx = Math.abs((x * 3 + y * 7)) % sc.length;
                int dw = Math.min(tW - 1, sx + w - x);
                int dh = Math.min(tH - 1, sy + h - y);
                if (dw > 0 && dh > 0) fill(x, y, dw, dh, sc[idx]);
            }
        }
        for (int y = sy; y < sy + h; y += tH) fill(sx, y, w, 1, BROWN);
    }

    /** Wood plank floor */
    public void drawWoodFloor(int sx, int sy, int w, int h) {
        int pH = 5;
        for (int y = sy; y < sy + h; y += pH) {
            Color pc = ((y - sy) / pH % 2 == 0) ? WOOD1 : WOOD2;
            int dh = Math.min(pH, sy + h - y);
            fill(sx, y, w, dh, pc);
            fill(sx, y, w, 1, WOOD_LT);
            for (int x = sx + 10 + ((y / pH) % 3) * 5; x < sx + w; x += 20)
                dot(x, y + 2, DARK_BROWN);
        }
    }

    /** Animated conveyor belt */
    public void drawConveyor(int sx, int sy, int w, int frame) {
        // Rails
        fill(sx, sy, w, 1, BROWN);
        fill(sx, sy + 7, w, 1, BROWN);
        fill(sx - 1, sy, 1, 8, DARK_BROWN);
        fill(sx + w, sy, 1, 8, DARK_BROWN);
        // Belt surface
        fill(sx, sy + 1, w, 6, DK_GRAY);
        fill(sx, sy + 2, w, 4, GRAY);
        // Animated stripes
        int off = frame % 4;
        for (int x = sx + off; x < sx + w; x += 4) {
            int dw = Math.min(2, sx + w - x);
            fill(x, sy + 2, dw, 4, LT_GRAY);
        }
        // Support legs
        for (int x = sx + 6; x < sx + w - 2; x += 18) {
            fill(x, sy + 8, 2, 4, BROWN);
            fill(x, sy + 11, 4, 1, DARK_BROWN);
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
        Color HAT_HI = Color.web("#ffe878");
        Color HAT    = GOLD;
        Color HAT_MD = DK_GOLD;
        Color HAT_DK = Color.web("#a07818");
        Color LAMP   = Color.web("#e0f0ff");

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
        dot(x + 5, y, Color.web("#fff8c0"));
        dot(x + 6, y, Color.web("#fff8c0"));

        // ── Face ──
        fill(x + 4, y + 4, 8, 5, SKIN);
        fill(x + 4, y + 4, 8, 1, SKIN_LT);
        fill(x + 4, y + 8, 8, 1, SKIN_DK);
        // Eyebrows
        fill(x + 5, y + 4, 2, 1, DARK_BROWN);
        fill(x + 9, y + 4, 2, 1, DARK_BROWN);
        // Eyes
        dot(x + 5, y + 5, BLACK);
        dot(x + 6, y + 5, Color.web("#302018"));
        dot(x + 9, y + 5, Color.web("#302018"));
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
        fill(x + 3, y + 9, 10, 1, Color.web("#f08038"));
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
        fill(x + 3, y + 19 + legA, 5, 1, Color.web("#383030"));
        fill(x + 7, y + 19 - legA, 5, 1, Color.web("#383030"));
        // Boot sole highlight
        fill(x + 3, y + 20 + legA, 5, 1, Color.web("#282020"));
        fill(x + 7, y + 20 - legA, 5, 1, Color.web("#282020"));

        // ── Status indicators ──
        if (frame == 3) {
            fill(x + 6, y - 5, 4, 3, RED);
            fill(x + 7, y - 4, 2, 1, BRIGHT_RED);
            dot(x + 7, y - 2, Color.TRANSPARENT);
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
        Color METAL_HI = Color.web("#e0e0e0");
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
            dot(x + 5, y - 2, light.deriveColor(0, 1, 1, 0.5));
            dot(x + 8, y - 2, light.deriveColor(0, 1, 1, 0.5));
        }

        // ── Head ──
        fill(x + 3, y + 2, 8, 5, METAL);
        fill(x + 3, y + 2, 8, 1, METAL_HI);
        fill(x + 3, y + 6, 8, 1, METAL_MD);
        fill(x + 3, y + 2, 1, 5, METAL_HI);
        fill(x + 10, y + 2, 1, 5, METAL_DK);
        // Visor
        fill(x + 4, y + 3, 6, 3, BLACK);
        fill(x + 4, y + 3, 6, 1, Color.web("#181828"));
        // Eyes
        if (frame == 3) {
            dot(x + 5, y + 4, METAL_DK);
            dot(x + 8, y + 4, METAL_DK);
        } else if (frame == 2) {
            dot(x + 5, y + 5, main);
            dot(x + 8, y + 4, main);
            dot(x + 6, y + 4, light.deriveColor(0, 1, 1, 0.4));
        } else {
            dot(x + 5, y + 4, main);
            dot(x + 8, y + 4, main);
            if (frame == 1) {
                dot(x + 5, y + 5, light);
                dot(x + 8, y + 5, light);
                dot(x + 6, y + 4, main.deriveColor(0, 0.5, 1.3, 1));
                dot(x + 7, y + 4, main.deriveColor(0, 0.5, 1.3, 1));
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
            dot(x + 5, y + 10, main.darker());
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
     * Draw a philosopher character seated (14w x 18h pixels) — HD-2D detailed.
     * frame: 0=thinking, 1=eating, 2=waiting
     * bobOffset: vertical bob for idle animation
     */
    public void drawPhilosopher(int x, int y, int frame, Color robe, Color robeDk, Color hair, int bobY) {
        int by = y + bobY;
        Color hairHi = hair.brighter();
        Color robeHi = robe.brighter();
        Color SASH = GOLD;

        // ── Hair ──
        fill(x + 4, by, 6, 1, hairHi);
        fill(x + 3, by + 1, 8, 2, hair);
        fill(x + 3, by + 1, 2, 1, hairHi);
        dot(x + 10, by + 2, hair.darker());

        // ── Face ──
        fill(x + 4, by + 3, 6, 5, SKIN);
        fill(x + 4, by + 3, 6, 1, SKIN_LT);
        fill(x + 4, by + 7, 6, 1, SKIN_DK);
        // Eyebrows
        fill(x + 5, by + 3, 2, 1, hair.darker());
        fill(x + 8, by + 3, 2, 1, hair.darker());
        // Eyes
        dot(x + 5, by + 4, BLACK);
        dot(x + 8, by + 4, BLACK);
        dot(x + 6, by + 4, WHITE);
        dot(x + 9, by + 4, WHITE);
        // Nose
        dot(x + 7, by + 5, SKIN_DK);
        // Ears
        dot(x + 3, by + 4, SKIN_DK);
        dot(x + 10, by + 4, SKIN_DK);

        if (frame == 1) {
            fill(x + 6, by + 7, 2, 1, DK_ORANGE);
        } else {
            fill(x + 6, by + 7, 2, 1, SKIN_DK);
        }

        // ── Body (robe) ──
        fill(x + 3, by + 8, 8, 5, robe);
        fill(x + 3, by + 8, 1, 5, robeDk);
        fill(x + 10, by + 8, 1, 5, robeDk);
        fill(x + 5, by + 8, 4, 1, robeHi);
        // Collar
        fill(x + 5, by + 8, 4, 1, WHITE);
        dot(x + 5, by + 8, VLT_GRAY);
        dot(x + 8, by + 8, VLT_GRAY);
        // Sash
        fill(x + 6, by + 9, 2, 4, SASH);
        dot(x + 6, by + 9, SASH.brighter());
        // Robe pattern
        dot(x + 4, by + 10, robeDk);
        dot(x + 9, by + 10, robeDk);

        // ── Arms ──
        if (frame == 0) {
            fill(x + 1, by + 8, 2, 4, robe);
            dot(x + 1, by + 8, robeHi);
            fill(x + 11, by + 5, 2, 5, robe);
            dot(x + 11, by + 5, SKIN);
            dot(x + 12, by + 5, SKIN);
        } else if (frame == 1) {
            fill(x + 1, by + 8, 2, 4, robe);
            dot(x + 1, by + 8, robeHi);
            fill(x + 11, by + 8, 2, 3, robe);
            dot(x + 12, by + 8, SKIN);
            // Fork
            fill(x + 12, by + 5, 1, 4, LT_GRAY);
            dot(x + 11, by + 5, LT_GRAY);
            dot(x + 13, by + 5, LT_GRAY);
            dot(x + 12, by + 4, LT_GRAY);
        } else {
            fill(x + 0, by + 7, 3, 4, robe);
            dot(x + 0, by + 7, robeHi);
            fill(x + 11, by + 7, 3, 4, robe);
            dot(x + 13, by + 7, robeDk);
        }

        // ── Legs (seated) ──
        fill(x + 4, by + 13, 3, 2, DARK_BROWN);
        fill(x + 7, by + 13, 3, 2, DARK_BROWN);
        fill(x + 4, by + 13, 3, 1, BROWN);
        fill(x + 7, by + 13, 3, 1, BROWN);

        // ── Chair ──
        fill(x + 1, by + 12, 12, 1, WOOD1);
        fill(x + 1, by + 12, 12, 1, WOOD_LT);
        fill(x + 1, by + 13, 1, 4, WOOD2);
        fill(x + 12, by + 13, 1, 4, WOOD2);
        fill(x + 1, by + 16, 1, 1, WOOD_DK);
        fill(x + 12, by + 16, 1, 1, WOOD_DK);
        // Chair back
        fill(x + 1, by + 10, 1, 3, WOOD2);
        dot(x + 1, by + 10, WOOD_LT);

        // ── Status particles ──
        if (frame == 0) {
            // Thought bubble z's
            dot(x + 2, by - 1, PLUM);
            fill(x + 0, by - 3, 2, 1, LT_PLUM);
            dot(x + 1, by - 2, PLUM);
            dot(x + 3, by - 2, LT_PLUM.deriveColor(0, 1, 1, 0.5));
        }
        if (frame == 2) {
            // ! exclamation
            fill(x + 6, by - 4, 2, 3, ORANGE);
            fill(x + 6, by - 1, 2, 1, ORANGE);
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
        fill(x, y, 7, 1, color.brighter());
        fill(x, y, 1, 7, color.brighter());
        fill(x + 1, y + 1, 5, 1, color.brighter().brighter());
        // Bottom and right shadow bevel
        fill(x + 6, y, 1, 7, color.darker());
        fill(x, y + 6, 7, 1, color.darker());
        fill(x + 1, y + 5, 5, 1, color.darker().darker());
        // Crystal sparkle highlights
        dot(x + 2, y + 2, WHITE);
        dot(x + 3, y + 1, color.brighter());
        dot(x + 4, y + 3, color.brighter().brighter());
        // Crack detail
        dot(x + 3, y + 4, color.darker());
        dot(x + 4, y + 5, color.darker());
    }

    /** Plate (8x4) — HD-2D style with ceramic shading */
    public void drawPlate(int x, int y, boolean hasFood) {
        fill(x + 1, y, 6, 1, VLT_GRAY);
        fill(x, y + 1, 8, 2, WHITE);
        fill(x, y + 1, 8, 1, Color.web("#f0f0f0"));
        fill(x + 1, y + 3, 6, 1, VLT_GRAY);
        // Rim highlight
        dot(x + 1, y, Color.web("#fafafa"));
        if (hasFood) {
            fill(x + 2, y + 1, 4, 2, GOLD);
            fill(x + 2, y + 1, 4, 1, Color.web("#d0a030"));
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

    /** Round table (top-down view) */
    public void drawRoundTable(int cx, int cy, int radius) {
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) (Math.sqrt(radius * radius - dy * dy));
            fill(cx - dx, cy + dy, dx * 2 + 1, 1, WOOD1);
        }
        // Edge ring
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) (Math.sqrt(radius * radius - dy * dy));
            dot(cx - dx, cy + dy, WOOD_DK);
            dot(cx + dx, cy + dy, WOOD_DK);
        }
        for (int dx2 = -radius; dx2 <= radius; dx2++) {
            int dy2 = (int) (Math.sqrt(radius * radius - dx2 * dx2));
            dot(cx + dx2, cy - dy2, WOOD_DK);
            dot(cx + dx2, cy + dy2, WOOD_DK);
        }
        // Tablecloth center
        int inner = radius - 4;
        for (int dy = -inner; dy <= inner; dy++) {
            int dx = (int) (Math.sqrt(inner * inner - dy * dy));
            fill(cx - dx, cy + dy, dx * 2 + 1, 1, CREAM);
        }
        // Center plate
        fill(cx - 2, cy - 2, 5, 5, WHITE);
        fill(cx - 1, cy - 1, 3, 3, GOLD);
        dot(cx, cy, RED);
    }

    // ═══════════════════════════════════════
    //  DECORATIONS
    // ═══════════════════════════════════════

    /** Torch on wall */
    public void drawTorch(int x, int y, int frame) {
        fill(x + 1, y + 4, 1, 3, BROWN);
        fill(x, y + 7, 3, 1, BROWN);
        if (frame % 4 < 2) {
            fill(x, y + 1, 3, 3, ORANGE);
            fill(x + 1, y, 1, 1, GOLD);
            dot(x, y + 1, GOLD);
        } else {
            fill(x, y + 2, 3, 2, ORANGE);
            fill(x, y + 1, 3, 1, GOLD);
            dot(x + 1, y, RED);
            dot(x + 2, y + 1, GOLD);
        }
    }

    /** Window on wall */
    public void drawWindow(int x, int y) {
        fill(x, y, 10, 10, BROWN);
        fill(x + 1, y + 1, 8, 8, SKY_BLUE);
        fill(x + 1, y + 1, 8, 2, SKY_LT);
        fill(x + 5, y + 1, 1, 8, BROWN);
        fill(x + 1, y + 5, 8, 1, BROWN);
    }

    /** Painting/picture on wall */
    public void drawPainting(int x, int y, Color frameColor, Color art1, Color art2) {
        fill(x, y, 8, 6, frameColor);
        fill(x + 1, y + 1, 6, 4, art1);
        fill(x + 2, y + 3, 4, 2, art2);
        fill(x + 3, y + 2, 2, 1, art2.brighter());
    }

    /** Barrel */
    public void drawBarrel(int x, int y) {
        fill(x + 1, y, 4, 1, BROWN);
        fill(x, y + 1, 6, 5, WOOD1);
        fill(x, y + 1, 1, 5, WOOD_DK);
        fill(x + 5, y + 1, 1, 5, WOOD_DK);
        fill(x, y + 3, 6, 1, DK_GRAY);
        fill(x + 1, y + 6, 4, 1, BROWN);
    }

    /** Crate */
    public void drawCrate(int x, int y) {
        fill(x, y, 6, 6, WOOD2);
        fill(x, y, 6, 1, WOOD_LT);
        fill(x, y, 1, 6, WOOD_LT);
        fill(x + 5, y, 1, 6, WOOD_DK);
        fill(x, y + 5, 6, 1, WOOD_DK);
        // X pattern
        dot(x + 1, y + 1, WOOD_DK);
        dot(x + 4, y + 1, WOOD_DK);
        dot(x + 2, y + 2, WOOD_DK);
        dot(x + 3, y + 2, WOOD_DK);
        dot(x + 2, y + 3, WOOD_DK);
        dot(x + 3, y + 3, WOOD_DK);
        dot(x + 1, y + 4, WOOD_DK);
        dot(x + 4, y + 4, WOOD_DK);
    }

    /** Warning stripes (factory) */
    public void drawWarningStripes(int x, int y, int w) {
        fill(x, y, w, 2, GOLD);
        for (int i = 0; i < w; i += 4) {
            int dw = Math.min(2, w - i);
            fill(x + i, y, dw, 2, BLACK);
        }
    }

    /** Pipe (horizontal) */
    public void drawPipeH(int x, int y, int w) {
        fill(x, y, w, 1, DK_GRAY);
        fill(x, y + 1, w, 2, LT_GRAY);
        fill(x, y + 1, w, 1, VLT_GRAY);
        fill(x, y + 3, w, 1, DK_GRAY);
        // Joints
        for (int jx = x + 8; jx < x + w - 2; jx += 16) {
            fill(jx, y, 3, 4, GRAY);
        }
    }

    /** Pipe (vertical) */
    public void drawPipeV(int x, int y, int h) {
        fill(x, y, 1, h, DK_GRAY);
        fill(x + 1, y, 2, h, LT_GRAY);
        fill(x + 1, y, 1, h, VLT_GRAY);
        fill(x + 3, y, 1, h, DK_GRAY);
    }

    /** Gauge/meter on wall */
    public void drawGauge(int x, int y, double value) {
        fill(x, y, 6, 6, DK_GRAY);
        fill(x + 1, y + 1, 4, 4, BLACK);
        // Needle position based on value
        int needleX = x + 1 + (int)(value * 3);
        fill(needleX, y + 2, 1, 2, RED);
        dot(x + 1, y + 4, GREEN);
    }

    /** Mine entrance (dark opening with supports) */
    public void drawMineEntrance(int x, int y) {
        // Support beams
        fill(x, y, 2, 20, WOOD2);
        fill(x + 18, y, 2, 20, WOOD2);
        fill(x, y, 20, 2, WOOD1);
        // Cross beam
        fill(x + 2, y + 2, 16, 1, WOOD_DK);
        // Dark interior
        fill(x + 2, y + 3, 16, 17, BLACK);
        fill(x + 3, y + 3, 14, 1, Color.web("#2a2028"));
        // Rail tracks coming out
        fill(x + 6, y + 18, 8, 2, GRAY);
        fill(x + 7, y + 17, 1, 3, BROWN);
        fill(x + 12, y + 17, 1, 3, BROWN);
    }

    /** Shelf with bottles */
    public void drawShelf(int x, int y) {
        fill(x, y + 4, 12, 1, WOOD1);
        fill(x, y + 4, 12, 1, WOOD_DK);
        // Bottles
        Color[] bottleColors = {GREEN, RED, TEAL, GOLD};
        for (int i = 0; i < 4; i++) {
            fill(x + 1 + i * 3, y + 1, 2, 3, bottleColors[i]);
            dot(x + 1 + i * 3, y, bottleColors[i].darker());
        }
    }

    /** Fireplace */
    public void drawFireplace(int x, int y, int frame) {
        // Brick structure
        fill(x, y, 16, 2, BRICK2);
        fill(x + 1, y + 2, 14, 12, BRICK1);
        fill(x + 3, y + 4, 10, 10, BLACK);
        // Fire
        if (frame % 3 == 0) {
            fill(x + 5, y + 8, 6, 6, ORANGE);
            fill(x + 6, y + 6, 4, 4, GOLD);
            dot(x + 7, y + 5, GOLD);
        } else if (frame % 3 == 1) {
            fill(x + 4, y + 9, 8, 5, ORANGE);
            fill(x + 5, y + 7, 6, 4, GOLD);
            dot(x + 8, y + 6, RED);
        } else {
            fill(x + 5, y + 8, 6, 6, ORANGE);
            fill(x + 6, y + 7, 4, 3, GOLD);
            dot(x + 6, y + 6, GOLD);
            dot(x + 9, y + 7, RED);
        }
        // Logs
        fill(x + 4, y + 12, 8, 2, WOOD_DK);
        fill(x + 5, y + 11, 6, 1, WOOD2);
    }

    /** Draw pixel text using the loaded font */
    public void drawText(String text, double px, double py, Color color, double fontSize) {
        gc.setFill(color);
        gc.setFont(javafx.scene.text.Font.font("Press Start 2P", fontSize));
        gc.fillText(text, px * S, py * S + fontSize);
    }

    /** Draw a simple particle/spark */
    public void drawSpark(int x, int y, int frame, Color color) {
        int spread = frame % 6;
        if (spread < 3) {
            dot(x - spread, y - spread, color);
            dot(x + spread, y - spread - 1, color.brighter());
            dot(x + spread + 1, y, color);
        }
    }

    /** Draw smoke puff */
    public void drawSmoke(int x, int y, int frame) {
        int rise = (frame % 12);
        Color smokeC = Color.web("#c0c0c0");
        if (rise < 4) {
            dot(x, y - rise, smokeC);
            dot(x + 1, y - rise, smokeC);
        } else if (rise < 8) {
            dot(x - 1, y - rise, smokeC.deriveColor(0, 1, 1, 0.6));
            dot(x + 1, y - rise, smokeC.deriveColor(0, 1, 1, 0.6));
        }
    }
}
