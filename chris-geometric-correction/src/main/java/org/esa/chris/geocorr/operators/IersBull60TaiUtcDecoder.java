package org.esa.chris.geocorr.operators;

import com.bc.ceres.core.ProgressMonitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentNavigableMap;

/**
 * @author Marco Peters
 * @since CHRIS-BOX 3.0
 */
public class IersBull60TaiUtcDecoder implements TimeTableDecoder {
    @Override
    public ConcurrentNavigableMap<Double, Double> decode(InputStream streamToDecode, ProgressMonitor pm) throws IOException {
        throw new RuntimeException("NotImplemented");
    }
}
