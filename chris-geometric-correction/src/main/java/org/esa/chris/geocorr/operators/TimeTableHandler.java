package org.esa.chris.geocorr.operators;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.chris.geocorr.GeoCorrUtils;
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
import java.util.concurrent.ConcurrentNavigableMap;

/**
 * @author Marco Peters
 * @since CHRIS-BOX 3.0
 */
public class TimeTableHandler {

    private final TimeTableDecoder decoder;
    private final String remoteUrl;
    private final String localFileName;

    public TimeTableHandler(TimeTableDecoder decoder, String remoteUrl, String localFileName) {
        this.decoder = decoder;
        this.remoteUrl = remoteUrl;
        this.localFileName = localFileName;
    }

    public ConcurrentNavigableMap<Double, Double> initFromAuxdata(ProgressMonitor pm) throws IOException {
        try (InputStream inputStream = getAuxdataInputStream(localFileName)) {
            synchronized (this) {
                return decoder.decode(inputStream, pm);
            }
        }
    }

    public ConcurrentNavigableMap<Double, Double> updateFromRemote(ProgressMonitor pm) throws IOException {
        pm.beginTask("Updating leap second information", 10);
        try {
            synchronized (this) {
                storeFromRemoteAsAuxdata();
                pm.worked(4);
                return initFromAuxdata(SubProgressMonitor.create(pm, 6));
            }
        } finally {
            pm.done();
        }
    }

    /**
     * Returns the date (in millis) when the time table data handeled by an
     * instance of this class were last updated.
     *
     * @return the date (millis) of last update.
     */
    public long lastUpdated() {
        synchronized (this) {
            final File file = getAuxdataFile(localFileName);
            if (file.exists()) {
                return file.lastModified();
            }
            return 0;
        }
    }

    private void storeFromRemoteAsAuxdata() throws IOException {
        try (InputStream stream = getRemoteInputStream(remoteUrl)) {
            saveAsAuxdataFile(stream, localFileName);
        }
    }

    private InputStream getRemoteInputStream(String urlString) throws IOException {
        final URL url = new URL(urlString);
        final URLConnection connection = url.openConnection();
        return connection.getInputStream();
    }


    private static void saveAsAuxdataFile(InputStream stream, String fileName) throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(getAuxdataFile(fileName))) {
            int length;
            byte[] bytes = new byte[1024];

            // copy data from input stream to output stream
            while ((length = stream.read(bytes)) != -1) {
                fileOutputStream.write(bytes, 0, length);
            }
        }
    }

    private  InputStream getAuxdataInputStream(String name) throws FileNotFoundException {
        final File finalsFile = getAuxdataFile(name);
        if (finalsFile.exists()) {
            return new BufferedInputStream(new FileInputStream(finalsFile));
        } else {
            return decoder.getClass().getResourceAsStream(name);
        }
    }

    @NotNull
    static File getAuxdataFile(String fileName) {
        final File auxDataDir = GeoCorrUtils.getAuxdataDir().toFile();
        auxDataDir.mkdirs();
        return new File(auxDataDir, fileName);
    }

}
