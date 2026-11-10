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
import net.runelite.api.*;
import net.runelite.api.events.AmbientSoundEffectCreated;
import net.runelite.api.events.AreaSoundEffectPlayed;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import javax.sound.sampled.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@PluginDescriptor(
        name = "Sound Swapper",
        description = "Allows the user to replace any sound effect.<br><br>" +
                "To replace a sound, add its ID to the list in the plugin menu, then place a .wav file with the same name in your root<br>" +
                "RuneLite folder. The plugin will grab the sound and use it instead!<br><br>" +
                "* Note: Some regions may block sound swapping per Jagex TOS."
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

    @Inject
    public ChatMessageManager chatMessageManager;

    public HashMap<Integer, Sound> customSounds = new HashMap<>();

    public HashMap<Integer, Sound> customAreaSounds = new HashMap<>();

    public Set<Integer> whitelistedSounds = new HashSet<>();
    public Set<Integer> whitelistedAreaSounds = new HashSet<>();
    public Set<Integer> blacklistedSounds = new HashSet<>();
    public Set<Integer> blacklistedAreaSounds = new HashSet<>();
    public List<Integer> nativeSoundIDsToSwap = new ArrayList<>();
    public List<Integer> nativeSoundIDReplacements = new ArrayList<>();

    public HashMap<SoundType, String> failureMessages = new HashMap<>();

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
            case "soundEffects":
            {
                if (event.getNewValue().equals("true"))
                    updateSoundList(SoundType.SOUND, customSounds, config.customSounds());
                break;
            }

            case "customSounds":
            {
                updateSoundList(SoundType.SOUND, customSounds, event.getNewValue());
                break;
            }

            case "areaSoundEffects":
            {
                if (event.getNewValue().equals("true"))
                    updateSoundList(SoundType.AREA, customAreaSounds, config.customAreaSounds());
                break;
            }

            case "customAreaSounds":
            {
                updateSoundList(SoundType.AREA, customAreaSounds, event.getNewValue());
                break;
            }

            case "whitelistSounds":
            {
                whitelistedSounds = getIdSet(event.getNewValue());
                break;
            }

            case "whitelistAreaSounds":
            {
                whitelistedAreaSounds = getIdSet(event.getNewValue());
                break;
            }

            case "blacklistedSounds":
            {
                blacklistedSounds = getIdSet(event.getNewValue());
                break;
            }

            case "blacklistedAreaSounds":
            {
                blacklistedAreaSounds = getIdSet(event.getNewValue());
                break;
            }

            case "consumeAmbientSounds":
            {
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

            case "nativeSoundIDsToReplace":
            {
                nativeSoundIDsToSwap = getIds(event.getNewValue());
                break;
            }

            case "nativeSoundIDReplacements":
            {
                nativeSoundIDReplacements = getIds(event.getNewValue());
                break;
            }
        }

        soundEffectOverlay.resetLines();
    }

    void updateLists()
    {
        if (!config.customSounds().isEmpty())
        {
            updateSoundList(SoundType.SOUND, customSounds, config.customSounds());
        }

        if (!config.customAreaSounds().isEmpty())
        {
            updateSoundList(SoundType.AREA, customAreaSounds, config.customAreaSounds());
        }

        if (!config.whitelistSounds().isEmpty())
        {
            whitelistedSounds = getIdSet(config.whitelistSounds());
        }

        if (!config.whitelistAreaSounds().isEmpty())
        {
            whitelistedAreaSounds = getIdSet(config.whitelistAreaSounds());
        }

        if (!config.blacklistedSounds().isEmpty())
        {
            blacklistedSounds = getIdSet(config.blacklistedSounds());
        }

        if (!config.blacklistedAreaSounds().isEmpty())
        {
            blacklistedAreaSounds = getIdSet(config.blacklistedAreaSounds());
        }

        if (!config.nativeSoundIDsToReplace().isEmpty())
        {
            nativeSoundIDsToSwap = getIds(config.nativeSoundIDsToReplace());
        }

        if (!config.nativeSoundIDReplacements().isEmpty())
        {
            nativeSoundIDReplacements = getIds(config.nativeSoundIDReplacements());
        }
    }

    @Subscribe
    public void onSoundEffectPlayed(SoundEffectPlayed event)
    {
        int soundId = event.getSoundId();

        if (config.consumeSoundEffects() || blacklistedSounds.contains(soundId))
        {
            if (!whitelistedSounds.isEmpty() && whitelistedSounds.contains(soundId))
            {
                log.debug("whitelisted other sound effect passed: {}", soundId);
                return;
            }

            log.debug("consumed other sound effect: {}", soundId);
            event.consume();
            return;
        }

        if (BlockedRegions.inBlockedRegion(client))
        {
            return;
        }

        if (config.nativeSoundIDSwapEnable())
        {
            if (nativeSoundIDsToSwap.contains(soundId))
            {
                int idx = nativeSoundIDsToSwap.indexOf(soundId);
                Preferences preferences = client.getPreferences();
                int originalVolume = preferences.getSoundEffectVolume();
                int soundVolume =  originalVolume;

                if (config.enableCustomSoundsVolume())
                {
                    soundVolume = config.customSoundsVolume() * SoundEffectVolume.HIGH / 100;
                }

                if (idx < nativeSoundIDReplacements.size())
                {
                    event.consume();
                    preferences.setSoundEffectVolume(soundVolume);
                    client.playSoundEffect(nativeSoundIDReplacements.get(idx), soundVolume);
                    preferences.setSoundEffectVolume(originalVolume);
                    return;
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
    }

    @Subscribe
    public void onAreaSoundEffectPlayed(AreaSoundEffectPlayed event)
    {
        int soundId = event.getSoundId();

        if (config.consumeAreaSounds() || blacklistedAreaSounds.contains(soundId))
        {
            if (!whitelistedAreaSounds.isEmpty() && whitelistedAreaSounds.contains(soundId))
            {
                log.debug("whitelisted area sound effect passed: {}", soundId);
                return;
            }

            log.debug("consumed area sound effect: {}", soundId);
            event.consume();
            return;
        }

        if (BlockedRegions.inBlockedRegion(client))
        {
            return;
        }

        if (config.nativeSoundIDSwapEnable()) {
            if (nativeSoundIDsToSwap.contains(soundId))
            {
                int idx = nativeSoundIDsToSwap.indexOf(soundId);
                Preferences preferences = client.getPreferences();
                int originalVolume = preferences.getAreaSoundEffectVolume();
                int soundVolume =  originalVolume;

                if (config.enableCustomSoundsVolume())
                {
                    soundVolume = config.customSoundsVolume() * SoundEffectVolume.HIGH / 100;
                }

                if (idx < nativeSoundIDReplacements.size())
                {
                    event.consume();
                    preferences.setAreaSoundEffectVolume(soundVolume);
                    client.playSoundEffect(nativeSoundIDReplacements.get(idx), soundVolume);
                    preferences.setAreaSoundEffectVolume(originalVolume);
                    return;
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
    }

    @Subscribe
    public void onAmbientSoundEffectCreated(AmbientSoundEffectCreated event)
    {
        if (config.consumeAmbientSounds())
        {
            client.getAmbientSoundEffects().clear();
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

            // Notify the user on login for sound failures that are determined on Startup
            queueFailureMessages();
        }
    }

    private boolean tryLoadSound(SoundType type, HashMap<Integer, Sound> sounds, String sound_name, Integer sound_id)
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
            catch (UnsupportedAudioFileException e)
            {
                failureMessages.put(type, sound_name + ": unsupported audio format");
				log.warn("Unable to load custom sound {}", sound_name, e);
            }
            catch (IOException e)
            {
                failureMessages.put(type, sound_name + ": IO error");
				log.warn("Unable to load custom sound {}", sound_name, e);
            }
            catch (Exception e)
            {
                failureMessages.put(type, sound_name + ": unknown error");
                log.warn("Unable to load custom sound {}", sound_name, e);
            }
        }
        else
        {
            // Attempt a ".wav.wav" fallback for users who put the file type in name
            File fallback_file = new File(SOUND_DIR, sound_name + ".wav" + ".wav");
            if (fallback_file.exists())
            {
                log.debug("Located fallback file for custom sound {}", sound_name);
                return tryLoadSound(type, sounds, sound_name + ".wav", sound_id);
            }
            else
            {
                failureMessages.put(type, sound_name + ": file does not exist");
                log.warn("Unable to locate file for custom sound {}", sound_name);
            }
        }

        return false;
    }

    private void updateSoundList(SoundType type, HashMap<Integer, Sound> sounds, String configText)
    {
        sounds.clear();
        failureMessages.clear();

        for (String s : SoundListParser.parse(configText))
        {
            try
            {
                int id = Integer.parseInt(s);
                tryLoadSound(type, sounds, s, id);

            } catch (NumberFormatException e)
            {
                queueChatMessage("Sound Swapper has been provided with an invalid " + type.getType() + " Id: " + s);
                log.warn("Invalid {} sound ID: {}", type.getType(), s);
            }
        }

        queueFailureMessages();
    }

    private Set<Integer> getIdSet(String configText)
    {
        return new HashSet<>(getIds(configText));
    }

    private List<Integer> getIds(String configText)
    {
        if (configText == null || configText.isEmpty())
        {
            return List.of();
        }

        boolean shouldNotify = false;
        List<Integer> ids = new ArrayList<>();
        for (String s : SoundListParser.parse(configText))
        {
            try
            {
                int id = Integer.parseInt(s);
                ids.add(id);
            }
            catch (NumberFormatException e)
            {
                log.warn("Invalid id when parsing {}: {}", configText, s);
                shouldNotify = true;
            }
        }

        if (shouldNotify)
            queueChatMessage("Sound Swapper failed to parse a portion of the recently altered config due to an invalid value: " + configText);

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
        whitelistedSounds = new HashSet<>();
        whitelistedAreaSounds = new HashSet<>();
        blacklistedSounds = new HashSet<>();
        blacklistedAreaSounds = new HashSet<>();
        nativeSoundIDsToSwap = new ArrayList<>();
        nativeSoundIDReplacements = new ArrayList<>();
        failureMessages = new HashMap<>();
        soundEffectOverlay.resetLines();
    }

    public void queueChatMessage(String message)
    {
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.GAMEMESSAGE).value(message).build());
        }
    }

    public void queueFailureMessages()
    {
        if (failureMessages.isEmpty())
            return;

        queueChatMessage("Sound Swapper failed to load the following sounds:");

        failureMessages.forEach((type, failure) ->
        {
            // e.g. "- Sound Id 2266: file does not exist" or "- Area Sound Id 10236: unsupported audio file format"
            String message = "- " + type.getType() + " Id " + failure;

            queueChatMessage(message);
        });
    }
}
