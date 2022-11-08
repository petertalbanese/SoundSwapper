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
			description = "Sounds to replace with your own custom .wav files. Separate with comma."
	)
	default String customSounds()
	{
		return "";
	}
}
