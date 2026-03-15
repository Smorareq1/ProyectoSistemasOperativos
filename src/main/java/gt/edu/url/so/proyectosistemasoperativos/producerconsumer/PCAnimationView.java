package gt.edu.url.so.proyectosistemasoperativos.producerconsumer;

import gt.edu.url.so.proyectosistemasoperativos.common.AnimationUtils;
import gt.edu.url.so.proyectosistemasoperativos.common.LogPanel;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.List;

public class PCAnimationView extends BorderPane {

    private static final Color COLOR_ACTIVE     = Color.web("#2cb67d");
    private static final Color COLOR_PAR        = Color.web("#7f5af0");
    private static final Color COLOR_IMPAR      = Color.web("#2cb67d");
    private static final Color COLOR_PRIMO      = Color.web("#ff8906");
    private static final Color COLOR_SLOT_EMPTY = Color.web("#1a1a2e");
    private static final Color COLOR_SLOT_BORDER= Color.web("#393b52");

    private final PCController controller;
    private final LogPanel logPanel;

    // Producer visual
    private final Label producerEmoji = new Label("\uD83D\uDC77");
    private final Label producerLabel = new Label("PRODUCTOR");
    private final Label producerStatus = new Label("\u23F8 Idle");
    private final Label producerNumero = new Label("");
    private final Label smokeLabel = new Label("");
    private FadeTransition producerPulse;

    // Buffer visual
    private static final int BUFFER_SIZE = 12;
    private final StackPane[] bufferSlotPanes = new StackPane[BUFFER_SIZE];
    private final Rectangle[] bufferSlots = new Rectangle[BUFFER_SIZE];
    private final Label[] bufferLabels = new Label[BUFFER_SIZE];
    private final Label[] bufferEmojis = new Label[BUFFER_SIZE];
    private final ProgressBar bufferProgress = new ProgressBar(0);
    private final Label bufferCountLabel = new Label("0/" + BUFFER_SIZE);

    // Consumer visuals
    private final Label[] consumerEmojis = new Label[3];
    private final Label[] consumerNames = new Label[3];
    private final Label[] consumerSumas = new Label[3];
    private final Label[] consumerStatus = new Label[3];
    private final Label[] consumerBinEmojis = new Label[3];
    private final FadeTransition[] consumerPulses = new FadeTransition[3];

    // Gear animations
    private final Label gearLeft = new Label("\u2699\uFE0F");
    private final Label gearRight = new Label("\u2699\uFE0F");

    private Timeline updateTimeline;
    private boolean paused = false;

    public PCAnimationView(Runnable onBack) {
        logPanel = new LogPanel();
        controller = new PCController(logPanel);
        getStyleClass().add("pc-root");
        setupCallbacks();
        buildUI(onBack);
        startUpdateLoop();
    }

    private void setupCallbacks() {
        controller.setProducerCallbacks(
                estado -> Platform.runLater(() -> updateProducerState(estado)),
                numero -> Platform.runLater(() -> {
                    producerNumero.setText(String.valueOf(numero));
                    smokeLabel.setText("\uD83D\uDCA8");
                    FadeTransition ft = new FadeTransition(Duration.millis(600), smokeLabel);
                    ft.setFromValue(1.0); ft.setToValue(0.0); ft.play();
                })
        );
        controller.setConsumerParCallbacks(
                estado -> Platform.runLater(() -> updateConsumerState(0, estado)),
                (num, suma) -> Platform.runLater(() -> consumerSumas[0].setText("\u03A3 = " + suma))
        );
        controller.setConsumerImparCallbacks(
                estado -> Platform.runLater(() -> updateConsumerState(1, estado)),
                (num, suma) -> Platform.runLater(() -> consumerSumas[1].setText("\u03A3 = " + suma))
        );
        controller.setConsumerPrimoCallbacks(
                estado -> Platform.runLater(() -> updateConsumerState(2, estado)),
                (num, suma) -> Platform.runLater(() -> consumerSumas[2].setText("\u03A3 = " + suma))
        );
    }

    private void buildUI(Runnable onBack) {
        // ---- TOP BAR ----
        Button backBtn = new Button("\u25C0 Menu");
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> { cleanup(); onBack.run(); });

