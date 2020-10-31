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
public class UsnoUt1UtcDecoderTest {

    @Test
    public void testDecodedData() throws IOException {
        final UsnoUt1UtcDecoder decoder = new UsnoUt1UtcDecoder();
        final InputStream inputStream = UsnoUt1UtcDecoderTest.class.getResourceAsStream("test_finals.data");
        final ConcurrentNavigableMap<Double, Double> timeDataTable = decoder.decode(inputStream, ProgressMonitor.NULL);

        assertEquals(48622.0, timeDataTable.firstKey(), 1.0E-6);
        assertEquals(55394.0, timeDataTable.lastKey(), 1.0E-6);

        // 1992-JAN-01
        final double mjd = 48622.0;
        assertEquals(-0.1251669, timeDataTable.floorEntry(mjd).getValue(), 0.0);
        assertEquals(-0.1251669, timeDataTable.ceilingEntry(mjd).getValue(), 0.0);

        // 2008-NOV-13
        assertEquals(-0.5391981, timeDataTable.floorEntry(54783.0).getValue(), 0.0);
        assertEquals(-0.5391981, timeDataTable.floorEntry(54783.1).getValue(), 0.0);
        assertEquals(-0.5403142, timeDataTable.ceilingEntry(54783.1).getValue(), 0.0);

        // 2009-NOV-21
        assertEquals(0.1457441, timeDataTable.floorEntry(55156.0).getValue(), 0.0);
        assertEquals(0.1457441, timeDataTable.floorEntry(55156.1).getValue(), 0.0);
        assertEquals(0.1450065, timeDataTable.ceilingEntry(55156.1).getValue(), 0.0);
    }
}