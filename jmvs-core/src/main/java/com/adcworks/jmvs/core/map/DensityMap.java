package com.adcworks.jmvs.core.map;

import com.adcworks.jmvs.core.model.BoundingSphere;

/**
 * A density map sampled on a regular grid of {@code nx × ny × nz} points, such as a cryo-EM map. Point
 * {@code (i, j, k)} has linear index {@code i + nx * (j + ny * k)} and lies at {@code origin + i·a + j·b + k·c},
 * where {@code a, b, c} are the grid steps along the cell axes in Å ({@code axes}, 3 floats each). The axes are
 * orthogonal for nearly all EM maps; crystallographic maps may have oblique cells.
 *
 * @param name       file name or other description
 * @param values     {@code nx · ny · nz} density values
 * @param origin     Cartesian position of point {@code (0, 0, 0)} in Å, 3 floats
 * @param axes       grid steps along i, j and k in Å: {@code a}, then {@code b}, then {@code c}; 9 floats
 * @param statistics statistics of {@code values}
 */
public record DensityMap(String name, int nx, int ny, int nz, float[] values, float[] origin, float[] axes,
                         Statistics statistics) {

    /**
     * Value statistics. {@code rms} is the standard deviation from the mean, as in the MRC header, so a contour
     * level is {@code (level − mean) / rms} σ above the mean.
     */
    public record Statistics(float min, float max, float mean, float rms) {

        public static Statistics of(float[] values) {
            float min = Float.POSITIVE_INFINITY;
            float max = Float.NEGATIVE_INFINITY;
            double sum = 0;
            for (float v : values) {
                min = Math.min(min, v);
                max = Math.max(max, v);
                sum += v;
            }
            double mean = sum / values.length;
            double squares = 0;
            for (float v : values) {
                squares += (v - mean) * (v - mean);
            }
            return new Statistics(min, max, (float) mean, (float) Math.sqrt(squares / values.length));
        }

        /** The level {@code sigma} standard deviations above the mean. */
        public float level(float sigma) {
            return mean + sigma * rms;
        }

        /** How many standard deviations {@code level} is above the mean; 0 for a flat map. */
        public float sigma(float level) {
            return rms > 0 ? (level - mean) / rms : 0;
        }
    }

    public DensityMap {
        if (nx < 2 || ny < 2 || nz < 2) {
            throw new IllegalArgumentException("A map needs at least 2 points per axis: " + nx + "×" + ny + "×" + nz);
        }
        if (values.length != (long) nx * ny * nz) {
            throw new IllegalArgumentException(values.length + " values for a " + nx + "×" + ny + "×" + nz + " grid");
        }
        if (origin.length != 3 || axes.length != 9) {
            throw new IllegalArgumentException("origin needs 3 floats and axes 9");
        }
    }

    /**
     * A map with orthogonal axes and the given voxel size, statistics computed from the values.
     */
    public static DensityMap orthogonal(String name, int nx, int ny, int nz, float[] values, float originX,
                                        float originY, float originZ, float voxelX, float voxelY, float voxelZ) {
        return new DensityMap(name, nx, ny, nz, values, new float[]{originX, originY, originZ},
                new float[]{voxelX, 0, 0, 0, voxelY, 0, 0, 0, voxelZ}, Statistics.of(values));
    }

    public int size() {
        return nx * ny * nz;
    }

    public int index(int i, int j, int k) {
        return i + nx * (j + ny * k);
    }

    public float value(int i, int j, int k) {
        return values[index(i, j, k)];
    }

    /**
     * Writes the Cartesian position of grid coordinates {@code (u, v, w)}, which may be fractional, to
     * {@code out[0..2]}.
     */
    public void position(float u, float v, float w, float[] out) {
        for (int d = 0; d < 3; d++) {
            out[d] = origin[d] + u * axes[d] + v * axes[3 + d] + w * axes[6 + d];
        }
    }

    /** Sphere around the map's box, to frame the camera on a map shown without a structure. */
    public BoundingSphere boundingSphere() {
        float[][] corners = new float[8][3];
        float cx = 0, cy = 0, cz = 0;
        for (int c = 0; c < 8; c++) {
            position((c & 1) * (nx - 1), ((c >> 1) & 1) * (ny - 1), ((c >> 2) & 1) * (nz - 1), corners[c]);
            cx += corners[c][0] / 8;
            cy += corners[c][1] / 8;
            cz += corners[c][2] / 8;
        }
        float max = 0;
        for (float[] p : corners) {
            float dx = p[0] - cx, dy = p[1] - cy, dz = p[2] - cz;
            max = Math.max(max, dx * dx + dy * dy + dz * dz);
        }
        return new BoundingSphere(cx, cy, cz, (float) Math.sqrt(max));
    }

    /** Volume of one voxel in Å³. */
    public double voxelVolume() {
        float[] a = axes;
        return Math.abs(a[0] * (a[4] * a[8] - a[5] * a[7]) - a[1] * (a[3] * a[8] - a[5] * a[6])
                + a[2] * (a[3] * a[7] - a[4] * a[6]));
    }

    @Override
    public String toString() {
        return "%s (%d×%d×%d)".formatted(name, nx, ny, nz);
    }
}
