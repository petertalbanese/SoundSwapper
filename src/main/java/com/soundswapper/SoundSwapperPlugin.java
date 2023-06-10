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
import net.runelite.api.events.AreaSoundEffectPlayed;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.client.RuneLite;
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
		enabledByDefault = false,
		description = "Allows the user to replace any sound effect.\n" +
				"\n" +
				"To replace a sound, add its ID to the list in the plugin menu, then place a .wav file with the same name in your root\n" +
				"RuneLite folder. The plugin will grab the sound and use it instead!"
)
public class SoundSwapperPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private EventBus eventBus;

	@Inject
	private SoundSwapperConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private SoundEffectOverlay soundEffectOverlay;

	public HashMap<Integer, Clip> soundClips = new HashMap<>();
	public HashMap<Integer, Clip> areaSoundClips = new HashMap<>();

	public List<Integer> whitelistedSounds = new ArrayList<>();
	public List<Integer> whitelistedAreaSounds = new ArrayList<>();
	public List<Integer> blacklistedSounds = new ArrayList<>();
	public List<Integer> blacklistedAreaSounds = new ArrayList<>();

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
				updateClipList(soundClips, event.getNewValue());
				break;
			}

			case "customAreaSounds":
			{
				updateClipList(areaSoundClips, event.getNewValue());
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
		}

		soundEffectOverlay.resetLines();
	}

	void updateLists()
	{
		if (!config.customSounds().isEmpty())
		{
			updateClipList(soundClips, config.customSounds());
		}

		if (!config.customAreaSounds().isEmpty())
		{
			updateClipList(areaSoundClips, config.customAreaSounds());
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
	}

	@Subscribe
	public void onSoundEffectPlayed(SoundEffectPlayed event)
	{
		int soundId = event.getSoundId();

		if (config.soundEffects())
		{
			if (soundClips.containsKey(soundId))
			{
				event.consume();
				playCustomSound(soundClips.get(soundId));
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

		if (config.areaSoundEffects())
		{
			if (areaSoundClips.containsKey(soundId))
			{
				event.consume();
				playCustomSound(areaSoundClips.get(soundId));
				return;
			}
		}

		if (config.consumeAreaSounds() || blacklistedSounds.contains(soundId))
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

	private boolean tryLoadSound(HashMap<Integer, Clip> map, String sound_name, Integer sound_id)
	{
		File sound_file = new File(SOUND_DIR, sound_name + ".wav");

		if (sound_file.exists())
		{
			try
			{
				InputStream fileStream = new BufferedInputStream(new FileInputStream(sound_file));
				AudioInputStream sound = AudioSystem.getAudioInputStream(fileStream);

				Clip clip = AudioSystem.getClip();
				clip.open(sound);
				map.put(sound_id, clip);

				return true;
			}
			catch (UnsupportedAudioFileException | IOException | LineUnavailableException e)
			{
				log.warn("Unable to load custom sound " + sound_name, e);
			}
		}

		return false;
	}

	private void updateClipList(HashMap<Integer, Clip> clips, String configText)
	{
		for (int sound : clips.keySet())
		{
			Clip clip = clips.get(sound);

			if (clip != null)
			{
				if (clip.isOpen())
				{
					clip.close();
				}
			}
		}

		clips.clear();

		for (String s : Text.fromCSV(configText))
		{
			try
			{
				int id = Integer.parseInt(s);
				tryLoadSound(clips, s, id);
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

	private void playCustomSound(Clip clip)
	{
		if (clip == null)
		{
			return;
		}

		clip.setFramePosition(0);
		clip.start();
	}

	private void reset()
	{
		soundClips = new HashMap<>();
		areaSoundClips = new HashMap<>();
		whitelistedSounds = new ArrayList<>();
		whitelistedAreaSounds = new ArrayList<>();
		blacklistedSounds = new ArrayList<>();
		blacklistedAreaSounds = new ArrayList<>();
		soundEffectOverlay.resetLines();
	}
}