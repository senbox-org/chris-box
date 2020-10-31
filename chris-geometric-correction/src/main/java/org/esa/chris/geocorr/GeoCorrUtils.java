package org.esa.chris.geocorr;

import org.esa.snap.core.util.SystemUtils;

import java.nio.file.Path;

/**
 * @author Marco Peters
 * @since
 */
public class GeoCorrUtils {
    public static Path getAuxdataDir() {
        return SystemUtils.getAuxDataPath().resolve("chris").resolve("geometric-correction");
    }
}
