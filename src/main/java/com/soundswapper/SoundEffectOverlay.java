/*
 * Copyright (c) 2018, WooxSolo <https://github.com/WooxSolo>
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

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.AreaSoundEffectPlayed;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

import javax.inject.Inject;
import java.awt.*;

@Slf4j
class SoundEffectOverlay extends OverlayPanel
{
    private final static int MAX_LINES = 10;
    private final static Color COLOR_SOUND_EFFECT = Color.WHITE;
    private final static Color COLOR_AREA_SOUND_EFFECT = Color.YELLOW;
    private final static Color COLOR_SILENT_SOUND_EFFECT = Color.GRAY;
    private final static Color COLOR_ALLOWED = Color.GREEN;
    private final static Color COLOR_CONSUMED = Color.RED;
    private final static Color COLOR_CUSTOM = Color.PINK;
    private final static Color COLOR_BLACKLISTED = Color.ORANGE;
    private final static Color COLOR_WHITELISTED = Color.WHITE;

    public static final String ALLOWED = "Allowed";
    public static final String BLACKLISTED = "Blacklisted";
    public static final String CONSUMED = "Consumed";
    public static final String CUSTOM = "Custom";
    public static final String WHITELISTED = "Whitelisted";

    private final Client client;
    private SoundSwapperPlugin plugin;

    private SoundSwapperConfig config;

    @Inject
    SoundEffectOverlay(Client client, SoundSwapperPlugin plugin, SoundSwapperConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Sound Effects")
                .leftColor(Color.CYAN)
                .build());

        setClearChildren(false);
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.debugSoundEffects())
        {
            return null;
        }

        return super.render(graphics);
    }

    @Subscribe
    public void onSoundEffectPlayed(SoundEffectPlayed event)
    {
        if (!config.debugSoundEffects())
        {
            return;
        }

        int soundId = event.getSoundId();

        String text = "G: " + soundId;

        String action = ALLOWED;
        Color actionColor = COLOR_ALLOWED;

        if (config.consumeSoundEffects())
        {
            action = CONSUMED;
            actionColor = COLOR_CONSUMED;
        }

        if (plugin.blacklistedSounds.contains(soundId))
        {
            action = BLACKLISTED;
            actionColor = COLOR_BLACKLISTED;
        }

        if (plugin.whitelistedSounds.contains(soundId))
        {
            action = WHITELISTED;
            actionColor = COLOR_WHITELISTED;
        }

        if (plugin.customSounds.containsKey(soundId))
        {
            action = CUSTOM;
            actionColor = COLOR_CUSTOM;
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left(text)
                .leftColor(COLOR_SOUND_EFFECT)
                .right(action)
                .rightColor(actionColor)
                .build());

        checkMaxLines();
    }

    @Subscribe
    public void onAreaSoundEffectPlayed(AreaSoundEffectPlayed event)
    {
        if (!config.debugSoundEffects())
        {
            return;
        }

        Color textColor = COLOR_AREA_SOUND_EFFECT;
        Color actionColor = COLOR_ALLOWED;

        int soundId = event.getSoundId();
        String text = "A: " + soundId;
        String action = ALLOWED;

        // Check if the player is within range to hear the sound
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer != null)
        {
            LocalPoint lp = localPlayer.getLocalLocation();
            if (lp != null)
            {
                int sceneX = lp.getSceneX();
                int sceneY = lp.getSceneY();
                int distance = Math.abs(sceneX - event.getSceneX()) + Math.abs(sceneY - event.getSceneY());
                if (distance > event.getRange())
                {
                    textColor = COLOR_SILENT_SOUND_EFFECT;
                    text = "SA: " + soundId;
                }
            }
        }

        if (config.consumeAreaSounds())
        {
            action = CONSUMED;
            actionColor = COLOR_CONSUMED;
        }

        if (plugin.blacklistedAreaSounds.contains(soundId))
        {
            action = BLACKLISTED;
            actionColor = COLOR_BLACKLISTED;
        }

        if (plugin.whitelistedAreaSounds.contains(soundId))
        {
            action = WHITELISTED;
            actionColor = COLOR_WHITELISTED;
        }

        if (plugin.customAreaSounds.containsKey(soundId))
        {
            action = CUSTOM;
            actionColor = COLOR_CUSTOM;
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left(text)
                .leftColor(textColor)
                .right(action)
                .rightColor(actionColor)
                .build());

        checkMaxLines();
    }

    private void checkMaxLines()
    {
        while (panelComponent.getChildren().size() > MAX_LINES)
        {
            panelComponent.getChildren().remove(1);
        }
    }

    public void resetLines()
    {
        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Sound Effects")
                .leftColor(Color.CYAN)
                .build());
    }
}
