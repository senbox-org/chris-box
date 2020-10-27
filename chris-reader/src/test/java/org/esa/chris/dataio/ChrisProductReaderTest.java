package org.esa.chris.dataio;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 * @since
 */
public class ChrisProductReaderTest {

    @Test
    public void testFormatTimeString() {

        // already expected format
        String correctTimeStr = "08:45:23";
        assertSame(correctTimeStr, ChrisProductReader.formatTimeString(correctTimeStr));

        assertEquals("12:12:49", ChrisProductReader.formatTimeString("0.50890046"));


        assertEquals("03:05:19", ChrisProductReader.formatTimeString("0.12869634"));

    }
}