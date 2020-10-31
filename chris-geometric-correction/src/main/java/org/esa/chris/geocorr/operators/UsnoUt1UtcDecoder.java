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
 * @since CHRIS-BOX-3.0
 */
class UsnoUt1UtcDecoder implements TimeTableDecoder {
    // Can decode
    // * ftp://maia.usno.navy.mil/ser7/finals.data
    // * https://datacenter.iers.org/data/latestVersion/10_FINALS.DATA_IAU2000_V2013_0110.txt
    // * ftp://cddis.gsfc.nasa.gov/pub/products/iers/finals2000A.data

    
    @Override
    public ConcurrentNavigableMap<Double, Double> decode(InputStream streamToDecode, ProgressMonitor pm) throws IOException {
        pm.beginTask("Decoding leap second UT1-UTC information", ProgressMonitor.UNKNOWN);
        final ConcurrentSkipListMap<Double, Double> utDiffMap = new ConcurrentSkipListMap<>();
        try {
            final Scanner scanner = new Scanner(streamToDecode, StandardCharsets.US_ASCII.name());
            scanner.useLocale(Locale.US);
            while (scanner.hasNextLine()) {
                if (pm.isCanceled()) {
                    throw new IOException("Cancelled by user request.");
                }

                final String line = scanner.nextLine();
                final String mjdString = line.substring(7, 15);
                final String utdString = line.substring(58, 68);
                if (mjdString.trim().isEmpty() || utdString.trim().isEmpty()) {
                    continue; // try next line
                }

                try {
                    final double mjd = Double.parseDouble(mjdString);
                    final double utd = Double.parseDouble(utdString);
                    utDiffMap.put(mjd, utd);
                } catch (NumberFormatException e) {
                    throw new IOException("An error occurred while parsing the UT1-UTC data.", e);
                }
            }
            return utDiffMap;
        } finally {
            pm.done();
        }
    }
}
