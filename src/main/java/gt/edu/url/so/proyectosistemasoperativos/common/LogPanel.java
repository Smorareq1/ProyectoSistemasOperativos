package gt.edu.url.so.proyectosistemasoperativos.common;

import javafx.application.Platform;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LogPanel extends VBox {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_ENTRIES = 200;

    private final ListView<String> listView;

    public LogPanel() {
        getStyleClass().add("log-panel-container");

        Label title = new Label(">> LOG DE EVENTOS");
        title.getStyleClass().add("log-title");

        listView = new ListView<>();
        listView.getStyleClass().add("log-panel");
        listView.setPrefHeight(140);
        listView.setFocusTraversable(false);

        setSpacing(4);
        getChildren().addAll(title, listView);
    }

    public void log(String message) {
        String entry = "[" + LocalTime.now().format(TIME_FMT) + "] " + message;
        Platform.runLater(() -> {
            listView.getItems().add(entry);
            if (listView.getItems().size() > MAX_ENTRIES) {
                listView.getItems().remove(0);
            }
            listView.scrollTo(listView.getItems().size() - 1);
        });
    }

    public void clear() {
        Platform.runLater(() -> listView.getItems().clear());
    }
}
