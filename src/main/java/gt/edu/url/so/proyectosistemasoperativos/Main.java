package gt.edu.url.so.proyectosistemasoperativos;

import gt.edu.url.so.proyectosistemasoperativos.philosophers.DPAnimationView;
import gt.edu.url.so.proyectosistemasoperativos.producerconsumer.PCAnimationView;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.Glow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {

    private Stage primaryStage;
    private Scene menuScene;

    @Override
    public void start(Stage stage) {
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
        // ASCII art banner
        Label asciiArt = new Label(
            "  \u2588\u2588\u2588\u2588\u2588\u2588\u2557 \u2588\u2588\u2588\u2588\u2588\u2588\u2557  \u2588\u2588\u2588\u2588\u2588\u2588\u2557 \u2588\u2588\u2557   \u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557 \u2588\u2588\u2588\u2588\u2588\u2588\u2557 \n"
          + "  \u2588\u2588\u2554\u2550\u2550\u2550\u2550\u255D\u2588\u2588\u2554\u2550\u2550\u2550\u2588\u2588\u2557\u2588\u2588\u2554\u2550\u2550\u2550\u2588\u2588\u2557\u255A\u2588\u2588\u2557 \u2588\u2588\u2554\u255D\u2588\u2588\u2554\u2550\u2550\u2550\u2550\u255D\u2588\u2588\u2554\u2550\u2550\u2550\u2550\u255D\u255A\u2550\u2550\u2588\u2588\u2554\u2550\u2550\u255D\u2588\u2588\u2554\u2550\u2550\u2550\u2588\u2588\u2557\n"
          + "  \u2588\u2588\u2588\u2588\u2588\u2557  \u2588\u2588\u2588\u2588\u2588\u2588\u2554\u255D\u2588\u2588\u2551   \u2588\u2588\u2551 \u255A\u2588\u2588\u2588\u2588\u2554\u255D \u2588\u2588\u2588\u2588\u2588\u2557  \u2588\u2588\u2551        \u2588\u2588\u2551   \u2588\u2588\u2551   \u2588\u2588\u2551\n"
          + "  \u2588\u2588\u2554\u2550\u2550\u255D  \u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2557\u2588\u2588\u2551   \u2588\u2588\u2551  \u255A\u2588\u2588\u2554\u255D  \u2588\u2588\u2554\u2550\u2550\u255D  \u2588\u2588\u2551        \u2588\u2588\u2551   \u2588\u2588\u2551   \u2588\u2588\u2551\n"
          + "  \u2588\u2588\u2551     \u2588\u2588\u2551  \u2588\u2588\u2551\u255A\u2588\u2588\u2588\u2588\u2588\u2588\u2554\u255D   \u2588\u2588\u2551   \u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2588\u2588\u2557   \u2588\u2588\u2551   \u255A\u2588\u2588\u2588\u2588\u2588\u2588\u2554\u255D\n"
          + "  \u255A\u2550\u255D     \u255A\u2550\u255D  \u255A\u2550\u255D \u255A\u2550\u2550\u2550\u2550\u2550\u255D    \u255A\u2550\u255D   \u255A\u2550\u2550\u2550\u2550\u2550\u2550\u255D\u255A\u2550\u2550\u2550\u2550\u2550\u255D   \u255A\u2550\u255D    \u255A\u2550\u2550\u2550\u2550\u2550\u255D "
        );
        asciiArt.getStyleClass().add("ascii-banner");

        // Animated glow on banner
        Glow bannerGlow = new Glow(0.0);
        asciiArt.setEffect(bannerGlow);
        Timeline glowAnim = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(bannerGlow.levelProperty(), 0.2)),
            new KeyFrame(Duration.seconds(1.5), new KeyValue(bannerGlow.levelProperty(), 0.8))
        );
        glowAnim.setCycleCount(Animation.INDEFINITE);
        glowAnim.setAutoReverse(true);
        glowAnim.play();

        Label subtitle = new Label("\u2592\u2592\u2592  SIMULADOR DE CONCURRENCIA  \u2592\u2592\u2592");
        subtitle.getStyleClass().add("subtitle-label");

        Label creditsLine = new Label("Universidad Rafael Landivar \u2022 Sistemas Operativos");
        creditsLine.getStyleClass().add("credits-label");

        // Decorative separator
        HBox separator = new HBox();
        for (int i = 0; i < 40; i++) {
            Rectangle pixel = new Rectangle(8, 8);
            pixel.setFill(i % 2 == 0 ? Color.web("#ff8906") : Color.web("#e53170"));
            separator.getChildren().add(pixel);
        }
        separator.setAlignment(Pos.CENTER);
        separator.setSpacing(4);

        // Menu buttons with emoji icons
        Button pcButton = new Button("\uD83C\uDFED  PRODUCTOR - CONSUMIDOR  \uD83D\uDCE6");
        pcButton.getStyleClass().add("menu-button");
        pcButton.setPrefWidth(420);
        pcButton.setPrefHeight(60);
        pcButton.setOnAction(e -> showProducerConsumer());

        Button dpButton = new Button("\uD83E\uDDD1\u200D\uD83C\uDF73  FILOSOFOS COMENSALES  \uD83C\uDF5D");
        dpButton.getStyleClass().add("menu-button");
        dpButton.setPrefWidth(420);
        dpButton.setPrefHeight(60);
        dpButton.setOnAction(e -> showDiningPhilosophers());

        // Decorative bottom emojis
        Label decor = new Label("\u2699\uFE0F \uD83D\uDD27 \u2699\uFE0F \uD83D\uDD29 \u2699\uFE0F \uD83D\uDD27 \u2699\uFE0F");
        decor.getStyleClass().add("decor-label");

        // Animated bottom bar
        HBox bottomBar = new HBox();
        for (int i = 0; i < 40; i++) {
            Rectangle pixel = new Rectangle(8, 8);
            pixel.setFill(i % 3 == 0 ? Color.web("#7f5af0") : (i % 3 == 1 ? Color.web("#2cb67d") : Color.web("#ff8906")));
            bottomBar.getChildren().add(pixel);
        }
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.setSpacing(4);

        VBox layout = new VBox(18, asciiArt, subtitle, separator, creditsLine, pcButton, dpButton, decor, bottomBar);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));
        layout.getStyleClass().add("menu-root");

        Scene scene = new Scene(layout);
        scene.getStylesheets().add(getClass().getResource(
                "/gt/edu/url/so/proyectosistemasoperativos/common/AppTheme.css").toExternalForm());
        return scene;
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
        primaryStage.setScene(menuScene);
    }

    public static void main(String[] args) {
        launch();
    }
}
