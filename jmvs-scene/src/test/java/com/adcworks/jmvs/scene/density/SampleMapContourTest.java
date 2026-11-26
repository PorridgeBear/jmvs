package com.adcworks.jmvs.scene.density;

import com.adcworks.jmvs.core.io.MrcMapReader;
import com.adcworks.jmvs.core.map.DensityMap;
import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.Structure;
import com.adcworks.jmvs.scene.Samples;
import com.adcworks.jmvs.scene.TriangleMesh;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * The sample cryo-EM map EMD-3488 (haemoglobin) contoured at EMDB's recommended level, zone-coloured by its fitted
 * model 5ME2.
 */
class SampleMapContourTest {

    private static final float AUTHOR_CONTOUR = 0.09f;
    private static DensityMap map;
    private static Structure model;

    @BeforeAll
    static void read() throws Exception {
        map = new MrcMapReader(32_000_000).read(Path.of(System.getProperty("jmvs.dataDir"), "emd_3488.map.gz"));
        model = Samples.read("5me2.cif");
    }

    @Test
    void contourAtTheRecommendedLevel() {
        Isosurface iso = IsosurfaceBuilder.build(map, AUTHOR_CONTOUR);

        assertThat(iso.triangleCount()).isBetween(60_000, 200_000);
        assertThat(iso.meshes().size()).isGreaterThanOrEqualTo(1);
        // About 2% of the 105 Å box: the size of a 64 kDa tetramer.
        assertThat(iso.volume()).isCloseTo(0.0192 * Math.pow(105, 3), within(0.001 * Math.pow(105, 3)));
        assertThat(iso.lines()).hasSameSizeAs(iso.meshes());
    }

    @Test
    void zoneColouringFollowsTheFittedModel() {
        Isosurface iso = ZoneColouring.assign(IsosurfaceBuilder.build(map, AUTHOR_CONTOUR), Optional.of(model), 3f,
                () -> false);
        long assigned = 0;
        long total = 0;
        for (TriangleMesh mesh : iso.meshes()) {
            float[] p = mesh.positions();
            for (int v = 0; v < mesh.vertexCount(); v++) {
                total++;
                int atom = mesh.atoms()[v];
                if (atom == Isosurface.NO_ATOM) {
                    continue;
                }
                assigned++;
                Atom a = model.atoms().get(atom);
                assertThat(a.isHydrogen()).isFalse();
                double dx = a.x() - p[v * 3], dy = a.y() - p[v * 3 + 1], dz = a.z() - p[v * 3 + 2];
                assertThat(Math.sqrt(dx * dx + dy * dy + dz * dz)).isLessThanOrEqualTo(3.0 + 1e-4);
            }
        }
        // Most of the contour is explained by the model.
        assertThat((double) assigned / total).isGreaterThan(0.8);
    }

    @Test
    void withoutAModelEveryVertexHasTheMapColour() {
        Isosurface iso = ZoneColouring.assign(IsosurfaceBuilder.build(map, AUTHOR_CONTOUR), Optional.empty(), 3f,
                () -> false);
        for (TriangleMesh mesh : iso.meshes()) {
            for (int atom : mesh.atoms()) {
                assertThat(atom).isEqualTo(Isosurface.NO_ATOM);
            }
        }
    }

    @Test
    void contouringTheSampleIsFast() {
        IsosurfaceBuilder.build(map, AUTHOR_CONTOUR);
        long start = System.nanoTime();
        IsosurfaceBuilder.build(map, AUTHOR_CONTOUR);
        // Generous bound for CI; about 50 ms on a laptop.
        assertThat((System.nanoTime() - start) / 1e6).isLessThan(2000);
    }
}
