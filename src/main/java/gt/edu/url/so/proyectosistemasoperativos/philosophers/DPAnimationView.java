package gt.edu.url.so.proyectosistemasoperativos.philosophers;

import gt.edu.url.so.proyectosistemasoperativos.common.AnimationUtils;
import gt.edu.url.so.proyectosistemasoperativos.common.LogPanel;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class DPAnimationView extends BorderPane {

    private static final Color COLOR_THINKING    = Color.web("#7f5af0");
    private static final Color COLOR_WAITING     = Color.web("#ff8906");
    private static final Color COLOR_EATING      = Color.web("#2cb67d");
    private static final Color COLOR_TABLE       = Color.web("#1a1a2e");
    private static final Color COLOR_TABLE_BORDER= Color.web("#ff8906");

    private static final String[] PHIL_EMOJIS = {
        "\uD83E\uDDD4", "\uD83D\uDC68\u200D\uD83C\uDF93", "\uD83D\uDC69\u200D\uD83C\uDF93",
        "\uD83E\uDDD3", "\uD83E\uDDD1\u200D\uD83D\uDCBB", "\uD83D\uDC68\u200D\uD83D\uDD2C",
        "\uD83D\uDC69\u200D\uD83D\uDD2C", "\uD83E\uDDD1\u200D\uD83C\uDFEB", "\uD83D\uDC68\u200D\u2696\uFE0F",
        "\uD83D\uDC69\u200D\u2696\uFE0F"
    };

    private final LogPanel logPanel;
    private PhilosopherConfig config;
    private DPController controller;
    private Philosopher[] philosophers;

    // Visual elements on the Pane
    private Label[] philEmojiLabels;
    private Label[] philNameLabels;
    private Label[] philStateLabels;
    private Label[] plateLabels;
    private Label[] forkEmojiLabels;
    private FadeTransition[] philPulses;

    // State panel (right side)
    private Label[] stateInfoLabels;
    private Label[] forkInfoLabels;

    private Timeline updateTimeline;
    private boolean running = false;
    private boolean paused = false;

    private Pane tablePane;
    private VBox statePanel;

    public DPAnimationView(Runnable onBack) {
        logPanel = new LogPanel();
        config = new PhilosopherConfig();
        getStyleClass().add("dp-root");
        buildUI(onBack);
    }

    private void buildUI(Runnable onBack) {
        // ---- TOP BAR ----
        Button backBtn = new Button("\u25C0 Menu");
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> { cleanup(); onBack.run(); });

        Label title = new Label("\uD83C\uDF5D  CENA DE FILOSOFOS  \uD83C\uDF5D");
        title.getStyleClass().add("title-label");

        HBox topBar = new HBox(20, backBtn, title);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("dp-top-bar");

        // ---- CONTROLS ----
        Label nLabel = new Label("\uD83E\uDDD1\u200D\uD83C\uDF93 Filosofos (N):");
        nLabel.getStyleClass().add("config-label");
        Spinner<Integer> nSpinner = new Spinner<>(2, 10, config.getNumFilosofos());
        nSpinner.setPrefWidth(70);
        nSpinner.valueProperty().addListener((obs, o, n) -> {
            if (!running) { config.setNumFilosofos(n); rebuildTable(); }
        });

        Button playBtn = new Button("\u25B6 Play");
        Button pauseBtn = new Button("\u23F8 Pausa");
        Button stopBtn = new Button("\u23F9 Stop");
        playBtn.getStyleClass().add("control-button");
        pauseBtn.getStyleClass().add("control-button");
        stopBtn.getStyleClass().add("control-button");

        Label speedLabel = new Label("\u26A1 Velocidad:");
        speedLabel.getStyleClass().add("speed-label");
        Slider speedSlider = new Slider(100, 5000, 1000);
        speedSlider.setShowTickLabels(true);
        speedSlider.setPrefWidth(200);
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

        // ---- CENTER ----
        tablePane = new Pane();
        tablePane.setPrefSize(500, 420);
        tablePane.setMinSize(500, 420);
        tablePane.getStyleClass().add("table-pane");

        statePanel = new VBox(4);
        statePanel.getStyleClass().add("state-panel");
        statePanel.setPrefWidth(300);

        // Legend
        HBox legend = new HBox(10);
        legend.setAlignment(Pos.CENTER);
        legend.getChildren().addAll(
            makeLegend("\uD83D\uDFEA", "Pensando", COLOR_THINKING),
            makeLegend("\uD83D\uDFE0", "Esperando", COLOR_WAITING),
            makeLegend("\uD83D\uDFE2", "Comiendo", COLOR_EATING)
        );

        VBox rightSide = new VBox(8, legend, statePanel);
        rightSide.setAlignment(Pos.TOP_CENTER);

        HBox centerContent = new HBox(15, tablePane, rightSide);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setPadding(new Insets(8, 15, 5, 15));
        setCenter(centerContent);

        // ---- BOTTOM ----
        VBox bottomSection = new VBox(logPanel);
        bottomSection.setPadding(new Insets(5, 15, 10, 15));
        setBottom(bottomSection);

        rebuildTable();
    }

    private HBox makeLegend(String emoji, String text, Color color) {
        Label e = new Label(emoji);
        Label t = new Label(text);
        t.setTextFill(color);
        t.getStyleClass().add("legend-text");
        return new HBox(3, e, t);
    }

    private void rebuildTable() {
        int N = config.getNumFilosofos();
        tablePane.getChildren().clear();
        statePanel.getChildren().clear();

        double cx = 250, cy = 210, radius = 160;

        // ---- Table center ----
        Circle tableBg = new Circle(cx, cy, 75);
        tableBg.setFill(COLOR_TABLE);
        tableBg.setStroke(COLOR_TABLE_BORDER);
        tableBg.setStrokeWidth(4);
        DropShadow tableGlow = new DropShadow();
        tableGlow.setColor(COLOR_TABLE_BORDER);
        tableGlow.setRadius(15);
        tableGlow.setSpread(0.2);
        tableBg.setEffect(tableGlow);

        // Table center emoji (spaghetti)
        Label tableEmoji = new Label("\uD83C\uDF5D");
        tableEmoji.getStyleClass().add("emoji-table-center");
        tableEmoji.setLayoutX(cx - 18);
        tableEmoji.setLayoutY(cy - 25);

        Label tableLabel = new Label("MESA");
        tableLabel.getStyleClass().add("table-label");
        tableLabel.setLayoutX(cx - 18);
        tableLabel.setLayoutY(cy + 5);

        tablePane.getChildren().addAll(tableBg, tableEmoji, tableLabel);

        // ---- Init arrays ----
        philEmojiLabels = new Label[N];
        philNameLabels  = new Label[N];
        philStateLabels = new Label[N];
        plateLabels     = new Label[N];
        forkEmojiLabels = new Label[N];
        philPulses      = new FadeTransition[N];
        stateInfoLabels = new Label[N];
        forkInfoLabels  = new Label[N];

        for (int i = 0; i < N; i++) {
            double angle = 2 * Math.PI * i / N - Math.PI / 2;
            double px = cx + radius * Math.cos(angle);
            double py = cy + radius * Math.sin(angle);

            // ---- Philosopher emoji ----
            philEmojiLabels[i] = new Label(PHIL_EMOJIS[i % PHIL_EMOJIS.length]);
            philEmojiLabels[i].getStyleClass().add("emoji-philosopher");
            philEmojiLabels[i].setLayoutX(px - 18);
            philEmojiLabels[i].setLayoutY(py - 22);

            // Background circle for glow
            Circle philBg = new Circle(px, py, 28);
            philBg.setFill(Color.TRANSPARENT);
            philBg.setStroke(COLOR_THINKING);
            philBg.setStrokeWidth(3);
            philBg.setId("philBg" + i);

            philNameLabels[i] = new Label("F" + i);
            philNameLabels[i].getStyleClass().add("phil-label");
            philNameLabels[i].setLayoutX(px - 8);
            philNameLabels[i].setLayoutY(py + 18);

            philStateLabels[i] = new Label("\uD83D\uDCA4 Pensando");
            philStateLabels[i].getStyleClass().add("phil-state-label");
            philStateLabels[i].setLayoutX(px - 30);
            philStateLabels[i].setLayoutY(py + 32);

            // ---- Plate between philosopher and table ----
            double plateR = radius * 0.6;
            double platX = cx + plateR * Math.cos(angle);
            double platY = cy + plateR * Math.sin(angle);
            plateLabels[i] = new Label("\uD83C\uDF7D\uFE0F");
            plateLabels[i].getStyleClass().add("emoji-plate");
            plateLabels[i].setLayoutX(platX - 10);
            plateLabels[i].setLayoutY(platY - 10);

            tablePane.getChildren().addAll(philBg, philEmojiLabels[i], philNameLabels[i], philStateLabels[i], plateLabels[i]);

            // ---- Fork emoji between philosophers ----
            double forkAngle = 2 * Math.PI * (i + 0.5) / N - Math.PI / 2;
            double forkR = radius * 0.42;
            double fkx = cx + forkR * Math.cos(forkAngle);
            double fky = cy + forkR * Math.sin(forkAngle);

            forkEmojiLabels[i] = new Label("\uD83C\uDF74");
            forkEmojiLabels[i].getStyleClass().add("emoji-fork");
            forkEmojiLabels[i].setLayoutX(fkx - 8);
            forkEmojiLabels[i].setLayoutY(fky - 8);

            tablePane.getChildren().add(forkEmojiLabels[i]);

            // ---- State panel entries ----
            stateInfoLabels[i] = new Label(PHIL_EMOJIS[i % PHIL_EMOJIS.length] + " F" + i + ": \uD83D\uDCA4 PENSANDO");
            stateInfoLabels[i].getStyleClass().add("state-info-thinking");

            forkInfoLabels[i] = new Label("\uD83C\uDF74 T" + i + ": libre");
            forkInfoLabels[i].getStyleClass().add("fork-info-free");
        }

        Label stTitle = new Label("\uD83E\uDDD1\u200D\uD83C\uDF93 Estado de los Filosofos");
        stTitle.getStyleClass().add("section-title");
        statePanel.getChildren().add(stTitle);
        for (int i = 0; i < N; i++) statePanel.getChildren().add(stateInfoLabels[i]);

        Label ftTitle = new Label("\uD83C\uDF74 Estado de los Tenedores");
        ftTitle.getStyleClass().add("section-title");
        statePanel.getChildren().add(ftTitle);
        for (int i = 0; i < N; i++) statePanel.getChildren().add(forkInfoLabels[i]);
    }

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

        updateTimeline = new Timeline(new KeyFrame(Duration.millis(100), e -> updateView()));
        updateTimeline.setCycleCount(Animation.INDEFINITE);
        updateTimeline.play();

        logPanel.log("\uD83C\uDF5D Simulacion de Filosofos Comensales iniciada (N=" + N + ")");
    }

    private void pausarSimulacion() {
        paused = true;
        if (philosophers != null) {
            for (Philosopher p : philosophers) p.pausar();
        }
        logPanel.log("\u23F8 Simulacion pausada");
    }

    private void reanudarSimulacion() {
        paused = false;
        if (philosophers != null) {
            for (Philosopher p : philosophers) p.reanudar();
        }
        logPanel.log("\u25B6 Simulacion reanudada");
    }

    private void detenerSimulacion() {
        running = false;
        paused = false;
        if (philosophers != null) {
            for (Philosopher p : philosophers) p.detener();
        }
        if (updateTimeline != null) updateTimeline.stop();
        logPanel.log("\u23F9 Simulacion detenida");
        rebuildTable();
    }

    private void updateView() {
        if (controller == null) return;
        int N = config.getNumFilosofos();
        int[] snapshot = controller.getSnapshot();

        Platform.runLater(() -> {
            for (int i = 0; i < N; i++) {
                EstadoFilosofo estado = EstadoFilosofo.values()[snapshot[i]];
                int forkHolder = snapshot[N + i];

                // Get the background circle
                Circle philBg = (Circle) tablePane.lookup("#philBg" + i);

                switch (estado) {
                    case PENSANDO -> {
                        philEmojiLabels[i].setText(PHIL_EMOJIS[i % PHIL_EMOJIS.length]);
                        philStateLabels[i].setText("\uD83D\uDCA4 Pensando");
                        plateLabels[i].setText("\uD83C\uDF7D\uFE0F");
                        stateInfoLabels[i].setText(PHIL_EMOJIS[i % PHIL_EMOJIS.length] + " F" + i + ": \uD83D\uDCA4 PENSANDO");
                        stateInfoLabels[i].getStyleClass().removeAll("state-info-thinking", "state-info-waiting", "state-info-eating");
                        stateInfoLabels[i].getStyleClass().add("state-info-thinking");
                        if (philBg != null) { philBg.setStroke(COLOR_THINKING); philBg.setEffect(null); }
                        if (philPulses[i] != null) {
                            AnimationUtils.stopPulse(philEmojiLabels[i], philPulses[i]);
                            philPulses[i] = null;
                        }
                    }
                    case ESPERANDO -> {
                        philEmojiLabels[i].setText("\uD83E\uDD14");
                        philStateLabels[i].setText("\u23F3 Esperando");
                        plateLabels[i].setText("\uD83C\uDF7D\uFE0F");
                        stateInfoLabels[i].setText("\uD83E\uDD14 F" + i + ": \u23F3 ESPERANDO");
                        stateInfoLabels[i].getStyleClass().removeAll("state-info-thinking", "state-info-waiting", "state-info-eating");
                        stateInfoLabels[i].getStyleClass().add("state-info-waiting");
                        if (philBg != null) { philBg.setStroke(COLOR_WAITING); philBg.setEffect(null); }
                        if (philPulses[i] == null) {
                            philPulses[i] = AnimationUtils.pulseNode(philEmojiLabels[i]);
                        }
                    }
                    case COMIENDO -> {
                        philEmojiLabels[i].setText("\uD83E\uDD24");
                        philStateLabels[i].setText("\uD83C\uDF5D Comiendo!");
                        plateLabels[i].setText("\uD83C\uDF5D");
                        stateInfoLabels[i].setText("\uD83E\uDD24 F" + i + ": \uD83C\uDF5D COMIENDO");
                        stateInfoLabels[i].getStyleClass().removeAll("state-info-thinking", "state-info-waiting", "state-info-eating");
                        stateInfoLabels[i].getStyleClass().add("state-info-eating");
                        if (philBg != null) {
                            philBg.setStroke(COLOR_EATING);
                            DropShadow eatGlow = new DropShadow();
                            eatGlow.setColor(COLOR_EATING);
                            eatGlow.setRadius(18);
                            eatGlow.setSpread(0.5);
                            philBg.setEffect(eatGlow);
                        }
                        if (philPulses[i] != null) {
                            AnimationUtils.stopPulse(philEmojiLabels[i], philPulses[i]);
                            philPulses[i] = null;
                        }
                    }
                }

                // Fork visuals
                if (forkHolder >= 0) {
                    forkEmojiLabels[i].setText("\u274C");
                    forkInfoLabels[i].setText("\uD83C\uDF74 T" + i + ": \u26D4 F" + forkHolder);
                    forkInfoLabels[i].getStyleClass().removeAll("fork-info-free", "fork-info-taken");
                    forkInfoLabels[i].getStyleClass().add("fork-info-taken");
                } else {
                    forkEmojiLabels[i].setText("\uD83C\uDF74");
                    forkInfoLabels[i].setText("\uD83C\uDF74 T" + i + ": \u2705 libre");
                    forkInfoLabels[i].getStyleClass().removeAll("fork-info-free", "fork-info-taken");
                    forkInfoLabels[i].getStyleClass().add("fork-info-free");
                }
            }
        });
    }

    public void cleanup() {
        detenerSimulacion();
    }
}
