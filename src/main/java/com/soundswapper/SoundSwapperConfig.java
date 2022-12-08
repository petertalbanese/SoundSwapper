package com.soundswapper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("soundswapper")
public interface SoundSwapperConfig extends Config
{
	@ConfigItem(
			keyName = "SoundEffects",
			name = "Sounds Effects",
			description = "Swap sound effects with custom sounds",
			position = 0
	)
	default boolean soundEffects() { return false; }

	@ConfigItem(
			keyName = "AreaSoundEffects",
			name = "Area Sound Effects",
			description = "Swap area sound effects with custom sounds",
			position = 1
	)
	default boolean areaSoundEffects() { return false; }

	@ConfigItem(
			keyName = "CustomSounds",
			name = "Custom Sounds",
			description = "Sounds to replace with your own custom .wav files. Separate with comma. Sound List: https://oldschool.runescape.wiki/w/List_of_in-game_sound_IDs",
			position = 2
	)
	default String customSounds()
	{
		return "";
	}
}

