package com.adcworks.jmvs.scene.mesh;

/**
 * A regular grid of {@code nx × ny × nz} points starting at an origin, {@code spacing} Å apart. Point
 * {@code (x, y, z)} has linear index {@code x + nx * (y + ny * z)}.
 */
public record Grid(float originX, float originY, float originZ, float spacing, int nx, int ny, int nz) {

    public Grid {
        if (nx < 2 || ny < 2 || nz < 2) {
            throw new IllegalArgumentException("Grid needs at least 2 points per axis");
        }
    }

    /** A grid in index units: origin 0, spacing 1. */
    public static Grid ofIndices(int nx, int ny, int nz) {
        return new Grid(0, 0, 0, 1, nx, ny, nz);
    }

    public int size() {
        return nx * ny * nz;
    }

    public int index(int x, int y, int z) {
        return x + nx * (y + ny * z);
    }

    public float x(int i) {
        return originX + i * spacing;
    }

    public float y(int j) {
        return originY + j * spacing;
    }

    public float z(int k) {
        return originZ + k * spacing;
    }
}
