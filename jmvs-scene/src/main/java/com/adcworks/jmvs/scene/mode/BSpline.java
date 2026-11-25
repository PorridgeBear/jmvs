package com.adcworks.jmvs.scene.mode;

/**
 * Open uniform B-spline through a list of 3D control points, after Paul Bourke
 * (http://paulbourke.net/geometry/spline/), as used by the legacy ribbons. Only the basis functions that are
 * non-zero at each parameter value are evaluated.
 */
final class BSpline {

    private BSpline() {
    }

    /**
     * Samples the curve.
     *
     * @param control    control points, 3 floats each; at least {@code order} points
     * @param order      spline order (degree + 1)
     * @param resolution number of samples, at least 2; the last sample is the last control point
     * @return {@code resolution} points, 3 floats each
     */
    static float[] sample(float[] control, int order, int resolution) {
        int count = control.length / 3;
        if (count < order || resolution < 2) {
            throw new IllegalArgumentException("Need at least " + order + " control points and 2 samples");
        }
        int n = count - 1;
        int[] knots = knots(n, order);
        float[] out = new float[resolution * 3];
        double increment = (n - order + 2) / (double) (resolution - 1);
        double v = 0;
        for (int i = 0; i < resolution - 1; i++) {
            int span = Math.min(Math.max(order - 1, (int) Math.floor(v) + order - 1), n);
            for (int k = Math.max(0, span - order + 1); k <= span; k++) {
                double b = blend(k, order, knots, v);
                out[i * 3] += (float) (control[k * 3] * b);
                out[i * 3 + 1] += (float) (control[k * 3 + 1] * b);
                out[i * 3 + 2] += (float) (control[k * 3 + 2] * b);
            }
            v += increment;
        }
        System.arraycopy(control, n * 3, out, (resolution - 1) * 3, 3);
        return out;
    }

    static int[] knots(int n, int t) {
        int[] u = new int[n + t + 1];
        for (int j = 0; j < u.length; j++) {
            u[j] = j < t ? 0 : j <= n ? j - t + 1 : n - t + 2;
        }
        return u;
    }

    static double blend(int k, int t, int[] u, double v) {
        if (t == 1) {
            return u[k] <= v && v < u[k + 1] ? 1 : 0;
        }
        double value = 0;
        if (u[k + t - 1] != u[k]) {
            value += (v - u[k]) / (u[k + t - 1] - u[k]) * blend(k, t - 1, u, v);
        }
        if (u[k + t] != u[k + 1]) {
            value += (u[k + t] - v) / (u[k + t] - u[k + 1]) * blend(k + 1, t - 1, u, v);
        }
        return value;
    }
}
