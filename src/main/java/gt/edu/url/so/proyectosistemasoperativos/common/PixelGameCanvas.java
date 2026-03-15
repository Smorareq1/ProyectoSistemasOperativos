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

    /** Brick wall pattern — HD-2D with mortar depth, wear, and ambient gradient */
    public void drawBrickWall(int sx, int sy, int w, int h) {
        // Mortar base with vertical ambient gradient (lighter at top = torch light)
        for (int y = sy; y < sy + h; y++) {
            double t = (double)(y - sy) / Math.max(h, 1);
            int r = (int)(0xd0 - t * 0x28), g = (int)(0xb8 - t * 0x30), b = (int)(0x98 - t * 0x28);
            fill(sx, y, w, 1, Color.rgb(clamp(r), clamp(g), clamp(b)));
        }
        // Dark grout shadow lines
        int bW = 7, bH = 3;
        int row = 0;
        for (int y = sy; y < sy + h; y += bH + 1) {
            fill(sx, y + bH, w, 1, Color.web("#6a5038"));
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
                    fill(dx, y, dw, 1, bc.deriveColor(0, 0.9, 1.25, 1.0));
                    // Bottom edge shadow
                    fill(dx, y + dh - 1, dw, 1, bc.darker());
                    // Left edge micro-highlight
                    dot(dx, y, bc.deriveColor(0, 0.8, 1.35, 1.0));
                    // Right edge shadow
                    if (dw > 1) dot(dx + dw - 1, y + dh - 1, bc.darker().darker());
                    // Occasional wear/damage
                    if (hash > 225 && dw > 3) {
                        dot(dx + 2, y + 1, bc.darker().desaturate());
                        dot(dx + 1, y + 1, bc.darker());
                    }
                    // Occasional mortar stain
                    if (hash > 200 && hash <= 225 && dw > 4) {
                        dot(dx + 3, y + 1, MORTAR.darker());
                    }
                }
            }
            row++;
        }
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    /** Stone floor tiles — HD-2D with specular highlights, grout shadows, and wear */
    public void drawStoneFloor(int sx, int sy, int w, int h) {
        Color[] sc = {TAN, MED_BROWN, Color.web("#b09870"), Color.web("#a08060"),
                       Color.web("#c0a880"), Color.web("#908068")};
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
                    dot(x, y, base.deriveColor(0, 0.8, 1.3, 1.0));
                    if (dw > 1) dot(x + 1, y, base.deriveColor(0, 0.9, 1.15, 1.0));
                    // Bottom-right shadow
                    if (dh > 1) fill(x, y + dh - 1, dw, 1, base.darker());
                    if (dw > 1) dot(x + dw - 1, y + dh - 1, base.darker().darker());
                    // Occasional surface crack
                    int hash = Math.abs(x * 11 + y * 17) & 0xFF;
                    if (hash > 230 && dw > 2 && dh > 2) {
                        dot(x + 1, y + 1, base.darker());
                        dot(x + 2, y + 2, base.darker());
                    }
                }
            }
        }
        // Dark grout lines between rows
        for (int y = sy; y < sy + h; y += tH) {
            fill(sx, y, w, 1, Color.web("#5a4030"));
        }
        // Vertical grout accents
        for (int y = sy; y < sy + h; y += tH) {
            int off = ((y - sy) / tH % 2) * 3;
            for (int x = sx + off; x < sx + w; x += tW) {
                if (x > sx) dot(x - 1, y + 1, Color.web("#6a5040"));
            }
        }
    }

    /** Wood plank floor — HD-2D with grain, knots, and plank shadow gaps */
    public void drawWoodFloor(int sx, int sy, int w, int h) {
        int pH = 5;
        Color[] planks = {WOOD1, WOOD2, Color.web("#a07848"), Color.web("#c09060")};
        for (int y = sy; y < sy + h; y += pH) {
            int plankIdx = ((y - sy) / pH) % planks.length;
            Color pc = planks[plankIdx];
            int dh = Math.min(pH, sy + h - y);
            fill(sx, y, w, dh, pc);
            // Top edge highlight (light catching the plank edge)
            fill(sx, y, w, 1, pc.deriveColor(0, 0.85, 1.2, 1.0));
            // Bottom gap shadow between planks
            fill(sx, y + dh - 1, w, 1, pc.darker().darker());
            // Wood grain streaks (lighter lines along the plank)
            for (int gx = sx + (plankIdx * 3); gx < sx + w; gx += 8) {
                int gw = Math.min(3, sx + w - gx);
                fill(gx, y + 2, gw, 1, pc.deriveColor(0, 0.7, 1.15, 1.0));
            }
            // Knot marks
            int knotOff = 10 + ((y / pH) % 3) * 7;
            for (int x = sx + knotOff; x < sx + w; x += 22) {
                dot(x, y + 2, DARK_BROWN);
                dot(x + 1, y + 2, pc.darker());
                dot(x, y + 3, pc.darker());
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
        fill(sx, sy, w, 1, Color.web("#606060"));
        fill(sx, sy, w, 1, DK_GRAY);
        // Bottom rail
        fill(sx, sy + 7, w, 1, Color.web("#505050"));
        // Belt surface with depth
        fill(sx, sy + 1, w, 6, Color.web("#454545"));
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
        fill(sx, sy + 1, 2, 6, Color.web("#707070"));
        fill(sx, sy + 2, 2, 1, VLT_GRAY); // roller shine
        fill(sx + w - 2, sy + 1, 2, 6, Color.web("#707070"));
        fill(sx + w - 2, sy + 2, 2, 1, VLT_GRAY);
        // Support legs with reinforced base
        for (int x = sx + 6; x < sx + w - 2; x += 18) {
            fill(x, sy + 8, 2, 4, DK_GRAY);
            fill(x, sy + 8, 2, 1, LT_GRAY); // top shine
            fill(x - 1, sy + 11, 4, 1, Color.web("#404040"));
            fill(x - 1, sy + 12, 4, 1, Color.web("#353535"));
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
     * Draw a philosopher character seated (14w x 18h pixels).
     * HD-2D style with detailed robe folds, expressive face, ornate chair.
     * frame: 0=thinking, 1=eating, 2=waiting
     * bobOffset: vertical bob for idle animation
     */
    public void drawPhilosopher(int x, int y, int frame, Color robe, Color robeDk, Color hair, int bobY) {
        int by = y + bobY;
        Color hairHi = hair.brighter();
        Color robeHi = robe.brighter();
        Color SASH = GOLD;

        // ── Hair ──
        fill(x + 4, by, 6, 1, hair);
        fill(x + 3, by + 1, 8, 2, hair);
        // Hair highlight
        dot(x + 5, by, hair.brighter());
        dot(x + 6, by + 1, hair.brighter());
        // Side hair / sideburns
        dot(x + 3, by + 3, hair);
        dot(x + 10, by + 3, hair);

        // ── Face ──
        fill(x + 4, by + 3, 6, 5, SKIN);
        fill(x + 4, by + 3, 6, 1, SKIN_LT);  // forehead highlight
        // Eyebrows
        fill(x + 4, by + 4, 2, 1, hair.darker());
        fill(x + 8, by + 4, 2, 1, hair.darker());
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
        dot(x + 4, by + 6, Color.rgb(220, 160, 130, 0.6));
        dot(x + 9, by + 6, Color.rgb(220, 160, 130, 0.6));
        // Beard stubble (subtle on some)
        dot(x + 5, by + 7, SKIN_DK);
        dot(x + 8, by + 7, SKIN_DK);

        // ── Body (robe) ──
        fill(x + 3, by + 8, 8, 5, robe);
        // Robe shading — left dark, right light
        fill(x + 3, by + 8, 1, 5, robeDk);
        fill(x + 10, by + 8, 1, 5, robeDk);
        // Collar / neckline
        fill(x + 5, by + 8, 4, 1, robe.brighter());
        dot(x + 6, by + 8, robe.brighter().brighter());
        dot(x + 7, by + 8, robe.brighter().brighter());
        // Robe fold lines
        dot(x + 5, by + 10, robeDk);
        dot(x + 8, by + 10, robeDk);
        dot(x + 6, by + 11, robeDk);
        dot(x + 7, by + 12, robeDk);
        // Belt / sash
        fill(x + 3, by + 12, 8, 1, robeDk.darker());
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

    /** Round table (top-down view) — HD-2D with 3D bevel, textured cloth, shadow */
    public void drawRoundTable(int cx, int cy, int radius) {
        // Drop shadow beneath table
        int sR = radius + 2;
        for (int dy = -sR; dy <= sR; dy++) {
            int dx = (int) (Math.sqrt(sR * sR - dy * dy));
            fill(cx - dx + 1, cy + dy + 2, dx * 2 + 1, 1, Color.rgb(40, 30, 20, 0.35));
        }
        // Main table surface
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) (Math.sqrt(radius * radius - dy * dy));
            // Subtle vertical gradient on wood (lighter at top)
            double t = (double)(dy + radius) / (2.0 * radius);
            Color wood = WOOD1.interpolate(WOOD2, t);
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
            dot(cx + dx2, cy + dy2, Color.web("#604028"));
        }
        // Tablecloth center with texture
        int inner = radius - 4;
        for (int dy = -inner; dy <= inner; dy++) {
            int dx = (int) (Math.sqrt(inner * inner - dy * dy));
            Color cloth = ((dy + inner) % 3 == 0) ? CREAM : Color.web("#e8d4b0");
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
        dot(x - 1, y + 2, Color.rgb(255, 160, 40, 0.15));
        dot(x + 3, y + 2, Color.rgb(255, 160, 40, 0.15));
        dot(x, y - 1, Color.rgb(255, 200, 80, 0.1));
        dot(x + 2, y - 1, Color.rgb(255, 200, 80, 0.1));
        // Multi-tone flame animation
        if (frame % 4 < 2) {
            // Flame shape 1 — tall
            fill(x, y + 2, 3, 2, ORANGE);          // outer flame
            fill(x, y + 1, 3, 1, DK_ORANGE);        // mid flame
            fill(x + 1, y, 1, 3, GOLD);             // inner flame
            dot(x + 1, y + 1, Color.web("#fff0c0")); // white-hot core
            dot(x + 1, y, Color.web("#ffe880"));      // bright tip
            // Ember sparks
            dot(x + 2, y - 1, Color.rgb(255, 100, 30, 0.7));
            dot(x - 1, y, Color.rgb(255, 140, 40, 0.5));
        } else {
            // Flame shape 2 — wide
            fill(x, y + 2, 3, 2, ORANGE);
            fill(x, y + 1, 3, 2, DK_ORANGE);
            fill(x + 1, y + 1, 1, 2, GOLD);
            dot(x + 1, y + 1, Color.web("#fff0c0")); // white-hot core
            dot(x, y, GOLD);
            dot(x + 2, y, RED);
            // Ember sparks (different positions)
            dot(x, y - 1, Color.rgb(255, 120, 30, 0.6));
            dot(x + 3, y, Color.rgb(255, 80, 20, 0.4));
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
        fill(x + 1, y + 1, 8, 8, Color.web("#1a1a3a"));
        fill(x + 1, y + 1, 8, 3, Color.web("#202050")); // lighter near horizon
        fill(x + 1, y + 6, 8, 3, Color.web("#151530")); // darker at bottom
        // Stars
        dot(x + 2, y + 2, Color.web("#ffffcc"));
        dot(x + 7, y + 1, Color.web("#ccccff"));
        dot(x + 4, y + 3, Color.web("#eeeedd"));
        dot(x + 8, y + 4, Color.web("#ddddff"));
        dot(x + 3, y + 6, Color.web("#ffffdd"));
        // Moon crescent
        dot(x + 6, y + 2, Color.web("#f0e8c0"));
        dot(x + 7, y + 2, Color.web("#e8e0b0"));
        dot(x + 6, y + 3, Color.web("#e8e0b0"));
        // Window dividers
        fill(x + 5, y + 1, 1, 8, BROWN);
        fill(x + 1, y + 5, 8, 1, BROWN);
        // Glass refraction / warm interior light reflection
        dot(x + 2, y + 1, Color.rgb(255, 200, 120, 0.3));
        dot(x + 3, y + 2, Color.rgb(255, 200, 120, 0.2));
        dot(x + 7, y + 6, Color.rgb(255, 180, 100, 0.25));
    }

    /** Painting/picture on wall — HD-2D with gilded frame, shadow, and art detail */
    public void drawPainting(int x, int y, Color frameColor, Color art1, Color art2) {
        // Drop shadow behind frame
        fill(x + 1, y + 1, 8, 6, Color.rgb(30, 20, 10, 0.4));
        // Ornate frame
        fill(x, y, 8, 6, frameColor);
        fill(x, y, 8, 1, frameColor.brighter());    // top highlight
        fill(x, y, 1, 6, frameColor.brighter());    // left highlight
        fill(x + 7, y, 1, 6, frameColor.darker());  // right shadow
        fill(x, y + 5, 8, 1, frameColor.darker());  // bottom shadow
        // Gold corner accents
        dot(x, y, DK_GOLD);
        dot(x + 7, y, DK_GOLD);
        dot(x, y + 5, DK_GOLD);
        dot(x + 7, y + 5, DK_GOLD);
        // Art canvas
        fill(x + 1, y + 1, 6, 4, art1);
        fill(x + 2, y + 3, 4, 2, art2);
        fill(x + 3, y + 2, 2, 1, art2.brighter());
        // Highlight shimmer on canvas
        dot(x + 1, y + 1, art1.deriveColor(0, 0.7, 1.3, 1.0));
    }

    /** Barrel — HD-2D with stave detail, metallic bands, and shadow */
    public void drawBarrel(int x, int y) {
        // Drop shadow
        fill(x + 1, y + 7, 5, 1, Color.rgb(30, 20, 10, 0.4));
        // Top rim
        fill(x + 1, y, 4, 1, WOOD_LT);
        dot(x + 2, y, WOOD1.brighter());
        // Staves
        fill(x, y + 1, 6, 5, WOOD1);
        fill(x, y + 1, 1, 5, WOOD_DK);      // left shadow stave
        fill(x + 5, y + 1, 1, 5, WOOD_DK);  // right shadow stave
        fill(x + 1, y + 1, 1, 5, WOOD1.deriveColor(0, 0.9, 1.1, 1.0)); // highlight stave
        // Wood grain on center stave
        dot(x + 3, y + 2, WOOD_LT);
        dot(x + 3, y + 4, WOOD_LT);
        // Metal band with shine
        fill(x, y + 3, 6, 1, DK_GRAY);
        dot(x + 2, y + 3, LT_GRAY); // metallic highlight
        dot(x + 4, y + 3, LT_GRAY);
        // Second band
        fill(x, y + 1, 6, 1, Color.web("#585858"));
        dot(x + 3, y + 1, LT_GRAY);
        // Bottom rim
        fill(x + 1, y + 6, 4, 1, WOOD_DK);
    }

    /** Crate — HD-2D with planked texture, nails, and cast shadow */
    public void drawCrate(int x, int y) {
        // Cast shadow
        fill(x + 1, y + 6, 6, 1, Color.rgb(30, 20, 10, 0.35));
        // Main body
        fill(x, y, 6, 6, WOOD2);
        // Plank lines
        fill(x + 2, y, 1, 6, WOOD2.darker());
        fill(x + 4, y, 1, 6, WOOD2.darker());
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
        fill(x, y + 3, w, 1, Color.web("#a08020")); // bottom shadow
        for (int i = 0; i < w; i += 4) {
            int dw = Math.min(2, w - i);
            fill(x + i, y, dw, 3, BLACK);
            dot(x + i, y, Color.web("#303030")); // stripe highlight
        }
    }

    /** Pipe (horizontal) — HD-2D with metallic sheen and bolted joints */
    public void drawPipeH(int x, int y, int w) {
        fill(x, y, w, 1, Color.web("#404040"));     // top shadow
        fill(x, y + 1, w, 1, VLT_GRAY);              // top shine
        fill(x, y + 2, w, 1, LT_GRAY);               // body
        fill(x, y + 3, w, 1, Color.web("#404040"));  // bottom shadow
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
        fill(x, y, 1, h, Color.web("#404040"));      // left shadow
        fill(x + 1, y, 1, h, VLT_GRAY);               // left shine
        fill(x + 2, y, 1, h, LT_GRAY);                // body
        fill(x + 3, y, 1, h, Color.web("#404040"));   // right shadow
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
        fill(x + 5, y, 1, 6, Color.web("#303030")); // right shadow
        fill(x, y + 5, 6, 1, Color.web("#303030")); // bottom shadow
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
        dot(needleX, y + 1, Color.web("#ff6060"));
        // Status LED
        dot(x + 1, y + 4, value > 0.5 ? RED : GREEN);
        dot(x + 4, y + 4, GOLD);
    }

    /** Mine entrance — HD-2D with depth, lantern light, and reinforced beams */
    public void drawMineEntrance(int x, int y) {
        // Shadow around entrance
        fill(x - 1, y + 1, 22, 20, Color.rgb(20, 15, 10, 0.3));
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
        fill(x + 3, y + 3, 14, 2, Color.web("#2a2028"));
        fill(x + 4, y + 5, 12, 2, Color.web("#1a1018"));
        // Lantern glow inside
        dot(x + 4, y + 5, Color.rgb(255, 180, 80, 0.4));
        dot(x + 5, y + 6, Color.rgb(255, 160, 60, 0.3));
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
            dot(x + 1 + i * 3, y, bc.darker()); // cork/cap
            // Glass shine
            dot(x + 1 + i * 3, y + 1, bc.brighter());
            // Label on bottle
            dot(x + 1 + i * 3, y + 2, bc.desaturate().brighter());
            dot(x + 2 + i * 3, y + 2, bc.desaturate().brighter());
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
        fill(x + 3, y + 4, 10, 1, Color.web("#1a1018")); // soot line
        // Ambient glow on interior walls
        fill(x + 3, y + 8, 1, 6, Color.web("#3a1808"));
        fill(x + 12, y + 8, 1, 6, Color.web("#3a1808"));
        // Fire animation with multi-tone flames
        if (frame % 3 == 0) {
            fill(x + 5, y + 8, 6, 6, ORANGE);            // outer
            fill(x + 6, y + 7, 4, 5, GOLD);               // mid
            fill(x + 7, y + 6, 2, 3, Color.web("#fff0b0")); // white-hot core
            dot(x + 7, y + 5, Color.web("#ffe8a0"));       // flame tip
            dot(x + 5, y + 8, RED);                        // ember
            dot(x + 10, y + 9, DK_ORANGE);
        } else if (frame % 3 == 1) {
            fill(x + 4, y + 9, 8, 5, ORANGE);
            fill(x + 5, y + 7, 6, 5, GOLD);
            fill(x + 6, y + 7, 4, 3, Color.web("#fff0b0"));
            dot(x + 8, y + 6, Color.web("#ffe8a0"));
            dot(x + 4, y + 10, RED);
            dot(x + 11, y + 10, DK_ORANGE);
        } else {
            fill(x + 5, y + 8, 6, 6, ORANGE);
            fill(x + 6, y + 7, 4, 4, GOLD);
            fill(x + 7, y + 7, 2, 2, Color.web("#fff0b0"));
            dot(x + 6, y + 6, Color.web("#ffe8a0"));
            dot(x + 9, y + 7, RED);
            dot(x + 5, y + 9, DK_ORANGE);
        }
        // Floating ember sparks above fire
        int emX = x + 6 + (frame % 5);
        int emY = y + 4 - (frame % 3);
        if (emY >= y + 4) dot(emX, emY, Color.rgb(255, 120, 30, 0.6));
        // Logs with detail
        fill(x + 4, y + 12, 8, 2, WOOD_DK);
        fill(x + 5, y + 11, 6, 1, WOOD2);
        dot(x + 5, y + 12, WOOD2);        // log end grain
        dot(x + 11, y + 12, WOOD2);
        // Glow on hearth floor
        fill(x + 5, y + 13, 6, 1, Color.rgb(200, 100, 20, 0.3));
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
