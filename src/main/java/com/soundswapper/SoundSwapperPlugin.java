/*
 * Copyright (c) 2023, petertalbanese <https://github.com/petertalbanese>
 * Copyright (c) 2023, damencs <https://github.com/damencs>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.soundswapper;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.AreaSoundEffectPlayed;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import javax.sound.sampled.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@PluginDescriptor(
        name = "Sound Swapper",
        description = "Allows the user to replace any sound effect.<br><br>" +
                "To replace a sound, add its ID to the list in the plugin menu, then place a .wav file with the same name in your root<br>" +
                "RuneLite folder. The plugin will grab the sound and use it instead!"
)
public class SoundSwapperPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private EventBus eventBus;

    @Inject
    private SoundSwapperConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private SoundEffectOverlay soundEffectOverlay;

    public HashMap<Integer, Sound> customSounds = new HashMap<>();

    public HashMap<Integer, Sound> customAreaSounds = new HashMap<>();

    public List<Integer> whitelistedSounds = new ArrayList<>();
    public List<Integer> whitelistedAreaSounds = new ArrayList<>();
    public List<Integer> blacklistedSounds = new ArrayList<>();
    public List<Integer> blacklistedAreaSounds = new ArrayList<>();
    public List<Integer> simpleIdsToSwap = new ArrayList<>();
    public List<Integer> simpleIdReplacements = new ArrayList<>();

    private static final File SOUND_DIR = new File(RuneLite.RUNELITE_DIR, "SoundSwapper");

    private static final String CONFIG_GROUP = "soundswapper";

    @Provides
    SoundSwapperConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(SoundSwapperConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        try
        {
            if (!SOUND_DIR.exists())
            {
                SOUND_DIR.mkdir();
            }
        }
        catch (SecurityException securityException)
        {
            log.error("Attempted to create SoundSwapper directory and a security exception prompted a fault");
        }

        updateLists();

        overlayManager.add(soundEffectOverlay);
        eventBus.register(soundEffectOverlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        eventBus.unregister(soundEffectOverlay);
        overlayManager.remove(soundEffectOverlay);
        reset();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!CONFIG_GROUP.equals(event.getGroup()))
        {
            return;
        }

        switch (event.getKey())
        {
            case "customSounds":
            {
                updateSoundList(customSounds, event.getNewValue());
                break;
            }

            case "customAreaSounds":
            {
                updateSoundList(customAreaSounds, event.getNewValue());
                break;
            }

            case "whitelistSounds":
            {
                whitelistedSounds = getIds(event.getNewValue());
                break;
            }

            case "whitelistAreaSounds":
            {
                whitelistedAreaSounds = getIds(event.getNewValue());
                break;
            }

            case "blacklistedSounds":
            {
                blacklistedSounds = getIds(event.getNewValue());
                break;
            }

            case "blacklistedAreaSounds":
            {
                blacklistedAreaSounds = getIds(event.getNewValue());
                break;
            }

            case "consumeAmbientSounds": {
                clientThread.invokeLater(() ->
                {
                    // Reload the scene to reapply ambient sounds
                    if (client.getGameState() == GameState.LOGGED_IN)
                    {
                        client.setGameState(GameState.LOADING);
                    }
                });
                break;
            }

            case "simpleIdsToReplace": {
                simpleIdsToSwap = getIds(event.getNewValue());
                break;
            }

            case "simpleIdsReplacements": {
                simpleIdReplacements = getIds(event.getNewValue());
                break;
            }
        }

        soundEffectOverlay.resetLines();
    }

    void updateLists()
    {
        if (!config.customSounds().isEmpty())
        {
            updateSoundList(customSounds, config.customSounds());
        }

        if (!config.customAreaSounds().isEmpty())
        {
            updateSoundList(customAreaSounds, config.customAreaSounds());
        }

        if (!config.whitelistSounds().isEmpty())
        {
            whitelistedSounds = getIds(config.whitelistSounds());
        }

        if (!config.whitelistAreaSounds().isEmpty())
        {
            whitelistedAreaSounds = getIds(config.whitelistAreaSounds());
        }

        if (!config.blacklistedSounds().isEmpty())
        {
            blacklistedSounds = getIds(config.blacklistedSounds());
        }

        if (!config.blacklistedAreaSounds().isEmpty())
        {
            blacklistedAreaSounds = getIds(config.blacklistedAreaSounds());
        }

        if (!config.simpleIdsToReplace().isEmpty())
        {
            simpleIdsToSwap = getIds(config.simpleIdsReplacements());
        }

        if (!config.simpleIdsReplacements().isEmpty())
        {
            simpleIdReplacements = getIds(config.simpleIdsReplacements());
        }
    }

    @Subscribe
    public void onSoundEffectPlayed(SoundEffectPlayed event)
    {
        int soundId = event.getSoundId();

        if (config.simpleIdSwaps()) {
            if (simpleIdsToSwap.contains(soundId))
            {
                int idx = simpleIdsToSwap.indexOf(soundId);

                if (idx < simpleIdReplacements.size())
                {
                    event.consume();
                    soundId = -1;
                    client.playSoundEffect(simpleIdReplacements.get(idx), 100 );
                }
            }
        }

        if (config.soundEffects())
        {
            if (customSounds.containsKey(soundId))
            {
                event.consume();
                playCustomSound(customSounds.get(soundId), (config.enableCustomSoundsVolume() ? config.customSoundsVolume() : -1));
                return;
            }
        }

        if (config.consumeSoundEffects() || blacklistedSounds.contains(soundId))
        {
            if (!whitelistedSounds.isEmpty() && whitelistedSounds.contains(soundId))
            {
                log.debug("whitelisted other sound effect passed: {}", soundId);
                return;
            }

            log.debug("consumed other sound effect: {}", soundId);
            event.consume();
        }
    }

    @Subscribe
    public void onAreaSoundEffectPlayed(AreaSoundEffectPlayed event)
    {
        int soundId = event.getSoundId();

        if (config.simpleIdSwaps()) {
            if (simpleIdsToSwap.contains(soundId))
            {
                int idx = simpleIdsToSwap.indexOf(soundId);

                if (idx < simpleIdReplacements.size())
                {
                    event.consume();
                    soundId = -1;
                    client.playSoundEffect(simpleIdReplacements.get(idx));
                }
            }
        }

        if (config.areaSoundEffects())
        {
            if (customAreaSounds.containsKey(soundId))
            {
                event.consume();
                playCustomSound(customAreaSounds.get(soundId), (config.enableCustomAreaSoundsVolume() ? config.customAreaSoundsVolume() : -1));
                return;
            }
        }

        if (config.consumeAreaSounds() || blacklistedAreaSounds.contains(soundId))
        {
            if (!whitelistedAreaSounds.isEmpty() && whitelistedAreaSounds.contains(soundId))
            {
                log.debug("whitelisted area sound effect passed: {}", soundId);
                return;
            }

            log.debug("consumed area sound effect: {}", soundId);
            event.consume();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        GameState gameState = gameStateChanged.getGameState();
        if (gameState == GameState.LOGGED_IN)
        {
            if (config.consumeAmbientSounds())
            {
                client.getAmbientSoundEffects().clear();
            }
        }
    }

    private boolean tryLoadSound(HashMap<Integer, Sound> sounds, String sound_name, Integer sound_id)
    {
        File sound_file = new File(SOUND_DIR, sound_name + ".wav");

        if (sound_file.exists())
        {
            try
            {
                InputStream fileStream = new BufferedInputStream(new FileInputStream(sound_file));
                AudioInputStream stream = AudioSystem.getAudioInputStream(fileStream);

                int streamLen = (int)stream.getFrameLength() * stream.getFormat().getFrameSize();
                byte[] bytes = new byte[streamLen];
                stream.read(bytes);

                Sound sound = new Sound(bytes, stream.getFormat(), streamLen);
                sounds.put(sound_id, sound);

                return true;
            }
            catch (UnsupportedAudioFileException | IOException e)
            {
                log.warn("Unable to load custom sound " + sound_name, e);
            }
        }

        return false;
    }

    private void updateSoundList(HashMap<Integer, Sound> sounds, String configText)
    {
        sounds.clear();

        for (String s : Text.fromCSV(configText))
        {
            try
            {
                int id = Integer.parseInt(s);
                tryLoadSound(sounds, s, id);
            } catch (NumberFormatException e)
            {
                log.warn("Invalid sound ID: {}", s);
            }
        }
    }

    private List<Integer> getIds(String configText)
    {
        if (configText == null || configText.isEmpty())
        {
            return List.of();
        }

        List<Integer> ids = new ArrayList<>();
        for (String s : Text.fromCSV(configText))
        {
            try
            {
                int id = Integer.parseInt(s);
                ids.add(id);
            }
            catch (NumberFormatException e)
            {
                log.warn("Invalid id when parsing {}: {}", configText, s);
            }
        }

        return ids;
    }

    private void playCustomSound(Sound sound, int volume)
    {
        try
        {
            Clip clip = AudioSystem.getClip();
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            clip.open(sound.getFormat(), sound.getBytes(), 0, sound.getNumBytes());

            if (volume != -1)
            {
                FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

                if (control != null)
                {
                    control.setValue((float)(volume / 2 - 45));
                }
            }

            clip.setFramePosition(0);
            clip.start();
        } catch (LineUnavailableException e)
        {
            log.warn("Failed to play custom sound");
        }
    }

    private void reset()
    {
        customSounds = new HashMap<>();
        customAreaSounds = new HashMap<>();
        whitelistedSounds = new ArrayList<>();
        whitelistedAreaSounds = new ArrayList<>();
        blacklistedSounds = new ArrayList<>();
        blacklistedAreaSounds = new ArrayList<>();
        soundEffectOverlay.resetLines();
    }
}
