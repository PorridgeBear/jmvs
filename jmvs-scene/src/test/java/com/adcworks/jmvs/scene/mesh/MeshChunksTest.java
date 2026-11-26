package com.adcworks.jmvs.scene.mesh;

import com.adcworks.jmvs.scene.TriangleMesh;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MeshChunksTest {

    /** A flat {@code size × size} grid of vertices, each cell one quad split into two triangles. */
    private static SurfaceNets.Mesh sheet(int size) {
        float[] positions = new float[size * size * 3];
        float[] normals = new float[size * size * 3];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int v = (x + y * size) * 3;
                positions[v] = x;
                positions[v + 1] = y;
                normals[v + 2] = 1;
            }
        }
        int cells = size - 1;
        int[] triangles = new int[cells * cells * 6];
        int t = 0;
        for (int y = 0; y < cells; y++) {
            for (int x = 0; x < cells; x++) {
                int a = x + y * size, b = a + 1, c = a + size + 1, d = a + size;
                triangles[t++] = a;
                triangles[t++] = b;
                triangles[t++] = c;
                triangles[t++] = a;
                triangles[t++] = c;
                triangles[t++] = d;
            }
        }
        return new SurfaceNets.Mesh(positions, normals, triangles);
    }

    @Test
    void splitsLargeMeshesKeepingQuadsTogether() {
        SurfaceNets.Mesh mesh = sheet(300);
        int[] atoms = new int[mesh.vertexCount()];
        for (int v = 0; v < atoms.length; v++) {
            atoms[v] = v;
        }
        List<TriangleMesh> chunks = MeshChunks.split(mesh, atoms);

        assertThat(chunks).hasSizeGreaterThan(1);
        int triangles = 0;
        for (TriangleMesh chunk : chunks) {
            assertThat(chunk.vertexCount()).isLessThanOrEqualTo(MeshChunks.MAX_VERTICES);
            assertThat(chunk.indices().length % 6).isZero();
            triangles += chunk.indices().length / 3;
            // Each chunk vertex keeps its own position and atom.
            for (int v = 0; v < chunk.vertexCount(); v++) {
                int original = chunk.atoms()[v];
                assertThat(chunk.positions()[v * 3]).isEqualTo(mesh.positions()[original * 3]);
                assertThat(chunk.positions()[v * 3 + 1]).isEqualTo(mesh.positions()[original * 3 + 1]);
            }
        }
        assertThat(triangles).isEqualTo(mesh.triangles().length / 3);
    }

    @Test
    void quadEdgesSkipDiagonals() {
        SurfaceNets.Mesh mesh = sheet(4);
        TriangleMesh chunk = MeshChunks.split(mesh, new int[mesh.vertexCount()]).getFirst();
        int[] lines = MeshChunks.quadEdges(chunk);

        // A 3 × 3 grid of cells has 2 · 3 · 4 = 24 grid edges and no diagonals.
        assertThat(lines).hasSize(24 * 2);
        float[] p = chunk.positions();
        for (int i = 0; i < lines.length; i += 2) {
            float dx = p[lines[i] * 3] - p[lines[i + 1] * 3];
            float dy = p[lines[i] * 3 + 1] - p[lines[i + 1] * 3 + 1];
            assertThat(dx * dx + dy * dy).isEqualTo(1f);
        }
    }

    @Test
    void areaOfAFlatSheet() {
        assertThat(MeshChunks.area(sheet(5))).isEqualTo(16.0);
    }
}
