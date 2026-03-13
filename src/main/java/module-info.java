module gt.edu.url.so.proyectosistemasoperativos {
    requires javafx.controls;
    requires javafx.fxml;


    opens gt.edu.url.so.proyectosistemasoperativos to javafx.fxml;
    exports gt.edu.url.so.proyectosistemasoperativos;
}