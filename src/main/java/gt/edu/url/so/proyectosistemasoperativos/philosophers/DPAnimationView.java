package gt.edu.url.so.proyectosistemasoperativos.philosophers;

import gt.edu.url.so.proyectosistemasoperativos.common.LogPanel;
import gt.edu.url.so.proyectosistemasoperativos.common.PixelGameCanvas;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import static gt.edu.url.so.proyectosistemasoperativos.common.PixelGameCanvas.*;

public class DPAnimationView extends BorderPane {

    // ── Philosopher robe colors ──
    private static final Color[] ROBE_COLORS = {
        Color.web("#4060c0"), Color.web("#c04040"), Color.web("#40a040"),
        Color.web("#c08040"), Color.web("#8040a0"), Color.web("#40a0a0"),
        Color.web("#a06040"), Color.web("#6080c0"), Color.web("#c06080"),
        Color.web("#80a040")
    };
    private static final Color[] ROBE_DARK = {
        Color.web("#304890"), Color.web("#903030"), Color.web("#307830"),
        Color.web("#906030"), Color.web("#603078"), Color.web("#307878"),
        Color.web("#784830"), Color.web("#486090"), Color.web("#904860"),
        Color.web("#607830")
    };
    private static final Color[] HAIR_COLORS = {
        DARK_BROWN, Color.web("#202020"), Color.web("#c89060"),
        Color.web("#909090"), Color.web("#402820"), DARK_BROWN,
        Color.web("#202020"), Color.web("#c89060"), Color.web("#402820"),
        Color.web("#909090")
    };

    private final LogPanel logPanel;
    private PhilosopherConfig config;
    private DPController controller;
    private Philosopher[] philosophers;

    // ── Canvas ──
    private PixelGameCanvas canvas;
    private AnimationTimer gameLoop;
    private int animFrame = 0;

    // ── State ──
    private boolean running = false;
    private boolean paused = false;

    // ── Info panel ──
    private Label[] stateInfoLabels;
    private Label[] forkInfoLabels;
    private VBox statePanel;

    // Canvas dimensions
    private static final int CW = 570, CH = 420;

    public DPAnimationView(Runnable onBack) {
        logPanel = new LogPanel();
        config = new PhilosopherConfig();
        getStyleClass().add("dp-root");
        buildUI(onBack);
        startGameLoop();
    }

    // ═══════════════════════════════════════
    //  BUILD UI
    // ═══════════════════════════════════════
    private void buildUI(Runnable onBack) {
        // ── TOP ──
        Button backBtn = new Button("\u25C0 Menu");
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> { cleanup(); onBack.run(); });

        Label title = new Label("\uD83C\uDF5D  CENA DE FILOSOFOS  \uD83C\uDF5D");
        title.getStyleClass().add("title-label");

        HBox topBar = new HBox(20, backBtn, title);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("dp-top-bar");

        // Controls
        Label nLabel = new Label("N:");
        nLabel.getStyleClass().add("config-label");
        Spinner<Integer> nSpinner = new Spinner<>(2, 10, config.getNumFilosofos());
        nSpinner.setPrefWidth(70);
        nSpinner.valueProperty().addListener((obs, o, n) -> {
            if (!running) { config.setNumFilosofos(n); rebuildStatePanel(); }
        });

        Button playBtn = new Button("\u25B6 Play");
        Button pauseBtn = new Button("\u23F8 Pausa");
        Button stopBtn = new Button("\u23F9 Stop");
        playBtn.getStyleClass().add("control-button");
        pauseBtn.getStyleClass().add("control-button");
        stopBtn.getStyleClass().add("control-button");

        Label speedLabel = new Label("\u26A1 Vel:");
        speedLabel.getStyleClass().add("speed-label");
        Slider speedSlider = new Slider(100, 5000, 1000);
        speedSlider.setShowTickLabels(true);
        speedSlider.setPrefWidth(160);
        speedSlider.valueProperty().addListener((obs, o, n) -> {
            int val = n.intValue();
            config.setTiempoMinPensar(val / 2);
            config.setTiempoMaxPensar(val * 2);
            config.setTiempoMinComer(val / 2);
            config.setTiempoMaxComer((int) (val * 1.5));
        });

