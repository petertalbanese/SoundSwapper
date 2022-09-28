package com.soundswapper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("soundswapper")
public interface SoundSwapperConfig extends Config
{
	@ConfigItem(
			keyName = "CustomSounds",
			name = "Custom Sounds",
			description = "Sounds to replace with your own custom .wav files. Separate with comma. Sound List: https://oldschool.runescape.wiki/w/List_of_in-game_sound_IDs"
	)
	default String customSounds()
	{
		return "";
	}
}
