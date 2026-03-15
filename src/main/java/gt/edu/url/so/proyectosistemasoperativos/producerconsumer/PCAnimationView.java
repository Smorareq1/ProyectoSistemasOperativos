package gt.edu.url.so.proyectosistemasoperativos.producerconsumer;

import gt.edu.url.so.proyectosistemasoperativos.common.LogPanel;
import gt.edu.url.so.proyectosistemasoperativos.common.PixelGameCanvas;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.List;

import static gt.edu.url.so.proyectosistemasoperativos.common.PixelGameCanvas.*;

public class PCAnimationView extends BorderPane {

    // ── Pixel Art Color Scheme ──
    private static final Color COLOR_PAR   = TEAL;
    private static final Color COLOR_IMPAR = GREEN;
    private static final Color COLOR_PRIMO = ORANGE;

    private final PCController controller;
    private final LogPanel logPanel;
    private PixelGameCanvas canvas;

    // ── Animation state ──
    private int animFrame = 0;
    private int minerState = 0;  // 0=idle, 1=mining-up, 2=mining-down, 3=blocked, 4=done
    private int[] robotStates = {0, 0, 0}; // 0=idle, 1=processing, 2=searching, 3=done
    private int lastMinedNumber = -1;

    // ── Info panel labels ──
    private final Label minerStatusLabel = new Label("IDLE");
    private final Label minerNumberLabel = new Label("-");
    private final Label bufferCountLabel = new Label("0/12");
    private final ProgressBar bufferProgress = new ProgressBar(0);
    private final Label[] robotNameLabels = new Label[3];
    private final Label[] robotScoreLabels = new Label[3];
    private final Label[] robotStatusLabels = new Label[3];

    private static final int BUFFER_SIZE = 12;
    private Timeline updateTimeline;
    private AnimationTimer gameLoop;
    private boolean paused = false;

    // ── Canvas pixel grid (240 x 128 at scale 3 = 720x384 screen) ──
    private static final int CW = 720, CH = 384;

    public PCAnimationView(Runnable onBack) {
        logPanel = new LogPanel();
        controller = new PCController(logPanel);
        getStyleClass().add("pc-root");
        setupCallbacks();
        buildUI(onBack);
        startGameLoop();
        startBufferUpdateLoop();
    }

    // ═══════════════════════════════════════
    //  CALLBACKS
    // ═══════════════════════════════════════
    private void setupCallbacks() {
        controller.setProducerCallbacks(
                estado -> Platform.runLater(() -> {
                    switch (estado) {
                        case "ACTIVO"    -> { minerState = 1; minerStatusLabel.setText("MINING..."); }
                        case "BLOQUEADO" -> { minerState = 3; minerStatusLabel.setText("BELT FULL!"); }
                        case "TERMINADO" -> { minerState = 4; minerStatusLabel.setText("DONE"); }
                    }
                }),
                numero -> Platform.runLater(() -> {
                    lastMinedNumber = numero;
                    minerNumberLabel.setText(String.valueOf(numero));
                })
        );
        controller.setConsumerParCallbacks(
                estado -> Platform.runLater(() -> updateRobotState(0, estado)),
                (num, suma) -> Platform.runLater(() -> robotScoreLabels[0].setText(String.valueOf(suma)))
        );
        controller.setConsumerImparCallbacks(
                estado -> Platform.runLater(() -> updateRobotState(1, estado)),
                (num, suma) -> Platform.runLater(() -> robotScoreLabels[1].setText(String.valueOf(suma)))
        );
        controller.setConsumerPrimoCallbacks(
                estado -> Platform.runLater(() -> updateRobotState(2, estado)),
                (num, suma) -> Platform.runLater(() -> robotScoreLabels[2].setText(String.valueOf(suma)))
        );
    }

    private void updateRobotState(int idx, String estado) {
        switch (estado) {
            case "ACTIVO"    -> { robotStates[idx] = 1; robotStatusLabels[idx].setText("CONSUMING"); }
            case "BLOQUEADO" -> { robotStates[idx] = 2; robotStatusLabels[idx].setText("SEARCHING"); }
            case "TERMINADO" -> { robotStates[idx] = 3; robotStatusLabels[idx].setText("COMPLETE"); }
        }
    }

    // ═══════════════════════════════════════
    //  BUILD UI
    // ═══════════════════════════════════════
    private void buildUI(Runnable onBack) {
        // ── TOP: Controls ──
        Button backBtn = new Button("\u25C0 BACK");
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> { cleanup(); onBack.run(); });

