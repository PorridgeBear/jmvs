package com.adcworks.jmvs.render;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit meshes used as instance templates: an icosphere (radius 1) and an open cylinder (radius 1, y in [0, 1]).
 * Vertices are interleaved {@code x, y, z, nx, ny, nz}.
 */
final class Primitives {

    private Primitives() {
    }

    static Mesh icosphere(int subdivisions) {
        List<float[]> vertices = new ArrayList<>();
        List<int[]> faces = new ArrayList<>();
        float t = (float) ((1 + Math.sqrt(5)) / 2);
        float[][] base = {{-1, t, 0}, {1, t, 0}, {-1, -t, 0}, {1, -t, 0}, {0, -1, t}, {0, 1, t}, {0, -1, -t},
                {0, 1, -t}, {t, 0, -1}, {t, 0, 1}, {-t, 0, -1}, {-t, 0, 1}};
        for (float[] v : base) {
            vertices.add(normalise(v));
        }
        int[][] baseFaces = {{0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11}, {1, 5, 9}, {5, 11, 4},
                {11, 10, 2}, {10, 7, 6}, {7, 1, 8}, {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9},
                {4, 9, 5}, {2, 4, 11}, {6, 2, 10}, {8, 6, 7}, {9, 8, 1}};
        faces.addAll(List.of(baseFaces));
        for (int s = 0; s < subdivisions; s++) {
            Map<Long, Integer> midpoints = new HashMap<>();
            List<int[]> next = new ArrayList<>();
            for (int[] f : faces) {
                int a = midpoint(vertices, midpoints, f[0], f[1]);
                int b = midpoint(vertices, midpoints, f[1], f[2]);
                int c = midpoint(vertices, midpoints, f[2], f[0]);
                next.add(new int[]{f[0], a, c});
                next.add(new int[]{f[1], b, a});
                next.add(new int[]{f[2], c, b});
                next.add(new int[]{a, b, c});
            }
            faces = next;
        }
        float[] data = new float[vertices.size() * 6];
        for (int i = 0; i < vertices.size(); i++) {
            float[] v = vertices.get(i);
            System.arraycopy(v, 0, data, i * 6, 3);
            System.arraycopy(v, 0, data, i * 6 + 3, 3);
        }
        short[] indices = new short[faces.size() * 3];
        for (int i = 0; i < faces.size(); i++) {
            for (int k = 0; k < 3; k++) {
                indices[i * 3 + k] = (short) faces.get(i)[k];
            }
        }
        return mesh(data, indices);
    }

    static Mesh cylinder(int segments) {
        float[] data = new float[segments * 2 * 6];
        for (int i = 0; i < segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            float x = (float) Math.cos(angle), z = (float) Math.sin(angle);
            for (int end = 0; end < 2; end++) {
                int o = (i * 2 + end) * 6;
                data[o] = x;
                data[o + 1] = end;
                data[o + 2] = z;
                data[o + 3] = x;
                data[o + 4] = 0;
                data[o + 5] = z;
            }
        }
        short[] indices = new short[segments * 6];
        for (int i = 0; i < segments; i++) {
            int a = i * 2, b = a + 1, c = ((i + 1) % segments) * 2, d = c + 1;
            int o = i * 6;
            indices[o] = (short) a;
            indices[o + 1] = (short) b;
            indices[o + 2] = (short) c;
            indices[o + 3] = (short) c;
            indices[o + 4] = (short) b;
            indices[o + 5] = (short) d;
        }
        return mesh(data, indices);
    }

    private static Mesh mesh(float[] data, short[] indices) {
        Mesh mesh = new Mesh(true, data.length / 6, indices.length,
                new VertexAttributes(VertexAttribute.Position(), VertexAttribute.Normal()));
        mesh.setVertices(data);
        mesh.setIndices(indices);
        return mesh;
    }

    private static int midpoint(List<float[]> vertices, Map<Long, Integer> cache, int i, int j) {
        long key = ((long) Math.min(i, j) << 32) | Math.max(i, j);
        return cache.computeIfAbsent(key, k -> {
            float[] a = vertices.get(i), b = vertices.get(j);
            vertices.add(normalise(new float[]{(a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2}));
            return vertices.size() - 1;
        });
    }

    private static float[] normalise(float[] v) {
        float length = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new float[]{v[0] / length, v[1] / length, v[2] / length};
    }
}
