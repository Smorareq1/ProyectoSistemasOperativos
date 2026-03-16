package gt.edu.url.so.proyectosistemasoperativos.philosophers;

import gt.edu.url.so.proyectosistemasoperativos.common.GameLogger;

import java.util.Random;

public class Philosopher extends Thread {
    private final int id;
    private final DPController controller;
    private final GameLogger log;
    private final PhilosopherConfig config;
    private final Random random = new Random();
    private volatile boolean running = true;
    private volatile boolean paused = false;

    public Philosopher(int id, DPController controller, GameLogger log, PhilosopherConfig config) {
        super("Filosofo-" + id);
        setDaemon(true);
        this.id = id;
        this.controller = controller;
        this.log = log;
        this.config = config;
    }

    private void esperarSiPausado() throws InterruptedException {
        while (paused && running) {
            Thread.sleep(100);
        }
    }

    @Override
    public void run() {
        try {
            while (running) {
                esperarSiPausado();
                if (!running) break;

                // Pensar
                log.log("F" + id + " esta pensando");
                int thinkTime = config.getTiempoMinPensar() +
                        random.nextInt(config.getTiempoMaxPensar() - config.getTiempoMinPensar() + 1);
                Thread.sleep(thinkTime);

                esperarSiPausado();
                if (!running) break;

                // Tomar tenedores
                log.log("F" + id + " quiere comer — intentando tomar tenedores");
                controller.tomarTenedores(id);

                esperarSiPausado();
                if (!running) break;

                // Comer
                int leftFork = id;
                int rightFork = (id + 1) % controller.getN();
                log.log("F" + id + " tomo tenedores T" + leftFork + " y T" + rightFork + " — comiendo");
                int eatTime = config.getTiempoMinComer() +
                        random.nextInt(config.getTiempoMaxComer() - config.getTiempoMinComer() + 1);
                Thread.sleep(eatTime);

                esperarSiPausado();
                if (!running) break;

                // Soltar tenedores
                controller.soltarTenedores(id);
                log.log("F" + id + " solto tenedores T" + leftFork + " y T" + rightFork + " — pensando");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getPhilosopherId() { return id; }
    public void pausar() { paused = true; }
    public void reanudar() { paused = false; }
    public void detener() { running = false; this.interrupt(); }
}
