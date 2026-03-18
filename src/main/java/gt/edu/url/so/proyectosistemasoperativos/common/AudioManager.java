package gt.edu.url.so.proyectosistemasoperativos.common;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

/**
 * Manages audio for the project.
 * Note: Place your audio files in src/main/resources/audio/
 */
public class AudioManager {
    private static AudioManager instance;

    private Music currentMusic;
    private Sound btnSound;

    // assets/audio folder is mapped as root in build.gradle sourceSets
    private final String AUDIO_PATH = "audio/";

    private AudioManager() {
    }

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    public void playMusic(String filename, boolean loop) {
        Gdx.app.log("AudioManager", "Playing music: " + filename);
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic.dispose();
        }
        try {
            // Using internal() which points to the assets folder
            currentMusic = Gdx.audio.newMusic(Gdx.files.internal(AUDIO_PATH + filename));
            currentMusic.setLooping(loop);
            currentMusic.setVolume(0.4f); // Reduced volume
            currentMusic.play();
            Gdx.app.log("AudioManager", "Music started: " + filename);
        } catch (Exception e) {
            Gdx.app.error("AudioManager", "Could not load music: " + filename + " Error: " + e.getMessage());
        }
    }

    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
        }
    }

    public void playButtonSound() {
        if (btnSound == null) {
            try {
                Gdx.app.log("AudioManager", "Loading sound: button_click.mp3");
                btnSound = Gdx.audio.newSound(Gdx.files.internal(AUDIO_PATH + "button_click.mp3"));
            } catch (Exception e) {
                Gdx.app.error("AudioManager", "Could not load sound: button_click.mp3 Error: " + e.getMessage());
                return;
            }
        }
        btnSound.play(0.5f); // Reduced volume
        Gdx.app.log("AudioManager", "Button sound played");
    }

    public void dispose() {
        if (currentMusic != null)
            currentMusic.dispose();
        if (btnSound != null)
            btnSound.dispose();
    }
}
