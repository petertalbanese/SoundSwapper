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
import java.util.Map;
import java.util.HashMap;

@Slf4j
@PluginDescriptor(
	name = "Sound Swapper",
	enabledByDefault = false,
	description = "Allows the user to replace any sound named in the wiki sounds list: https://oldschool.runescape.wiki/w/List_of_in-game_sound_IDs\n" +
			"\n" +
			"To replace a sound, add its name to the list in the plugin menu, then place a .wav file with the same name in the \n" +
			"SoundSwapper folder in your root RuneLite folder. The plugin will grab the sound and use it instead!"
)
public class SoundSwapperPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private SoundSwapperConfig config;

	private Clip clip;

	private static final File SOUND_DIR = new File(RuneLite.RUNELITE_DIR, "SoundSwapper");

	private static final SoundIds soundIds = new SoundIds();
	private Map<Integer, String> soundList = new HashMap<Integer, String>();

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
		updateList();
	}

	@VisibleForTesting
	void updateList()
	{
		soundList.clear();
		for (String s : Text.fromCSV(config.customSounds()))
		{
			// I had to split soundIds into 2 pieces because it was too large
			Integer id = soundIds.soundIds1.get(s);
			if (id == null) {
				id = soundIds.soundIds2.get(s);
			}
			if (id != null) {
				soundList.put(id, s);
			}
		}
	}

	@Subscribe
	public void onSoundEffectPlayed(SoundEffectPlayed event) {
		String sound_name = soundList.get(event.getSoundId());
		if (sound_name != null) {
			event.consume();
			playCustomSound(sound_name + ".wav");
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
			return;
		}
	}

	private boolean tryLoadSound(String sound_name, File sound_file)
	{
		if (sound_file.exists())
		{
			try (InputStream fileStream = new BufferedInputStream(new FileInputStream(sound_file));
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
		try (InputStream fileStream = new BufferedInputStream(Notifier.class.getResourceAsStream(sound_name));
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
