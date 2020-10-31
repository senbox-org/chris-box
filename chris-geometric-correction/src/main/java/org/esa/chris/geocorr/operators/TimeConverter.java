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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Utility class for converting between several time systems.
 *
 * @author Ralf Quast
 * @since CHRIS-Box 1.5
 */
public class TimeConverter {

    private static final double SEVEN_DAYS_IN_MILLIS = 6.048E8;
    private static final String REMOTE_UT1_URL = "https://datacenter.iers.org/data/latestVersion/10_FINALS.DATA_IAU2000_V2013_0110.txt";
    //private static final String REMOTE_UT1_URL = "ftp://maia.usno.navy.mil/ser7/finals.data";
    private static final String REMOTE_TAI_URL = "ftp://hpiers.obspm.fr/iers/bul/bulc/Leap_Second.dat";
    //private static final String REMOTE_TAI_URL = "ftp://maia.usno.navy.mil/ser7/leapsec.dat";
    private static final String FILE_NAME_UT1 = "finals.dat";
    private static final String FILE_NAME_TAI = "leapsec.dat";
    private final TimeTableHandler taiUtcHandler;
    private final TimeTableHandler ut1UtcHandler;
    /**
     * Internal TAI-UTC table.
     * <p/>
     * Since 1972-JAN-01 the difference between TAI and UTC
     * consists of leap-seconds only.
     */
    private ConcurrentNavigableMap<Double, Double> tai;
    /**
     * Internal UT1-UTC table.
     */
    private ConcurrentNavigableMap<Double, Double> ut1;
    /**
     * The epoch (days) for the Julian Date (JD) which
     * corresponds to 4713-01-01 12:00 BC.
     */
    public static final double EPOCH_JD = -2440587.5;
    /**
     * The epoch (days) for the Modified Julian Date (MJD) which
     * corresponds to 1858-11-17 00:00.
     */
    public static final double EPOCH_MJD = -40587.0;
    /**
     * The epoch (days) for the MJD2000 which
     * corresponds to 2000-01-01 00:00.
     */
    public static final double EPOCH_MJD2000 = 10957.0;
    /**
     * The number of days between {@link #EPOCH_MJD} and {@link #EPOCH_JD}.
     */
    public static final double MJD_TO_JD_OFFSET = EPOCH_MJD - EPOCH_JD; // 2400000.5;
    /**
     * The number of milli-seconds per day.
     */
    public static final double MILLIS_PER_DAY = 86400000.0;

    /**
     * The number of seconds per day.
     */
    public static final double SECONDS_PER_DAY = 86400.0;
    private static volatile TimeConverter uniqueInstance;