        Label title = new Label("\uD83C\uDFED  PIXEL FACTORY  \uD83C\uDFED");
        title.getStyleClass().add("title-label");

        HBox topBar = new HBox(14, backBtn, title);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("pc-top-bar");

        Button playBtn  = new Button("\u25B6 START");
        Button pauseBtn = new Button("\u23F8 PAUSE");
        Button stopBtn  = new Button("\u23F9 STOP");
        playBtn.getStyleClass().add("control-button");
        pauseBtn.getStyleClass().add("control-button");
        stopBtn.getStyleClass().add("control-button");

        Label speedLabel = new Label("\u26A1 SPD:");
        speedLabel.getStyleClass().add("speed-label");
        Slider speedSlider = new Slider(50, 2000, 500);
        speedSlider.setShowTickLabels(true);
        speedSlider.setPrefWidth(150);
        speedSlider.valueProperty().addListener((obs, o, n) -> controller.setDelay(n.intValue()));

        HBox controls = new HBox(10, playBtn, pauseBtn, stopBtn, speedLabel, speedSlider);
        controls.setAlignment(Pos.CENTER);
        controls.getStyleClass().add("pc-controls");

        playBtn.setOnAction(e -> {
            if (!controller.isRunning()) {
                controller.setDelay((int) speedSlider.getValue());
                controller.iniciar();
            } else if (paused) { controller.reanudar(); paused = false; }
        });
        pauseBtn.setOnAction(e -> {
            if (controller.isRunning() && !paused) { controller.pausar(); paused = true; }
        });
        stopBtn.setOnAction(e -> { controller.detener(); paused = false; });

        VBox topSection = new VBox(0, topBar, controls);
        setTop(topSection);

        // ── CENTER: Canvas + Info Panel ──
        canvas = new PixelGameCanvas(CW, CH, 3);

        VBox infoPanel = buildInfoPanel();

        ScrollPane canvasScroll = new ScrollPane(canvas);
        canvasScroll.getStyleClass().add("scroll-pane");
        canvasScroll.setFitToHeight(true);

        HBox centerContent = new HBox(8, canvasScroll, infoPanel);
        centerContent.setAlignment(Pos.TOP_CENTER);
        centerContent.setPadding(new Insets(4, 8, 4, 8));
        setCenter(centerContent);