        HBox controls = new HBox(10, nLabel, nSpinner, playBtn, pauseBtn, stopBtn, speedLabel, speedSlider);
        controls.setAlignment(Pos.CENTER);
        controls.getStyleClass().add("dp-controls");

        playBtn.setOnAction(e -> {
            if (!running) { nSpinner.setDisable(true); iniciarSimulacion(); }
            else if (paused) { reanudarSimulacion(); }
        });
        pauseBtn.setOnAction(e -> { if (running && !paused) pausarSimulacion(); });
        stopBtn.setOnAction(e -> { nSpinner.setDisable(false); detenerSimulacion(); });

        VBox topSection = new VBox(0, topBar, controls);
        setTop(topSection);

        // ── CENTER ──
        canvas = new PixelGameCanvas(CW, CH, 3);

        statePanel = new VBox(4);
        statePanel.setPadding(new Insets(8));
        statePanel.setPrefWidth(270);
        statePanel.setMinWidth(240);
        statePanel.setStyle("-fx-background-color: #f0dcc0; -fx-border-color: #8b2018; -fx-border-width: 4;");

        rebuildStatePanel();

        // Legend
        HBox legend = new HBox(8);
        legend.setAlignment(Pos.CENTER);
        Label lThink = new Label("\uD83D\uDFEA Pensando");
        lThink.setTextFill(PLUM);   lThink.getStyleClass().add("legend-text");
        Label lWait  = new Label("\uD83D\uDFE0 Esperando");
        lWait.setTextFill(ORANGE);  lWait.getStyleClass().add("legend-text");
        Label lEat   = new Label("\uD83D\uDFE2 Comiendo");
        lEat.setTextFill(GREEN);    lEat.getStyleClass().add("legend-text");
        legend.getChildren().addAll(lThink, lWait, lEat);

        ScrollPane sp = new ScrollPane(statePanel);
        sp.getStyleClass().add("scroll-pane");
        sp.setFitToWidth(true);
        sp.setPrefHeight(CH);

        VBox rightSide = new VBox(6, legend, sp);
        rightSide.setAlignment(Pos.TOP_CENTER);

        HBox centerContent = new HBox(8, canvas, rightSide);
        centerContent.setAlignment(Pos.TOP_CENTER);
        centerContent.setPadding(new Insets(4, 8, 4, 8));
        setCenter(centerContent);

