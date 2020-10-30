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
import org.esa.snap.core.util.SystemUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
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
    private static final String REMOTE_UT1_URL = "ftp://maia.usno.navy.mil/ser7/finals.data";
    private static final String REMOTE_TAI_URL = "ftp://maia.usno.navy.mil/ser7/leapsec.dat";
    private static final String FILE_NAME_UT1 = "finals.data";
    private static final String FILE_NAME_TAI = "leapsec.dat";
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
        return new Date().getTime() - lastModified() > SEVEN_DAYS_IN_MILLIS;
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
        try {
            synchronized (this) {
                pm.beginTask("Updating UT1 and leap second time tables", 100);
                try (InputStream stream = getRemoteInputStream(REMOTE_TAI_URL)) {
                    updateTAI(stream, SubProgressMonitor.create(pm, 10));
                }
                try (InputStream stream = getRemoteInputStream(REMOTE_UT1_URL)) {
                    updateUT1(stream, SubProgressMonitor.create(pm, 90));
                }
            }
        } finally {
            pm.done();
        }
    }

    private static TimeConverter createInstance() throws IOException {
        final TimeConverter timeConverter = new TimeConverter();

        timeConverter.tai = readTAI(FILE_NAME_TAI);
        readUT1(FILE_NAME_UT1, timeConverter.ut1);

        return timeConverter;
    }

    private TimeConverter() {
        tai = new ConcurrentSkipListMap<>();
        ut1 = new ConcurrentSkipListMap<>();
    }

    /**
     * Returns the date (in millis) when the time tables used by an
     * instance of this class were last modified (updated).
     *
     * @return the date (millis) of last modification.
     */
    private long lastModified() {
        synchronized (this) {
            final File file = getFile(FILE_NAME_UT1);
            if (file != null) {
                return file.lastModified();
            }
            return 0L;
        }
    }

    /**
     * Updates the internal UT1-UTC table with newer data read from a URL.
     *
     * @param inputStream the input stream to read from
     * @param pm          the {@link ProgressMonitor}.
     * @throws IOException if an error occurred.
     */
    private void updateTAI(InputStream inputStream, ProgressMonitor pm) throws IOException {
        pm.beginTask("Updating leap second time tables", 10);
        try {
            File file = saveAsAuxdataFile(inputStream, FILE_NAME_TAI, SubProgressMonitor.create(pm, 7));
            try (InputStream localInputStream = Files.newInputStream(file.toPath())) {
                tai = readTAI(localInputStream, SubProgressMonitor.create(pm, 3));
            }
        } finally {
            pm.done();
        }
    }

    /**
     * Updates the internal UT1-UTC table with newer data read from a URL.
     *
     * @param inputStream the input stream to read from
     * @param pm          the {@link ProgressMonitor}.
     * @throws IOException if an error occurred.
     */
    private void updateUT1(InputStream inputStream, ProgressMonitor pm) throws IOException {
        final String[] lines = readUT1(inputStream, ut1, pm);
        writeLines(FILE_NAME_UT1, lines);
    }

    private InputStream getRemoteInputStream(String urlString) throws IOException {
        final URL url = new URL(urlString);
        final URLConnection connection = url.openConnection();
        return connection.getInputStream();
    }

    private static ConcurrentNavigableMap<Double, Double> readTAI(String name) throws IOException {
        try (InputStream inputStream = getInputStream(name)) {
            return readTAI(inputStream, ProgressMonitor.NULL);
        }
    }

    private static ConcurrentNavigableMap<Double, Double> readTAI(InputStream is, ProgressMonitor pm) throws IOException {
        UsnoTaiUtcDecoder decoder = new UsnoTaiUtcDecoder();
        return decoder.decode(is, pm);
    }

    private static void readUT1(String name, Map<Double, Double> map) throws IOException {
        try (InputStream inputStream = getInputStream(name)) {
            readUT1(inputStream, map, ProgressMonitor.NULL);
        }
    }

    private static InputStream getInputStream(String name) throws FileNotFoundException {
        final File finalsFile = getFile(name);
        if (finalsFile == null) {
            return TimeConverter.class.getResourceAsStream(name);
        } else {
            return new BufferedInputStream(new FileInputStream(finalsFile));
        }
    }

    private static String[] readUT1(InputStream is, Map<Double, Double> map, ProgressMonitor pm) throws IOException {
        final String[] lines = readLines(is, "Reading UT1-UTC data", pm);

        for (final String line : lines) {
            final String mjdString = line.substring(7, 15);
            final String utdString = line.substring(58, 68);
            if (mjdString.trim().isEmpty() || utdString.trim().isEmpty()) {
                continue; // try next line
            }

            final double mjd;
            final double utd;

            try {
                mjd = Double.parseDouble(mjdString);
                utd = Double.parseDouble(utdString);
            } catch (NumberFormatException e) {
                throw new IOException("An error occurred while parsing the UT1-UTC data.", e);
            }
            map.put(mjd, utd);
        }

        return lines;
    }

    private static Writer getWriter(String fileName) {
        final File file = getAuxdataFile(fileName);
        try {
            final OutputStream os = new FileOutputStream(file);
            return new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.US_ASCII));
        } catch (FileNotFoundException e) {
            // ignore
        }

        return null;
    }

    @NotNull
    private static File getAuxdataFile(String fileName) {
        final File auxDataDir = getAuxdataDir();
        auxDataDir.mkdirs();
        return new File(auxDataDir, fileName);
    }

    @NotNull
    private static File getAuxdataDir() {
        return SystemUtils.getAuxDataPath().resolve("chris").resolve("geometric-correction").toFile();
    }

    private static String[] readLines(InputStream is, String taskName, ProgressMonitor pm) throws IOException {
        final Scanner scanner = new Scanner(is, StandardCharsets.US_ASCII.name());
        scanner.useLocale(Locale.US);

        final ArrayList<String> lineList = new ArrayList<>();
        try {
            pm.beginTask(taskName, ProgressMonitor.UNKNOWN);
            while (scanner.hasNextLine()) {
                if (pm.isCanceled()) {
                    throw new IOException("Cancelled by user request.");
                }
                final String line = scanner.nextLine();
                lineList.add(line);
                pm.worked(1);
            }
        } finally {
            pm.done();
            scanner.close();
        }

        return lineList.toArray(new String[0]);
    }

    private static File saveAsAuxdataFile(InputStream stream, String fileName, ProgressMonitor pm) throws IOException {
        final File file = getAuxdataFile(fileName);
        pm.beginTask("Saving auxdata data", ProgressMonitor.UNKNOWN);
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            int length;
            byte[] bytes = new byte[1024];

            // copy data from input stream to output stream
            while ((length = stream.read(bytes)) != -1) {
                fileOutputStream.write(bytes, 0, length);
            }
        } finally {
            pm.done();
        }
        return file;
    }

    private static void writeLines(String fileName, String[] lines) throws IOException {
        final Writer writer = getWriter(fileName);

        if (writer != null) {
            try {
                for (final String line : lines) {
                    writer.write(line);
                    writer.write("\n");
                }
            } finally {
                try {
                    writer.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private static File getFile(String fileName) {
        final File fileDir = getAuxdataDir();

        final File file = new File(fileDir, fileName);
        if (file.canRead()) {
            return file;
        }

        return null;
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

    /**
     * Returns the Julian Date (JD) corresponding to a date.
     *
     * @param date the date.
     * @return the JD corresponding to the date.
     */
    public static double dateToJD(Date date) {
        return date.getTime() / MILLIS_PER_DAY - EPOCH_JD;
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
     * Returns the date corresponding to a Modified Julian Date (MJD).
     *
     * @param mjd the MJD.
     * @return the date corresponding to the MJD.
     */
    public static Date mjdToDate(double mjd) {
        return new Date(Math.round((EPOCH_MJD + mjd) * MILLIS_PER_DAY));
    }

    /**
     * Returns the date corresponding to an MJD2000.
     *
     * @param mjd2000 the MJD2000.
     * @return the date corresponding to the MJD2000.
     */
    public static Date mjd2000ToDate(double mjd2000) {
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
    public static double mjdToGST(double mjd) {
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
     * Returns the Julian Date (JD) corresponding to a Modified Julian Date (JD).
     *
     * @param mjd the MJD.
     * @return the JD corresponding to the MJD.
     */
    public static double mjdToJD(double mjd) {
        return mjd + MJD_TO_JD_OFFSET;
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
    public static double julianDate(int year, int month, int dayOfMonth, int hourOfDay, int minute, int second) {
        final GregorianCalendar utc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(year, month, dayOfMonth, hourOfDay, minute, second);
        utc.set(Calendar.MILLISECOND, 0);

        return utc.getTimeInMillis() / MILLIS_PER_DAY - EPOCH_JD;
    }
}
