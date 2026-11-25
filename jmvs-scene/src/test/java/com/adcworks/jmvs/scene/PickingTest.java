package com.adcworks.jmvs.scene;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PickingTest {

    private final SphereInstances spheres = new SphereInstances(
            new float[]{0, 0, 0, 1, 0, 0, -5, 1, 3, 3, 0, 0.5f},
            new int[]{10, 20, 30});

    @Test
    void picksNearestSphereAlongRay() {
        assertThat(Picking.pick(spheres, new float[]{0, 0, 10}, new float[]{0, 0, -1})).hasValue(10);
        assertThat(Picking.pick(spheres, new float[]{0, 0, -10}, new float[]{0, 0, 2})).hasValue(20);
    }

    @Test
    void missesWhenRayPassesBesideAllSpheres() {
        assertThat(Picking.pick(spheres, new float[]{2, 2, 10}, new float[]{0, 0, -1})).isEmpty();
        assertThat(Picking.pick(spheres, new float[]{3, 3, -10}, new float[]{0, 0, -1})).isEmpty();
    }
}