    /**
     * Returns a reference to the single instance of this class.
     * <p/>
     * When this method is called for the first time, a new instance
     * of this class is created.
     *
     * @return a reference to the single instance of this class.
     * @throws IOException if an error occurred.
     */
    public static TimeConverter getInstance() throws IOException {
        if (uniqueInstance == null) {
            synchronized (TimeConverter.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = createInstance();
                }
            }
        }
        return uniqueInstance;
    }

    /**
     * Returns the number of seconds GPS time runs ahead of UTC
     * for a Modified Julian Date (MJD) of interest.
     *
     * @param mjd the MJD.
     * @return the number of seconds GPS time runs ahead of UTC.
     */
    public final double deltaGPS(double mjd) {
        return deltaTAI(mjd) - 19.0;
    }

    /**
     * Returns the number of seconds International Atomic Time (TAI) runs ahead of UTC
     * for a Modified Julian Date (MJD) of interest.
     *
     * @param mjd the MJD.
     * @return the number of seconds TAI runs ahead of UTC.
     */
    public final double deltaTAI(double mjd) {
        if (mjd < tai.firstKey()) {
            throw new IllegalArgumentException(
                    MessageFormat.format("No TAI-UTC data available before {0}.", mjdToDate(tai.firstKey())));
        }
        return tai.floorEntry(mjd).getValue();
    }

    /**
     * Returns the number of seconds UT1 runs ahead of UTC
     * for a Modified Julian Date (MJD) of interest.
     *
     * @param mjd the MJD.
     * @return the number of seconds UT1 runs ahead of UTC. When UT1 lags
     * behind UTC the sign of the number returned is negative.
     */
    public final double deltaUT1(double mjd) {
        if (mjd < ut1.firstKey()) {
            throw new IllegalArgumentException(
                    MessageFormat.format("No UT1-UTC data available before {0}.", mjdToDate(ut1.firstKey())));
        }
        if (mjd > ut1.lastKey()) {
            throw new IllegalArgumentException(
                    MessageFormat.format("No UT1-UTC data available after {0}.", mjdToDate(ut1.lastKey())));
        }

        return interpolate(mjd, ut1.floorEntry(mjd), ut1.ceilingEntry(mjd));
    }

    /**
     * Returns {@code true}, when the time tables used by an
     * instance of this class are older than seven days.
     *
     * @return {@code true}, when the time tables used by an
     * instance of this class are older than seven days,
     * {@code false} otherwise.
     */
    public boolean isOutdated() {
        final long now = new Date().getTime();
        return now - Math.min(taiUtcHandler.lastUpdated(), ut1UtcHandler.lastUpdated()) > SEVEN_DAYS_IN_MILLIS;
    }

    /**
     * Updates the time tables used by an instance of this class by
     * fetching the latest versions of  the files 'leapsec.dat' and
     * 'finals.dat' from ftp://maia.usno.navy.mil/ser7/
     *
     * @param pm the {@link ProgressMonitor}.
     * @throws IOException when an IO error occurred.
     */
    public void updateTimeTables(ProgressMonitor pm) throws IOException {
        pm.beginTask("Updating UT1 and leap second time tables", 100);
        try {
            synchronized (this) {
                tai = taiUtcHandler.updateFromRemote(SubProgressMonitor.create(pm, 20));
                ut1 = ut1UtcHandler.updateFromRemote(SubProgressMonitor.create(pm, 80));
            }
        } finally {
            pm.done();
        }
    }

    /**
     * Returns the Modified Julian Date (MJD) corresponding to a date.
     *
     * @param date the date.
     * @return the MJD corresponding to the date.
     */
    public static double dateToMJD(Date date) {
        return date.getTime() / MILLIS_PER_DAY - EPOCH_MJD;
    }

    /**
     * Converts UT1 into Greenwich Mean Sidereal Time (GST, IAU 1982 model).
     * <p/>
     * Note that the unit of GST is radian (rad).
     *
     * @param jd the UT1 expressed as Julian Date (JD).
     * @return the GST corresponding to the JD given.
     */
    public static double jdToGST(double jd) {
        return mjdToGST(jdToMJD(jd));
    }

    /**
     * Returns the Modified Julian Date (MJD) corresponding to a Julian Date (JD).
     *
     * @param jd the JD.
     * @return the MJD corresponding to the JD.
     */
    public static double jdToMJD(double jd) {
        return jd - MJD_TO_JD_OFFSET;
    }

    /**
     * Returns the Julian Date (JD) for the given parameters.
     *
     * @param year       the year.
     * @param month      the month (zero-based, e.g. use 0 for January and 11 for December).
     * @param dayOfMonth the day-of-month.
     * @return the Julian Date.
     */
    public static double julianDate(int year, int month, int dayOfMonth) {
        return julianDate(year, month, dayOfMonth, 0, 0, 0);
    }


    /**
     * Returns the Julian Date (JD) corresponding to a date.
     *
     * @param date the date.
     * @return the JD corresponding to the date.
     */
    static double dateToJD(Date date) {
        return date.getTime() / MILLIS_PER_DAY - EPOCH_JD;
    }

    /**
     * Returns the date corresponding to a Modified Julian Date (MJD).
     *
     * @param mjd the MJD.
     * @return the date corresponding to the MJD.
     */
    static Date mjdToDate(double mjd) {
        return new Date(Math.round((EPOCH_MJD + mjd) * MILLIS_PER_DAY));
    }

    /**
     * Returns the date corresponding to an MJD2000.
     *
     * @param mjd2000 the MJD2000.
     * @return the date corresponding to the MJD2000.
     */
    static Date mjd2000ToDate(double mjd2000) {
        return new Date(Math.round((EPOCH_MJD2000 + mjd2000) * MILLIS_PER_DAY));
    }

    /**
     * Converts UT1 into Greenwich Mean Sidereal Time (GST, IAU 1982 model).
     * <p/>
     * Note that the unit of GST is radian (rad).
     *
     * @param mjd the UT1 expressed as Modified Julian Date (MJD).
     * @return the GST corresponding to the MJD given.
     */
    static double mjdToGST(double mjd) {
        // radians per sidereal second
        final double secRad = 7.272205216643039903848712E-5;

        // seconds per day, days per Julian century
        final double daySec = 86400.0;
        final double cenDay = 36525.0;

        // reference epoch (J2000)
        final double mjd0 = 51544.5;

        // coefficients of IAU 1982 GMST-UT1 model
        final double a = 24110.54841;
        final double b = 8640184.812866;
        final double c = 0.093104;
        final double d = 6.2E-6;

        final double mjd1 = Math.floor(mjd);
        final double mjd2 = mjd - mjd1;

        // Julian centuries since epoch
        final double t = (mjd2 + (mjd1 - mjd0)) / cenDay;
        // fractional part of MJD(UT1) in seconds
        final double f = daySec * mjd2;

        final double twoPi = 2.0 * Math.PI;
        final double gst = (secRad * ((a + (b + (c - d * t) * t) * t) + f)) % twoPi;

        if (gst < 0.0) {
            return gst + twoPi;
        }

        return gst;
    }

    /**
     * Calculates the Julian Date (JD) from the given parameter.
     *
     * @param year       the year.
     * @param month      the month (zero-based, e.g. use 0 for January and 11 for December).
     * @param dayOfMonth the day-of-month.
     * @param hourOfDay  the hour-of-day.
     * @param minute     the minute.
     * @param second     the second.
     * @return the Julian Date.
     */
    private static double julianDate(int year, int month, int dayOfMonth, int hourOfDay, int minute, int second) {
        final GregorianCalendar utc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(year, month, dayOfMonth, hourOfDay, minute, second);
        utc.set(Calendar.MILLISECOND, 0);

        return utc.getTimeInMillis() / MILLIS_PER_DAY - EPOCH_JD;
    }

    private static double interpolate(double mjd, Map.Entry<Double, Double> floor, Map.Entry<Double, Double> ceiling) {
        final double floorKey = floor.getKey();
        final double floorValue = floor.getValue();
        final double ceilingKey = ceiling.getKey();

        if (floorKey == ceilingKey) {
            return floorValue;
        }

        return floorValue + (ceiling.getValue() - floorValue) * ((mjd - floorKey) / (ceilingKey - floorKey));
    }

    private static TimeConverter createInstance() throws IOException {
        final TimeTableHandler taiUtcHandler = new TimeTableHandler(new UsnoTaiUtcDecoder(), TimeConverter.REMOTE_TAI_URL, TimeConverter.FILE_NAME_TAI);
        final TimeTableHandler ut1UtcHandler = new TimeTableHandler(new UsnoUt1UtcDecoder(), TimeConverter.REMOTE_UT1_URL, TimeConverter.FILE_NAME_UT1);
        return getTimeConverter(taiUtcHandler, ut1UtcHandler);
    }

    @NotNull
    private static TimeConverter getTimeConverter(TimeTableHandler taiUtcHandler, TimeTableHandler ut1UtcHandler) throws IOException {
        final TimeConverter timeConverter = new TimeConverter(taiUtcHandler, ut1UtcHandler);

        timeConverter.tai = timeConverter.taiUtcHandler.initFromAuxdata(ProgressMonitor.NULL);
        timeConverter.ut1 = timeConverter.ut1UtcHandler.initFromAuxdata(ProgressMonitor.NULL);

        return timeConverter;
    }

    private TimeConverter(TimeTableHandler taiUtcHandler, TimeTableHandler ut1UtcHandler) {
        this.taiUtcHandler = taiUtcHandler;
        this.ut1UtcHandler = ut1UtcHandler;
        tai = new ConcurrentSkipListMap<>();
        ut1 = new ConcurrentSkipListMap<>();
    }

}
