package com.adcworks.jmvs.scene.density;

import com.adcworks.jmvs.core.map.DensityMap;

import java.util.Locale;

/**
 * A requested contour level: an absolute value in the map's units, or a number of standard deviations above the
 * mean, which only resolves once the map is read.
 */
public sealed interface ContourLevel {

    float resolve(DensityMap.Statistics statistics);

    record Absolute(float level) implements ContourLevel {
        @Override
        public float resolve(DensityMap.Statistics statistics) {
            return level;
        }
    }

    record Sigma(float sigma) implements ContourLevel {
        @Override
        public float resolve(DensityMap.Statistics statistics) {
            return statistics.level(sigma);
        }
    }

    /**
     * Parses {@code "0.09"} (absolute) or {@code "2.5sigma"} / {@code "2.5σ"}.
     *
     * @throws IllegalArgumentException if the text is not a number with an optional sigma suffix
     */
    static ContourLevel parse(String text) {
        String trimmed = text.trim().toLowerCase(Locale.ROOT);
        boolean sigma = trimmed.endsWith("sigma") || trimmed.endsWith("σ");
        String number = sigma ? trimmed.substring(0, trimmed.length() - (trimmed.endsWith("σ") ? 1 : 5)) : trimmed;
        float value;
        try {
            value = Float.parseFloat(number.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Contour level must be a number or <n>sigma, not '" + text + "'", e);
        }
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("Contour level must be finite, not '" + text + "'");
        }
        return sigma ? new Sigma(value) : new Absolute(value);
    }
}
