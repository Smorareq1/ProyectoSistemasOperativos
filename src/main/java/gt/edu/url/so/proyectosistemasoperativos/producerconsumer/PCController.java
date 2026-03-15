package gt.edu.url.so.proyectosistemasoperativos.producerconsumer;

import gt.edu.url.so.proyectosistemasoperativos.common.LogPanel;

import java.util.function.BiConsumer;

public class PCController {
    private static final int BUFFER_CAPACITY = 12;

    private SharedBuffer buffer;
    private Producer producer;
    private gt.edu.url.so.proyectosistemasoperativos.producerconsumer.Consumer consumerPar;
    private gt.edu.url.so.proyectosistemasoperativos.producerconsumer.Consumer consumerImpar;
    private gt.edu.url.so.proyectosistemasoperativos.producerconsumer.Consumer consumerPrimo;
    private LogPanel log;
    private boolean running = false;

    private java.util.function.Consumer<String> producerEstadoCallback;
    private java.util.function.Consumer<Integer> producerNumeroCallback;
    private java.util.function.Consumer<String> consumerParEstadoCallback;
    private java.util.function.Consumer<String> consumerImparEstadoCallback;
    private java.util.function.Consumer<String> consumerPrimoEstadoCallback;
    private BiConsumer<Integer, Integer> sumaParCallback;
    private BiConsumer<Integer, Integer> sumaImparCallback;
    private BiConsumer<Integer, Integer> sumaPrimoCallback;

    public PCController(LogPanel log) {
        this.log = log;
    }

    public void setProducerCallbacks(java.util.function.Consumer<String> estado,
                                     java.util.function.Consumer<Integer> numero) {
        this.producerEstadoCallback = estado;
        this.producerNumeroCallback = numero;
    }

    public void setConsumerParCallbacks(java.util.function.Consumer<String> estado,
                                        BiConsumer<Integer, Integer> suma) {
        this.consumerParEstadoCallback = estado;
        this.sumaParCallback = suma;
    }

    public void setConsumerImparCallbacks(java.util.function.Consumer<String> estado,
                                           BiConsumer<Integer, Integer> suma) {
        this.consumerImparEstadoCallback = estado;
        this.sumaImparCallback = suma;
    }

    public void setConsumerPrimoCallbacks(java.util.function.Consumer<String> estado,
                                           BiConsumer<Integer, Integer> suma) {
        this.consumerPrimoEstadoCallback = estado;
        this.sumaPrimoCallback = suma;
    }

    public void iniciar() {
        if (running) return;
        running = true;

        buffer = new SharedBuffer(BUFFER_CAPACITY);

        producer = new Producer(buffer, log, producerEstadoCallback, producerNumeroCallback);
        consumerPar = new gt.edu.url.so.proyectosistemasoperativos.producerconsumer.Consumer(
                "C1-Pares", buffer, TipoNumero.PAR, log, consumerParEstadoCallback, sumaParCallback);
        consumerImpar = new gt.edu.url.so.proyectosistemasoperativos.producerconsumer.Consumer(
                "C2-Impares", buffer, TipoNumero.IMPAR, log, consumerImparEstadoCallback, sumaImparCallback);
        consumerPrimo = new gt.edu.url.so.proyectosistemasoperativos.producerconsumer.Consumer(
                "C3-Primos", buffer, TipoNumero.PRIMO, log, consumerPrimoEstadoCallback, sumaPrimoCallback);

        producer.start();
        consumerPar.start();
        consumerImpar.start();
        consumerPrimo.start();

        // Monitor thread: waits for producer to finish, then notifies consumers
        Thread monitor = new Thread(() -> {
            try {
                producer.join();
                Thread.sleep(500);
                consumerPar.notifyProducerDone();
                consumerImpar.notifyProducerDone();
                consumerPrimo.notifyProducerDone();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Monitor");
        monitor.setDaemon(true);
        monitor.start();

        log.log("Simulacion Productor-Consumidor iniciada (buffer=" + BUFFER_CAPACITY + ")");
    }

    public void pausar() {
        if (!running) return;
        producer.pausar();
        consumerPar.pausar();
        consumerImpar.pausar();
        consumerPrimo.pausar();
        log.log("Simulacion pausada");
    }

    public void reanudar() {
        if (!running) return;
        producer.reanudar();
        consumerPar.reanudar();
        consumerImpar.reanudar();
        consumerPrimo.reanudar();
        log.log("Simulacion reanudada");
    }

    public void detener() {
        if (!running) return;
        running = false;
        producer.detener();
        consumerPar.detener();
        consumerImpar.detener();
        consumerPrimo.detener();
        log.log("Simulacion detenida");
    }

    public void setDelay(int ms) {
        if (producer != null) producer.setDelay(ms);
        if (consumerPar != null) consumerPar.setDelay(ms);
        if (consumerImpar != null) consumerImpar.setDelay(ms);
        if (consumerPrimo != null) consumerPrimo.setDelay(ms);
    }

    public SharedBuffer getBuffer() { return buffer; }
    public boolean isRunning() { return running; }
}
