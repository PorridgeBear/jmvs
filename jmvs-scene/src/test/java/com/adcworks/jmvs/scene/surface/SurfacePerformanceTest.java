package com.adcworks.jmvs.scene.surface;

import com.adcworks.jmvs.core.model.Structure;
import com.adcworks.jmvs.scene.Samples;
import com.adcworks.jmvs.scene.config.SurfaceSettings;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Roadmap slice 11: {@code 4hhb} shows a surface in under a few seconds. Measured about 0.25 s on an Apple M1-class
 * Mac (arc42 §10).
 */
@Tag("performance")
class SurfacePerformanceTest {

    @Test
    void haemoglobinExcludedSurfaceBuildsWithinThreeSeconds() {
        Structure haemoglobin = Samples.read("4hhb.cif");
        SurfaceBuilder builder = new SurfaceBuilder(SurfaceSettings.DEFAULTS);
        builder.build(haemoglobin, SurfaceKind.SES);
        long start = System.nanoTime();
        Surface surface = builder.build(haemoglobin, SurfaceKind.SES);
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertThat(surface.triangleCount()).isPositive();
        assertThat(millis).isLessThan(3000);
    }
}
