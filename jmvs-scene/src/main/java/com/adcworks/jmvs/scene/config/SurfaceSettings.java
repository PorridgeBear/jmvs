package com.adcworks.jmvs.scene.config;

import com.adcworks.jmvs.core.config.ConfigurationException;

import java.util.Properties;

/**
 * Molecular surface settings from {@code jmvs.properties}.
 *
 * @param probeRadius solvent probe radius in Å ({@code surface.probe})
 * @param spacing     target grid spacing in Å ({@code surface.spacing}); coarsened for large structures
 * @param maxVoxels   largest grid to allocate ({@code surface.maxVoxels}); bounds memory use
 */
public record SurfaceSettings(float probeRadius, float spacing, int maxVoxels) {

    public static final SurfaceSettings DEFAULTS = new SurfaceSettings(1.4f, 0.5f, 8_000_000);

    public SurfaceSettings {
        if (!(probeRadius >= 0)) {
            throw new ConfigurationException("surface.probe must be zero or positive: " + probeRadius);
        }
        if (!(spacing > 0)) {
            throw new ConfigurationException("surface.spacing must be positive: " + spacing);
        }
        if (maxVoxels < 1000) {
            throw new ConfigurationException("surface.maxVoxels must be at least 1000: " + maxVoxels);
        }
    }

    /**
     * Settings from properties, falling back to {@link #DEFAULTS} for missing keys.
     */
    public static SurfaceSettings parse(Properties p) {
        return new SurfaceSettings(
                number(p, "surface.probe", DEFAULTS.probeRadius()),
                number(p, "surface.spacing", DEFAULTS.spacing()),
                (int) number(p, "surface.maxVoxels", DEFAULTS.maxVoxels()));
    }

    private static float number(Properties p, String key, float fallback) {
        String value = p.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            throw new ConfigurationException(key + " is not a number: '" + value.trim() + "'", e);
        }
    }
}
