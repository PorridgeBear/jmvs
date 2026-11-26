package com.adcworks.jmvs.core.io;

import com.adcworks.jmvs.core.chem.ElementTable;
import com.adcworks.jmvs.core.model.SecondaryStructure;
import com.adcworks.jmvs.core.model.Structure;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Golden values for the sample files in {@code data/} (see data/README.md). 1crn's counts match the legacy
 * CIFReaderTest; 4hhb.bcif must equal 4hhb.cif; files with alternate locations (1usc, 1zpd, 3nir) have fewer atoms
 * than {@code ATOM}/{@code HETATM} lines because only the first alternate location is kept.
 */
class SampleFilesGoldenTest {

    private static final StructureReaders READERS = StructureReaders.defaults(ElementTable.load());

    @ParameterizedTest
    @CsvSource({
            // file,       models, chains, residues, atoms, bonds, helix, sheet, heterogen, minX,    maxZ
            "1crn.cif,     1,      1,      46,       327,   337,   21,    8,     0,         -3.097,  19.580",
            "1usc.cif,     1,      8,      771,      3285,  2974,  53,    148,   481,       33.233,  45.305",
            "1usc.cif.gz,  1,      8,      771,      3285,  2974,  53,    148,   481,       33.233,  45.305",
            "1zpd.cif,     1,      20,     4841,     19789, 17572, 967,   387,   2729,      -41.934, 59.272",
            "6l1f.cif,     1,      3,      105,      667,   656,   5,     27,    42,        -22.698, 4.647",
            "4hhb.cif,     1,      14,     801,      4779,  4574,  448,   0,     395,       -24.242, 70.977",
            "4hhb.bcif,    1,      14,     801,      4779,  4574,  448,   0,     395,       -24.242, 70.977",
            "3nir.cif,     1,      6,      150,      784,   762,   13,    4,     110,       -10.142, 25.234",
            "1d3z.cif,     10,     10,     760,      12310, 12370, 220,   230,   0,         35.630,  17.911",
            "1aon.cif.gz,  1,      35,     8029,     58870, 59279, 3835,  1153,  196,       7.075,   77.793",
            "AF-P69905-F1-model_v6.cif, 1, 1, 142, 1077, 1105, 111, 0, 0, -19.915, 20.000",
    })
    void sampleMatchesGoldenValues(String file, int models, int chains, int residues, int atoms, int bonds,
                                   int helix, int sheet, long heterogen, float minX, float maxZ) throws Exception {
        Structure s = READERS.read(Path.of(System.getProperty("jmvs.dataDir"), file));

        assertThat(s.models()).hasSize(models);
        assertThat(s.chains()).hasSize(chains);
        assertThat(s.residues()).hasSize(residues);
        assertThat(s.atoms()).hasSize(atoms);
        assertThat(s.bonds()).hasSize(bonds);
        assertThat(s.countResidues(SecondaryStructure.HELIX)).isEqualTo(helix);
        assertThat(s.countResidues(SecondaryStructure.SHEET)).isEqualTo(sheet);
        assertThat(s.atoms().stream().filter(a -> a.heterogen()).count()).isEqualTo(heterogen);
        assertThat(s.bounds().minX()).isCloseTo(minX, within(0.001f));
        assertThat(s.bounds().maxZ()).isCloseTo(maxZ, within(0.001f));
        assertThat(s.name()).isEqualTo(file.replace(".gz", ""));
        assertThat(s.models().stream().mapToInt(m -> m.number()).toArray())
                .isEqualTo(java.util.stream.IntStream.rangeClosed(1, models).toArray());
    }
}
