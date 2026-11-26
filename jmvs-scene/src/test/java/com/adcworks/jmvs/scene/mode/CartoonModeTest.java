package com.adcworks.jmvs.scene.mode;

import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.Residue;
import com.adcworks.jmvs.core.model.SecondaryStructure;
import com.adcworks.jmvs.core.model.Structure;
import com.adcworks.jmvs.scene.Registry;
import com.adcworks.jmvs.scene.Samples;
import com.adcworks.jmvs.scene.Scene;
import com.adcworks.jmvs.scene.TriangleMesh;
import com.adcworks.jmvs.scene.config.CartoonSettings;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Cartoon geometry, checked through its cross-section rings: in a protein tube mesh, vertices come in rings of
 * {@link CartoonMode#RING} (vertex 0 and RING/2 lie on the width axis, RING/4 and 3·RING/4 on the thickness axis),
 * followed by two end caps.
 */
class CartoonModeTest {

    private static final CartoonSettings SETTINGS = CartoonSettings.DEFAULTS;
    private static final Structure USC = Samples.read("1usc.cif");
    private static final Structure BNA = Samples.read("1bna.cif");

    private record Section(int atom, double width, double thickness) {
    }

    @Test
    void proteinChainsBecomeTubesTaggedWithAlphaCarbons() {
        Scene scene = new CartoonMode().build(USC);

        assertThat(scene.meshes()).isNotEmpty();
        assertThat(scene.spheres().count()).isZero();
        assertThat(scene.cylinders().count()).isZero();
        for (TriangleMesh mesh : scene.meshes()) {
            assertThat(mesh.vertexCount()).isLessThanOrEqualTo(65_535);
            assertThat(Arrays.stream(mesh.indices())).allMatch(i -> i >= 0 && i < mesh.vertexCount());
            assertThat(normalLengths(mesh)).allSatisfy(l -> assertThat(l).isCloseTo(1.0, within(0.01)));
            assertThat(Arrays.stream(mesh.atoms()).mapToObj(i -> USC.atoms().get(i).name())
                    .collect(Collectors.toSet())).containsExactly("CA");
        }
        assertThat(scene.pickTargets().count()).isGreaterThan(300);
    }

    @Test
    void helicesAreWideThickRibbonsAndCoilIsAThinTube() {
        List<Section> sections = sections(new CartoonMode().build(USC));

        Section helix = widest(sections, SecondaryStructure.HELIX);
        assertThat(helix.width).isCloseTo(SETTINGS.helixWidth(), within(0.05));
        assertThat(helix.thickness).isCloseTo(SETTINGS.helixThickness(), within(0.05));
        double coilWidth = sections.stream().filter(s -> ss(s) == SecondaryStructure.COIL)
                .mapToDouble(Section::width).min().orElseThrow();
        assertThat(coilWidth).isCloseTo(2 * SETTINGS.coilRadius(), within(0.05));
    }

    @Test
    void strandsEndInArrowheadsWiderThanTheStrand() {
        List<Section> sections = sections(new CartoonMode().build(USC));
        Set<Integer> arrowAtoms = arrowResidueAtoms(USC);

        double arrow = sections.stream().filter(s -> arrowAtoms.contains(s.atom)).mapToDouble(Section::width)
                .max().orElseThrow();
        // Strand sections away from a boundary (where widths blend into the neighbour's) have the strand width.
        double[] strand = sections.stream().filter(s -> ss(s) == SecondaryStructure.SHEET
                && !arrowAtoms.contains(s.atom)).mapToDouble(Section::width).sorted().toArray();

        assertThat(arrowAtoms).isNotEmpty();
        assertThat(arrow).isCloseTo(SETTINGS.arrowWidth(), within(0.05));
        assertThat(strand[strand.length / 2]).isCloseTo(SETTINGS.sheetWidth(), within(0.05));
        // Arrowheads narrow to the coil radius by the end of the residue.
        assertThat(sections.stream().filter(s -> arrowAtoms.contains(s.atom)).mapToDouble(Section::width).min()
                .orElseThrow()).isLessThan(SETTINGS.sheetWidth());
    }

    @Test
    void nucleicAcidsHaveABackboneTubeAndOneBlockAndStickPerBase() {
        Scene scene = new CartoonMode().build(BNA);

        Set<String> tubeAtoms = new HashSet<>();
        Set<String> blockAtoms = new HashSet<>();
        int blockVertices = 0;
        for (TriangleMesh mesh : scene.meshes()) {
            Set<String> names = Arrays.stream(mesh.atoms()).mapToObj(i -> BNA.atoms().get(i).name())
                    .collect(Collectors.toSet());
            if (names.contains("C1'")) {
                blockAtoms.addAll(names);
                blockVertices += mesh.vertexCount();
            } else {
                tubeAtoms.addAll(names);
            }
        }
        assertThat(tubeAtoms).containsAnyOf("P", "C4'").isSubsetOf("P", "C4'");
        assertThat(blockAtoms).containsExactly("C1'");
        assertThat(blockVertices).isEqualTo(24 * CartoonMode.BLOCK_VERTICES);
        assertThat(scene.cylinders().count()).isEqualTo(24);
        assertThat(Arrays.stream(scene.cylinders().atoms()).mapToObj(i -> BNA.atoms().get(i).name()))
                .containsOnly("C1'");
        assertThat(Arrays.stream(scene.pickTargets().atoms()).mapToObj(i -> BNA.atoms().get(i).name()))
                .contains("P", "C1'");
    }

    @Test
    void longChainsAreSplitIntoMeshesWithSixteenBitIndices() {
        CartoonSettings fine = new CartoonSettings(2.4f, 0.5f, 2.0f, 0.5f, 3.4f, 0.3f, 0.8f, 64);
        Scene scene = new CartoonMode(fine).build(Samples.read("4hhb.cif"));

        // 141-146 residues per chain at 64 samples each is over 9,000 rings per chain.
        assertThat(scene.meshes().size()).isGreaterThan(4);
        assertThat(scene.meshes()).allSatisfy(m -> assertThat(m.vertexCount()).isLessThanOrEqualTo(65_535));
    }

    @Test
    void cartoonIsRegisteredAndDoesNotSupportCpk() {
        Registry registry = Registry.load();
        DisplayMode cartoon = registry.mode("cartoon");

        assertThat(cartoon.displayName()).isEqualTo("Cartoon");
        assertThat(cartoon.supports(registry.scheme("cpk"))).isFalse();
        assertThat(registry.schemeFor(cartoon, registry.scheme("cpk")).id()).isEqualTo("chain");
    }

    /** Cross-sections of every protein tube mesh with both end caps, in order. */
    private static List<Section> sections(Scene scene) {
        List<Section> result = new ArrayList<>();
        int ring = CartoonMode.RING;
        for (TriangleMesh mesh : scene.meshes()) {
            int ringVertices = mesh.vertexCount() - 2 * (ring + 1);
            if (ringVertices <= 0 || ringVertices % ring != 0) {
                continue;
            }
            float[] p = mesh.positions();
            for (int r = 0; r < ringVertices / ring; r++) {
                int v = r * ring;
                result.add(new Section(mesh.atoms()[v], distance(p, v, v + ring / 2),
                        distance(p, v + ring / 4, v + 3 * ring / 4)));
            }
        }
        return result;
    }

    private static SecondaryStructure ss(Section section) {
        Atom atom = USC.atoms().get(section.atom);
        return USC.residueOf(atom).secondaryStructure();
    }

    private static Section widest(List<Section> sections, SecondaryStructure type) {
        return sections.stream().filter(s -> ss(s) == type)
                .max((a, b) -> Double.compare(a.width, b.width)).orElseThrow();
    }

    /** Cα atoms of the last residue of each strand. */
    private static Set<Integer> arrowResidueAtoms(Structure s) {
        Set<Integer> result = new HashSet<>();
        for (var chain : s.chains()) {
            List<Residue> residues = s.residuesOf(chain);
            for (int i = 0; i < residues.size(); i++) {
                Residue r = residues.get(i);
                boolean last = i + 1 == residues.size()
                        || residues.get(i + 1).secondaryStructure() != SecondaryStructure.SHEET;
                if (r.secondaryStructure() == SecondaryStructure.SHEET && last) {
                    s.findAtom(r, "CA").ifPresent(a -> result.add(a.index()));
                }
            }
        }
        return result;
    }

    private static double distance(float[] p, int a, int b) {
        double dx = p[a * 3] - p[b * 3], dy = p[a * 3 + 1] - p[b * 3 + 1], dz = p[a * 3 + 2] - p[b * 3 + 2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static List<Double> normalLengths(TriangleMesh mesh) {
        float[] n = mesh.normals();
        List<Double> lengths = new ArrayList<>();
        for (int i = 0; i < n.length; i += 3) {
            lengths.add(Math.sqrt(n[i] * n[i] + n[i + 1] * n[i + 1] + n[i + 2] * n[i + 2]));
        }
        return lengths;
    }
}
