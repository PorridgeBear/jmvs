package com.adcworks.jmvs.scene.surface;

import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * Extracts the zero level set of a scalar field (negative inside) as a welded triangle mesh with naive surface nets:
 * one vertex per grid cube the surface crosses, at the mean of the cube's edge crossings, and one quad per crossed
 * grid edge joining the four cubes around it. Compared with marching cubes it needs no case tables and gives fewer,
 * better-shaped triangles. Triangles wind counter-clockwise seen from outside; normals are the normalised field
 * gradient, interpolated from central differences at the grid points.
 */
final class SurfaceNets {

    /**
     * Welded mesh: 3 floats per vertex in {@code positions} and {@code normals}, 3 vertex indices per triangle.
     */
    record Mesh(float[] positions, float[] normals, int[] triangles) {

        int vertexCount() {
            return positions.length / 3;
        }
    }

    /**
     * Cube corner pairs joined by the 12 cube edges; corner bit 0 is +x, bit 1 +y, bit 2 +z.
     */
    private static final int[][] EDGES = {
            {0, 1}, {2, 3}, {4, 5}, {6, 7},
            {0, 2}, {1, 3}, {4, 6}, {5, 7},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}};

    private SurfaceNets() {
    }

    static Mesh extract(Grid grid, float[] field, BooleanSupplier cancelled) {
        if (field.length != grid.size()) {
            throw new IllegalArgumentException("field has " + field.length + " values, grid has " + grid.size());
        }
        int nx = grid.nx(), ny = grid.ny(), nz = grid.nz();
        int cubesX = nx - 1, cubesY = ny - 1;
        int[][] slabs = {new int[cubesX * cubesY], new int[cubesX * cubesY]};
        FloatList positions = new FloatList();
        FloatList normals = new FloatList();
        IntList triangles = new IntList();
        float[] corner = new float[8];
        float[] gradient = new float[3];
        float s = grid.spacing();

        for (int z = 0; z < nz - 1; z++) {
            if (cancelled.getAsBoolean()) {
                throw new CancellationException("Surface extraction cancelled");
            }
            int[] current = slabs[z & 1];
            int[] previous = slabs[(z + 1) & 1];
            Arrays.fill(current, -1);
            for (int y = 0; y < ny - 1; y++) {
                for (int x = 0; x < nx - 1; x++) {
                    int mask = 0;
                    for (int c = 0; c < 8; c++) {
                        corner[c] = field[grid.index(x + (c & 1), y + ((c >> 1) & 1), z + ((c >> 2) & 1))];
                        if (corner[c] < 0) {
                            mask |= 1 << c;
                        }
                    }
                    if (mask == 0 || mask == 0xFF) {
                        continue;
                    }
                    float u = 0, v = 0, w = 0;
                    int crossings = 0;
                    for (int[] edge : EDGES) {
                        int a = edge[0], b = edge[1];
                        if (((mask >> a) & 1) != ((mask >> b) & 1)) {
                            float t = corner[a] / (corner[a] - corner[b]);
                            u += (a & 1) + t * ((b & 1) - (a & 1));
                            v += ((a >> 1) & 1) + t * (((b >> 1) & 1) - ((a >> 1) & 1));
                            w += ((a >> 2) & 1) + t * (((b >> 2) & 1) - ((a >> 2) & 1));
                            crossings++;
                        }
                    }
                    u /= crossings;
                    v /= crossings;
                    w /= crossings;
                    current[x + cubesX * y] = positions.size() / 3;
                    positions.add(grid.x(x) + u * s, grid.y(y) + v * s, grid.z(z) + w * s);
                    gradient(grid, field, x, y, z, u, v, w, gradient);
                    normals.add(gradient[0], gradient[1], gradient[2]);
                }
            }
            for (int y = 0; y < ny - 1; y++) {
                for (int x = 0; x < nx - 1; x++) {
                    float origin = field[grid.index(x, y, z)];
                    boolean inside = origin < 0;
                    // Edge along x: cubes around it are offset in y and z.
                    if (y > 0 && z > 0 && inside != (field[grid.index(x + 1, y, z)] < 0)) {
                        quad(triangles, positions, inside,
                                current[x + cubesX * y], current[x + cubesX * (y - 1)],
                                previous[x + cubesX * (y - 1)], previous[x + cubesX * y]);
                    }
                    // Edge along y: offset in z then x.
                    if (z > 0 && x > 0 && inside != (field[grid.index(x, y + 1, z)] < 0)) {
                        quad(triangles, positions, inside,
                                current[x + cubesX * y], previous[x + cubesX * y],
                                previous[x - 1 + cubesX * y], current[x - 1 + cubesX * y]);
                    }
                    // Edge along z: offset in x then y.
                    if (x > 0 && y > 0 && inside != (field[grid.index(x, y, z + 1)] < 0)) {
                        quad(triangles, positions, inside,
                                current[x + cubesX * y], current[x - 1 + cubesX * y],
                                current[x - 1 + cubesX * (y - 1)], current[x + cubesX * (y - 1)]);
                    }
                }
            }
        }
        return new Mesh(positions.toArray(), normals.toArray(), triangles.toArray());
    }

    /**
     * Two triangles for the quad {@code c0..c3}, which winds counter-clockwise around the edge direction; reversed
     * when the edge starts outside. Splits along the shorter diagonal.
     */
    private static void quad(IntList triangles, FloatList positions, boolean inside, int c0, int c1, int c2, int c3) {
        if (!inside) {
            int t = c1;
            c1 = c3;
            c3 = t;
        }
        if (distanceSquared(positions, c0, c2) <= distanceSquared(positions, c1, c3)) {
            triangles.add(c0, c1, c2);
            triangles.add(c0, c2, c3);
        } else {
            triangles.add(c0, c1, c3);
            triangles.add(c1, c2, c3);
        }
    }

    private static float distanceSquared(FloatList positions, int a, int b) {
        float dx = positions.get(a * 3) - positions.get(b * 3);
        float dy = positions.get(a * 3 + 1) - positions.get(b * 3 + 1);
        float dz = positions.get(a * 3 + 2) - positions.get(b * 3 + 2);
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Unit field gradient at local position {@code (u, v, w)} in cube {@code (x, y, z)}: central differences at the
     * cube's corners, blended trilinearly.
     */
    private static void gradient(Grid grid, float[] field, int x, int y, int z, float u, float v, float w,
                                 float[] out) {
        float gx = 0, gy = 0, gz = 0;
        for (int c = 0; c < 8; c++) {
            int cx = x + (c & 1), cy = y + ((c >> 1) & 1), cz = z + ((c >> 2) & 1);
            float weight = ((c & 1) == 1 ? u : 1 - u) * (((c >> 1) & 1) == 1 ? v : 1 - v)
                    * (((c >> 2) & 1) == 1 ? w : 1 - w);
            gx += weight * (sample(grid, field, cx + 1, cy, cz) - sample(grid, field, cx - 1, cy, cz));
            gy += weight * (sample(grid, field, cx, cy + 1, cz) - sample(grid, field, cx, cy - 1, cz));
            gz += weight * (sample(grid, field, cx, cy, cz + 1) - sample(grid, field, cx, cy, cz - 1));
        }
        float length = (float) Math.sqrt(gx * gx + gy * gy + gz * gz);
        if (length > 0) {
            out[0] = gx / length;
            out[1] = gy / length;
            out[2] = gz / length;
        } else {
            out[0] = 0;
            out[1] = 0;
            out[2] = 1;
        }
    }

    private static float sample(Grid grid, float[] field, int x, int y, int z) {
        x = Math.clamp(x, 0, grid.nx() - 1);
        y = Math.clamp(y, 0, grid.ny() - 1);
        z = Math.clamp(z, 0, grid.nz() - 1);
        return field[grid.index(x, y, z)];
    }

    static final class FloatList {
        private float[] data = new float[1024];
        private int size;

        void add(float a, float b, float c) {
            if (size + 3 > data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            data[size++] = a;
            data[size++] = b;
            data[size++] = c;
        }

        float get(int i) {
            return data[i];
        }

        int size() {
            return size;
        }

        float[] toArray() {
            return Arrays.copyOf(data, size);
        }
    }

    static final class IntList {
        private int[] data = new int[1024];
        private int size;

        void add(int a, int b, int c) {
            if (size + 3 > data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            data[size++] = a;
            data[size++] = b;
            data[size++] = c;
        }

        int size() {
            return size;
        }

        int[] toArray() {
            return Arrays.copyOf(data, size);
        }
    }
}
