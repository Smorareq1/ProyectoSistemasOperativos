package gt.edu.url.so.proyectosistemasoperativos;

import gt.edu.url.so.proyectosistemasoperativos.common.PixelGameCanvas;
import gt.edu.url.so.proyectosistemasoperativos.philosophers.DPAnimationView;
import gt.edu.url.so.proyectosistemasoperativos.producerconsumer.PCAnimationView;
import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.Glow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import static gt.edu.url.so.proyectosistemasoperativos.common.PixelGameCanvas.*;

public class Main extends Application {

    private Stage primaryStage;
    private Scene menuScene;
    private AnimationTimer menuAnimLoop;

    @Override
    public void start(Stage stage) {
        // Load pixel font
        Font.loadFont(getClass().getResourceAsStream(
                "/gt/edu/url/so/proyectosistemasoperativos/common/fonts/PressStart2P-Regular.ttf"), 10);

        this.primaryStage = stage;
        menuScene = createMenuScene();

        stage.setTitle("Proyecto SO \u2014 Simulador de Concurrencia");
        stage.setScene(menuScene);
        stage.setWidth(1100);
        stage.setHeight(750);
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.show();
    }

    private Scene createMenuScene() {
        // ── Background pixel art canvas ──
        PixelGameCanvas bgCanvas = new PixelGameCanvas(1100, 750, 3);
        int[] animFrame = {0};

        menuAnimLoop = new AnimationTimer() {
            long last = 0;
            @Override
            public void handle(long now) {
                if (now - last >= 120_000_000L) {
                    last = now;
                    animFrame[0]++;
                    drawMenuBackground(bgCanvas, animFrame[0]);
                }
            }
        };
        menuAnimLoop.start();

        // ── Menu UI overlay ──
        Label asciiArt = new Label(
            "  \u2588\u2588\u2588\u2588\u2588\u2588\u2557 \u2588\u2588\u2588\u2588\u2588\u2588\u2557  \u2588\u2588\u2588\u2588\u2588\u2588\u2557 \u2588\u2588\u2557   \u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557 \u2588\u2588\u2588\u2588\u2588\u2588\u2557 \n"
          + "  \u2588\u2588\u2554\u2550\u2550\u2550\u2550\u255D\u2588\u2588\u2554\u2550\u2550\u2550\u2588\u2588\u2557\u2588\u2588\u2554\u2550\u2550\u2550\u2588\u2588\u2557\u255A\u2588\u2588\u2557 \u2588\u2588\u2554\u255D\u2588\u2588\u2554\u2550\u2550\u2550\u2550\u255D\u2588\u2588\u2554\u2550\u2550\u2550\u2550\u255D\u255A\u2550\u2550\u2588\u2588\u2554\u2550\u2550\u255D\u2588\u2588\u2554\u2550\u2550\u2550\u2588\u2588\u2557\n"
          + "  \u2588\u2588\u2588\u2588\u2588\u2557  \u2588\u2588\u2588\u2588\u2588\u2588\u2554\u255D\u2588\u2588\u2551   \u2588\u2588\u2551 \u255A\u2588\u2588\u2588\u2588\u2554\u255D \u2588\u2588\u2588\u2588\u2588\u2557  \u2588\u2588\u2551        \u2588\u2588\u2551   \u2588\u2588\u2551   \u2588\u2588\u2551\n"
          + "  \u2588\u2588\u2554\u2550\u2550\u255D  \u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2557\u2588\u2588\u2551   \u2588\u2588\u2551  \u255A\u2588\u2588\u2554\u255D  \u2588\u2588\u2554\u2550\u2550\u255D  \u2588\u2588\u2551        \u2588\u2588\u2551   \u2588\u2588\u2551   \u2588\u2588\u2551\n"
          + "  \u2588\u2588\u2551     \u2588\u2588\u2551  \u2588\u2588\u2551\u255A\u2588\u2588\u2588\u2588\u2588\u2588\u2554\u255D   \u2588\u2588\u2551   \u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2588\u2588\u2557   \u2588\u2588\u2551   \u255A\u2588\u2588\u2588\u2588\u2588\u2588\u2554\u255D\n"
          + "  \u255A\u2550\u255D     \u255A\u2550\u255D  \u255A\u2550\u255D \u255A\u2550\u2550\u2550\u2550\u2550\u255D    \u255A\u2550\u255D   \u255A\u2550\u2550\u2550\u2550\u2550\u2550\u255D\u255A\u2550\u2550\u2550\u2550\u2550\u255D   \u255A\u2550\u255D    \u255A\u2550\u2550\u2550\u2550\u2550\u255D "
        );
        asciiArt.getStyleClass().add("ascii-banner");

        Glow bannerGlow = new Glow(0.0);
        asciiArt.setEffect(bannerGlow);
        Timeline glowAnim = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(bannerGlow.levelProperty(), 0.1)),
            new KeyFrame(Duration.seconds(1.5), new KeyValue(bannerGlow.levelProperty(), 0.5))
        );
        glowAnim.setCycleCount(Animation.INDEFINITE);
        glowAnim.setAutoReverse(true);
        glowAnim.play();

        Label subtitle = new Label("\u2592\u2592\u2592  SIMULADOR DE CONCURRENCIA  \u2592\u2592\u2592");
        subtitle.getStyleClass().add("subtitle-label");

        Label creditsLine = new Label("Universidad Rafael Landivar \u2022 Sistemas Operativos");
        creditsLine.getStyleClass().add("credits-label");

        HBox separator = new HBox();
        for (int i = 0; i < 40; i++) {
            Rectangle pixel = new Rectangle(8, 8);
            pixel.setFill(i % 2 == 0 ? Color.web("#e8682a") : Color.web("#f0c040"));
            separator.getChildren().add(pixel);
        }
        separator.setAlignment(Pos.CENTER);
        separator.setSpacing(4);

        Button pcButton = new Button("\uD83C\uDFED  PRODUCTOR - CONSUMIDOR  \uD83D\uDCE6");
        pcButton.getStyleClass().add("menu-button");
        pcButton.setPrefWidth(480);
        pcButton.setPrefHeight(60);
        pcButton.setOnAction(e -> { menuAnimLoop.stop(); showProducerConsumer(); });

        Button dpButton = new Button("\uD83E\uDDD1\u200D\uD83C\uDF73  FILOSOFOS COMENSALES  \uD83C\uDF5D");
        dpButton.getStyleClass().add("menu-button");
        dpButton.setPrefWidth(480);
        dpButton.setPrefHeight(60);
        dpButton.setOnAction(e -> { menuAnimLoop.stop(); showDiningPhilosophers(); });

        Label decor = new Label("\u2699\uFE0F \uD83D\uDD27 \u2699\uFE0F \uD83D\uDD29 \u2699\uFE0F \uD83D\uDD27 \u2699\uFE0F");
        decor.getStyleClass().add("decor-label");

        HBox bottomBar = new HBox();
        for (int i = 0; i < 40; i++) {
            Rectangle pixel = new Rectangle(8, 8);
            pixel.setFill(i % 3 == 0 ? Color.web("#8b2018") : (i % 3 == 1 ? Color.web("#68b030") : Color.web("#3898b8")));
            bottomBar.getChildren().add(pixel);
        }
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.setSpacing(4);

        VBox menuContent = new VBox(18, asciiArt, subtitle, separator, creditsLine, pcButton, dpButton, decor, bottomBar);
        menuContent.setAlignment(Pos.CENTER);
        menuContent.setPadding(new Insets(40));

        StackPane root = new StackPane(bgCanvas, menuContent);
        root.getStyleClass().add("menu-root");

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource(
                "/gt/edu/url/so/proyectosistemasoperativos/common/AppTheme.css").toExternalForm());
        return scene;
    }

    /** Draw an animated pixel art background for the menu */
    private void drawMenuBackground(PixelGameCanvas c, int frame) {
        int pw = c.pxW();
        int ph = c.pxH();

        c.clear(BG_CREAM);

        // ── Brick wall (top ~40%) ──
        c.drawBrickWall(0, 0, pw, ph * 2 / 5);
        int wallEnd = ph * 2 / 5;
        c.fill(0, wallEnd, pw, 2, DARK_BROWN);

        // ── Warning stripes border ──
        c.drawWarningStripes(0, wallEnd - 1, pw);

        // ── Wall decorations ──
        // Torches
        for (int tx = 15; tx < pw; tx += 50) {
            c.drawTorch(tx, wallEnd - 18, frame);
        }
        // Windows
        c.drawWindow(40, wallEnd - 30);
        c.drawWindow(pw - 52, wallEnd - 30);
        // Paintings
        c.drawPainting(pw / 3, wallEnd - 28, BROWN, Color.web("#4080a0"), Color.web("#60a060"));
        c.drawPainting(pw * 2 / 3, wallEnd - 26, WOOD_DK, ORANGE, GOLD);
        // Shelves
        c.drawShelf(pw / 4, wallEnd - 20);
        c.drawShelf(pw * 3 / 4, wallEnd - 20);
        // Gauges
        c.drawGauge(pw / 2 - 20, wallEnd - 20, (frame % 20) / 20.0);
        c.drawGauge(pw / 2 + 14, wallEnd - 20, 0.65);
        // Pipes
        c.drawPipeH(0, wallEnd - 8, pw);

        // ── Wood floor (middle ~30%) ──
        c.drawWoodFloor(0, wallEnd + 2, pw, ph * 3 / 10);

        // ── Stone floor (bottom ~30%) ──
        int stoneStart = wallEnd + 2 + ph * 3 / 10;
        c.drawStoneFloor(0, stoneStart, pw, ph - stoneStart);
        c.fill(0, stoneStart, pw, 1, DARK_BROWN);

        // ── Decorative objects scattered around ──
        // Barrels and crates on the floor
        c.drawBarrel(10, stoneStart + 5);
        c.drawBarrel(18, stoneStart + 8);
        c.drawCrate(pw - 16, stoneStart + 4);
        c.drawCrate(pw - 24, stoneStart + 7);
        c.drawCrate(pw - 20, stoneStart + 2);

        // More crates and barrels
        c.drawBarrel(pw / 3 - 10, stoneStart + 6);
        c.drawCrate(pw * 2 / 3 + 5, stoneStart + 5);

        // ── Animated characters on the floor ──
        // Miner walking left to right
        int minerX = (frame * 2) % (pw + 20) - 10;
        c.drawMiner(minerX, wallEnd + 10, (frame % 4 < 2) ? 1 : 2);

        // Robots standing around
        c.drawRobot(pw / 4, wallEnd + 20, (frame % 8 < 4) ? 0 : 1, TEAL, DK_TEAL, LT_TEAL);
        c.drawRobot(pw / 2, wallEnd + 25, 0, GREEN, DK_GREEN, LT_GREEN);
        c.drawRobot(pw * 3 / 4, wallEnd + 18, (frame % 6 < 3) ? 1 : 0, ORANGE, DK_ORANGE, GOLD);

        // Philosophers sitting
        int philY = stoneStart - 18;
        c.drawPhilosopher(pw / 5, philY, (frame % 12 < 4) ? 0 : (frame % 12 < 8 ? 1 : 2),
                Color.web("#4060c0"), Color.web("#304890"), DARK_BROWN, frame % 4 < 2 ? 0 : -1);
        c.drawPhilosopher(pw * 4 / 5, philY, (frame % 10 < 5) ? 1 : 0,
                Color.web("#c04040"), Color.web("#903030"), Color.web("#202020"), 0);

        // ── Smoke/particles ──
        c.drawSmoke(pw / 3, wallEnd - 2, frame);
        c.drawSmoke(pw * 2 / 3, wallEnd - 3, frame + 3);
    }

    private void showProducerConsumer() {
        PCAnimationView view = new PCAnimationView(this::backToMenu);
        Scene scene = new Scene(view);
        scene.getStylesheets().addAll(
                getClass().getResource("/gt/edu/url/so/proyectosistemasoperativos/common/AppTheme.css").toExternalForm(),
                getClass().getResource("/gt/edu/url/so/proyectosistemasoperativos/common/LogPanel.css").toExternalForm(),
                getClass().getResource("/gt/edu/url/so/proyectosistemasoperativos/producerconsumer/PCView.css").toExternalForm()
        );
        primaryStage.setScene(scene);
    }

    private void showDiningPhilosophers() {
        DPAnimationView view = new DPAnimationView(this::backToMenu);
        Scene scene = new Scene(view);
        scene.getStylesheets().addAll(
                getClass().getResource("/gt/edu/url/so/proyectosistemasoperativos/common/AppTheme.css").toExternalForm(),
                getClass().getResource("/gt/edu/url/so/proyectosistemasoperativos/common/LogPanel.css").toExternalForm(),
                getClass().getResource("/gt/edu/url/so/proyectosistemasoperativos/philosophers/DPView.css").toExternalForm()
        );
        primaryStage.setScene(scene);
    }

    private void backToMenu() {
        menuAnimLoop.start();
        primaryStage.setScene(menuScene);
    }

    public static void main(String[] args) {
        launch();
    }
}
