package com.adcworks.jmvs.core.io;

import com.adcworks.jmvs.core.chem.ElementTable;
import com.adcworks.jmvs.core.model.Bond;
import com.adcworks.jmvs.core.model.Chain;
import com.adcworks.jmvs.core.model.Residue;
import com.adcworks.jmvs.core.model.SecondaryStructure;
import com.adcworks.jmvs.core.model.Structure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CifStructureReaderTest {

    private static final ElementTable ELEMENTS = ElementTable.load();

    private static Structure synthetic() throws Exception {
        try (InputStream in = CifStructureReaderTest.class.getResourceAsStream("synthetic.cif")) {
            return new CifStructureReader(ELEMENTS).read(in, "synthetic.cif");
        }
    }

    @Test
    void groupsAtomsByModelNumberChainAndResidue() throws Exception {
        Structure s = synthetic();

        assertThat(s.models()).singleElement().satisfies(m -> assertThat(m.number()).isEqualTo(2));
        assertThat(s.chains()).extracting(Chain::id).containsExactly("A", "B", "C", "D");
        assertThat(s.residues()).extracting(Residue::name)
                .containsExactly("GLY", "ALA", "GLY", "ALA", "HOH", "HOH", "X Y");
        assertThat(s.atoms()).hasSize(8);
    }

    @Test
    void keepsOnlyFirstAlternateLocation() throws Exception {
        Structure s = synthetic();
        Residue ala = s.residues().get(1);

        assertThat(s.atomsOf(ala)).singleElement().satisfies(a -> assertThat(a.z()).isEqualTo(1.0f));
    }

    @Test
    void handlesQuotedValuesAndSeparatesNonPolymerResiduesByAuthorSequence() throws Exception {
        Structure s = synthetic();

        assertThat(s.residues().get(4).polymer()).isFalse();
        assertThat(s.residues().get(4).seqId()).isEqualTo(100);
        assertThat(s.residues().get(5).seqId()).isEqualTo(101);
        assertThat(s.atoms().get(7).name()).isEqualTo("O'");
        assertThat(s.atoms().get(7).heterogen()).isTrue();
    }

    @Test
    void appliesSecondaryStructureOnlyToTheNamedChain() throws Exception {
        Structure s = synthetic();

        assertThat(s.residues()).extracting(Residue::secondaryStructure).containsExactly(
                SecondaryStructure.HELIX, SecondaryStructure.HELIX,
                SecondaryStructure.SHEET, SecondaryStructure.TURN,
                SecondaryStructure.COIL, SecondaryStructure.COIL, SecondaryStructure.COIL);
    }

    @Test
    void computesBoundsForAllNegativeCoordinates() throws Exception {
        Structure s = synthetic();

        assertThat(s.bounds().minX()).isEqualTo(-7.5f);
        assertThat(s.bounds().maxX()).isEqualTo(-1f);
        assertThat(s.bounds().maxY()).isEqualTo(-2f);
        assertThat(s.bounds().minZ()).isEqualTo(-6f);
        assertThat(s.bounds().maxZ()).isEqualTo(1f);
    }

    @Test
    void infersBondsWithinTheModel() throws Exception {
        assertThat(synthetic().bonds()).containsExactly(
                new Bond(0, 1), new Bond(5, 6), new Bond(5, 7), new Bond(6, 7));
    }

    @Test
    void readsBFactorsAndMarksMissingOnesAsNaN(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("b.cif");
        Files.writeString(file, """
                data_B
                loop_
                _atom_site.group_PDB
                _atom_site.id
                _atom_site.type_symbol
                _atom_site.label_atom_id
                _atom_site.label_comp_id
                _atom_site.label_asym_id
                _atom_site.label_seq_id
                _atom_site.Cartn_x
                _atom_site.Cartn_y
                _atom_site.Cartn_z
                _atom_site.B_iso_or_equiv
                ATOM 1 N N  GLY A 1 0.0 0.0 0.0 91.25
                ATOM 2 C CA GLY A 1 1.4 0.0 0.0 ?
                """, StandardCharsets.UTF_8);

        Structure s = StructureReaders.defaults(ELEMENTS).read(file);

        assertThat(s.atoms().get(0).bFactor()).isEqualTo(91.25f);
        assertThat(s.atoms().get(1).hasBFactor()).isFalse();
        assertThat(synthetic().atoms()).noneMatch(a -> a.hasBFactor());
    }

    @Test
    void rejectsMalformedInput(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("broken.cif");
        Files.writeString(file, "data_X\nloop_\n_atom_site.id\n_atom_site.Cartn_x\n1 'unterminated\n",
                StandardCharsets.UTF_8);

        assertThatThrownBy(() -> StructureReaders.defaults(ELEMENTS).read(file))
                .isInstanceOf(StructureReadException.class)
                .hasMessageStartingWith("broken.cif: ");
    }

    @Test
    void rejectsFilesWithoutAtoms(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("empty.cif");
        Files.writeString(file, "data_X\n_struct.title test\n", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> StructureReaders.defaults(ELEMENTS).read(file))
                .isInstanceOf(StructureReadException.class)
                .hasMessageContaining("No atoms");
    }

    @Test
    void rejectsUnsupportedAndMissingFiles(@TempDir Path dir) {
        StructureReaders readers = StructureReaders.defaults(ELEMENTS);

        assertThat(readers.supports("x.cif.gz")).isTrue();
        assertThat(readers.supports("x.pdb")).isFalse();
        assertThatThrownBy(() -> readers.read(dir.resolve("x.pdb")))
                .isInstanceOf(StructureReadException.class).hasMessageContaining("Unsupported");
        assertThatThrownBy(() -> readers.read(dir.resolve("missing.cif")))
                .isInstanceOf(StructureReadException.class).hasMessageContaining("not found");
    }
}
