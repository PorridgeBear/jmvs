package com.adcworks.jmvs.scene.surface;

/**
 * A regular grid of {@code nx × ny × nz} points starting at an origin, {@code spacing} Å apart. Point
 * {@code (x, y, z)} has linear index {@code x + nx * (y + ny * z)}.
 */
record Grid(float originX, float originY, float originZ, float spacing, int nx, int ny, int nz) {

    Grid {
        if (nx < 2 || ny < 2 || nz < 2) {
            throw new IllegalArgumentException("Grid needs at least 2 points per axis");
        }
    }

    int size() {
        return nx * ny * nz;
    }

    int index(int x, int y, int z) {
        return x + nx * (y + ny * z);
    }

    float x(int i) {
        return originX + i * spacing;
    }

    float y(int j) {
        return originY + j * spacing;
    }

    float z(int k) {
        return originZ + k * spacing;
    }
}
