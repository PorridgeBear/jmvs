package com.adcworks.jmvs.scene.config;

import com.adcworks.jmvs.core.config.ConfigurationException;
import com.adcworks.jmvs.scene.colour.Rgb;
import com.adcworks.jmvs.scene.density.DensityStyle;

import java.util.Properties;

/**
 * Density map settings from {@code jmvs.properties}.
 *
 * @param maxVoxels    largest map held in memory ({@code density.maxVoxels}); larger maps are binned
 * @param defaultSigma initial contour level in standard deviations above the mean ({@code density.sigma})
 * @param zoneRadius   a contour vertex takes the colour of the nearest model atom within this many Å
 *                     ({@code density.zoneRadius}); 0 turns zone colouring off
 * @param colour       packed RGB of the contour away from the model ({@code density.colour})
 * @param style        initial drawing style ({@code density.style}: {@code surface} or {@code mesh})
 */
public record DensitySettings(int maxVoxels, float defaultSigma, float zoneRadius, int colour, DensityStyle style) {

    public static final DensitySettings DEFAULTS =
            new DensitySettings(32_000_000, 2.5f, 3f, Rgb.of(150, 160, 175), DensityStyle.MESH);

    public DensitySettings {
        if (maxVoxels < 1000) {
            throw new ConfigurationException("density.maxVoxels must be at least 1000: " + maxVoxels);
        }
        if (!Float.isFinite(defaultSigma)) {
            throw new ConfigurationException("density.sigma must be a number: " + defaultSigma);
        }
        if (!(zoneRadius >= 0)) {
            throw new ConfigurationException("density.zoneRadius must be zero or positive: " + zoneRadius);
        }
    }

    /**
     * Settings from properties, falling back to {@link #DEFAULTS} for missing keys.
     */
    public static DensitySettings parse(Properties p) {
        String colour = p.getProperty("density.colour");
        String style = p.getProperty("density.style");
        return new DensitySettings(
                (int) number(p, "density.maxVoxels", DEFAULTS.maxVoxels()),
                number(p, "density.sigma", DEFAULTS.defaultSigma()),
                number(p, "density.zoneRadius", DEFAULTS.zoneRadius()),
                colour == null || colour.isBlank() ? DEFAULTS.colour() : Rgb.parse("density.colour", colour.trim()),
                style == null || style.isBlank() ? DEFAULTS.style() : DensityStyle.byId(style.trim()).orElseThrow(
                        () -> new ConfigurationException("density.style must be surface or mesh: '" + style.trim()
                                + "'")));
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
