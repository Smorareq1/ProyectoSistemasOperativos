package gt.edu.url.so.proyectosistemasoperativos.producerconsumer;

import gt.edu.url.so.proyectosistemasoperativos.common.GameLogger;

import java.io.*;
import java.util.function.Consumer;

public class Producer extends Thread {
    private final SharedBuffer buffer;
    private final GameLogger log;
    private final Consumer<String> estadoCallback;
    private final Consumer<Integer> numeroCallback;
    private volatile boolean running = true;
    private volatile boolean paused = false;
    private int delayMs = 500;
    private final File archivoExterno;

    public Producer(SharedBuffer buffer, GameLogger log,
                    Consumer<String> estadoCallback, Consumer<Integer> numeroCallback) {
        this(buffer, log, estadoCallback, numeroCallback, null);
    }

    public Producer(SharedBuffer buffer, GameLogger log,
                    Consumer<String> estadoCallback, Consumer<Integer> numeroCallback,
                    File archivoExterno) {
        super("Productor");
        setDaemon(true);
        this.buffer = buffer;
        this.log = log;
        this.estadoCallback = estadoCallback;
        this.numeroCallback = numeroCallback;
        this.archivoExterno = archivoExterno;
    }

    private BufferedReader abrirArchivo() throws IOException {
        if (archivoExterno != null) {
            return new BufferedReader(new FileReader(archivoExterno));
        }
        InputStream is = getClass().getResourceAsStream(
                "/gt/edu/url/so/proyectosistemasoperativos/producerconsumer/data/numeros.txt");
        if (is == null) throw new IOException("No se encontro el archivo de numeros por defecto");
        return new BufferedReader(new InputStreamReader(is));
    }

    @Override
    public void run() {
        try (BufferedReader reader = abrirArchivo()) {

            String line;
            while ((line = reader.readLine()) != null && running) {
                while (paused && running) {
                    Thread.sleep(100);
                }
                if (!running) break;

                line = line.trim();
                if (line.isEmpty()) continue;

                int numero = Integer.parseInt(line);
                estadoCallback.accept("BLOQUEADO");
                log.log("Productor intentando insertar " + numero + " (" + NumberClassifier.clasificar(numero) + ")");

                buffer.insertar(numero);

                estadoCallback.accept("ACTIVO");
                numeroCallback.accept(numero);
                TipoNumero tipo = NumberClassifier.clasificar(numero);
                log.log("Productor inserto " + numero + " (" + tipo + ") en el buffer");

                Thread.sleep(delayMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.log("Error en productor: " + e.getMessage());
        } finally {
            estadoCallback.accept("TERMINADO");
            log.log("Productor termino de leer el archivo");
        }
    }

    public void pausar() { paused = true; }
    public void reanudar() { paused = false; }
    public void detener() { running = false; this.interrupt(); }
    public boolean isRunning() { return running; }
    public void setDelay(int ms) { this.delayMs = ms; }
}