        // ── BOTTOM ──
        VBox bottomSection = new VBox(logPanel);
        bottomSection.setPadding(new Insets(5, 10, 10, 10));
        setBottom(bottomSection);
    }

    private void rebuildStatePanel() {
        statePanel.getChildren().clear();
        int N = config.getNumFilosofos();
        stateInfoLabels = new Label[N];
        forkInfoLabels = new Label[N];

        Label stTitle = new Label("\uD83E\uDDD1 Filosofos");
        stTitle.getStyleClass().add("section-title");
        statePanel.getChildren().add(stTitle);

        for (int i = 0; i < N; i++) {
            stateInfoLabels[i] = new Label("F" + i + ": \uD83D\uDCA4 PENSANDO");
            stateInfoLabels[i].setStyle("-fx-font-size: 7; -fx-font-family: 'Press Start 2P'; -fx-text-fill: #6858a0;");
            statePanel.getChildren().add(stateInfoLabels[i]);
        }

        Label ftTitle = new Label("\uD83C\uDF74 Tenedores");
        ftTitle.getStyleClass().add("section-title");
        statePanel.getChildren().add(ftTitle);

        for (int i = 0; i < N; i++) {
            forkInfoLabels[i] = new Label("T" + i + ": \u2705 libre");
            forkInfoLabels[i].setStyle("-fx-font-size: 7; -fx-font-family: 'Press Start 2P'; -fx-text-fill: #8b7355;");
            statePanel.getChildren().add(forkInfoLabels[i]);
        }
    }

    // ═══════════════════════════════════════
    //  GAME LOOP - Canvas Rendering
    // ═══════════════════════════════════════
    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            long last = 0;
            @Override
            public void handle(long now) {
                if (now - last >= 100_000_000L) {
                    last = now;
                    animFrame++;
                    renderScene();
                    if (running) updateStateFromController();
                }
            }
        };
        gameLoop.start();
    }

    private void renderScene() {
        int pw = canvas.pxW(); // ~190

        canvas.clear(BG_CREAM);

        // ── Background ──
        // Brick wall top
        canvas.drawBrickWall(0, 0, pw, 28);
        canvas.fill(0, 28, pw, 2, DARK_BROWN);

        // Wall decorations
        canvas.drawTorch(8, 6, animFrame);
        canvas.drawTorch(pw - 12, 6, animFrame);
        canvas.drawWindow(30, 6);
        canvas.drawWindow(pw - 42, 6);
        canvas.drawPainting(70, 8, BROWN, Color.web("#4080a0"), Color.web("#60a060"));
        canvas.drawShelf(pw / 2 - 6, 10);

        // Sign
        canvas.fill(pw / 2 - 30, 2, 60, 10, WOOD2);
        canvas.fill(pw / 2 - 29, 3, 58, 8, WOOD1);
        canvas.fill(pw / 2 - 28, 4, 56, 6, DK_RED);
        canvas.drawText("TAVERNA", pw / 6.0 + 2, 2, WHITE, 6);

        // Wood floor
        canvas.drawWoodFloor(0, 30, pw, 96);

        // Stone floor base
        canvas.drawStoneFloor(0, 126, pw, 14);
        canvas.fill(0, 125, pw, 1, DARK_BROWN);

        // ── Fireplace ──
        canvas.drawFireplace(pw / 2 - 8, 16, animFrame);

        // ── Table ──
        int cx = pw / 2;
        int cy = 78;
        int tableR = 22;
        canvas.drawRoundTable(cx, cy, tableR);

        // ── Philosophers around table ──
        int N = config.getNumFilosofos();
        double philR = tableR + 22; // distance from center to philosopher

        for (int i = 0; i < N; i++) {
            double angle = 2 * Math.PI * i / N - Math.PI / 2;
            int px = cx + (int)(philR * Math.cos(angle)) - 5;
            int py = cy + (int)(philR * Math.sin(angle)) - 6;

            // Determine state from controller
            int philFrame = 0; // thinking
            if (running && controller != null) {
                int[] snapshot = controller.getSnapshot();
                if (snapshot != null && i < N) {
                    EstadoFilosofo estado = EstadoFilosofo.values()[snapshot[i]];
                    philFrame = switch (estado) {
                        case PENSANDO  -> 0;
                        case ESPERANDO -> 2;
                        case COMIENDO  -> 1;
                    };
                }
            }

            int bobY = (philFrame == 2) ? (animFrame % 4 < 2 ? 0 : -1) : 0;

            canvas.drawPhilosopher(px, py, philFrame,
                    ROBE_COLORS[i % ROBE_COLORS.length],
                    ROBE_DARK[i % ROBE_DARK.length],
                    HAIR_COLORS[i % HAIR_COLORS.length],
                    bobY);

            // Label
            canvas.drawText("F" + i, (px + 3.0), (py + 14.0), DARK_BROWN, 4);

            // Plate in front of philosopher (between phil and table)
            double plateR = tableR + 8;
            int platX = cx + (int)(plateR * Math.cos(angle)) - 3;
            int platY = cy + (int)(plateR * Math.sin(angle)) - 1;
            canvas.drawPlate(platX, platY, philFrame == 1);

            // Fork between this philosopher and the next
            double forkAngle = 2 * Math.PI * (i + 0.5) / N - Math.PI / 2;
            double forkR = tableR + 4;
            int fkx = cx + (int)(forkR * Math.cos(forkAngle));
            int fky = cy + (int)(forkR * Math.sin(forkAngle));

            boolean forkFree = true;
            if (running && controller != null) {
                int[] snapshot = controller.getSnapshot();
                if (snapshot != null) {
                    int forkHolder = snapshot[N + i];
                    forkFree = (forkHolder < 0);
                }
            }
            canvas.drawForkItem(fkx, fky, forkFree);
            if (!forkFree) {
                canvas.dot(fkx, fky, RED);
                canvas.dot(fkx, fky + 1, RED);
            }
        }

        // ── Floor decorations ──
        canvas.drawBarrel(5, 110);
        canvas.drawBarrel(12, 112);
        canvas.drawCrate(pw - 12, 112);
        canvas.drawCrate(pw - 20, 110);
    }

    private void updateStateFromController() {
        if (controller == null) return;
        int N = config.getNumFilosofos();
        int[] snapshot = controller.getSnapshot();
        if (snapshot == null) return;

        Platform.runLater(() -> {
            for (int i = 0; i < N; i++) {
                EstadoFilosofo estado = EstadoFilosofo.values()[snapshot[i]];
                int forkHolder = snapshot[N + i];

                switch (estado) {
                    case PENSANDO -> {
                        stateInfoLabels[i].setText("F" + i + ": \uD83D\uDCA4 PENSANDO");
                        stateInfoLabels[i].setStyle("-fx-font-size: 7; -fx-font-family: 'Press Start 2P'; -fx-text-fill: #6858a0;");
                    }
                    case ESPERANDO -> {
                        stateInfoLabels[i].setText("F" + i + ": \u23F3 ESPERANDO");
                        stateInfoLabels[i].setStyle("-fx-font-size: 7; -fx-font-family: 'Press Start 2P'; -fx-text-fill: #e8682a;");
                    }
                    case COMIENDO -> {
                        stateInfoLabels[i].setText("F" + i + ": \uD83C\uDF5D COMIENDO");
                        stateInfoLabels[i].setStyle("-fx-font-size: 7; -fx-font-family: 'Press Start 2P'; -fx-text-fill: #4a8820;");
                    }
                }

                if (forkHolder >= 0) {
                    forkInfoLabels[i].setText("T" + i + ": \u26D4 F" + forkHolder);
                    forkInfoLabels[i].setStyle("-fx-font-size: 7; -fx-font-family: 'Press Start 2P'; -fx-text-fill: #c83830; -fx-font-weight: bold;");
                } else {
                    forkInfoLabels[i].setText("T" + i + ": \u2705 libre");
                    forkInfoLabels[i].setStyle("-fx-font-size: 7; -fx-font-family: 'Press Start 2P'; -fx-text-fill: #8b7355;");
                }
            }
        });
    }

    // ═══════════════════════════════════════
    //  SIMULATION CONTROL
    // ═══════════════════════════════════════
    private void iniciarSimulacion() {
        int N = config.getNumFilosofos();
        controller = new DPController(N);
        philosophers = new Philosopher[N];
        running = true;
        paused = false;

        for (int i = 0; i < N; i++) {
            philosophers[i] = new Philosopher(i, controller, logPanel, config);
            philosophers[i].start();
        }
        logPanel.log("\uD83C\uDF5D Simulacion iniciada (N=" + N + ")");
    }

    private void pausarSimulacion() {
        paused = true;
        if (philosophers != null) for (Philosopher p : philosophers) p.pausar();
        logPanel.log("\u23F8 Simulacion pausada");
    }

    private void reanudarSimulacion() {
        paused = false;
        if (philosophers != null) for (Philosopher p : philosophers) p.reanudar();
        logPanel.log("\u25B6 Simulacion reanudada");
    }

    private void detenerSimulacion() {
        running = false;
        paused = false;
        if (philosophers != null) for (Philosopher p : philosophers) p.detener();
        logPanel.log("\u23F9 Simulacion detenida");
        rebuildStatePanel();
    }

    public void cleanup() {
        detenerSimulacion();
        if (gameLoop != null) gameLoop.stop();
    }
}