        Label title = new Label("\uD83C\uDFED  FABRICA: PRODUCTOR - CONSUMIDOR  \uD83C\uDFED");
        title.getStyleClass().add("title-label");

        HBox topBar = new HBox(20, backBtn, title);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("pc-top-bar");

        // ---- CONTROLS ----
        Button playBtn = new Button("\u25B6 Play");
        Button pauseBtn = new Button("\u23F8 Pausa");
        Button stopBtn = new Button("\u23F9 Stop");
        playBtn.getStyleClass().add("control-button");
        pauseBtn.getStyleClass().add("control-button");
        stopBtn.getStyleClass().add("control-button");

        Label speedLabel = new Label("\u26A1 Velocidad:");
        speedLabel.getStyleClass().add("speed-label");
        Slider speedSlider = new Slider(50, 2000, 500);
        speedSlider.setShowTickLabels(true);
        speedSlider.setPrefWidth(200);
        speedSlider.valueProperty().addListener((obs, o, n) -> controller.setDelay(n.intValue()));

        HBox controls = new HBox(10, playBtn, pauseBtn, stopBtn, speedLabel, speedSlider);
        controls.setAlignment(Pos.CENTER);
        controls.getStyleClass().add("pc-controls");

        VBox topSection = new VBox(0, topBar, controls);

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

        setTop(topSection);

        // ---- CENTER: Factory Floor ----
        VBox producerBox = buildProducerBox();
        VBox bufferBox = buildBufferBox();
        VBox consumersBox = buildConsumersBox();

        // Conveyor belt left
        VBox conveyorLeft = buildConveyor("\u27A1\uFE0F");
        VBox conveyorRight = buildConveyor("\u27A1\uFE0F");

        HBox centerContent = new HBox(8, producerBox, conveyorLeft, bufferBox, conveyorRight, consumersBox);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setPadding(new Insets(15, 15, 10, 15));

        // Factory floor decoration
        HBox floorDecor = new HBox(2);
        floorDecor.setAlignment(Pos.CENTER);
        for (int i = 0; i < 80; i++) {
            Rectangle tile = new Rectangle(10, 6);
            tile.setFill(i % 2 == 0 ? Color.web("#1a1a2e") : Color.web("#242838"));
            tile.setStroke(Color.web("#393b52"));
            tile.setStrokeWidth(0.5);
            floorDecor.getChildren().add(tile);
        }

        VBox centerWrapper = new VBox(5, centerContent, floorDecor);
        centerWrapper.setAlignment(Pos.CENTER);
        setCenter(centerWrapper);