        // ── BOTTOM: Log ──
        VBox bottomSection = new VBox(logPanel);
        bottomSection.setPadding(new Insets(3, 10, 6, 10));
        setBottom(bottomSection);
    }

    private VBox buildInfoPanel() {
        VBox panel = new VBox(6);
        panel.setPadding(new Insets(8));
        panel.setPrefWidth(220);
        panel.setMinWidth(200);
        panel.setStyle("-fx-background-color: linear-gradient(to bottom, #3a2818, #2a1e14); -fx-border-color: #6b4c38 #1a1008 #1a1008 #6b4c38; -fx-border-width: 3; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 6, 0, -2, 0);");

        // Miner section
        Label minerTitle = new Label("\u26CF MINER");
        minerTitle.getStyleClass().add("section-title");
        minerStatusLabel.getStyleClass().add("small-label");

        Label numTitle = new Label("MINED BLOCK:");
        numTitle.getStyleClass().add("small-label");
        minerNumberLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #f0c040; -fx-font-family: 'Press Start 2P';");

        // Buffer section
        Label bufTitle = new Label("\u2699 BUFFER");
        bufTitle.getStyleClass().add("section-title");
        bufferProgress.setPrefWidth(190);
        bufferCountLabel.getStyleClass().add("small-label");

        // Legend
        Label legendTitle = new Label("LEGEND:");
        legendTitle.getStyleClass().add("small-label");
        Label legEven  = new Label("\u25A0 EVEN");
        Label legOdd   = new Label("\u25A0 ODD");
        Label legPrime = new Label("\u25A0 PRIME");
        legEven.setTextFill(COLOR_PAR);   legEven.getStyleClass().add("legend-text");
        legOdd.setTextFill(COLOR_IMPAR);  legOdd.getStyleClass().add("legend-text");
        legPrime.setTextFill(COLOR_PRIMO); legPrime.getStyleClass().add("legend-text");
        HBox legend = new HBox(8, legEven, legOdd, legPrime);

        // Robots section
        Label robTitle = new Label("\uD83E\uDD16 ROBOTS");
        robTitle.getStyleClass().add("section-title");

        String[] names  = {"BLUE [EVEN]", "GREEN [ODD]", "GOLD [PRIME]"};
        Color[]  colors = {COLOR_PAR, COLOR_IMPAR, COLOR_PRIMO};
        VBox robotsInfo = new VBox(4);
        for (int i = 0; i < 3; i++) {
            robotNameLabels[i] = new Label(names[i]);
            robotNameLabels[i].setTextFill(colors[i]);
            robotNameLabels[i].getStyleClass().add("legend-text");
            robotScoreLabels[i] = new Label("0");
            robotScoreLabels[i].setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #f0dcc0; -fx-font-family: 'Press Start 2P';");
            robotStatusLabels[i] = new Label("IDLE");
            robotStatusLabels[i].getStyleClass().add("small-label");

            HBox scoreRow = new HBox(6, new Label("SUM:"), robotScoreLabels[i]);
            scoreRow.setAlignment(Pos.CENTER_LEFT);

            VBox robotCard = new VBox(2, robotNameLabels[i], scoreRow, robotStatusLabels[i]);
            robotCard.setPadding(new Insets(4));
            robotCard.setStyle("-fx-background-color: #fff8e8; -fx-border-color: " + colorToHex(colors[i]) + "; -fx-border-width: 2;");
            robotsInfo.getChildren().add(robotCard);
        }

        panel.getChildren().addAll(
                minerTitle, minerStatusLabel, numTitle, minerNumberLabel,
                new Separator(), bufTitle, bufferProgress, bufferCountLabel, legend,
                new Separator(), robTitle, robotsInfo
        );
        return panel;
    }

    // ═══════════════════════════════════════
    //  GAME LOOP - Canvas Rendering
    // ═══════════════════════════════════════
    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            long last = 0;
            @Override
            public void handle(long now) {
                if (now - last >= 100_000_000L) { // ~10 fps for pixel art feel
                    last = now;
                    animFrame++;
                    renderScene();
                }
            }
        };
        gameLoop.start();
    }

    private void renderScene() {
        int pw = canvas.pxW(); // 240
        int ph = canvas.pxH(); // 128

        canvas.clear(BG_CREAM);

        // ── Background ──
        // Brick wall (top)
        canvas.drawBrickWall(0, 0, pw, 30);
        // Wall-floor border
        canvas.fill(0, 30, pw, 2, DARK_BROWN);
        canvas.drawWarningStripes(0, 29, pw);
        // Stone floor (bottom)
        canvas.drawStoneFloor(0, 100, pw, 28);
        // Factory floor
        canvas.fill(0, 32, pw, 68, Color.web("#d8c8a0"));

        // ── Wall decorations ──
        canvas.drawTorch(8, 8, animFrame);
        canvas.drawTorch(228, 8, animFrame);
        canvas.drawGauge(50, 10, (animFrame % 30) / 30.0);
        canvas.drawGauge(180, 10, 0.7);
        canvas.drawPipeH(60, 22, 120);

        // Sign on wall
        canvas.fill(85, 4, 70, 12, WOOD2);
        canvas.fill(86, 5, 68, 10, WOOD1);
        canvas.fill(87, 6, 66, 8, DK_RED);
        canvas.fill(87, 6, 66, 2, RED);
        canvas.drawText("PIXEL FACTORY", 30, 3.5, WHITE, 7);

        // ── Mine entrance (left) ──
        canvas.drawMineEntrance(5, 34);

        // ── Miner character ──
        int mFrame = minerState;
        if (minerState == 1) {
            // Alternate between mining up (1) and down (2)
            mFrame = (animFrame % 4 < 2) ? 1 : 2;
        }
        canvas.drawMiner(18, 50, mFrame);

        // Sparks when mining
        if (minerState == 1) {
            canvas.drawSpark(32, 58, animFrame, GOLD);
            canvas.drawSpark(34, 56, animFrame + 2, ORANGE);
        }

        // Mined number display above miner
        if (lastMinedNumber >= 0 && minerState != 4) {
            TipoNumero tipo = NumberClassifier.clasificar(lastMinedNumber);
            Color numColor = switch (tipo) {
                case PAR   -> COLOR_PAR;
                case IMPAR -> COLOR_IMPAR;
                case PRIMO -> COLOR_PRIMO;
            };
            canvas.drawOreBlock(18, 48, numColor);
            canvas.drawText(String.valueOf(lastMinedNumber), 8.5, 16.5, WHITE, 6);
        }

        // ── Pipe from mine to conveyor ──
        canvas.drawPipeH(35, 65, 25);
        // Animated arrows on pipe
        int arrowOff = animFrame % 8;
        for (int i = 0; i < 3; i++) {
            int ax = 40 + i * 6 + arrowOff;
            if (ax < 58) {
                canvas.dot(ax, 66, GOLD);
                canvas.dot(ax + 1, 67, GOLD);
            }
        }

        // ── Conveyor belt ──
        canvas.drawConveyor(60, 57, 110, animFrame);

        // ── Buffer items on belt ──
        drawBufferOnCanvas();

        // ── Pipe from conveyor to robots ──
        canvas.drawPipeH(170, 65, 20);
        for (int i = 0; i < 3; i++) {
            int ax = 175 + i * 5 + (animFrame % 6);
            if (ax < 188) {
                canvas.dot(ax, 66, GOLD);
                canvas.dot(ax + 1, 67, GOLD);
            }
        }

        // ── Robot station ──
        // Station background
        canvas.fill(190, 33, 45, 67, Color.web("#e0d0b0"));
        canvas.fill(190, 33, 45, 2, BROWN);
        canvas.fill(190, 33, 1, 67, BROWN);
        canvas.fill(234, 33, 1, 67, BROWN);

        // Draw 3 robots
        Color[][] robotColors = {
            {TEAL, DK_TEAL, LT_TEAL},
            {GREEN, DK_GREEN, LT_GREEN},
            {ORANGE, DK_ORANGE, GOLD}
        };
        for (int i = 0; i < 3; i++) {
            int rx = 198, ry = 35 + i * 25;
            canvas.drawRobot(rx, ry, robotStates[i], robotColors[i][0], robotColors[i][1], robotColors[i][2]);

            // Robot label
            canvas.drawText(switch (i) {
                case 0 -> "PAR";
                case 1 -> "IMP";
                default -> "PRI";
            }, 71, 13.5 + i * 7.3, robotColors[i][0], 5);
        }

        // ── Floor decorations ──
        canvas.drawCrate(3, 90);
        canvas.drawCrate(10, 92);
        canvas.drawBarrel(230, 88);
        canvas.drawBarrel(222, 90);
        canvas.drawCrate(215, 93);

        // ── Smoke/steam from factory ──
        canvas.drawSmoke(100, 28, animFrame);
        canvas.drawSmoke(140, 26, animFrame + 4);
    }

    private void drawBufferOnCanvas() {
        if (controller.getBuffer() == null) return;
        List<Integer> snapshot = controller.getBuffer().getSnapshot();

        for (int i = 0; i < BUFFER_SIZE && i < snapshot.size(); i++) {
            int num = snapshot.get(i);
            TipoNumero tipo = NumberClassifier.clasificar(num);
            Color color = switch (tipo) {
                case PAR   -> COLOR_PAR;
                case IMPAR -> COLOR_IMPAR;
                case PRIMO -> COLOR_PRIMO;
            };
            // Position on conveyor: 2 rows x 6 cols
            int col = i % 6;
            int row = i / 6;
            int bx = 64 + col * 16;
            int by = 50 + row * 9;
            canvas.drawOreBlock(bx, by, color);
            canvas.drawText(String.valueOf(num), (bx + 1.0) / 1, (by + 0.5) / 1, WHITE, 4);
        }
    }

    // ═══════════════════════════════════════
    //  BUFFER UPDATE LOOP (separate from render)
    // ═══════════════════════════════════════
    private void startBufferUpdateLoop() {
        updateTimeline = new Timeline(new KeyFrame(Duration.millis(100), e -> updateBufferInfo()));
        updateTimeline.setCycleCount(Animation.INDEFINITE);
        updateTimeline.play();
    }

    private void updateBufferInfo() {
        if (controller.getBuffer() == null) return;
        List<Integer> snapshot = controller.getBuffer().getSnapshot();
        int count = snapshot.size();
        bufferProgress.setProgress((double) count / BUFFER_SIZE);
        bufferCountLabel.setText(count + "/" + BUFFER_SIZE);
    }

    // ═══════════════════════════════════════
    //  UTILITY
    // ═══════════════════════════════════════
    private static String colorToHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }

    public void cleanup() {
        controller.detener();
        if (updateTimeline != null) updateTimeline.stop();
        if (gameLoop != null) gameLoop.stop();
    }
}
