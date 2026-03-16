package gt.edu.url.so.proyectosistemasoperativos;

import com.badlogic.gdx.Game;

public class SOGame extends Game {

    @Override
    public void create() {
        setScreen(new MenuScreen(this));
    }

    public void showMenu() {
        setScreen(new MenuScreen(this));
    }

    public void showDiningPhilosophers() {
        setScreen(new DPScreen(this));
    }

    public void showProducerConsumer() {
        setScreen(new PCScreen(this));
    }
}
