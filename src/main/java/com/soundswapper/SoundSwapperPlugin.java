package com.soundswapper;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
        name = "Sound Swapper",
        enabledByDefault = false,
        description = "Allows the user to replace any sound effect.\n" +
                "\n" +
                "To replace a sound, add its ID to the list in the plugin menu, then place a .wav file with the same name in your root\n" +
                "RuneLite folder. The plugin will grab the sound and use it instead!"
)
public class SoundSwapperPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private SoundSwapperConfig config;

    private final HashMap<Integer, Clip> clips = new HashMap<>();
    private static final File SOUND_DIR = new File(RuneLite.RUNELITE_DIR, "SoundSwapper");

    @Provides
    SoundSwapperConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(SoundSwapperConfig.class);
    }

    @Override
    protected void startUp() {
        SOUND_DIR.mkdir();
        updateList();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        updateList();
    }

    //Updates soundList from runelite plugin config
    @VisibleForTesting
    void updateList() {
        //Creates intlist from config ids
        List<Integer> ids = Text.fromCSV(config.customSounds()).stream()
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        //Removes all clips that are not in the list and closes them
        for (Integer id : ids) {
            Clip clip = clips.get(id);
            if (!clips.containsKey(id)) {
                //closes removed clip
                clip.close();
                //'Clip' used without 'try'-with-resources statement
                //removes clip from map
                clips.remove(id);
            }
        }
        //Loads all clips which are in the config if they arent loaded yet
        loadClips(ids);
    }

    private void loadClips(List<Integer> ids) {
        //Loads all clips to the map
        Thread sounds = new Thread(() -> {
            for (Integer id : ids) {
                Clip clip = clips.get(id);
                if (clip == null || !clip.isOpen()) {
                    File file = new File(SOUND_DIR, id + ".wav");
                    try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file) ){
                        if (!file.exists()) {
                            log.warn("File {} does not exist", file);
                            return;
                        }
                        clip = AudioSystem.getClip();
                        clip.open(audioInputStream);
                        clips.put(id, clip);
                    } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                        log.warn("Error loading sound {}", id, e);
                    }
                }
            }
        });
        sounds.start();
    }

    @Subscribe
    public void onSoundEffectPlayed(SoundEffectPlayed event) {
        int eventSound = event.getSoundId();
        Clip clip = clips.get(eventSound);
        if (clip != null && clip.isOpen()) {
            event.consume();
            clip.loop(0);
        }
    }
}
