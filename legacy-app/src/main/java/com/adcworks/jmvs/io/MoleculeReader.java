package com.adcworks.jmvs.io;

import com.adcworks.jmvs.model.Molecule;

import java.io.IOException;

/**
 *
 * @author  allistairc
 */
public interface MoleculeReader {
    public Molecule read(java.io.File file);
}
