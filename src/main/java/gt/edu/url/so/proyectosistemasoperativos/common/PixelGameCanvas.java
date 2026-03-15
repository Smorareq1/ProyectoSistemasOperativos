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
     * Draw a miner character (10w x 15h pixels).
     * frame: 0=idle, 1=mine-up, 2=mine-down, 3=blocked, 4=done
     */
    public void drawMiner(int x, int y, int frame) {
        // ── Hard Hat ──
        fill(x + 2, y, 6, 2, GOLD);
        fill(x + 3, y, 4, 1, Color.web("#ffe070"));
        fill(x + 1, y + 2, 8, 1, DK_GOLD);

        // ── Face ──
        fill(x + 2, y + 3, 6, 4, SKIN);
        dot(x + 3, y + 4, BLACK);
        dot(x + 6, y + 4, BLACK);

        if (frame == 3) {
            // Blocked - grimace
            fill(x + 4, y + 6, 2, 1, RED);
        } else if (frame == 4) {
            // Done - smile
            dot(x + 4, y + 6, SKIN_DK);
            dot(x + 5, y + 6, SKIN_DK);
        } else {
            fill(x + 4, y + 6, 2, 1, SKIN_DK);
        }

        // ── Body (overalls) ──
        fill(x + 1, y + 7, 8, 4, ORANGE);
        fill(x + 1, y + 7, 1, 4, DK_ORANGE);
        fill(x + 4, y + 7, 2, 1, WHITE);
        fill(x + 1, y + 10, 8, 1, BROWN);
        fill(x + 4, y + 10, 2, 1, GOLD);

        // ── Arms ──
        if (frame == 1) {
            // Mining up - right arm extended up with pickaxe
            fill(x + 9, y + 6, 2, 3, SKIN);
            fill(x + 10, y + 1, 1, 6, BROWN);
            fill(x + 9, y + 0, 3, 2, GRAY);
            dot(x + 11, y + 2, GRAY);
            fill(x + 0, y + 7, 1, 3, SKIN);
        } else if (frame == 2) {
            // Mining down - right arm swinging down
            fill(x + 9, y + 8, 2, 3, SKIN);
            fill(x + 10, y + 8, 1, 5, BROWN);
            fill(x + 9, y + 12, 3, 2, GRAY);
            dot(x + 11, y + 11, GRAY);
            fill(x + 0, y + 7, 1, 3, SKIN);
        } else if (frame == 3) {
            // Blocked - both arms up
            fill(x + 0, y + 5, 1, 4, SKIN);
            fill(x + 9, y + 5, 1, 4, SKIN);
        } else {
            // Idle/done - arms at sides
            fill(x + 0, y + 7, 1, 3, SKIN);
            fill(x + 9, y + 7, 1, 3, SKIN);
            // Pickaxe resting
            if (frame == 0) {
                fill(x + 9, y + 3, 1, 8, BROWN);
                fill(x + 8, y + 2, 3, 2, GRAY);
                dot(x + 10, y + 4, GRAY);
            }
        }

        // ── Legs ──
        int legAnim = (frame == 1) ? 1 : 0;
        fill(x + 2, y + 11, 3, 3 + legAnim, DARK_BROWN);
        fill(x + 5, y + 11, 3, 3 - legAnim, DARK_BROWN);
        fill(x + 2, y + 13 + legAnim, 3, 1, BLACK);
        fill(x + 5, y + 13 - legAnim, 3, 1, BLACK);

        // ── Status indicators ──
        if (frame == 3) {
            fill(x + 4, y - 3, 2, 2, RED);
            dot(x + 4, y - 1, Color.TRANSPARENT);
            dot(x + 5, y - 1, Color.TRANSPARENT);
        }
        if (frame == 4) {
            dot(x + 3, y - 1, GREEN);
            dot(x + 4, y - 2, GREEN);
            dot(x + 5, y - 3, GREEN);
            dot(x + 6, y - 2, GREEN);
            dot(x + 7, y - 1, GREEN);
        }
    }

    /**
     * Draw a robot character (10w x 14h pixels).
     * frame: 0=idle, 1=processing, 2=searching, 3=done
     */
    public void drawRobot(int x, int y, int frame, Color main, Color dark, Color light) {
        // ── Antenna ──
        dot(x + 4, y, main);
        dot(x + 5, y, main);
        fill(x + 4, y + 1, 2, 1, LT_GRAY);

        // ── Head ──
        fill(x + 2, y + 2, 6, 4, LT_GRAY);
        fill(x + 2, y + 2, 6, 1, VLT_GRAY);
        fill(x + 2, y + 5, 6, 1, GRAY);

        // Eyes
        if (frame == 3) {
            dot(x + 3, y + 3, DK_GRAY);
            dot(x + 6, y + 3, DK_GRAY);
            dot(x + 4, y + 4, DK_GRAY);
            dot(x + 5, y + 4, DK_GRAY);
        } else if (frame == 2) {
            dot(x + 3, y + 4, main);
            dot(x + 6, y + 3, main);
        } else {
            dot(x + 3, y + 3, main);
            dot(x + 6, y + 3, main);
            if (frame == 1) {
                dot(x + 3, y + 4, main);
                dot(x + 6, y + 4, main);
            }
        }
        fill(x + 4, y + 5, 2, 1, DK_GRAY);

        // ── Body ──
        fill(x + 1, y + 6, 8, 5, main);
        fill(x + 1, y + 6, 1, 5, dark);
        fill(x + 8, y + 6, 1, 5, dark);
        fill(x + 1, y + 6, 8, 1, light);
        // Chest panel
        fill(x + 3, y + 7, 4, 3, DK_GRAY);
        if (frame == 1) {
            fill(x + 4, y + 8, 2, 1, GREEN);
        } else if (frame == 3) {
            fill(x + 4, y + 8, 2, 1, RED);
        } else {
            fill(x + 4, y + 8, 2, 1, main);
        }

        // ── Arms ──
        if (frame == 1) {
            fill(x + 0, y + 4, 1, 5, main);
            fill(x + 9, y + 4, 1, 5, main);
            dot(x + 0, y + 4, light);
            dot(x + 9, y + 4, light);
        } else if (frame == 2) {
            // Searching - arms out
            fill(x + 0, y + 7, 1, 3, main);
            fill(x + 9, y + 6, 1, 4, main);
        } else {
            fill(x + 0, y + 7, 1, 3, main);
            fill(x + 9, y + 7, 1, 3, main);
        }

        // ── Legs ──
        fill(x + 2, y + 11, 2, 3, DK_GRAY);
        fill(x + 6, y + 11, 2, 3, DK_GRAY);
        fill(x + 2, y + 13, 3, 1, GRAY);
        fill(x + 5, y + 13, 3, 1, GRAY);

        // ── Status ──
        if (frame == 1) {
            dot(x + 1, y - 1, GOLD);
            dot(x + 8, y, GOLD);
        }
        if (frame == 2) {
            dot(x + 4, y - 3, GOLD);
            fill(x + 4, y - 2, 2, 1, GOLD);
            dot(x + 5, y - 1, GOLD);
        }
    }

    /**
     * Draw a philosopher character seated (11w x 12h pixels).
     * frame: 0=thinking, 1=eating, 2=waiting
     * bobOffset: vertical bob for idle animation
     */
    public void drawPhilosopher(int x, int y, int frame, Color robe, Color robeDk, Color hair, int bobY) {
        int by = y + bobY;

        // ── Hair ──
        fill(x + 3, by, 5, 2, hair);
        fill(x + 2, by + 1, 7, 1, hair);

        // ── Face ──
        fill(x + 3, by + 2, 5, 4, SKIN);
        fill(x + 3, by + 2, 5, 1, SKIN_LT);
        dot(x + 4, by + 3, BLACK);
        dot(x + 6, by + 3, BLACK);

        if (frame == 1) {
            dot(x + 5, by + 5, DK_ORANGE);
        } else {
            dot(x + 5, by + 5, SKIN_DK);
        }

        // ── Body (robe) ──
        fill(x + 2, by + 6, 7, 4, robe);
        fill(x + 2, by + 6, 1, 4, robeDk);
        fill(x + 8, by + 6, 1, 4, robeDk);
        fill(x + 4, by + 6, 3, 1, robe.brighter());

        // ── Arms ──
        if (frame == 0) {
            // Thinking - hand on chin
            fill(x + 1, by + 6, 1, 3, robe);
            fill(x + 9, by + 4, 1, 4, robe);
            dot(x + 8, by + 4, SKIN);
        } else if (frame == 1) {
            // Eating - arm reaching to plate with fork
            fill(x + 1, by + 6, 1, 3, robe);
            fill(x + 9, by + 6, 2, 2, robe);
            dot(x + 10, by + 6, SKIN);
            // Fork
            fill(x + 10, by + 4, 1, 3, LT_GRAY);
            dot(x + 9, by + 4, LT_GRAY);
            dot(x + 10, by + 3, LT_GRAY);
        } else {
            // Waiting - arms up/crossed
            fill(x + 0, by + 5, 2, 3, robe);
            fill(x + 9, by + 5, 2, 3, robe);
        }

        // ── Legs (seated, short) ──
        fill(x + 3, by + 10, 2, 2, DARK_BROWN);
        fill(x + 6, by + 10, 2, 2, DARK_BROWN);

        // ── Chair ──
        fill(x + 1, by + 9, 9, 1, WOOD1);
        fill(x + 1, by + 10, 1, 3, WOOD2);
        fill(x + 9, by + 10, 1, 3, WOOD2);
        fill(x + 1, by + 12, 1, 1, WOOD_DK);
        fill(x + 9, by + 12, 1, 1, WOOD_DK);

        // ── Status particles ──
        if (frame == 0) {
            // Z's for thinking
            dot(x + 1, by - 1, PLUM);
            dot(x + 0, by - 2, LT_PLUM);
        }
        if (frame == 2) {
            // ! for waiting
            fill(x + 4, by - 3, 2, 2, ORANGE);
            fill(x + 4, by - 1, 2, 1, ORANGE);
        }
    }

    // ═══════════════════════════════════════
    //  ITEMS
    // ═══════════════════════════════════════

    /** Ore block (5x5) */
    public void drawOreBlock(int x, int y, Color color) {
        fill(x, y, 5, 5, color);
        fill(x, y, 5, 1, color.brighter());
        fill(x, y, 1, 5, color.brighter());
        fill(x + 4, y, 1, 5, color.darker());
        fill(x, y + 4, 5, 1, color.darker());
        dot(x + 1, y + 1, WHITE);
    }

    /** Plate (6x3) */
    public void drawPlate(int x, int y, boolean hasFood) {
        fill(x, y, 6, 1, VLT_GRAY);
        fill(x - 1, y + 1, 8, 1, WHITE);
        fill(x, y + 2, 6, 1, VLT_GRAY);
        if (hasFood) {
            fill(x + 1, y, 4, 2, GOLD);
            dot(x + 2, y, RED);
        }
    }

    /** Fork item (1x5) */
    public void drawForkItem(int x, int y, boolean visible) {
        if (!visible) return;
        fill(x, y, 1, 5, LT_GRAY);
        dot(x - 1, y, LT_GRAY);
        dot(x + 1, y, LT_GRAY);
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
