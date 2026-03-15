package gt.edu.url.so.proyectosistemasoperativos.producerconsumer;

import gt.edu.url.so.proyectosistemasoperativos.common.LogPanel;

import java.util.function.BiConsumer;

public class Consumer extends Thread {
    private final SharedBuffer buffer;
    private final TipoNumero tipo;
    private final LogPanel log;
    private final java.util.function.Consumer<String> estadoCallback;
    private final BiConsumer<Integer, Integer> sumaCallback;
    private volatile boolean running = true;
    private volatile boolean paused = false;
    private volatile boolean producerDone = false;
    private int suma = 0;
    private int delayMs = 500;

    public Consumer(String name, SharedBuffer buffer, TipoNumero tipo, LogPanel log,
                    java.util.function.Consumer<String> estadoCallback,
                    BiConsumer<Integer, Integer> sumaCallback) {
        super(name);
        setDaemon(true);
        this.buffer = buffer;
        this.tipo = tipo;
        this.log = log;
        this.estadoCallback = estadoCallback;
        this.sumaCallback = sumaCallback;
    }

    @Override
    public void run() {
        try {
            while (running) {
                while (paused && running) {
                    Thread.sleep(100);
                }
                if (!running) break;

                estadoCallback.accept("BLOQUEADO");
                Integer elemento = buffer.buscarYExtraer(tipo);

                if (elemento == null) {
                    if (producerDone && !buffer.tieneElementosDeTipo(tipo)) {
                        break;
                    }
                    Thread.sleep(100);
                    continue;
                }

                estadoCallback.accept("ACTIVO");
                suma += elemento;
                sumaCallback.accept(elemento, suma);
                log.log(getName() + " extrajo " + elemento + " — Suma " + tipo + " = " + suma);

                Thread.sleep(delayMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            estadoCallback.accept("TERMINADO");
            log.log(getName() + " termino. Suma total de " + tipo + " = " + suma);
        }
    }

    public void notifyProducerDone() { producerDone = true; }
    public void pausar() { paused = true; }
    public void reanudar() { paused = false; }
    public void detener() { running = false; this.interrupt(); }
    public int getSuma() { return suma; }
    public TipoNumero getTipo() { return tipo; }
    public void setDelay(int ms) { this.delayMs = ms; }
}
