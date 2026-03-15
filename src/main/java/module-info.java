module gt.edu.url.so.proyectosistemasoperativos {
    requires javafx.controls;
    requires transitive javafx.graphics;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;

    exports gt.edu.url.so.proyectosistemasoperativos;
    exports gt.edu.url.so.proyectosistemasoperativos.common;
    exports gt.edu.url.so.proyectosistemasoperativos.producerconsumer;
    exports gt.edu.url.so.proyectosistemasoperativos.philosophers;
}
