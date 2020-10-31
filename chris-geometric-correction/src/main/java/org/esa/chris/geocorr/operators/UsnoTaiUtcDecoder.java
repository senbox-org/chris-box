package org.esa.chris.geocorr.operators;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.snap.core.util.SystemUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * @author Marco Peters
 * @since CHRIS-BOX 3.0
 */
public class UsnoTaiUtcDecoder implements TimeTableHandler {


    private final String remoteUrl;
    private final String localFileName;

    public UsnoTaiUtcDecoder(String remoteUrl, String localFileName) {
        this.remoteUrl = remoteUrl;
        this.localFileName = localFileName;
    }

    public ConcurrentNavigableMap<Double, Double> initFromAuxdata(ProgressMonitor pm) throws IOException {
        try (InputStream inputStream = getAuxdataInputStream(localFileName)) {
            return decode(inputStream, pm);
        }
    }

    public ConcurrentNavigableMap<Double, Double> updateFromRemote(ProgressMonitor pm) throws IOException {
        pm.beginTask("Updating leap second information", 10);
        try {
            storeAsAuxdata(SubProgressMonitor.create(pm, 6));
            return initFromAuxdata(SubProgressMonitor.create(pm, 4));
        } finally {
            pm.done();
        }
    }

    @Override
    public ConcurrentNavigableMap<Double, Double> decode(InputStream streamToDecode, ProgressMonitor pm) throws IOException {
        final Scanner scanner = new Scanner(streamToDecode, StandardCharsets.US_ASCII.name());
        scanner.useLocale(Locale.US);

        ConcurrentNavigableMap<Double, Double> leapSecondMap = new ConcurrentSkipListMap<>();
        pm.beginTask("Decoding leap second information", ProgressMonitor.UNKNOWN);
        try {
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

                final double jd;
                final double ls;

                try {
                    jd = Double.parseDouble(line.substring(datePos + 3, timePos));
                    ls = Double.parseDouble(line.substring(timePos + 8, stopPos));
                } catch (NumberFormatException e) {
                    throw new IOException("An error occurred while parsing the TAI-UTC data.", e);
                }
                leapSecondMap.put(jd - TimeConverter.MJD_TO_JD_OFFSET, ls);
            }
        } finally {
            pm.done();
            scanner.close();
        }

        return leapSecondMap;
    }

    private void storeAsAuxdata(ProgressMonitor pm) throws IOException {
        try (InputStream stream = getRemoteInputStream(remoteUrl)) {
            ProgressMonitor pm1 = SubProgressMonitor.create(pm, 10);
            pm1.beginTask("Saving leap second time tables", 10);
            try {
                saveAsAuxdataFile(stream, localFileName, SubProgressMonitor.create(pm1, 7));
            } finally {
                pm1.done();
            }
        }
    }

    private InputStream getRemoteInputStream(String urlString) throws IOException {
        final URL url = new URL(urlString);
        final URLConnection connection = url.openConnection();
        return connection.getInputStream();
    }


    private static void saveAsAuxdataFile(InputStream stream, String fileName, ProgressMonitor pm) throws IOException {
        pm.beginTask("Saving auxdata data", ProgressMonitor.UNKNOWN);
        try (FileOutputStream fileOutputStream = new FileOutputStream(getAuxdataFile(fileName))) {
            int length;
            byte[] bytes = new byte[1024];

            // copy data from input stream to output stream
            while ((length = stream.read(bytes)) != -1) {
                fileOutputStream.write(bytes, 0, length);
            }
        } finally {
            pm.done();
        }
    }

    private static InputStream getAuxdataInputStream(String name) throws FileNotFoundException {
        final File finalsFile = getAuxdataFile(name);
        if (finalsFile.exists()) {
            return new BufferedInputStream(new FileInputStream(finalsFile));
        } else {
            return UsnoTaiUtcDecoder.class.getResourceAsStream(name);
        }
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


}
