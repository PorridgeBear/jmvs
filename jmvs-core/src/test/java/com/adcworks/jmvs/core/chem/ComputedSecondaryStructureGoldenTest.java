package com.adcworks.jmvs.core.chem;

import com.adcworks.jmvs.core.io.StructureReaders;
import com.adcworks.jmvs.core.model.Residue;
import com.adcworks.jmvs.core.model.SecondaryStructure;
import com.adcworks.jmvs.core.model.Structure;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Computed secondary structure against the samples' file annotation. Agreement is per polymer residue in three
 * states (helix, sheet, other: turns count as other) and must reach a threshold rather than match exactly, because
 * PDB annotations come from different programs and authors (4hhb's helix ends differ most; the AlphaFold file's
 * annotation is DSSP 4's). Counts are the computed golden values. ADR 0018.
 */
class ComputedSecondaryStructureGoldenTest {

    private static final StructureReaders READERS = StructureReaders.defaults(ElementTable.load());

    @ParameterizedTest
    @CsvSource({
            // file,                     agreement, helix, sheet
            "1crn.cif,                   0.78,      22,    4",
            "1usc.cif,                   0.94,      47,    148",
            "1zpd.cif,                   0.95,      917,   385",
            "6l1f.cif,                   0.95,      3,     27",
            "4hhb.cif,                   0.81,      442,   0",
            "3nir.cif,                   0.83,      16,    4",
            "1d3z.cif,                   0.90,      180,   230",
            "1aon.cif.gz,                0.94,      3721,  1318",
            "AF-P69905-F1-model_v6.cif,  0.94,      106,   0",
    })
    void computedAgreesWithFileAnnotation(String file, double agreement, int helix, int sheet) throws Exception {
        Structure s = READERS.read(Path.of(System.getProperty("jmvs.dataDir"), file));

        List<SecondaryStructure> computed = new DsspAssigner().assign(s);

        assertThat(computed).hasSameSizeAs(s.residues());
        assertThat(computed.stream().filter(x -> x == SecondaryStructure.HELIX).count()).isEqualTo(helix);
        assertThat(computed.stream().filter(x -> x == SecondaryStructure.SHEET).count()).isEqualTo(sheet);
        List<Residue> polymer = s.residues().stream().filter(Residue::polymer).toList();
        long agreeing = polymer.stream()
                .filter(r -> threeState(r.secondaryStructure()) == threeState(computed.get(r.index())))
                .count();
        assertThat((double) agreeing / polymer.size()).isGreaterThanOrEqualTo(agreement);
        s.residues().stream().filter(r -> !r.polymer())
                .forEach(r -> assertThat(computed.get(r.index())).isEqualTo(SecondaryStructure.COIL));
    }

    private static SecondaryStructure threeState(SecondaryStructure ss) {
        return ss == SecondaryStructure.TURN ? SecondaryStructure.COIL : ss;
    }
}
