package com.adcworks.jmvs.scene.surface;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SurfaceNetsTest {

    private static final float RADIUS = 5f;
    private static final Grid GRID = new Grid(-8, -8, -8, 0.5f, 33, 33, 33);

    private static SurfaceNets.Mesh sphere() {
        float[] field = new float[GRID.size()];
        for (int k = 0; k < GRID.nz(); k++) {
            for (int j = 0; j < GRID.ny(); j++) {
                for (int i = 0; i < GRID.nx(); i++) {
                    float x = GRID.x(i), y = GRID.y(j), z = GRID.z(k);
                    field[GRID.index(i, j, k)] = (float) Math.sqrt(x * x + y * y + z * z) - RADIUS;
                }
            }
        }
        return SurfaceNets.extract(GRID, field, () -> false);
    }

    @Test
    void verticesLieOnTheSphereWithOutwardUnitNormals() {
        SurfaceNets.Mesh mesh = sphere();
        float[] p = mesh.positions(), n = mesh.normals();
        assertThat(mesh.vertexCount()).isGreaterThan(1000);
        for (int v = 0; v < mesh.vertexCount(); v++) {
            float x = p[v * 3], y = p[v * 3 + 1], z = p[v * 3 + 2];
            float r = (float) Math.sqrt(x * x + y * y + z * z);
            assertThat(r).isCloseTo(RADIUS, within(0.1f));
            float length = (float) Math.sqrt(n[v * 3] * n[v * 3] + n[v * 3 + 1] * n[v * 3 + 1]
                    + n[v * 3 + 2] * n[v * 3 + 2]);
            assertThat(length).isCloseTo(1f, within(1e-4f));
            assertThat((x * n[v * 3] + y * n[v * 3 + 1] + z * n[v * 3 + 2]) / r).isGreaterThan(0.95f);
        }
    }

    @Test
    void meshIsClosedAndConsistentlyWoundOutward() {
        SurfaceNets.Mesh mesh = sphere();
        int[] t = mesh.triangles();
        Map<Long, Integer> directed = new HashMap<>();
        float[] p = mesh.positions();
        for (int i = 0; i < t.length; i += 3) {
            for (int e = 0; e < 3; e++) {
                directed.merge(key(t[i + e], t[i + (e + 1) % 3]), 1, Integer::sum);
            }
            int a = t[i] * 3, b = t[i + 1] * 3, c = t[i + 2] * 3;
            float ux = p[b] - p[a], uy = p[b + 1] - p[a + 1], uz = p[b + 2] - p[a + 2];
            float vx = p[c] - p[a], vy = p[c + 1] - p[a + 1], vz = p[c + 2] - p[a + 2];
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            assertThat(nx * p[a] + ny * p[a + 1] + nz * p[a + 2]).isGreaterThan(0);
        }
        for (Map.Entry<Long, Integer> edge : directed.entrySet()) {
            int from = (int) (edge.getKey() >> 32), to = (int) (long) edge.getKey();
            assertThat(edge.getValue()).isEqualTo(1);
            assertThat(directed).containsKey(key(to, from));
        }
    }

    @Test
    void areaApproachesTheSphere() {
        assertThat(SurfaceBuilder.area(sphere())).isCloseTo(4 * Math.PI * RADIUS * RADIUS, within(0.03 * 4 * Math.PI
                * RADIUS * RADIUS));
    }

    private static long key(int from, int to) {
        return ((long) from << 32) | (to & 0xFFFFFFFFL);
    }
}
