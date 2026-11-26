package com.adcworks.jmvs.core.chem;

import com.adcworks.jmvs.core.io.StructureReaders;
import com.adcworks.jmvs.core.model.Residue;
import com.adcworks.jmvs.core.model.SecondaryStructure;
import com.adcworks.jmvs.core.model.SecondaryStructureSource;
import com.adcworks.jmvs.core.model.Structure;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SecondaryStructuresTest {

    private static final StructureReaders READERS = StructureReaders.defaults(ElementTable.load());
    private static final SecondaryStructures SOURCES = new SecondaryStructures();

    private static Structure read(String file) throws Exception {
        return READERS.read(Path.of(System.getProperty("jmvs.dataDir"), file));
    }

    @Test
    void autoComputesWhenTheFileHasNoAnnotation() throws Exception {
        Structure read = read("AF-P69905-F1-model_v6-noss.cif");

        Structure shown = SOURCES.apply(read, SecondaryStructureSource.AUTO);

        assertThat(read.countResidues(SecondaryStructure.HELIX)).isZero();
        assertThat(shown.countResidues(SecondaryStructure.HELIX)).isEqualTo(106);
        assertThat(shown.atoms()).isSameAs(read.atoms());
        assertThat(shown.bonds()).isSameAs(read.bonds());
    }

    @Test
    void autoKeepsTheFileAnnotationWhenPresent() throws Exception {
        Structure read = read("1crn.cif");

        assertThat(SOURCES.apply(read, SecondaryStructureSource.AUTO)).isSameAs(read);
        assertThat(SOURCES.apply(read, SecondaryStructureSource.FILE)).isSameAs(read);
    }

    @Test
    void computedIgnoresTheFileAnnotation() throws Exception {
        Structure read = read("1crn.cif");

        Structure computed = SOURCES.apply(read, SecondaryStructureSource.COMPUTED);

        assertThat(computed.countResidues(SecondaryStructure.HELIX)).isEqualTo(22);
        assertThat(computed.countResidues(SecondaryStructure.SHEET)).isEqualTo(4);
        assertThat(computed.residues()).extracting(Residue::name).isEqualTo(
                read.residues().stream().map(Residue::name).toList());
        assertThat(computed.secondaryStructureAnnotated()).isTrue();
    }

    @Test
    void fileSourceOnAnUnannotatedFileIsAllCoil() throws Exception {
        Structure read = read("AF-P69905-F1-model_v6-noss.cif");

        assertThat(SOURCES.apply(read, SecondaryStructureSource.FILE).countResidues(SecondaryStructure.COIL))
                .isEqualTo(read.residues().size());
    }

    @Test
    void sourcesAreFoundById() {
        assertThat(SecondaryStructureSource.byId("computed")).contains(SecondaryStructureSource.COMPUTED);
        assertThat(SecondaryStructureSource.byId("dssp")).isEmpty();
    }
}
