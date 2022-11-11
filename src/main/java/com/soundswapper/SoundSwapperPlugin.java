package com.soundswapper;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Provides;
import javax.inject.Inject;
import javax.sound.sampled.*;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.client.Notifier;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

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

	private Clip clip;

	private static final File SOUND_DIR = new File(RuneLite.RUNELITE_DIR, "SoundSwapper");

	private final ArrayList<Integer> soundList = new ArrayList<>();

	@Provides
	SoundSwapperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SoundSwapperConfig.class);
	}

	@Override
	protected void startUp()
	{
		SOUND_DIR.mkdir();
		updateList();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		updateList();
	}

	//Updates soundList from runelite plugin config
	@VisibleForTesting
	void updateList()
	{
		//clears soundList
		soundList.clear();
		for (String s : Text.fromCSV(config.customSounds()))
		{
			try {
				int id = Integer.parseInt(s);
				soundList.add(id);
			} catch (NumberFormatException e) {
				log.warn("Invalid sound ID: {}", s);
			}
		}
	}

	@Subscribe
	public void onSoundEffectPlayed(SoundEffectPlayed event) {
		int eventSound = event.getSoundId();
		if (soundList.contains(eventSound)) {
			event.consume();
			playCustomSound(eventSound + ".wav");
		}
	}

	private synchronized void playCustomSound(String sound_name) {
		File sound_file = new File(SOUND_DIR, sound_name);

		try {
			if (clip != null) {
				clip.close();
			}

			clip = AudioSystem.getClip();

			if (!tryLoadSound(sound_name, sound_file)) {
				return;
			}

			clip.loop(0);
		} catch (LineUnavailableException e) {
			log.warn("Unable to play custom sound " + sound_name, e);
		}
	}

	private boolean tryLoadSound(String sound_name, File sound_file)
	{
		if (sound_file.exists())
		{

			try (InputStream fileStream = Files.newInputStream(sound_file.toPath());
				 AudioInputStream sound = AudioSystem.getAudioInputStream(fileStream))
			{
				clip.open(sound);
				return true;
			}
			catch (UnsupportedAudioFileException | IOException | LineUnavailableException e)
			{
				log.warn("Unable to load custom sound " + sound_name, e);
			}
		}

		// Otherwise load from the classpath
		try (InputStream fileStream = Notifier.class.getResourceAsStream(sound_name);
			 AudioInputStream sound = AudioSystem.getAudioInputStream(fileStream))
		{
			clip.open(sound);
			return true;
		}
		catch (UnsupportedAudioFileException | IOException | LineUnavailableException e)
		{
			log.warn("Unable to load custom sound " + sound_name, e);
		}
		return false;
	}
}
