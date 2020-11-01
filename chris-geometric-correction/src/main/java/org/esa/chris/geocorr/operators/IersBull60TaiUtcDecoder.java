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
class IersBull60TaiUtcDecoder implements TimeTableDecoder {
    public static final String REMOTE_DEFAULT_URL = "ftp://hpiers.obspm.fr/iers/bul/bulc/Leap_Second.dat";

    @Override
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
                final String line = scanner.nextLine().trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                final String[] splits = line.split("\\s+");
                try {
                    final double mjd = Double.parseDouble(splits[0]);
                    final double ls = Double.parseDouble(splits[4]);
                    leapSecondMap.put(mjd, ls);
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
