package com.adcworks.jmvs.scene;

import java.util.OptionalInt;

/**
 * CPU ray picking against sphere instances. A linear scan is fast enough for interactive clicks on structures of
 * tens of thousands of atoms.
 */
public final class Picking {

    private Picking() {
    }

    /**
     * Atom index of the nearest sphere hit by the ray, if any.
     *
     * @param origin    ray origin {x, y, z}
     * @param direction ray direction {x, y, z}, need not be normalised
     */
    public static OptionalInt pick(SphereInstances targets, float[] origin, float[] direction) {
        double dx = direction[0], dy = direction[1], dz = direction[2];
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length == 0) {
            return OptionalInt.empty();
        }
        dx /= length;
        dy /= length;
        dz /= length;

        float[] d = targets.data();
        double nearest = Double.POSITIVE_INFINITY;
        int hit = -1;
        for (int i = 0; i < targets.count(); i++) {
            int o = i * SphereInstances.STRIDE;
            double ox = d[o] - origin[0], oy = d[o + 1] - origin[1], oz = d[o + 2] - origin[2];
            double r = d[o + 3];
            double along = ox * dx + oy * dy + oz * dz;
            double perpendicularSq = ox * ox + oy * oy + oz * oz - along * along;
            if (perpendicularSq > r * r) {
                continue;
            }
            double distance = along - Math.sqrt(r * r - perpendicularSq);
            if (distance < 0) {
                distance = along + Math.sqrt(r * r - perpendicularSq);
            }
            if (distance >= 0 && distance < nearest) {
                nearest = distance;
                hit = targets.atoms()[i];
            }
        }
        return hit < 0 ? OptionalInt.empty() : OptionalInt.of(hit);
    }
}
