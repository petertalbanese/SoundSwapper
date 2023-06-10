/*
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
import net.runelite.client.RuneLite;

import javax.sound.sampled.*;
import java.io.*;

@Slf4j
public class Sound
{
    private static final File SOUND_DIR = new File(RuneLite.RUNELITE_DIR, "SoundSwapper");

    public static synchronized void play(final Integer soundId)
    {
        File sound_file = new File(SOUND_DIR, String.valueOf(soundId) + ".wav");

        new Thread(() ->
        {
            try
            {
                InputStream fileStream = new BufferedInputStream(new FileInputStream(sound_file));
                AudioInputStream soundFile = AudioSystem.getAudioInputStream(fileStream);
                Clip clip  = AudioSystem.getClip();
                clip.open(soundFile);
                clip.start();
            }
            catch (LineUnavailableException | IOException | UnsupportedAudioFileException exception)
            {
                log.info("exception: {}", exception);
            }
        }).start();
    }
}
