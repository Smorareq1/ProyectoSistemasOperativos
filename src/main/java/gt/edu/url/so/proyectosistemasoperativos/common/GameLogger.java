package gt.edu.url.so.proyectosistemasoperativos.common;

/**
 * Platform-agnostic logging interface for simulation classes.
 * Replaces the JavaFX LogPanel dependency.
 */
public interface GameLogger {
    void log(String message);
    void clear();
}
