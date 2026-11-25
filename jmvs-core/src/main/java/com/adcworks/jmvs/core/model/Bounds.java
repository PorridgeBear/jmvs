package com.adcworks.jmvs.core.model;

import java.util.List;

/**
 * Axis-aligned bounds of a set of points, in ångströms.
 */
public record Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {

    public static final Bounds EMPTY = new Bounds(0, 0, 0, 0, 0, 0);

    public static Bounds of(List<Atom> atoms) {
        if (atoms.isEmpty()) {
            return EMPTY;
        }
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        for (Atom a : atoms) {
            minX = Math.min(minX, a.x());
            minY = Math.min(minY, a.y());
            minZ = Math.min(minZ, a.z());
            maxX = Math.max(maxX, a.x());
            maxY = Math.max(maxY, a.y());
            maxZ = Math.max(maxZ, a.z());
        }
        return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public float centerX() {
        return (minX + maxX) / 2f;
    }

    public float centerY() {
        return (minY + maxY) / 2f;
    }

    public float centerZ() {
        return (minZ + maxZ) / 2f;
    }
}
