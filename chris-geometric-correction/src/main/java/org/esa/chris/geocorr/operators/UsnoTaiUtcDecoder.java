package org.esa.chris.geocorr.operators;

import com.bc.ceres.core.ProgressMonitor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * @author Marco Peters
 * @since CHRIS-BOX 3.0
 */
class UsnoTaiUtcDecoder implements TimeTableDecoder {
    public static final String REMOTE_TAI_URL = "ftp://maia.usno.navy.mil/ser7/leapsec.dat";

    public ConcurrentNavigableMap<Double, Double> decode(InputStream streamToDecode, ProgressMonitor pm) throws IOException {
        pm.beginTask("Decoding leap second TAI-UTC information", ProgressMonitor.UNKNOWN);
        ConcurrentNavigableMap<Double, Double> leapSecondMap = new ConcurrentSkipListMap<>();
        try {
            final Scanner scanner = new Scanner(streamToDecode, StandardCharsets.US_ASCII.name());
            scanner.useLocale(Locale.US);
            while (scanner.hasNextLine()) {
                if (pm.isCanceled()) {
                    throw new IOException("Cancelled by user request.");
                }
                final String line = scanner.nextLine();

                final int datePos = line.indexOf("=JD");
                final int timePos = line.indexOf("TAI-UTC=");
                final int stopPos = line.indexOf(" S ");

                if (datePos == -1 || timePos == -1 || stopPos == -1) {
                    return null;
                }

                try {
                    final double jd = Double.parseDouble(line.substring(datePos + 3, timePos));
                    final double ls = Double.parseDouble(line.substring(timePos + 8, stopPos));
                    leapSecondMap.put(jd - TimeConverter.MJD_TO_JD_OFFSET, ls);
                } catch (NumberFormatException e) {
                    throw new IOException("An error occurred while parsing the TAI-UTC data.", e);
                }
            }
            return leapSecondMap;
        } finally {
            pm.done();
        }
    }

}
