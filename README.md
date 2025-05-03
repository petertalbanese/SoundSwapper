# Sound Swapper
Allows the user to replace any sound effect.

To replace a sound, add its ID to the list in the plugin menu, then place a .wav file with the same name in the
SoundSwapper folder in your root RuneLite folder. The plugin will grab the sound and use it instead!

E.g. *157,205,550* -> place *157.wav*, *205.wav*, and *550.wav* in the SoundSwapper folder in your RuneLite folder.

To find Sound IDs, you can check https://oldschool.runescape.wiki/w/List_of_in-game_sound_IDs or use the Visual Sounds
Plugin on the hub.

If you experience any issues or have any concerns, please reach out to Pete OR Damen via a GitHub Issue or by Discord Messaging directly ("Cyber#4309" OR "damen") OR via the Runelite Discord by mentioning @Paperman AND/OR @Damen.

You can also receive assistance for any plugin that Damen manages on the plugin hub in the following **Discord**: https://discord.gg/F2mfSvcnaj - This Discord will cover FAQ, Known Issues, and Feature Requests.

# Native Sound ID Swaps
In order to replace one native sound ID with another native sound ID, the Native Sound ID section can be enabled and populated. 

An example configuration to make sound ID 2508 play the sound corresponding to sound ID 3512 is:
IDs to Replace: 2508
Replacement IDs: 3892

In order to add more entries, create a comma separated list of values in the 'IDs to Replace' field, and give them a corresponding replacement id in the 'Replacement IDs' field. 

An example configuration of multiple native sound ID swaps:
IDs to Replace: 2508,1234,1111
Replacement IDs: 3892,1051,3892