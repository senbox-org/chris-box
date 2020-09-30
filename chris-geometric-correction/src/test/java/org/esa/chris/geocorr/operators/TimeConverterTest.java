/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.chris.geocorr.operators;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * Tests for class {@link TimeConverter}.
 *
 * @author Ralf Quast
 * @since CHRIS-Box 1.5
 */
public class TimeConverterTest {

    private static TimeConverter timeConverter;

    @Test
    public void getInstance() throws IOException {
        assertNotNull(timeConverter);
        assertSame(timeConverter, TimeConverter.getInstance());
    }

    @Test
    public void deltaGPS() {
        // 1999-JAN-01
        assertEquals(13.0, timeConverter.deltaGPS(51179.0), 0.0);
        // 2006-JAN-01
        assertEquals(14.0, timeConverter.deltaGPS(53736.0), 0.0);
        // 2009-JAN-01
        assertEquals(15.0, timeConverter.deltaGPS(54832.0), 0.0);
    }

    @Test
    public void deltaTAI() {
        try {
            timeConverter.deltaTAI(41316.0);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        // 1972-JAN-01
        assertEquals(10.0, timeConverter.deltaTAI(41317.0), 0.0);

        // 1999-JAN-01
        assertEquals(31.0, timeConverter.deltaTAI(51178.0), 0.0);
        assertEquals(32.0, timeConverter.deltaTAI(51179.0), 0.0);

        // 2006-JAN-01
        assertEquals(32.0, timeConverter.deltaTAI(53735.0), 0.0);
        assertEquals(33.0, timeConverter.deltaTAI(53736.0), 0.0);
    }

    @Test
    public void deltaUT1() {
        try {
            timeConverter.deltaUT1(48621.0);
            fail();
        } catch (IllegalArgumentException ignore) {
        }

        // 1992-JAN-01
        assertEquals(-0.1251669, timeConverter.deltaUT1(48622.0), 0.0);

        // 2008-NOV-13
        // NOTE: Failure of this tests usually occurs because newer time data are used for calculation
        //assertEquals(-0.5391981, timeConverter.deltaUT1(54783.0), 0.0);

        // 2008-NOV-13
        // NOTE: Failure of this tests usually occurs because newer time data are used for calculation
        //assertEquals(-0.5403142, timeConverter.deltaUT1(54784.0), 0.0);

        // 2009-NOV-21
        // NOTE: Failure of this tests usually occurs because newer time data are used for calculation
        //assertEquals(0.1457441, timeConverter.deltaUT1(55156.0), 0.0);
    }

    @Test
    public void dateToJD() {
        final Date date = TimeConverter.mjdToDate(41317.0);
        assertEquals(41317.0, TimeConverter.dateToMJD(date), 0.0);

        final double jd = TimeConverter.dateToJD(date);
        assertEquals(41317.0, TimeConverter.jdToMJD(jd), 0.0);
    }

    @Test
    public void dateToMJD() {
        final GregorianCalendar epoch1858 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        epoch1858.clear();
        epoch1858.set(1858, Calendar.NOVEMBER, 17, 0, 0, 0);

        assertEquals(0.0, TimeConverter.dateToMJD(epoch1858.getTime()), 0.0);

        final GregorianCalendar epoch2000 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        epoch2000.clear();
        epoch2000.set(2000, Calendar.JANUARY, 1, 0, 0, 0);

        assertEquals(51544.0, TimeConverter.dateToMJD(epoch2000.getTime()), 0.0);
    }

    @Test
    public void mjdToDate() {
        final Date date = TimeConverter.mjdToDate(41317.0);
        assertEquals(41317.0, TimeConverter.dateToMJD(date), 0.0);
    }

    @Test
    public void mjd2000ToDate() {
        final GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(2000, Calendar.JANUARY, 2, 0, 0, 0);

        assertEquals(calendar.getTimeInMillis(), TimeConverter.mjd2000ToDate(1.0).getTime());
    }

    @Test
    public void mjdToGST() {
        final GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(2008, Calendar.NOVEMBER, 19, 15, 0, 0);

        final double mjd = TimeConverter.dateToMJD(calendar.getTime());
        final double gst = TimeConverter.mjdToGST(mjd);

        // expected result taken from Luis Alonso
        assertEquals(4.9569015, gst, 1.0E-7);
    }

    @Test
    public void julianDate() {
        double jd = TimeConverter.julianDate(1999, 11, 26);
        assertEquals(2451538.5, jd, 0.0);
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        timeConverter = TimeConverter.getInstance();
    }
}
