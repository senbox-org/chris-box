package org.esa.chris.geocorr;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.util.ModuleMetadata;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Activator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuxdataInstaller implements Activator {

    public static AtomicBoolean activated = new AtomicBoolean(false);

    public static void activate() {
        if (!activated.getAndSet(true)) {

            Path sourceDirPath = ResourceInstaller.findModuleCodeBasePath(AuxdataInstaller.class).resolve("auxdata");
            ModuleMetadata moduleMetadata = SystemUtils.loadModuleMetadata(AuxdataInstaller.class);

            String version = "unknown";
            if (moduleMetadata != null) {
                version = moduleMetadata.getVersion();
            }

            Path auxdataDirectory = GeoCorrUtils.getAuxdataDir().resolve(version);
            final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirectory);

            try {
                SystemUtils.LOG.fine("Installing CHRIS geometric correction auxdata from " + sourceDirPath + " into " + auxdataDirectory);
                resourceInstaller.install(".*", ProgressMonitor.NULL);
            } catch (IOException e) {
                SystemUtils.LOG.severe("CHRIS geometric correction auxdata could not be extracted to" + auxdataDirectory);
            }

        }
    }

    @Override
    public void start() {
        activate();
    }

    @Override
    public void stop() {
        // empty
    }

}