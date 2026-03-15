package gt.edu.url.so.proyectosistemasoperativos.common;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

public class AnimationUtils {

    public static FadeTransition pulseNode(Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(500), node);
        ft.setFromValue(1.0);
        ft.setToValue(0.4);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();
        return ft;
    }

    public static void stopPulse(Node node, FadeTransition ft) {
        if (ft != null) {
            ft.stop();
        }
        node.setOpacity(1.0);
    }

    public static void glowEffect(Node node, Color color) {
        DropShadow glow = new DropShadow();
        glow.setColor(color);
        glow.setRadius(15);
        glow.setSpread(0.5);
        node.setEffect(glow);
    }

    public static void clearEffect(Node node) {
        node.setEffect(null);
    }

    public static ScaleTransition popIn(Node node) {
        node.setScaleX(0);
        node.setScaleY(0);
        ScaleTransition st = new ScaleTransition(Duration.millis(200), node);
        st.setToX(1.0);
        st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);
        st.play();
        return st;
    }

    public static FillTransition fillChange(Shape shape, Color from, Color to) {
        FillTransition ft = new FillTransition(Duration.millis(300), shape, from, to);
        ft.play();
        return ft;
    }

    public static Timeline conveyorBelt(Shape line) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(line.strokeDashOffsetProperty(), 0)),
                new KeyFrame(Duration.seconds(1), new KeyValue(line.strokeDashOffsetProperty(), -20))
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        return timeline;
    }
}
