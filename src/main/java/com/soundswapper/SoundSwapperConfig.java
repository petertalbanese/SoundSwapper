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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("soundswapper")
public interface SoundSwapperConfig extends Config
{
	/**
	 * Sections
	 */
	@ConfigSection(
			name = "Sound Effects",
			description = "Configuration settings for Sound Effects",
			position = 1,
			closedByDefault = true
	)
	String SOUND_EFFECTS_SECTION = "soundEffects";

	@ConfigSection(
			name = "Area Sound Effects",
			description = "Configuration settings for Area Sound Effects",
			position = 2,
			closedByDefault = true
	)
	String AREA_SOUND_EFFECTS_SECTION = "areaSoundEffects";

	/**
	 * Config Items
	 */
	@ConfigItem(
			keyName = "soundEffects",
			name = "Swap Sound Effects",
			description = "Swap sound effects with custom sounds",
			position = 1,
			section = SOUND_EFFECTS_SECTION
	)
	default boolean soundEffects() { return false; }

	@ConfigItem(
			keyName = "customSounds",
			name = "Custom Sounds",
			description = "Area Sounds to replace with your own custom .wav files. Separate with comma. Sound List: https://oldschool.runescape.wiki/w/List_of_in-game_sound_IDs",
			position = 2,
			section = SOUND_EFFECTS_SECTION
	)
	default String customSounds()
	{
		return "";
	}

	@ConfigItem(
			keyName = "consumeSoundEffects",
			name = "Consume Sounds Effects",
			description = "Consume any game sound effect that is not custom",
			position = 3,
			section = SOUND_EFFECTS_SECTION
	)
	default boolean consumeSoundEffects() { return false; }

	@ConfigItem(
			keyName = "whitelistSounds",
			name = "Whitelist Sounds",
			description = "Sound ids allowed to bypass the 'Consume Sound Effects' config option\n" +
					"Format: 123,456,789",
			position = 4,
			section = SOUND_EFFECTS_SECTION
	)
	default String whitelistSounds()
	{
		return "";
	}

	@ConfigItem(
			keyName = "blacklistedSounds",
			name = "Blacklist Sounds",
			description = "Sound ids consumed regardless of the 'Consume Sound Effects' config option being enabled\n" +
					"Format: 123,456,789",
			position = 5,
			section = SOUND_EFFECTS_SECTION
	)
	default String blacklistedSounds()
	{
		return "";
	}

	@ConfigItem(
			keyName = "areaSoundEffects",
			name = "Swap Area Sounds",
			description = "Swap area sound effects with custom sounds",
			position = 1,
			section = AREA_SOUND_EFFECTS_SECTION
	)
	default boolean areaSoundEffects() { return false; }

	@ConfigItem(
			keyName = "customAreaSounds",
			name = "Custom Area Sounds",
			description = "Area Sounds to replace with your own custom .wav files. Separate with comma. Sound List: https://oldschool.runescape.wiki/w/List_of_in-game_sound_IDs",
			position = 2,
			section = AREA_SOUND_EFFECTS_SECTION
	)
	default String customAreaSounds()
	{
		return "";
	}

	@ConfigItem(
			keyName = "consumeAreaSounds",
			name = "Consume Area Sounds",
			description = "Consume any area sound effect that is not custom\n" +
					"(Must have a custom sound file in the folder to allow this to work)",
			position = 3,
			section = AREA_SOUND_EFFECTS_SECTION
	)
	default boolean consumeAreaSounds() { return false; }

	@ConfigItem(
			keyName = "whitelistAreaSounds",
			name = "Whitelist Area Sounds",
			description = "Sound ids allowed to bypass the 'Consume Area Sound Effects' config option\n" +
					"Format: 123,456,789",
			position = 4,
			section = AREA_SOUND_EFFECTS_SECTION
	)
	default String whitelistAreaSounds()
	{
		return "";
	}

	@ConfigItem(
			keyName = "blacklistedAreaSounds",
			name = "Blacklist Area Sounds",
			description = "Sound ids consumed regardless of the 'Consume Area Sound Effects' config option being enabled\n" +
					"Format: 123,456,789",
			position = 5,
			section = AREA_SOUND_EFFECTS_SECTION
	)
	default String blacklistedAreaSounds()
	{
		return "";
	}

	@ConfigItem(
			keyName = "debugSoundEffects",
			name = "Debug Sounds Effects",
			description = "Display the sound effects that play (max 10 lines displayed)\n" +
					"White: Sound Effect (G)\n" +
					"Yellow: Area Sound Effect (A)\n" +
					"Gray: Silent Area Sound Effect (SA)",
			position = 99
	)
	default boolean debugSoundEffects() { return false; }
}