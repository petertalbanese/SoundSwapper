package com.soundswapper;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class SoundListParserTest
{
    @Test
    public void parsesCommaSeparatedList()
    {
        assertEquals(Arrays.asList("123", "456", "789"), SoundListParser.parse("123, 456 ,789"));
    }

    @Test
    public void handlesEmptyInput()
    {
        assertEquals(Collections.emptyList(), SoundListParser.parse(null));
        assertEquals(Collections.emptyList(), SoundListParser.parse(""));
        assertEquals(Collections.emptyList(), SoundListParser.parse(" , ,"));
    }

    @Test
    public void parsesMultipleLines()
    {
        assertEquals(Arrays.asList("1", "2", "3", "4", "5"), SoundListParser.parse("1,2\n3\r\n4,\r5"));
    }

    @Test
    public void ignoresBlankLines()
    {
        assertEquals(Arrays.asList("1", "2"), SoundListParser.parse("\n1\n\n   \n2\n"));
    }

    @Test
    public void ignoresLineComments()
    {
        assertEquals(Arrays.asList("1", "2", "3"), SoundListParser.parse(
                "// whole line comment\n" +
                "# another whole line comment\n" +
                "1, 2 // trailing comment, 99\n" +
                "3 # trailing comment, 98"));
    }

    @Test
    public void ignoresBlockComments()
    {
        assertEquals(Arrays.asList("1", "2", "3", "4"), SoundListParser.parse(
                "/* whole line */\n" +
                "1, /* inline, 99 */ 2\n" +
                "/* leading */ 3 /* trailing */\n" +
                "4 /* unterminated, 98"));
    }

    @Test
    public void blockCommentSeparatesSurroundingText()
    {
        assertEquals(Collections.singletonList("1 2"), SoundListParser.parse("1/* x */2"));
    }

    @Test
    public void commentMarkersInsideBlockCommentAreIgnored()
    {
        assertEquals(Arrays.asList("1", "2"), SoundListParser.parse("1, /* // # */ 2"));
    }

    @Test
    public void blockCommentDoesNotSpanLines()
    {
        assertEquals(Arrays.asList("1", "2"), SoundListParser.parse("1 /* start\n2"));
    }
}
