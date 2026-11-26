package com.adcworks.jmvs.scene.density;

import com.adcworks.jmvs.core.map.DensityMap;
import com.adcworks.jmvs.scene.TriangleMesh;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class IsosurfaceBuilderTest {

    private static final int N = 41;
    private static final float VOXEL = 0.5f;
    private static final float CENTRE = 10f;

    /** Density falling off linearly from the centre, so level {@code 10 − r} contours a sphere of radius r. */
    private static DensityMap ball(float[] axes) {
        float[] values = new float[N * N * N];
        float[] p = new float[3];
        DensityMap probe = new DensityMap("ball", N, N, N, values, new float[3], axes,
                new DensityMap.Statistics(0, 0, 0, 0));
        for (int k = 0; k < N; k++) {
            for (int j = 0; j < N; j++) {
                for (int i = 0; i < N; i++) {
                    probe.position(i, j, k, p);
                    double dx = p[0] - CENTRE, dy = p[1] - CENTRE, dz = p[2] - CENTRE;
                    values[probe.index(i, j, k)] = 10 - (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                }
            }
        }
        return new DensityMap("ball", N, N, N, values, new float[3], axes, DensityMap.Statistics.of(values));
    }

    private static DensityMap orthogonalBall() {
        return ball(new float[]{VOXEL, 0, 0, 0, VOXEL, 0, 0, 0, VOXEL});
    }

    @Test
    void contoursASphereWithOutwardNormals() {
        float radius = 6;
        Isosurface iso = IsosurfaceBuilder.build(orthogonalBall(), 10 - radius);

        assertThat(iso.level()).isEqualTo(4f);
        assertThat(iso.meshes()).hasSize(1);
        assertThat(iso.area()).isCloseTo(4 * Math.PI * radius * radius, within(0.03 * 4 * Math.PI * radius * radius));
        assertThat(iso.volume()).isCloseTo(4 / 3.0 * Math.PI * radius * radius * radius,
                within(0.03 * 4 / 3.0 * Math.PI * radius * radius * radius));
        TriangleMesh mesh = iso.meshes().getFirst();
        float[] p = mesh.positions(), n = mesh.normals();
        for (int v = 0; v < mesh.vertexCount(); v++) {
            float x = p[v * 3] - CENTRE, y = p[v * 3 + 1] - CENTRE, z = p[v * 3 + 2] - CENTRE;
            float r = (float) Math.sqrt(x * x + y * y + z * z);
            assertThat(r).isCloseTo(radius, within(0.1f));
            assertThat((x * n[v * 3] + y * n[v * 3 + 1] + z * n[v * 3 + 2]) / r).isGreaterThan(0.95f);
            assertThat(mesh.atoms()[v]).isEqualTo(Isosurface.NO_ATOM);
        }
    }

    @Test
    void meshIsClosedAndWoundOutward() {
        TriangleMesh mesh = IsosurfaceBuilder.build(orthogonalBall(), 4).meshes().getFirst();
        int[] t = mesh.indices();
        float[] p = mesh.positions();
        Map<Long, Integer> directed = new HashMap<>();
        for (int i = 0; i < t.length; i += 3) {
            for (int e = 0; e < 3; e++) {
                directed.merge(key(t[i + e], t[i + (e + 1) % 3]), 1, Integer::sum);
            }
            int a = t[i] * 3, b = t[i + 1] * 3, c = t[i + 2] * 3;
            float ux = p[b] - p[a], uy = p[b + 1] - p[a + 1], uz = p[b + 2] - p[a + 2];
            float vx = p[c] - p[a], vy = p[c + 1] - p[a + 1], vz = p[c + 2] - p[a + 2];
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            assertThat(nx * (p[a] - CENTRE) + ny * (p[a + 1] - CENTRE) + nz * (p[a + 2] - CENTRE)).isGreaterThan(0);
        }
        directed.forEach((edge, count) -> {
            assertThat(count).isEqualTo(1);
            assertThat(directed).containsKey(key((int) (long) edge, (int) (edge >> 32)));
        });
    }

    @Test
    void meshLinesOutlineQuadsWithoutDiagonals() {
        Isosurface iso = IsosurfaceBuilder.build(orthogonalBall(), 4);
        TriangleMesh mesh = iso.meshes().getFirst();
        int[] lines = iso.lines().getFirst();
        Set<Long> unique = new HashSet<>();
        for (int i = 0; i < lines.length; i += 2) {
            assertThat(unique.add(key(Math.min(lines[i], lines[i + 1]), Math.max(lines[i], lines[i + 1])))).isTrue();
        }
        // A closed quad mesh: V − E + F = 2 with F quads, and each quad has 4 edges shared by 2 quads.
        int quads = mesh.indices().length / 6;
        assertThat(unique).hasSize(quads * 2);
        assertThat(mesh.vertexCount() - unique.size() + quads).isEqualTo(2);
    }

    @Test
    void obliqueAxesMoveVerticesToCartesianSpace() {
        // A sheared grid still contours the same Cartesian sphere.
        float s = VOXEL;
        DensityMap oblique = ball(new float[]{s, 0, 0, s * 0.3f, s, 0, 0, s * 0.2f, s});
        float radius = 4;
        Isosurface iso = IsosurfaceBuilder.build(oblique, 10 - radius);
        TriangleMesh mesh = iso.meshes().getFirst();
        float[] p = mesh.positions(), n = mesh.normals();
        for (int v = 0; v < mesh.vertexCount(); v++) {
            float x = p[v * 3] - CENTRE, y = p[v * 3 + 1] - CENTRE, z = p[v * 3 + 2] - CENTRE;
            float r = (float) Math.sqrt(x * x + y * y + z * z);
            assertThat(r).isCloseTo(radius, within(0.15f));
            assertThat((x * n[v * 3] + y * n[v * 3 + 1] + z * n[v * 3 + 2]) / r).isGreaterThan(0.9f);
        }
        assertThat(iso.volume()).isCloseTo(4 / 3.0 * Math.PI * radius * radius * radius,
                within(0.05 * 4 / 3.0 * Math.PI * radius * radius * radius));
    }

    @Test
    void boundingSphereEnclosesTheContour() {
        var sphere = IsosurfaceBuilder.build(orthogonalBall(), 4).boundingSphere().orElseThrow();
        assertThat(sphere.x()).isCloseTo(CENTRE, within(0.05f));
        assertThat(sphere.radius()).isCloseTo(6f, within(0.2f));
    }

    @Test
    void levelAboveTheMaximumIsEmpty() {
        Isosurface iso = IsosurfaceBuilder.build(orthogonalBall(), 11);
        assertThat(iso.meshes()).isEmpty();
        assertThat(iso.boundingSphere()).isEmpty();
        assertThat(iso.area()).isZero();
        assertThat(iso.volume()).isZero();
    }

    @Test
    void cancellationStopsTheBuild() {
        assertThatThrownBy(() -> IsosurfaceBuilder.build(orthogonalBall(), 4, () -> true))
                .isInstanceOf(CancellationException.class);
    }

    @Test
    void inverseTransposeOfOrthogonalAxesIsTheReciprocal() {
        assertThat(IsosurfaceBuilder.inverseTranspose(new float[]{2, 0, 0, 0, 4, 0, 0, 0, 0.5f}))
                .containsExactly(0.5f, 0, 0, 0, 0.25f, 0, 0, 0, 2);
    }

    private static long key(int from, int to) {
        return ((long) from << 32) | (to & 0xFFFFFFFFL);
    }
}
