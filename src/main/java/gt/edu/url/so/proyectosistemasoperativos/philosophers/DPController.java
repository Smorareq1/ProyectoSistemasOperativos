package gt.edu.url.so.proyectosistemasoperativos.philosophers;

import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

public class DPController {
    private final int N;
    private final EstadoFilosofo[] estado;
    private final Semaphore mutex;
    private final Semaphore[] sem;
    private final Fork[] forks;
    private Consumer<int[]> stateChangeCallback;

    public DPController(int n) {
        this.N = n;
        this.estado = new EstadoFilosofo[N];
        this.mutex = new Semaphore(1);
        this.sem = new Semaphore[N];
        this.forks = new Fork[N];

        for (int i = 0; i < N; i++) {
            estado[i] = EstadoFilosofo.PENSANDO;
            sem[i] = new Semaphore(0);
            forks[i] = new Fork(i);
        }
    }

    public void setStateChangeCallback(Consumer<int[]> callback) {
        this.stateChangeCallback = callback;
    }

    private int izq(int i) { return (i + N - 1) % N; }
    private int der(int i) { return (i + 1) % N; }

    private void test(int i) {
        if (estado[i] == EstadoFilosofo.ESPERANDO
                && estado[izq(i)] != EstadoFilosofo.COMIENDO
                && estado[der(i)] != EstadoFilosofo.COMIENDO) {
            estado[i] = EstadoFilosofo.COMIENDO;
            forks[i].take(i);
            forks[der(i)].take(i);
            sem[i].release();
        }
    }

    public void tomarTenedores(int i) throws InterruptedException {
        mutex.acquire();
        try {
            estado[i] = EstadoFilosofo.ESPERANDO;
            test(i);
            notifyStateChange();
        } finally {
            mutex.release();
        }
        sem[i].acquire();
        notifyStateChange();
    }

    public void soltarTenedores(int i) throws InterruptedException {
        mutex.acquire();
        try {
            estado[i] = EstadoFilosofo.PENSANDO;
            forks[i].release();
            forks[der(i)].release();
            test(izq(i));
            test(der(i));
            notifyStateChange();
        } finally {
            mutex.release();
        }
    }

    private void notifyStateChange() {
        if (stateChangeCallback != null) {
            stateChangeCallback.accept(getSnapshot());
        }
    }

    public int[] getSnapshot() {
        int[] snap = new int[N * 2];
        for (int i = 0; i < N; i++) {
            snap[i] = estado[i].ordinal();
            snap[N + i] = forks[i].getHeldBy();
        }
        return snap;
    }

    public int getN() { return N; }

    public EstadoFilosofo getEstado(int i) { return estado[i]; }

    public Fork getFork(int i) { return forks[i]; }
}
