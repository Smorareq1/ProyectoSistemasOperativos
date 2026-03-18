package gt.edu.url.so.proyectosistemasoperativos;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import gt.edu.url.so.proyectosistemasoperativos.common.PixelArtRenderer;
import gt.edu.url.so.proyectosistemasoperativos.common.PostProcessingPipeline;

import static gt.edu.url.so.proyectosistemasoperativos.common.PixelArtRenderer.*;

/**
 * A themed loading screen that shows an OS-boot sequence before transitioning
 * to the target simulation screen. The visuals and boot messages differ
 * depending on whether the target is Producer-Consumer or Dining Philosophers.
 */
public class LoadingScreen extends ScreenAdapter {

    public enum Target { PRODUCER_CONSUMER, DINING_PHILOSOPHERS }

    private final SOGame game;
    private final Target target;

    private PixelArtRenderer renderer;
    private PostProcessingPipeline postFx;

    private float elapsed = 0f;
    private int animFrame = 0;
    private float frameTimer = 0f;

    private static final float TOTAL_DURATION = 2.8f; // seconds

    // ── PC boot messages ──
    private static final String[] PC_MESSAGES = {
            "> Iniciando sistema de produccion...",
            "> Calibrando brazos roboticos...",
            "> Sincronizando buffer de minas...",
            "> Cargando algoritmos de produccion...",
            "> Verificando capacidad de almacenamiento...",
            "> Conectando consumidores al bus de datos...",
            "> Sistema listo. Iniciando simulacion..."
    };

    // ── DP boot messages ──
    private static final String[] DP_MESSAGES = {
            "> Iniciando sistema de sincronizacion...",
            "> Preparando cubiertos...",
            "> Sincronizando hilos de pensamiento...",
            "> Verificando exclusion mutua...",
            "> Evitando deadlocks...",
            "> Sentando filosofos en la mesa...",
            "> Sistema listo. Iniciando simulacion..."
    };

    private static final int CW = 1100, CH = 750, SCALE = 3;

    public LoadingScreen(SOGame game, Target target) {
        this.game = game;
        this.target = target;
    }

    @Override
    public void show() {
        renderer = new PixelArtRenderer(CW, CH, SCALE);
        postFx = new PostProcessingPipeline(CW, CH);

        // Brighter CRT-like post-processing
        postFx.setBloomIntensity(0.55f);
        postFx.setBloomThreshold(0.30f);
        postFx.setBloomRadius(2.5f);
        postFx.setBloomPasses(3);
        postFx.setContrast(1.10f);
        postFx.setSaturation(1.15f);
        postFx.setVignetteRadius(0.75f);
        postFx.setVignetteSoftness(0.6f);

        if (target == Target.PRODUCER_CONSUMER) {
            postFx.setWarmth(0.92f); // cool teal tint
        } else {
            postFx.setWarmth(1.15f); // warm golden tint
        }
    }

    @Override
    public void render(float delta) {
        elapsed += delta;
        frameTimer += delta;
        if (frameTimer >= 0.1f) {
            frameTimer -= 0.1f;
            animFrame++;
        }

        float progress = Math.min(elapsed / TOTAL_DURATION, 1f);

        renderer.beginFrame();
        drawLoadingScene(progress);
        renderer.endFrame();
        postFx.render(renderer.getFrameBufferTexture());

        // Transition when done
        if (elapsed >= TOTAL_DURATION) {
            if (target == Target.PRODUCER_CONSUMER) {
                game.showProducerConsumer();
            } else {
                game.showDiningPhilosophers();
            }
        }
    }

