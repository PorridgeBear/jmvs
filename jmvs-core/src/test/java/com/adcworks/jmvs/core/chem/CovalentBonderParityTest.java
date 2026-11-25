package com.adcworks.jmvs.core.chem;

import com.adcworks.jmvs.core.io.StructureReaders;
import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.Bond;
import com.adcworks.jmvs.core.model.Model;
import com.adcworks.jmvs.core.model.Structure;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The spatial-grid bonder must produce exactly the bonds of the legacy all-pairs RasMol implementation.
 */
class CovalentBonderParityTest {

    /**
     * All-pairs port of legacy {@code Model.createCovalentBonds}.
     */
    static final class ReferenceBonder {
        static List<Bond> bond(List<Atom> atoms, boolean fast) {
            List<Bond> bonds = new ArrayList<>();
            for (int s = 0; s < atoms.size() - 1; s++) {
                for (int d = s + 1; d < atoms.size(); d++) {
                    if (CovalentBonder.isBonded(atoms.get(s), atoms.get(d), fast)) {
                        bonds.add(new Bond(atoms.get(s).index(), atoms.get(d).index()));
                    }
                }
            }
            return bonds;
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"1crn.cif", "1usc.cif", "6l1f.cif", "4hhb.bcif", "3nir.cif", "1d3z.cif"})
    void gridBondsEqualAllPairsBonds(String file) throws Exception {
        assertParity(file);
    }

    @Test
    @Tag("slow")
    void gridBondsEqualAllPairsBondsForLargeStructure() throws Exception {
        assertParity("1zpd.cif");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void bothRulesMatchOnTheSameAtoms(boolean fast) throws Exception {
        Structure s = read("1usc.cif");
        assertThat(new CovalentBonder().bond(s.atoms(), fast)).isEqualTo(ReferenceBonder.bond(s.atoms(), fast));
    }

    private static void assertParity(String file) throws Exception {
        Structure s = read(file);
        boolean fast = CovalentBonder.useFastRule(s.atoms().size());
        List<Bond> expected = new ArrayList<>();
        for (Model m : s.models()) {
            expected.addAll(ReferenceBonder.bond(s.atomsOf(m), fast));
        }
        assertThat(s.bonds()).isEqualTo(expected);
    }

    private static Structure read(String file) throws Exception {
        return StructureReaders.defaults(ElementTable.load()).read(Path.of(System.getProperty("jmvs.dataDir"), file));
    }
}
