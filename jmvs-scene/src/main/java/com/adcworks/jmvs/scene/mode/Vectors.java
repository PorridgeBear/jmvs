package com.adcworks.jmvs.scene.mode;

import com.adcworks.jmvs.core.model.Atom;

/**
 * Small 3-vector helpers on {@code double[3]}, shared by the ribbon and cartoon modes.
 */
final class Vectors {

    private Vectors() {
    }

    static double[] position(Atom a) {
        return new double[]{a.x(), a.y(), a.z()};
    }

    static double distance(Atom a, Atom b) {
        double dx = a.x() - b.x(), dy = a.y() - b.y(), dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    static double[] sub(Atom a, Atom b) {
        return new double[]{a.x() - b.x(), a.y() - b.y(), a.z() - b.z()};
    }

    static double[] sub(double[] a, double[] b) {
        return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] add(double[] a, double[] b) {
        return new double[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] scale(double[] a, double s) {
        return new double[]{a[0] * s, a[1] * s, a[2] * s};
    }

    static double[] cross(double[] a, double[] b) {
        return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    /** Unit vector along {@code v}, or +z for a zero vector. */
    static double[] normalise(double[] v) {
        double length = Math.sqrt(dot(v, v));
        return length == 0 ? new double[]{0, 0, 1} : new double[]{v[0] / length, v[1] / length, v[2] / length};
    }

    /** Any unit vector perpendicular to the unit vector {@code v}. */
    static double[] perpendicular(double[] v) {
        double[] axis = Math.abs(v[0]) < 0.9 ? new double[]{1, 0, 0} : new double[]{0, 1, 0};
        return normalise(cross(v, axis));
    }
}
