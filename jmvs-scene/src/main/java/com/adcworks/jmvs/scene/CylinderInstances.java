package com.adcworks.jmvs.scene;

/**
 * Cylinder instances: {@code data} holds {@code x1, y1, z1, x2, y2, z2, radius} per instance; {@code atoms[i]} is
 * the atom whose colour instance {@code i} takes. A bond is drawn as two half-cylinders, one per atom.
 */
public record CylinderInstances(float[] data, int[] atoms) {

    public static final int STRIDE = 7;
    public static final CylinderInstances EMPTY = new CylinderInstances(new float[0], new int[0]);

    public CylinderInstances {
        if (data.length != atoms.length * STRIDE) {
            throw new IllegalArgumentException("data must hold " + STRIDE + " floats per atom");
        }
    }

    public int count() {
        return atoms.length;
    }
}
