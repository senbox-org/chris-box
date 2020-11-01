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
public class FinalsUt1UtcDecoderTest {

    @Test
    public void testDecodedData_usno() throws IOException {
        final FinalsUt1UtcDecoder decoder = new FinalsUt1UtcDecoder();
        final InputStream inputStream = FinalsUt1UtcDecoderTest.class.getResourceAsStream("test_usno_finals.dat");
        final ConcurrentNavigableMap<Double, Double> timeDataTable = decoder.decode(inputStream, ProgressMonitor.NULL);

        assertEquals(48622.0, timeDataTable.firstKey(), 1.0E-6);
        assertEquals(55394.0, timeDataTable.lastKey(), 1.0E-6);

        // 1992-JAN-01
        assertEquals(-0.1251669, timeDataTable.floorEntry(48622.0).getValue(), 0.0);
        assertEquals(-0.1251669, timeDataTable.ceilingEntry(48622.0).getValue(), 0.0);

        // 2008-NOV-13
        assertEquals(-0.5391981, timeDataTable.floorEntry(54783.0).getValue(), 0.0);
        assertEquals(-0.5391981, timeDataTable.floorEntry(54783.1).getValue(), 0.0);
        assertEquals(-0.5403142, timeDataTable.ceilingEntry(54783.1).getValue(), 0.0);

        // 2009-NOV-21
        assertEquals(0.1457441, timeDataTable.floorEntry(55156.0).getValue(), 0.0);
        assertEquals(0.1457441, timeDataTable.floorEntry(55156.1).getValue(), 0.0);
        assertEquals(0.1450065, timeDataTable.ceilingEntry(55156.1).getValue(), 0.0);
    }

    @Test
    public void testDecodedData_iers() throws IOException {
        final FinalsUt1UtcDecoder decoder = new FinalsUt1UtcDecoder();
        final InputStream inputStream = FinalsUt1UtcDecoderTest.class.getResourceAsStream("test_iers_finals.dat");
        final ConcurrentNavigableMap<Double, Double> timeDataTable = decoder.decode(inputStream, ProgressMonitor.NULL);

        assertEquals(48622.0, timeDataTable.firstKey(), 1.0E-6);
        assertEquals(59524.0, timeDataTable.lastKey(), 1.0E-6);

        // 1992-JAN-01
        assertEquals(-0.1251659, timeDataTable.floorEntry(48622.0).getValue(), 0.0);
        assertEquals(-0.1251659, timeDataTable.ceilingEntry(48622.0).getValue(), 0.0);

        // 2008-NOV-13
        assertEquals(-0.5391942, timeDataTable.floorEntry(54783.0).getValue(), 0.0);
        assertEquals(-0.5391942, timeDataTable.floorEntry(54783.1).getValue(), 0.0);
        assertEquals(-0.5403097, timeDataTable.ceilingEntry(54783.1).getValue(), 0.0);

        // 2009-NOV-21
        assertEquals(0.1527877, timeDataTable.floorEntry(55156.0).getValue(), 0.0);
        assertEquals(0.1527877, timeDataTable.floorEntry(55156.1).getValue(), 0.0);
        assertEquals(0.1521636, timeDataTable.ceilingEntry(55156.1).getValue(), 0.0);

        // 2016-JUN-07
        assertEquals(-0.1951500, timeDataTable.floorEntry(57546.0).getValue(), 0.0);
        assertEquals(-0.1951500, timeDataTable.floorEntry(57546.1).getValue(), 0.0);
        assertEquals(-0.1961851, timeDataTable.ceilingEntry(57546.1).getValue(), 0.0);
    }
}