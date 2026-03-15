package gt.edu.url.so.proyectosistemasoperativos.producerconsumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class SharedBuffer {
    private final List<Integer> buffer;
    private final int capacity;
    private final Semaphore empty;
    private final Semaphore full;
    private final Semaphore mutex;

    public SharedBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new ArrayList<>(capacity);
        this.empty = new Semaphore(capacity);
        this.full = new Semaphore(0);
        this.mutex = new Semaphore(1);
    }

    public void insertar(int numero) throws InterruptedException {
        empty.acquire();
        mutex.acquire();
        try {
            buffer.add(numero);
        } finally {
            mutex.release();
        }
        full.release();
    }

    public Integer buscarYExtraer(TipoNumero tipo) throws InterruptedException {
        if (!full.tryAcquire(500, TimeUnit.MILLISECONDS)) {
            return null;
        }
        mutex.acquire();
        Integer found = null;
        try {
            for (int i = 0; i < buffer.size(); i++) {
                if (NumberClassifier.clasificar(buffer.get(i)) == tipo) {
                    found = buffer.remove(i);
                    break;
                }
            }
        } finally {
            mutex.release();
        }
        if (found != null) {
            empty.release();
        } else {
            full.release();
        }
        return found;
    }

    public boolean tieneElementosDeTipo(TipoNumero tipo) {
        try {
            mutex.acquire();
            try {
                for (int num : buffer) {
                    if (NumberClassifier.clasificar(num) == tipo) return true;
                }
            } finally {
                mutex.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    public List<Integer> getSnapshot() {
        try {
            mutex.acquire();
            try {
                return new ArrayList<>(buffer);
            } finally {
                mutex.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }
    }

    public int getCapacity() { return capacity; }

    public int getCount() {
        try {
            mutex.acquire();
            try {
                return buffer.size();
            } finally {
                mutex.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }
}
