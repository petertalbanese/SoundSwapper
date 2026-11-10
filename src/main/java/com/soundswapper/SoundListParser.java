package com.soundswapper;

import java.util.ArrayList;
import java.util.List;

// Parses the sound list config fields.
//
// Values may be separated by commas and/or newlines. Blank lines are ignored, as are comments:
// "//" and "#" ignore the rest of the line, and "/* ... */" ignores the enclosed text on a single line.
public final class SoundListParser
{
    private SoundListParser()
    {
    }

    public static List<String> parse(String configText)
    {
        List<String> values = new ArrayList<>();
        if (configText == null || configText.isEmpty())
        {
            return values;
        }

        for (String line : configText.split("\r?\n|\r"))
        {
            for (String value : stripComments(line).split(","))
            {
                String trimmed = value.trim();
                if (!trimmed.isEmpty())
                {
                    values.add(trimmed);
                }
            }
        }

        return values;
    }

    private static String stripComments(String line)
    {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < line.length())
        {
            if (line.startsWith("/*", i))
            {
                int end = line.indexOf("*/", i + 2);
                if (end == -1)
                {
                    // Block comments may not span lines, so an unterminated one ends at the end of the line
                    break;
                }
                // Replace the comment with a space so it still separates the text around it
                result.append(' ');
                i = end + 2;
            }
            else if (line.startsWith("//", i) || line.charAt(i) == '#')
            {
                break;
            }
            else
            {
                result.append(line.charAt(i));
                i++;
            }
        }
        return result.toString();
    }
}
