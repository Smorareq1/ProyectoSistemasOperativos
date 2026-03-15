package gt.edu.url.so.proyectosistemasoperativos.philosophers;

public class PhilosopherConfig {
    private int numFilosofos = 5;
    private int tiempoMinPensar = 500;
    private int tiempoMaxPensar = 3000;
    private int tiempoMinComer = 500;
    private int tiempoMaxComer = 2000;

    public int getNumFilosofos() { return numFilosofos; }
    public void setNumFilosofos(int n) { this.numFilosofos = Math.max(2, Math.min(10, n)); }

    public int getTiempoMinPensar() { return tiempoMinPensar; }
    public void setTiempoMinPensar(int t) { this.tiempoMinPensar = t; }

    public int getTiempoMaxPensar() { return tiempoMaxPensar; }
    public void setTiempoMaxPensar(int t) { this.tiempoMaxPensar = t; }

    public int getTiempoMinComer() { return tiempoMinComer; }
    public void setTiempoMinComer(int t) { this.tiempoMinComer = t; }

    public int getTiempoMaxComer() { return tiempoMaxComer; }
    public void setTiempoMaxComer(int t) { this.tiempoMaxComer = t; }
}
