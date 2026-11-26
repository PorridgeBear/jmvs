package com.adcworks.jmvs.scene.mesh;

import com.adcworks.jmvs.scene.TriangleMesh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Splits {@link SurfaceNets} meshes into renderer-sized pieces, and derives their area and quad outlines.
 */
public final class MeshChunks {

    /** Most vertices a chunk has, so renderers can use 16-bit indices. */
    public static final int MAX_VERTICES = 65_535;

    private MeshChunks() {
    }

    /**
     * Splits the welded mesh into meshes of at most {@link #MAX_VERTICES} vertices, duplicating vertices shared
     * across a split. The two triangles of each quad stay in the same chunk, in order.
     *
     * @param vertexAtoms atom whose colour each vertex takes
     */
    public static List<TriangleMesh> split(SurfaceNets.Mesh mesh, int[] vertexAtoms) {
        int[] tris = mesh.triangles();
        int[] local = new int[mesh.vertexCount()];
        int[] stamp = new int[mesh.vertexCount()];
        Arrays.fill(stamp, -1);
        List<TriangleMesh> chunks = new ArrayList<>();
        int chunk = 0;
        int[] vertices = new int[MAX_VERTICES];
        int vertexCount = 0;
        IntList indices = new IntList();
        for (int t = 0; t < tris.length; ) {
            // A quad (two triangles) at a time; a trailing odd triangle on its own.
            int length = t + 6 <= tris.length ? 6 : 3;
            int fresh = 0;
            for (int c = 0; c < length; c++) {
                int v = tris[t + c];
                if (stamp[v] != chunk) {
                    // Count each new vertex once, even if both triangles use it.
                    stamp[v] = chunk;
                    local[v] = -1;
                    fresh++;
                }
            }
            if (vertexCount + fresh > MAX_VERTICES) {
                chunks.add(chunk(mesh, vertexAtoms, vertices, vertexCount, indices.toArray()));
                chunk++;
                vertexCount = 0;
                indices = new IntList();
                for (int c = 0; c < length; c++) {
                    stamp[tris[t + c]] = chunk;
                    local[tris[t + c]] = -1;
                }
            }
            for (int c = 0; c < length; c++) {
                int v = tris[t + c];
                if (local[v] < 0) {
                    local[v] = vertexCount;
                    vertices[vertexCount++] = v;
                }
                indices.add(local[v]);
            }
            t += length;
        }
        if (indices.size() > 0) {
            chunks.add(chunk(mesh, vertexAtoms, vertices, vertexCount, indices.toArray()));
        }
        return chunks;
    }

    private static TriangleMesh chunk(SurfaceNets.Mesh mesh, int[] vertexAtoms, int[] vertices, int count,
                                      int[] indices) {
        float[] positions = new float[count * 3];
        float[] normals = new float[count * 3];
        int[] atoms = new int[count];
        for (int i = 0; i < count; i++) {
            int v = vertices[i];
            System.arraycopy(mesh.positions(), v * 3, positions, i * 3, 3);
            System.arraycopy(mesh.normals(), v * 3, normals, i * 3, 3);
            atoms[i] = vertexAtoms[v];
        }
        return new TriangleMesh(positions, normals, atoms, indices);
    }

    /**
     * The quad outlines of a chunk made by {@link #split}, without the diagonals that split each quad into
     * triangles: 2 vertex indices per line, each edge once.
     */
    public static int[] quadEdges(TriangleMesh mesh) {
        int[] t = mesh.indices();
        long[] keys = new long[t.length / 6 * 4 + (t.length % 6 == 0 ? 0 : 3)];
        int count = 0;
        int i = 0;
        for (; i + 6 <= t.length; i += 6) {
            // The diagonal is the edge both triangles share; keep the other four.
            for (int e = 0; e < 6; e++) {
                int base = i + (e / 3) * 3;
                int a = t[base + e % 3], b = t[base + (e + 1) % 3];
                if (!(contains(t, i + 3 - (e / 3) * 3, a) && contains(t, i + 3 - (e / 3) * 3, b))) {
                    keys[count++] = key(a, b);
                }
            }
        }
        for (; i < t.length; i += 3) {
            for (int e = 0; e < 3; e++) {
                keys[count++] = key(t[i + e], t[i + (e + 1) % 3]);
            }
        }
        Arrays.sort(keys, 0, count);
        IntList lines = new IntList();
        for (int k = 0; k < count; k++) {
            if (k == 0 || keys[k] != keys[k - 1]) {
                lines.add((int) (keys[k] >>> 32));
                lines.add((int) keys[k]);
            }
        }
        return lines.toArray();
    }

    private static boolean contains(int[] t, int triangle, int vertex) {
        return t[triangle] == vertex || t[triangle + 1] == vertex || t[triangle + 2] == vertex;
    }

    private static long key(int a, int b) {
        return ((long) Math.min(a, b) << 32) | Math.max(a, b);
    }

    /** Area of a mesh in Å². */
    public static double area(SurfaceNets.Mesh mesh) {
        float[] p = mesh.positions();
        int[] t = mesh.triangles();
        double area = 0;
        for (int i = 0; i < t.length; i += 3) {
            int a = t[i] * 3, b = t[i + 1] * 3, c = t[i + 2] * 3;
            double ux = p[b] - p[a], uy = p[b + 1] - p[a + 1], uz = p[b + 2] - p[a + 2];
            double vx = p[c] - p[a], vy = p[c + 1] - p[a + 1], vz = p[c + 2] - p[a + 2];
            double cx = uy * vz - uz * vy, cy = uz * vx - ux * vz, cz = ux * vy - uy * vx;
            area += Math.sqrt(cx * cx + cy * cy + cz * cz) / 2;
        }
        return area;
    }

    private static final class IntList {
        private int[] data = new int[1024];
        private int size;

        void add(int value) {
            if (size == data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            data[size++] = value;
        }

        int size() {
            return size;
        }

        int[] toArray() {
            return Arrays.copyOf(data, size);
        }
    }
}
