package gt.edu.url.so.proyectosistemasoperativos;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Proyecto SO \u2014 Simulador de Concurrencia");
        config.setWindowedMode(1100, 750);
        config.setResizable(true);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new SOGame(), config);
    }
}