        // ---- BOTTOM: Log ----
        VBox bottomSection = new VBox(logPanel);
        bottomSection.setPadding(new Insets(5, 15, 10, 15));
        setBottom(bottomSection);
    }

    private VBox buildConveyor(String arrow) {
        VBox conv = new VBox(2);
        conv.setAlignment(Pos.CENTER);
        for (int i = 0; i < 5; i++) {
            Label arrowLabel = new Label(arrow);
            arrowLabel.getStyleClass().add("conveyor-arrow");
            // Animate each arrow with a fade
            FadeTransition ft = new FadeTransition(Duration.millis(400 + i * 100), arrowLabel);
            ft.setFromValue(0.3); ft.setToValue(1.0);
            ft.setCycleCount(Animation.INDEFINITE); ft.setAutoReverse(true); ft.play();
            conv.getChildren().add(arrowLabel);
        }
        // Animated dashed line
        Line dashLine = new Line(0, 0, 0, 80);
        dashLine.setStroke(Color.web("#ff8906"));
        dashLine.setStrokeWidth(3);
        dashLine.getStrokeDashArray().addAll(8.0, 6.0);
        AnimationUtils.conveyorBelt(dashLine);
        conv.getChildren().add(dashLine);
        return conv;
    }

    private VBox buildProducerBox() {
        // Big emoji worker
        producerEmoji.getStyleClass().add("emoji-huge");

        // Smoke puff
        smokeLabel.getStyleClass().add("smoke-label");

        StackPane workerStack = new StackPane(producerEmoji, smokeLabel);
        StackPane.setAlignment(smokeLabel, Pos.TOP_RIGHT);

        // Machine structure
        Label machineTop = new Label("\u2593\u2593\u2593\u2593\u2593\u2593");
        machineTop.getStyleClass().add("machine-frame");

        // Spinning gear
        gearLeft.getStyleClass().add("emoji-gear");
        RotateTransition gearSpin = new RotateTransition(Duration.seconds(3), gearLeft);
        gearSpin.setByAngle(360); gearSpin.setCycleCount(Animation.INDEFINITE); gearSpin.play();

        producerLabel.getStyleClass().add("producer-label");
        producerStatus.getStyleClass().add("producer-status");
        producerNumero.getStyleClass().add("producer-number");

        Label numTitle = new Label("\uD83D\uDCE6 Ultimo:");
        numTitle.getStyleClass().add("small-label");

        VBox box = new VBox(6, machineTop, workerStack, gearLeft, producerLabel, producerStatus, numTitle, producerNumero);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("producer-box");
        box.setPrefWidth(170);
        return box;
    }

    private VBox buildBufferBox() {
        Label bufferTitle = new Label("\uD83D\uDCE6 CINTA TRANSPORTADORA \uD83D\uDCE6");
        bufferTitle.getStyleClass().add("buffer-title");

        // Two rows of buffer slots (6+6)
        HBox slotsRow1 = new HBox(5);
        slotsRow1.setAlignment(Pos.CENTER);
        HBox slotsRow2 = new HBox(5);
        slotsRow2.setAlignment(Pos.CENTER);

        for (int i = 0; i < BUFFER_SIZE; i++) {
            bufferSlots[i] = new Rectangle(42, 42);
            bufferSlots[i].setArcWidth(0);
            bufferSlots[i].setArcHeight(0);
            bufferSlots[i].setFill(COLOR_SLOT_EMPTY);
            bufferSlots[i].setStroke(COLOR_SLOT_BORDER);
            bufferSlots[i].setStrokeWidth(2);

            bufferLabels[i] = new Label("");
            bufferLabels[i].getStyleClass().add("buffer-slot-label");

            bufferEmojis[i] = new Label("");
            bufferEmojis[i].getStyleClass().add("buffer-slot-emoji");

            VBox slotContent = new VBox(0, bufferEmojis[i], bufferLabels[i]);
            slotContent.setAlignment(Pos.CENTER);
            bufferSlotPanes[i] = new StackPane(bufferSlots[i], slotContent);

            if (i < 6) slotsRow1.getChildren().add(bufferSlotPanes[i]);
            else slotsRow2.getChildren().add(bufferSlotPanes[i]);
        }

        // Legend
        HBox legend = new HBox(12);
        legend.setAlignment(Pos.CENTER);
        legend.getChildren().addAll(
            createLegendItem("\uD83D\uDFEA", "Par", COLOR_PAR),
            createLegendItem("\uD83D\uDFE2", "Impar", COLOR_IMPAR),
            createLegendItem("\uD83D\uDFE0", "Primo", COLOR_PRIMO)
        );

        bufferProgress.setPrefWidth(300);
        bufferCountLabel.getStyleClass().add("buffer-count");

        // Capacity bar label
        Label capLabel = new Label("\u2588\u2588 Capacidad:");
        capLabel.getStyleClass().add("small-label");

        VBox box = new VBox(8, bufferTitle, slotsRow1, slotsRow2, legend, capLabel, bufferProgress, bufferCountLabel);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("buffer-box");
        return box;
    }

    private HBox createLegendItem(String emoji, String text, Color color) {
        Label emojiL = new Label(emoji);
        emojiL.getStyleClass().add("legend-emoji");
        Label textL = new Label(text);
        textL.setTextFill(color);
        textL.getStyleClass().add("legend-text");
        HBox item = new HBox(3, emojiL, textL);
        item.setAlignment(Pos.CENTER);
        return item;
    }

    private VBox buildConsumersBox() {
        String[] names   = {"\uD83D\uDFEA C1: PARES", "\uD83D\uDFE2 C2: IMPARES", "\uD83D\uDFE0 C3: PRIMOS"};
        String[] emojis  = {"\uD83D\uDC68\u200D\uD83D\uDD27", "\uD83D\uDC69\u200D\uD83D\uDD27", "\uD83E\uDDD1\u200D\uD83D\uDCBB"};
        String[] bins    = {"\uD83D\uDDF3\uFE0F", "\uD83D\uDDF3\uFE0F", "\uD83D\uDDF3\uFE0F"};
        Color[]  colors  = {COLOR_PAR, COLOR_IMPAR, COLOR_PRIMO};
        String[] nameClasses = {"consumer-name-par", "consumer-name-impar", "consumer-name-primo"};

        // Spinning gear right
        gearRight.getStyleClass().add("emoji-gear");
        RotateTransition gearSpin2 = new RotateTransition(Duration.seconds(3), gearRight);
        gearSpin2.setByAngle(-360); gearSpin2.setCycleCount(Animation.INDEFINITE); gearSpin2.play();

        Label consTitle = new Label("\uD83D\uDC77 CONSUMIDORES \uD83D\uDC77");
        consTitle.getStyleClass().add("section-title");

        VBox consumersColumn = new VBox(8);
        consumersColumn.setAlignment(Pos.CENTER);

        for (int i = 0; i < 3; i++) {
            consumerEmojis[i] = new Label(emojis[i]);
            consumerEmojis[i].getStyleClass().add("emoji-medium");

            consumerBinEmojis[i] = new Label(bins[i]);
            consumerBinEmojis[i].getStyleClass().add("emoji-small");

            consumerNames[i] = new Label(names[i]);
            consumerNames[i].getStyleClass().add(nameClasses[i]);

            consumerSumas[i] = new Label("\u03A3 = 0");
            consumerSumas[i].getStyleClass().add("consumer-suma");

            consumerStatus[i] = new Label("\u23F8 Idle");
            consumerStatus[i].getStyleClass().add("consumer-status");

            // Card per consumer
            VBox infoCol = new VBox(1, consumerNames[i], consumerSumas[i], consumerStatus[i]);
            infoCol.setAlignment(Pos.CENTER_LEFT);
            HBox row = new HBox(6, consumerEmojis[i], infoCol, consumerBinEmojis[i]);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("consumer-card");
            row.setPadding(new Insets(6, 10, 6, 10));

            // Colored left border effect via background
            Rectangle colorBar = new Rectangle(4, 50);
            colorBar.setFill(colors[i]);
            HBox cardWithBar = new HBox(0, colorBar, row);
            cardWithBar.setAlignment(Pos.CENTER_LEFT);

            consumersColumn.getChildren().add(cardWithBar);
        }

        VBox box = new VBox(6, consTitle, gearRight, consumersColumn);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("consumers-box");
        box.setPrefWidth(260);
        return box;
    }

    private void updateProducerState(String estado) {
        switch (estado) {
            case "ACTIVO" -> {
                producerEmoji.setText("\uD83D\uDC77");
                producerStatus.setText("\u26A1 Produciendo...");
                producerStatus.getStyleClass().removeAll("status-active", "status-blocked", "status-idle");
                producerStatus.getStyleClass().add("status-active");
                if (producerPulse != null) AnimationUtils.stopPulse(producerEmoji, producerPulse);
                producerPulse = null;
                AnimationUtils.glowEffect(producerEmoji, COLOR_ACTIVE);
            }
            case "BLOQUEADO" -> {
                producerEmoji.setText("\uD83D\uDE2D");
                producerStatus.setText("\uD83D\uDED1 Buffer Lleno!");
                producerStatus.getStyleClass().removeAll("status-active", "status-blocked", "status-idle");
                producerStatus.getStyleClass().add("status-blocked");
                producerPulse = AnimationUtils.pulseNode(producerEmoji);
            }
            case "TERMINADO" -> {
                producerEmoji.setText("\uD83D\uDE34");
                producerStatus.setText("\u23F9 Terminado");
                producerStatus.getStyleClass().removeAll("status-active", "status-blocked", "status-idle");
                producerStatus.getStyleClass().add("status-idle");
                if (producerPulse != null) AnimationUtils.stopPulse(producerEmoji, producerPulse);
                producerPulse = null;
                AnimationUtils.clearEffect(producerEmoji);
            }
        }
    }

    private void updateConsumerState(int idx, String estado) {
        String[] activeEmojis  = {"\uD83D\uDC68\u200D\uD83D\uDD27", "\uD83D\uDC69\u200D\uD83D\uDD27", "\uD83E\uDDD1\u200D\uD83D\uDCBB"};
        switch (estado) {
            case "ACTIVO" -> {
                consumerEmojis[idx].setText(activeEmojis[idx]);
                consumerStatus[idx].setText("\u26A1 Consumiendo...");
                consumerStatus[idx].getStyleClass().removeAll("status-active", "status-blocked", "status-idle");
                consumerStatus[idx].getStyleClass().add("status-active");
                if (consumerPulses[idx] != null) AnimationUtils.stopPulse(consumerEmojis[idx], consumerPulses[idx]);
                consumerPulses[idx] = null;
                AnimationUtils.glowEffect(consumerEmojis[idx], COLOR_ACTIVE);
            }
            case "BLOQUEADO" -> {
                consumerEmojis[idx].setText("\uD83D\uDE34");
                consumerStatus[idx].setText("\uD83D\uDED1 Esperando...");
                consumerStatus[idx].getStyleClass().removeAll("status-active", "status-blocked", "status-idle");
                consumerStatus[idx].getStyleClass().add("status-blocked");
                consumerPulses[idx] = AnimationUtils.pulseNode(consumerEmojis[idx]);
            }
            case "TERMINADO" -> {
                consumerEmojis[idx].setText("\u2705");
                consumerStatus[idx].setText("\u23F9 Terminado");
                consumerStatus[idx].getStyleClass().removeAll("status-active", "status-blocked", "status-idle");
                consumerStatus[idx].getStyleClass().add("status-idle");
                if (consumerPulses[idx] != null) AnimationUtils.stopPulse(consumerEmojis[idx], consumerPulses[idx]);
                consumerPulses[idx] = null;
                AnimationUtils.clearEffect(consumerEmojis[idx]);
            }
        }
    }

    private void startUpdateLoop() {
        updateTimeline = new Timeline(new KeyFrame(Duration.millis(100), e -> updateBufferView()));
        updateTimeline.setCycleCount(Animation.INDEFINITE);
        updateTimeline.play();
    }

    private void updateBufferView() {
        if (controller.getBuffer() == null) return;
        List<Integer> snapshot = controller.getBuffer().getSnapshot();
        int count = snapshot.size();

        bufferProgress.setProgress((double) count / BUFFER_SIZE);
        bufferCountLabel.setText("\uD83D\uDCE6 " + count + "/" + BUFFER_SIZE);

        for (int i = 0; i < BUFFER_SIZE; i++) {
            if (i < snapshot.size()) {
                int num = snapshot.get(i);
                TipoNumero tipo = NumberClassifier.clasificar(num);
                bufferLabels[i].setText(String.valueOf(num));
                switch (tipo) {
                    case PAR -> {
                        bufferSlots[i].setFill(COLOR_PAR);
                        bufferEmojis[i].setText("\uD83D\uDFEA");
                    }
                    case IMPAR -> {
                        bufferSlots[i].setFill(COLOR_IMPAR);
                        bufferEmojis[i].setText("\uD83D\uDFE2");
                    }
                    case PRIMO -> {
                        bufferSlots[i].setFill(COLOR_PRIMO);
                        bufferEmojis[i].setText("\uD83D\uDFE0");
                    }
                }
                // Glow effect on filled slots
                DropShadow slotGlow = new DropShadow();
                slotGlow.setColor((Color) bufferSlots[i].getFill());
                slotGlow.setRadius(8);
                slotGlow.setSpread(0.3);
                bufferSlotPanes[i].setEffect(slotGlow);
            } else {
                bufferSlots[i].setFill(COLOR_SLOT_EMPTY);
                bufferLabels[i].setText("");
                bufferEmojis[i].setText("");
                bufferSlotPanes[i].setEffect(null);
            }
        }
    }

    public void cleanup() {
        controller.detener();
        if (updateTimeline != null) updateTimeline.stop();
    }
}
