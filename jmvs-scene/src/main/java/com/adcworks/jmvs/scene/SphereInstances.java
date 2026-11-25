package com.adcworks.jmvs.scene;

/**
 * Sphere instances: {@code data} holds {@code x, y, z, radius} per instance; {@code atoms[i]} is the atom drawn by
 * instance {@code i} (used for colouring and picking).
 */
public record SphereInstances(float[] data, int[] atoms) {

    public static final int STRIDE = 4;
    public static final SphereInstances EMPTY = new SphereInstances(new float[0], new int[0]);

    public SphereInstances {
        if (data.length != atoms.length * STRIDE) {
            throw new IllegalArgumentException("data must hold " + STRIDE + " floats per atom");
        }
    }

    public int count() {
        return atoms.length;
    }

    public float radius(int instance) {
        return data[instance * STRIDE + 3];
    }
}
