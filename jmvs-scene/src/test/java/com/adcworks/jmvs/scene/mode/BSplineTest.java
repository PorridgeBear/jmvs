package com.adcworks.jmvs.scene.mode;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BSplineTest {

    @Test
    void localEvaluationMatchesSumOverAllBasisFunctions() {
        float[] control = {0, 0, 0, 1, 2, 0, 3, 1, 1, 4, -1, 2, 6, 0, 0, 7, 3, 1};
        int order = 3, resolution = 50;
        float[] sampled = BSpline.sample(control, order, resolution);

        int n = control.length / 3 - 1;
        int[] knots = BSpline.knots(n, order);
        double increment = (n - order + 2) / (double) (resolution - 1);
        for (int i = 0; i < resolution - 1; i++) {
            double v = i * increment;
            for (int axis = 0; axis < 3; axis++) {
                double expected = 0;
                for (int k = 0; k <= n; k++) {
                    expected += control[k * 3 + axis] * BSpline.blend(k, order, knots, v);
                }
                assertThat((double) sampled[i * 3 + axis]).isCloseTo(expected, within(1e-4));
            }
        }
    }

    @Test
    void startsAndEndsAtTheEndControlPoints() {
        float[] control = {0, 0, 0, 1, 2, 0, 3, 1, 1, 4, -1, 2};
        float[] sampled = BSpline.sample(control, 3, 20);

        assertThat(sampled[0]).isZero();
        assertThat(sampled[19 * 3]).isEqualTo(4f);
        assertThat(sampled[19 * 3 + 2]).isEqualTo(2f);
    }
}
