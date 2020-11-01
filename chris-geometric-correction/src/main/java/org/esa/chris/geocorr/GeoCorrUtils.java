package org.esa.chris.geocorr;

import org.esa.snap.core.util.ModuleMetadata;
import org.esa.snap.core.util.SystemUtils;

import java.nio.file.Path;

/**
 * Provides utility methods for the geometric correction module.
 *
 * @author Marco Peters
 * @since CHRIS-BOX 3.0
 */
public class GeoCorrUtils {

    /**
     * Returns the auxiliary data directory. It considers the current version of the module. Auxdata of different
     * versions is stored in different directories.
     *
     * @return The path to the auxiliary data directory.
     */
    public static Path getAuxdataDir() {

        ModuleMetadata moduleMetadata = SystemUtils.loadModuleMetadata(AuxdataInstaller.class);

        String version = "unknown";
        if (moduleMetadata != null) {
            version = moduleMetadata.getVersion();
        }

        return SystemUtils.getAuxDataPath().resolve("chris").resolve(version).resolve("geometric-correction");
    }
}