    private void drawLoadingScene(float progress) {
        int pw = renderer.pxW();
        int ph = renderer.pxH();

        // ── COLOR SCHEME ──
        Color accent, accentDark, accentBright, barFill;
        if (target == Target.PRODUCER_CONSUMER) {
            accent = web("#60f0e0");
            accentDark = web("#308880");
            accentBright = web("#a0fff0");
            barFill = new Color(0.3f, 0.9f, 0.8f, 1f);
        } else {
            accent = web("#ffc040");
            accentDark = web("#a07830");
            accentBright = web("#ffe890");
            barFill = new Color(1f, 0.8f, 0.3f, 1f);
        }

        int wallEnd = ph * 45 / 100;

        // ══════════════════════════════════════
        //  BACKGROUND — different per target
        // ══════════════════════════════════════
        if (target == Target.PRODUCER_CONSUMER) {
            renderer.drawBrickWall(0, 0, pw, ph);
            renderer.fill(0, 0, pw, ph, new Color(0, 0, 0, 0.45f));
        } else {
            // Warm-toned dark hall for philosophers
            renderer.clear(web("#14100c"));
            // Wooden floor
            for (int fy = wallEnd; fy < ph; fy += 6) {
                Color plankColor = (fy / 6 % 2 == 0) ? web("#2a1e14") : web("#221812");
                renderer.fill(0, fy, pw, 5, plankColor);
                renderer.fill(0, fy + 5, pw, 1, web("#181008"));
            }
            // Dark stone wall top
            for (int wy = 0; wy < wallEnd; wy += 8) {
                for (int wx = 0; wx < pw; wx += 16) {
                    int offset = (wy / 8 % 2 == 0) ? 0 : 8;
                    Color stoneColor = ((wx + wy) % 32 < 16) ? web("#2a2420") : web("#242018");
                    renderer.fill(wx + offset, wy, 15, 7, stoneColor);
                    renderer.fill(wx + offset, wy + 7, 15, 1, web("#1a1610"));
                }
            }
            renderer.fill(0, wallEnd - 1, pw, 2, web("#3a3028"));
        }

        // Scanline CRT overlay
        for (int y = 0; y < ph; y += 4) {
            renderer.fill(0, y, pw, 1, new Color(0, 0, 0, 0.06f));
        }

        // ══════════════════════════════════════
        //  PC: FULL-SCREEN FACTORY SCENE
        // ══════════════════════════════════════
        if (target == Target.PRODUCER_CONSUMER) {
            int convY = ph / 2 + 10;

            renderer.drawTorch(pw / 6, wallEnd - 20, animFrame);
            renderer.drawTorch(pw / 2, wallEnd - 22, animFrame + 5);
            renderer.drawTorch(pw * 5 / 6, wallEnd - 20, animFrame + 9);

            renderer.drawMiner(30, wallEnd + 6, (animFrame % 4 < 2) ? 1 : 2);
            renderer.drawMiner(pw / 3 + 8, wallEnd + 4, (animFrame % 5 < 2) ? 2 : 1);
            renderer.drawMiner(pw * 2 / 3 - 8, wallEnd + 6, (animFrame % 4 < 2) ? 1 : 2);
            renderer.drawMiner(pw - 42, wallEnd + 4, (animFrame % 6 < 3) ? 2 : 1);

            renderer.drawConveyor(8, convY, pw - 16, animFrame * 2);

            renderer.drawRobot(12, convY - 14, (animFrame % 8 < 4) ? 0 : 1, TEAL, DK_TEAL, LT_TEAL);
            renderer.drawRobot(pw - 24, convY - 14, (animFrame % 8 < 4) ? 1 : 0, ORANGE, DK_ORANGE, GOLD);

            int prodOff = (animFrame * 2) % (pw - 40);
            renderer.drawOreBlock(30 + prodOff % (pw - 60), convY - 5, LT_GREEN);
            renderer.drawOreBlock(60 + (prodOff + 25) % (pw - 60), convY - 5, LT_TEAL);
            renderer.drawOreBlock(90 + (prodOff + 50) % (pw - 60), convY - 5, GOLD);

            renderer.drawMiner(50, convY + 14, (animFrame % 6 < 3) ? 1 : 2);
            renderer.drawMiner(pw - 60, convY + 14, (animFrame % 5 < 2) ? 2 : 1);

            renderer.drawCrate(pw / 3, convY + 16);
            renderer.drawCrate(pw / 3 + 14, convY + 18);
            renderer.drawBarrel(pw * 2 / 3, convY + 16);
            renderer.drawBarrel(pw * 2 / 3 + 12, convY + 18);
        }

        // ══════════════════════════════════════
        //  DP: FULL-SCREEN DINING HALL
        // ══════════════════════════════════════
        if (target == Target.DINING_PHILOSOPHERS) {
            // More candelabras on the wall to light up the wide hall
            for (int i = 1; i <= 7; i++) {
                renderer.drawTorch(pw * i / 8, wallEnd - 16 + (i % 2) * 2, animFrame + i * 3);
            }

            // Define table X positions (3 tables across the room)
            int[] tableXs = { pw / 6, pw / 2, pw * 5 / 6 };
            int tY = wallEnd + (ph - wallEnd) / 2 - 10;

            // Background wandering philosophers (behind tables)
            renderer.drawPhilosopher(pw / 3, tY - 6 + (animFrame % 6 < 3 ? 0 : 2), 1, web("#507090"), web("#405070"), web("#202020"),
                    (animFrame % 7 < 3) ? -1 : 0);
            renderer.drawPhilosopher(pw * 2 / 3, tY - 4 + (animFrame % 8 < 4 ? 0 : 2), 0, web("#804040"), web("#603030"), web("#181818"),
                    (animFrame % 5 < 2) ? -1 : 0);

            // === DRAW 3 TABLES & 9 SEATED PHILOSOPHERS ===
            for (int i = 0; i < 3; i++) {
                int tX = tableXs[i];
                renderer.drawRoundTable(tX, tY, 26);

                // Philosopher A (top-left of table i)
                Color shirtA = (i == 0) ? web("#4060c0") : (i == 1 ? web("#a06040") : web("#6040a0"));
                Color shadeA = (i == 0) ? web("#304890") : (i == 1 ? web("#805030") : web("#503080"));
                renderer.drawPhilosopher(tX - 35, tY - 18, 0, shirtA, shadeA, DARK_BROWN,
                        (animFrame % (4 + i) < 2) ? 0 : 1);
                renderer.drawPlate(tX - 18, tY - 6, (i % 2 == 0));

                // Philosopher B (top-right of table i)
                Color shirtB = (i == 0) ? web("#c04040") : (i == 1 ? web("#40a060") : web("#c0a040"));
                Color shadeB = (i == 0) ? web("#903030") : (i == 1 ? web("#308040") : web("#908030"));
                renderer.drawPhilosopher(tX + 18, tY - 18, 1, shirtB, shadeB, web("#202020"),
                        (animFrame % (6 - i) < 3) ? -1 : 0);
                renderer.drawPlate(tX + 10, tY - 6, (i % 2 != 0));

                // Philosopher C (bottom center of table i)
                Color shirtC = (i == 0) ? web("#40a060") : (i == 1 ? web("#c08040") : web("#4080c0"));
                Color shadeC = (i == 0) ? web("#308040") : (i == 1 ? web("#906030") : web("#306090"));
                renderer.drawPhilosopher(tX - 10, tY + 20, 2, shirtC, shadeC, web("#181818"),
                        (animFrame % (5 + i) < 2) ? -1 : 1);
                renderer.drawPlate(tX - 4, tY + 14, true);
            }

            // Foreground philosophers (waiting / observing)
            renderer.drawPhilosopher(pw / 4 - 20, ph - 65 + (animFrame % 4 < 2 ? 0 : 2), 2, web("#808080"), web("#606060"), GOLD, 0);
            renderer.drawPhilosopher(pw * 3 / 4 + 10, ph - 60 + (animFrame % 5 < 2 ? 0 : 2), 1, web("#40c080"), web("#309060"), DARK_BROWN, -1);

            // Extra crates/barrels scattered densely in the corners and bottom
            renderer.drawBarrel(15, ph - 55);
            renderer.drawBarrel(28, ph - 53);
            renderer.drawCrate(pw - 28, ph - 55);
            renderer.drawCrate(pw - 42, ph - 53);
            renderer.drawBarrel(pw / 2 - 40, ph - 45);
            renderer.drawBarrel(pw / 2 + 30, ph - 48);
        }

        // ══════════════════════════════════════
        //  BIG TITLE BANNER (top center)
        // ══════════════════════════════════════
        String bigTitle, subtitle;
        if (target == Target.PRODUCER_CONSUMER) {
            bigTitle = "PIXEL FACTORY";
            subtitle = "SISTEMA DE PRODUCCION v2.1";
        } else {
            bigTitle = "PHILOSOPHER OS";
            subtitle = "SISTEMA DE SINCRONIZACION v1.0";
        }

        int titleW = bigTitle.length() * 9 + 20;
        int titleX = pw / 2 - titleW / 2;
        int titleY = 8;
        int bannerH = 26;

        renderer.fill(titleX - 2, titleY - 2, titleW + 4, bannerH + 4, accent);
        renderer.fill(titleX - 1, titleY - 1, titleW + 2, bannerH + 2, accentDark);
        renderer.fill(titleX, titleY, titleW, bannerH, web("#0c1018"));
        renderer.fill(titleX, titleY, 2, 2, accent);
        renderer.fill(titleX + titleW - 2, titleY, 2, 2, accent);
        renderer.fill(titleX, titleY + bannerH - 2, 2, 2, accent);
        renderer.fill(titleX + titleW - 2, titleY + bannerH - 2, 2, 2, accent);

        renderer.drawText(bigTitle, titleX + 10, titleY + 3, accentBright, 8);
        renderer.drawText(subtitle, titleX + 10, titleY + 15, accent, 5);

        if (animFrame % 6 < 3) {
            renderer.fill(titleX + 10 + subtitle.length() * 5 + 3, titleY + 16, 4, 5, accent);
        }

        // ══════════════════════════════════════
        //  BOOT MESSAGES — single current message above bar
        // ══════════════════════════════════════
        String[] messages = (target == Target.PRODUCER_CONSUMER) ? PC_MESSAGES : DP_MESSAGES;
        int visibleMessages = Math.min((int) (progress * messages.length + 1), messages.length);
        int latest = Math.min(visibleMessages, messages.length) - 1;
        if (latest >= 0) {
            Color msgColor = (animFrame % 4 < 2) ? accent : accentBright;
            renderer.drawText(messages[latest], 12, ph - 30, msgColor, 4);
        }

        // ══════════════════════════════════════
        //  PROGRESS BAR (bottom)
        // ══════════════════════════════════════
        int barX = 10;
        int barY = ph - 18;
        int barW = pw - 20;
        int barH = 10;

        renderer.fill(barX - 2, barY - 2, barW + 4, barH + 4, accent);
        renderer.fill(barX - 1, barY - 1, barW + 2, barH + 2, accentDark);
        renderer.fill(barX, barY, barW, barH, web("#141820"));

        int filledW = (int) (barW * progress);
        for (int sx = 0; sx < filledW; sx += 3) {
            int segW = Math.min(2, filledW - sx);
            float segBrightness = 0.85f + 0.15f * ((float) Math.sin(sx * 0.3 + animFrame * 0.5) * 0.5f + 0.5f);
            Color segColor = new Color(
                    Math.min(barFill.r * segBrightness, 1f),
                    Math.min(barFill.g * segBrightness, 1f),
                    Math.min(barFill.b * segBrightness, 1f),
                    1f);
            renderer.fill(barX + sx, barY + 1, segW, barH - 2, segColor);
        }
        if (filledW > 1) {
            renderer.fill(barX + filledW - 1, barY, 2, barH, accentBright);
        }

        int pct = (int) (progress * 100);
        String pctText = pct + "%";
        renderer.drawText(pctText, barX + barW / 2 - pctText.length() * 3, barY - 8, accentBright, 5);

        int dots = (animFrame / 3) % 4;
        StringBuilder sb = new StringBuilder("CARGANDO");
        for (int d = 0; d < dots; d++) sb.append('.');
        renderer.drawText(sb.toString(), barX, barY - 8, accent, 4);

        // ── DYNAMIC LIGHTS (white only to avoid artifacts) ──
        renderer.setAmbientLight(0.30f, 0.28f, 0.25f, 1f);
        renderer.addPointLight(pw / 2f, ph / 2f, 100, WHITE, 0.2f);
    }

    @Override
    public void resize(int width, int height) {
        // no stage to resize
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        if (renderer != null) renderer.dispose();
        if (postFx != null) postFx.dispose();
    }
}
