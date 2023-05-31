package com.soundswapper;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Provides;
import javax.inject.Inject;
import javax.sound.sampled.*;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.AreaSoundEffectPlayed;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import java.io.*;
import java.util.HashMap;

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
	private SoundSwapperConfig config;

	private HashMap<Integer, Clip> clips = new HashMap<Integer, Clip>();

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
		SOUND_DIR.mkdir();
		updateList();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!CONFIG_GROUP.equals(event.getGroup())) {
			return;
		}

		updateList();
	}

	@VisibleForTesting
	void updateList()
	{
		for (int sound : clips.keySet()) {
			Clip clip = clips.get(sound);
			if (clip != null) {
				if (clip.isOpen()) {
					clip.close();
				}
			}
		}
		clips.clear();
		for (String s : Text.fromCSV(config.customSounds())) {
			try {
				int id = Integer.parseInt(s);
				tryLoadSound(s, id);
			} catch (NumberFormatException e) {
				log.warn("Invalid sound ID: {}", s);
			}
		}
	}

	@Subscribe
	public void onSoundEffectPlayed(SoundEffectPlayed event)
	{
		if (config.soundEffects()) {
			int eventSound = event.getSoundId();
			if (clips.containsKey(eventSound)) {
				event.consume();
				playCustomSound(eventSound);
			}
		}
	}

	@Subscribe
	public void onAreaSoundEffectPlayed(AreaSoundEffectPlayed event)
	{
		if (config.areaSoundEffects()) {
			int eventSound = event.getSoundId();
			if (clips.containsKey(eventSound)) {
				event.consume();
				playCustomSound(eventSound);
			}
		}
	}

	private boolean tryLoadSound(String sound_name, Integer sound_id)
	{
		File sound_file = new File(SOUND_DIR, sound_name + ".wav");
		if (sound_file.exists()) {
			try {
				InputStream fileStream = new BufferedInputStream(new FileInputStream(sound_file));
				AudioInputStream sound = AudioSystem.getAudioInputStream(fileStream);
				Clip clip = AudioSystem.getClip();
				clip.open(sound);
				clips.put(sound_id, clip);
				return true;
			} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
				log.warn("Unable to load custom sound " + sound_name, e);
			}
		}

		return false;
	}

	private synchronized void playCustomSound(Integer sound_id)
	{
		Clip clip = clips.get(sound_id);
		if (clip == null) {
			return;
		}

		clip.setFramePosition(0);
		clip.start();
	}
}
