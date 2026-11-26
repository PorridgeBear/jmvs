package com.adcworks.jmvs.scene.surface;

/**
 * Exact squared Euclidean distance transform with nearest-site labels (feature transform), by the separable
 * lower-envelope-of-parabolas method of Felzenszwalb and Huttenlocher, "Distance Transforms of Sampled Functions"
 * (Theory of Computing 8, 2012). Distances are in grid steps.
 */
final class DistanceTransform {

    static final float INFINITY = Float.POSITIVE_INFINITY;

    /**
     * Result: {@code distanceSquared[i]} to the nearest site, and that site's linear index in {@code nearest[i]}
     * ({@code -1} if there are no sites).
     */
    record Result(float[] distanceSquared, int[] nearest) {
    }

    private DistanceTransform() {
    }

    /**
     * Transform of the sites ({@code site[i]} true) on a {@code nx × ny × nz} grid with index
     * {@code x + nx * (y + ny * z)}.
     */
    static Result of(boolean[] site, int nx, int ny, int nz) {
        int size = nx * ny * nz;
        if (site.length != size) {
            throw new IllegalArgumentException("site has " + site.length + " entries, grid has " + size);
        }
        float[] d = new float[size];
        int[] label = new int[size];
        for (int i = 0; i < size; i++) {
            d[i] = site[i] ? 0 : INFINITY;
            label[i] = site[i] ? i : -1;
        }
        int longest = Math.max(nx, Math.max(ny, nz));
        Line line = new Line(longest);
        for (int z = 0; z < nz; z++) {
            for (int y = 0; y < ny; y++) {
                line.transform(d, label, nx * (y + ny * z), 1, nx);
            }
        }
        for (int z = 0; z < nz; z++) {
            for (int x = 0; x < nx; x++) {
                line.transform(d, label, x + nx * ny * z, nx, ny);
            }
        }
        for (int y = 0; y < ny; y++) {
            for (int x = 0; x < nx; x++) {
                line.transform(d, label, x + nx * y, nx * ny, nz);
            }
        }
        return new Result(d, label);
    }

    /**
     * Scratch buffers for the 1D transform along one grid line.
     */
    private static final class Line {
        private final float[] f;
        private final int[] l;
        private final int[] v;
        private final float[] z;

        Line(int n) {
            f = new float[n];
            l = new int[n];
            v = new int[n];
            z = new float[n + 1];
        }

        void transform(float[] d, int[] label, int start, int stride, int n) {
            for (int q = 0; q < n; q++) {
                f[q] = d[start + q * stride];
                l[q] = label[start + q * stride];
            }
            int k = -1;
            for (int q = 0; q < n; q++) {
                if (f[q] == INFINITY) {
                    continue;
                }
                float s = 0;
                while (k >= 0) {
                    int p = v[k];
                    s = ((f[q] + (float) q * q) - (f[p] + (float) p * p)) / (2f * (q - p));
                    if (s > z[k]) {
                        break;
                    }
                    k--;
                }
                k++;
                v[k] = q;
                z[k] = k == 0 ? Float.NEGATIVE_INFINITY : s;
                z[k + 1] = Float.POSITIVE_INFINITY;
            }
            if (k < 0) {
                for (int q = 0; q < n; q++) {
                    d[start + q * stride] = INFINITY;
                    label[start + q * stride] = -1;
                }
                return;
            }
            int j = 0;
            for (int q = 0; q < n; q++) {
                while (z[j + 1] < q) {
                    j++;
                }
                int p = v[j];
                float dq = q - p;
                d[start + q * stride] = dq * dq + f[p];
                label[start + q * stride] = l[p];
            }
        }
    }

}
