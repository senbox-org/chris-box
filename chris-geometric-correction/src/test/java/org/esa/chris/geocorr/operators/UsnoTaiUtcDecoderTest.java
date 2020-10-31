package org.esa.chris.geocorr.operators;

import com.bc.ceres.core.ProgressMonitor;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentNavigableMap;

import static org.junit.Assert.assertEquals;

/**
 * @author Marco Peters
 */
public class UsnoTaiUtcDecoderTest {

    @Test
    public void testDecodedData() throws IOException {
        final UsnoTaiUtcDecoder decoder = new UsnoTaiUtcDecoder();
        final InputStream inputStream = UsnoTaiUtcDecoderTest.class.getResourceAsStream("test_leapsec.dat");
        final ConcurrentNavigableMap<Double, Double> timeDataTable = decoder.decode(inputStream, ProgressMonitor.NULL);

        assertEquals(41317.0, timeDataTable.firstKey(), 1.0E-6);
        assertEquals(54832.0, timeDataTable.lastKey(), 1.0E-6);

        // 1972-JAN-01
        assertEquals(10.0, timeDataTable.floorEntry(41317.0).getValue(), 1.0E-6);
        assertEquals(10.0, timeDataTable.floorEntry(41400.0).getValue(), 1.0E-6);

        // 1999-JAN-01
        assertEquals(31.0, timeDataTable.floorEntry(51178.0).getValue(), 1.0E-6);
        assertEquals(32.0, timeDataTable.floorEntry(51179.0).getValue(), 1.0E-6);

        // 2006-JAN-01
        assertEquals(32.0, timeDataTable.floorEntry(53735.0).getValue(), 1.0E-6);
        assertEquals(33.0, timeDataTable.floorEntry(53736.0).getValue(), 1.0E-6);

    }
}