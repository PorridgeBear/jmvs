package com.adcworks.jmvs.io.cif;

import com.adcworks.jmvs.model.Molecule;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.File;

class CIFReaderTest {

     @Test
     void readsCorrectNumberOfAtomSiteCategories() throws Exception{

         CIFReader cifReader = new CIFReader();
         File file = new File("src/test/resources/1crn.cif");

         Molecule molecule = cifReader.read(file);
         assertEquals(1, molecule.totalModels());
         assertEquals(1, molecule.totalChains());
         assertEquals(46, molecule.totalResidues());
         assertEquals(327, molecule.totalAtoms());
         assertTrue(molecule.totalBonds() > 0);
     }
}
