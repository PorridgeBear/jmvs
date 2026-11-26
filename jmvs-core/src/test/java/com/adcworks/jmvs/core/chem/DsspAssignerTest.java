package com.adcworks.jmvs.core.chem;

import com.adcworks.jmvs.core.model.Element;
import com.adcworks.jmvs.core.model.SecondaryStructure;
import com.adcworks.jmvs.core.model.Structure;
import com.adcworks.jmvs.core.model.StructureBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.adcworks.jmvs.core.model.SecondaryStructure.COIL;
import static com.adcworks.jmvs.core.model.SecondaryStructure.HELIX;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Synthetic backbones built from ideal internal coordinates (NeRF): an α-helix must be assigned helix, an extended
 * strand without a partner coil, and prolines (no amide H) cannot form a helix.
 */
class DsspAssignerTest {

    private static final ElementTable ELEMENTS = ElementTable.load();
    private static final DsspAssigner ASSIGNER = new DsspAssigner();

    @Test
    void idealAlphaHelixIsHelix() {
        List<SecondaryStructure> ss = ASSIGNER.assign(backbone(20, "ALA", -57, -47, 0));

        assertThat(ss.subList(2, 18)).containsOnly(HELIX);
        assertThat(ss.stream().filter(s -> s == HELIX).count()).isGreaterThanOrEqualTo(16);
    }

    @Test
    void extendedStrandWithoutPartnerIsCoil() {
        assertThat(ASSIGNER.assign(backbone(12, "ALA", -120, 130, 0))).containsOnly(COIL);
    }

    @Test
    void prolinesHaveNoDonorsSoFormNoHelix() {
        assertThat(ASSIGNER.assign(backbone(20, "PRO", -57, -47, 0))).doesNotContain(HELIX);
    }

    @Test
    void chainBreakStopsHydrogenBondsAcrossTheGap() {
        // Second half moved 0.5 Å along x: no peptide bond across the gap, so a 6-residue helix piece each side
        // cannot reach i+4 across it; each half is still helical inside.
        List<SecondaryStructure> ss = ASSIGNER.assign(backbone(24, "ALA", -57, -47, 12));

        assertThat(ss.subList(2, 10)).containsOnly(HELIX);
        assertThat(ss.subList(14, 22)).containsOnly(HELIX);
        assertThat(ss.get(11)).isNotEqualTo(HELIX);
    }

    @Test
    void nonPolymerResiduesAreCoil() {
        Element carbon = ELEMENTS.bySymbol("C");
        Structure s = new StructureBuilder("ligand")
                .addAtom(1, "A", "A", null, 1, "", "HEM", 1, "C1", carbon, 0, 0, 0, true, Float.NaN)
                .build();

        assertThat(ASSIGNER.assign(s)).containsExactly(COIL);
    }

    /**
     * A single chain of {@code count} residues with constant phi/psi (trans peptides), as N, CA, C, O. If
     * {@code breakAt > 0}, residues from {@code breakAt} on are shifted by 5 Å so the C–N bond there is broken.
     */
    static Structure backbone(int count, String residueName, double phi, double psi, int breakAt) {
        double[][] atoms = new double[count * 4][];
        double[] n = {0, 0, 0}, ca = {1.458, 0, 0};
        double[] c = place(new double[] {0, 1, 0}, n, ca, 1.525, 111.2, -60);
        for (int i = 0; i < count; i++) {
            atoms[i * 4] = n;
            atoms[i * 4 + 1] = ca;
            atoms[i * 4 + 2] = c;
            atoms[i * 4 + 3] = place(n, ca, c, 1.231, 120.5, psi + 180);
            double[] nextN = place(n, ca, c, 1.329, 116.2, psi);
            double[] nextCa = place(ca, c, nextN, 1.458, 121.7, 180);
            double[] nextC = place(c, nextN, nextCa, 1.525, 111.2, phi);
            n = nextN;
            ca = nextCa;
            c = nextC;
        }
        Element carbon = ELEMENTS.bySymbol("C"), nitrogen = ELEMENTS.bySymbol("N"), oxygen = ELEMENTS.bySymbol("O");
        String[] names = {"N", "CA", "C", "O"};
        Element[] elements = {nitrogen, carbon, carbon, oxygen};
        var builder = new StructureBuilder("synthetic");
        for (int k = 0; k < atoms.length; k++) {
            int residue = k / 4;
            double shift = breakAt > 0 && residue >= breakAt ? 5 : 0;
            double[] p = atoms[k];
            builder.addAtom(1, "A", "A", residue + 1, residue + 1, "", residueName, k + 1, names[k % 4],
                    elements[k % 4], (float) (p[0] + shift), (float) p[1], (float) p[2], false, Float.NaN);
        }
        return builder.build();
    }

    /** NeRF: the point D with |CD| = length, angle BCD and torsion ABCD (degrees). */
    private static double[] place(double[] a, double[] b, double[] c, double length, double angle, double torsion) {
        double theta = Math.toRadians(angle), phi = Math.toRadians(torsion);
        double[] d2 = {-length * Math.cos(theta), length * Math.sin(theta) * Math.cos(phi),
                length * Math.sin(theta) * Math.sin(phi)};
        double[] bc = normalise(sub(c, b));
        double[] nrm = normalise(cross(sub(b, a), bc));
        double[] m = cross(nrm, bc);
        return new double[] {
                c[0] + bc[0] * d2[0] + m[0] * d2[1] + nrm[0] * d2[2],
                c[1] + bc[1] * d2[0] + m[1] * d2[1] + nrm[1] * d2[2],
                c[2] + bc[2] * d2[0] + m[2] * d2[1] + nrm[2] * d2[2]};
    }

    private static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] normalise(double[] a) {
        double l = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return new double[] {a[0] / l, a[1] / l, a[2] / l};
    }
}
