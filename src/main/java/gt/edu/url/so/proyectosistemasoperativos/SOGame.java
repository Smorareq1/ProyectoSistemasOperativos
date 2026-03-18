package gt.edu.url.so.proyectosistemasoperativos;

import com.badlogic.gdx.Game;
import gt.edu.url.so.proyectosistemasoperativos.common.AudioManager;

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

    public void showLoading(LoadingScreen.Target target) {
        setScreen(new LoadingScreen(this, target));
    }

    @Override
    public void dispose() {
        super.dispose();
        AudioManager.getInstance().dispose();
    }
}
