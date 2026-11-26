package com.adcworks.jmvs.scene.surface;

import com.adcworks.jmvs.core.chem.ElementTable;
import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.Element;
import com.adcworks.jmvs.core.model.Structure;
import com.adcworks.jmvs.core.model.StructureBuilder;
import com.adcworks.jmvs.scene.AtomRadii;
import com.adcworks.jmvs.scene.Samples;
import com.adcworks.jmvs.scene.TriangleMesh;
import com.adcworks.jmvs.scene.config.SurfaceSettings;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class SurfaceBuilderTest {

    private static final float PROBE = 1.4f;
    private static final float CARBON = AtomRadii.UNITED.get("C");
    private static final SurfaceBuilder BUILDER = new SurfaceBuilder(SurfaceSettings.DEFAULTS);
    private static final Structure CRAMBIN = Samples.read("1crn.cif");

    @Test
    void singleAtomSurfacesAreSpheres() {
        Structure atom = carbons(0);
        assertRadius(BUILDER.build(atom, SurfaceKind.SAS), CARBON + PROBE);
        assertRadius(BUILDER.build(atom, SurfaceKind.SES), CARBON);
    }

    @Test
    void excludedSurfaceBridgesTheGapBetweenTwoAtoms() {
        // Probe touching both atoms is centred sqrt((r + p)^2 - (d / 2)^2) from the axis, so the neck is p closer.
        float d = 3.0f;
        double neck = Math.sqrt(Math.pow(CARBON + PROBE, 2) - Math.pow(d / 2, 2)) - PROBE;
        Surface surface = BUILDER.build(carbons(0, d), SurfaceKind.SES);
        double narrowest = Double.POSITIVE_INFINITY;
        for (TriangleMesh mesh : surface.meshes()) {
            float[] p = mesh.positions();
            for (int v = 0; v < mesh.vertexCount(); v++) {
                if (Math.abs(p[v * 3] - d / 2) < 0.3) {
                    narrowest = Math.min(narrowest, Math.hypot(p[v * 3 + 1], p[v * 3 + 2]));
                }
            }
        }
        assertThat(narrowest).isCloseTo(neck, within(0.15));
    }

    @Test
    void accessibleAreaMatchesShrakeRupley() {
        // The grid rounds the creases where expanded atom spheres meet, so the mesh is a little smaller; the gap
        // closes as the spacing shrinks (4.4% at 0.5 Å, 2.3% at 0.25 Å for crambin).
        double reference = shrakeRupley(SurfaceBuilder.surfaceAtoms(CRAMBIN));
        assertThat(BUILDER.build(CRAMBIN, SurfaceKind.SAS).area()).isCloseTo(reference, within(0.05 * reference));
        SurfaceBuilder fine = new SurfaceBuilder(new SurfaceSettings(PROBE, 0.25f, 50_000_000));
        assertThat(fine.build(CRAMBIN, SurfaceKind.SAS).area()).isCloseTo(reference, within(0.03 * reference));
    }

    @Test
    void crambinGoldenAreas() {
        // Golden values (ADR 0017), probe 1.4 Å, spacing 0.5 Å, united-atom radii; SAS agrees with Shrake-Rupley.
        assertThat(BUILDER.build(CRAMBIN, SurfaceKind.SAS).area()).isCloseTo(GOLDEN_SAS, within(0.01 * GOLDEN_SAS));
        assertThat(BUILDER.build(CRAMBIN, SurfaceKind.SES).area()).isCloseTo(GOLDEN_SES, within(0.01 * GOLDEN_SES));
    }

    static final double GOLDEN_SAS = 2852.2;
    static final double GOLDEN_SES = 2226.8;

    @Test
    void verticesReferToSurfaceAtomsOnly() {
        Structure haemoglobin = Samples.read("4hhb.cif");
        Surface surface = BUILDER.build(haemoglobin, SurfaceKind.SES);
        Set<Integer> allowed = new HashSet<>();
        SurfaceBuilder.surfaceAtoms(haemoglobin).forEach(a -> allowed.add(a.index()));
        Set<Integer> chains = new HashSet<>();
        for (TriangleMesh mesh : surface.meshes()) {
            assertThat(mesh.vertexCount()).isLessThanOrEqualTo(Surface.MAX_MESH_VERTICES);
            for (int atom : mesh.atoms()) {
                assertThat(allowed).contains(atom);
                chains.add(haemoglobin.chainOf(haemoglobin.atoms().get(atom)).index());
            }
            for (int index : mesh.indices()) {
                assertThat(index).isBetween(0, mesh.vertexCount() - 1);
            }
        }
        assertThat(surface.meshes()).hasSizeGreaterThan(1);
        assertThat(chains).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    void surfaceAtomsExcludeWaterHydrogenAndLaterModels() {
        assertThat(SurfaceBuilder.surfaceAtoms(Samples.read("4hhb.cif")))
                .noneMatch(a -> a.isHydrogen())
                .hasSizeLessThan(4779);
        Structure nmr = Samples.read("1d3z.cif");
        List<Atom> atoms = SurfaceBuilder.surfaceAtoms(nmr);
        int firstModelAtoms = nmr.models().getFirst().atomCount();
        assertThat(atoms).isNotEmpty().allMatch(a -> a.index() < firstModelAtoms && !a.isHydrogen());
    }

    @Test
    void emptyStructureHasEmptySurface() {
        Surface surface = BUILDER.build(new StructureBuilder("empty").build(), SurfaceKind.SES);
        assertThat(surface.meshes()).isEmpty();
        assertThat(surface.area()).isZero();
    }

    @Test
    void cancelledBuildStops() {
        assertThatThrownBy(() -> BUILDER.build(CRAMBIN, SurfaceKind.SES, () -> true))
                .isInstanceOf(CancellationException.class);
    }

    @Test
    void largeStructuresCoarsenTheGrid() {
        SurfaceBuilder small = new SurfaceBuilder(new SurfaceSettings(PROBE, 0.5f, 50_000));
        assertThat(small.build(CRAMBIN, SurfaceKind.SAS).area()).isCloseTo(GOLDEN_SAS, within(0.1 * GOLDEN_SAS));
    }

    private static void assertRadius(Surface surface, float radius) {
        assertThat(surface.meshes()).hasSize(1);
        TriangleMesh mesh = surface.meshes().getFirst();
        float[] p = mesh.positions();
        for (int v = 0; v < mesh.vertexCount(); v++) {
            double r = Math.sqrt(p[v * 3] * p[v * 3] + p[v * 3 + 1] * p[v * 3 + 1] + p[v * 3 + 2] * p[v * 3 + 2]);
            assertThat(r).isCloseTo(radius, within(0.12));
        }
        assertThat(surface.area()).isCloseTo(4 * Math.PI * radius * radius, within(0.04 * 4 * Math.PI * radius
                * radius));
    }

    /** Carbon atoms on the x axis. */
    private static Structure carbons(float... xs) {
        StructureBuilder builder = new StructureBuilder("c");
        Element carbon = ElementTable.load().bySymbol("C");
        for (int i = 0; i < xs.length; i++) {
            builder.addAtom(1, "A", "A", 1, 1, "", "GLY", i + 1, "C" + i, carbon, xs[i], 0, 0, false, Float.NaN);
        }
        return builder.build();
    }

    /** Shrake-Rupley solvent-accessible area with a golden-spiral point set, as an independent reference. */
    private static double shrakeRupley(List<Atom> atoms) {
        int points = 2000;
        double[][] sphere = new double[points][3];
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < points; i++) {
            double y = 1 - 2 * (i + 0.5) / points, r = Math.sqrt(1 - y * y);
            sphere[i] = new double[]{Math.cos(golden * i) * r, y, Math.sin(golden * i) * r};
        }
        double area = 0;
        for (Atom a : atoms) {
            double ra = AtomRadii.united(a) + PROBE;
            int exposed = 0;
            for (double[] s : sphere) {
                double x = a.x() + s[0] * ra, y = a.y() + s[1] * ra, z = a.z() + s[2] * ra;
                boolean buried = false;
                for (Atom b : atoms) {
                    if (b == a) {
                        continue;
                    }
                    double rb = AtomRadii.united(b) + PROBE;
                    double dx = x - b.x(), dy = y - b.y(), dz = z - b.z();
                    if (dx * dx + dy * dy + dz * dz < rb * rb) {
                        buried = true;
                        break;
                    }
                }
                if (!buried) {
                    exposed++;
                }
            }
            area += 4 * Math.PI * ra * ra * exposed / points;
        }
        return area;
    }
}
