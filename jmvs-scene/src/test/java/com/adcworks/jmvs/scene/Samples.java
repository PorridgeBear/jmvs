package com.adcworks.jmvs.scene;

import com.adcworks.jmvs.core.chem.ElementTable;
import com.adcworks.jmvs.core.io.StructureReaders;
import com.adcworks.jmvs.core.model.Structure;

import java.nio.file.Path;

public final class Samples {

    private Samples() {
    }

    public static Structure read(String file) {
        try {
            return StructureReaders.defaults(ElementTable.load())
                    .read(Path.of(System.getProperty("jmvs.dataDir"), file));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
