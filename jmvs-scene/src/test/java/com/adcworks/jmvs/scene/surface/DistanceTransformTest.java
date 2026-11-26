package com.adcworks.jmvs.scene.surface;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class DistanceTransformTest {

    @Test
    void matchesBruteForceOnRandomSites() {
        Random random = new Random(3);
        int nx = 9, ny = 6, nz = 7;
        for (int trial = 0; trial < 20; trial++) {
            boolean[] site = new boolean[nx * ny * nz];
            for (int i = 0; i < site.length; i++) {
                site[i] = random.nextInt(12) == 0;
            }
            site[random.nextInt(site.length)] = true;
            DistanceTransform.Result result = DistanceTransform.of(site, nx, ny, nz);
            for (int i = 0; i < site.length; i++) {
                float expected = Float.POSITIVE_INFINITY;
                for (int j = 0; j < site.length; j++) {
                    if (site[j]) {
                        expected = Math.min(expected, distanceSquared(i, j, nx, ny));
                    }
                }
                assertThat(result.distanceSquared()[i]).isEqualTo(expected);
                int nearest = result.nearest()[i];
                assertThat(site[nearest]).isTrue();
                assertThat(distanceSquared(i, nearest, nx, ny)).isEqualTo(expected);
            }
        }
    }

    @Test
    void withoutSitesEverythingIsInfinitelyFar() {
        DistanceTransform.Result result = DistanceTransform.of(new boolean[27], 3, 3, 3);
        assertThat(result.distanceSquared()).containsOnly(Float.POSITIVE_INFINITY);
        assertThat(result.nearest()).containsOnly(-1);
    }

    private static float distanceSquared(int a, int b, int nx, int ny) {
        int ax = a % nx, ay = (a / nx) % ny, az = a / (nx * ny);
        int bx = b % nx, by = (b / nx) % ny, bz = b / (nx * ny);
        return (ax - bx) * (ax - bx) + (ay - by) * (ay - by) + (az - bz) * (az - bz);
    }
}
