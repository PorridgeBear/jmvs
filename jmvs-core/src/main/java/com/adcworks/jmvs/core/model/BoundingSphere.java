package com.adcworks.jmvs.core.model;

/**
 * A sphere enclosing a structure, used to frame the camera.
 */
public record BoundingSphere(float x, float y, float z, float radius) {

    /**
     * Sphere centred on the bounds centre that encloses every atom.
     */
    public static BoundingSphere of(Bounds bounds, Iterable<Atom> atoms) {
        float cx = bounds.centerX(), cy = bounds.centerY(), cz = bounds.centerZ();
        float max = 0;
        for (Atom a : atoms) {
            float dx = a.x() - cx, dy = a.y() - cy, dz = a.z() - cz;
            max = Math.max(max, dx * dx + dy * dy + dz * dz);
        }
        return new BoundingSphere(cx, cy, cz, (float) Math.sqrt(max));
    }
}
