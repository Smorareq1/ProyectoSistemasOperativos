package gt.edu.url.so.proyectosistemasoperativos.philosophers;

public class Fork {
    private final int id;
    private boolean taken = false;
    private int heldBy = -1;

    public Fork(int id) {
        this.id = id;
    }

    public int getId() { return id; }
    public boolean isTaken() { return taken; }
    public int getHeldBy() { return heldBy; }

    public void take(int philosopherId) {
        this.taken = true;
        this.heldBy = philosopherId;
    }

    public void release() {
        this.taken = false;
        this.heldBy = -1;
    }
}
