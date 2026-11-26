package com.adcworks.jmvs.scene.config;

import com.adcworks.jmvs.core.config.ConfigurationException;

import java.util.Properties;

/**
 * Cartoon display mode dimensions from {@code jmvs.properties} ({@code cartoon.*}), in Å. Widths and thicknesses
 * are full sizes, not half sizes.
 *
 * @param helixWidth        width of the helix ribbon
 * @param helixThickness    thickness of the helix ribbon
 * @param sheetWidth        width of a strand
 * @param sheetThickness    thickness of a strand
 * @param arrowWidth        width at the base of a strand's arrowhead
 * @param coilRadius        radius of the coil tube
 * @param nucleicRadius     radius of the nucleic-acid backbone tube
 * @param samplesPerResidue spline samples between consecutive residues
 */
public record CartoonSettings(float helixWidth, float helixThickness, float sheetWidth, float sheetThickness,
                              float arrowWidth, float coilRadius, float nucleicRadius, int samplesPerResidue) {

    public static final CartoonSettings DEFAULTS = new CartoonSettings(2.4f, 0.5f, 2.0f, 0.5f, 3.4f, 0.3f, 0.8f, 8);

    public CartoonSettings {
        positive("cartoon.helix.width", helixWidth);
        positive("cartoon.helix.thickness", helixThickness);
        positive("cartoon.sheet.width", sheetWidth);
        positive("cartoon.sheet.thickness", sheetThickness);
        positive("cartoon.arrow.width", arrowWidth);
        positive("cartoon.coil.radius", coilRadius);
        positive("cartoon.nucleic.radius", nucleicRadius);
        if (samplesPerResidue < 2 || samplesPerResidue > 64) {
            throw new ConfigurationException("cartoon.samples must be from 2 to 64: " + samplesPerResidue);
        }
    }

    /**
     * Settings from properties, falling back to {@link #DEFAULTS} for missing keys.
     */
    public static CartoonSettings parse(Properties p) {
        return new CartoonSettings(
                number(p, "cartoon.helix.width", DEFAULTS.helixWidth()),
                number(p, "cartoon.helix.thickness", DEFAULTS.helixThickness()),
                number(p, "cartoon.sheet.width", DEFAULTS.sheetWidth()),
                number(p, "cartoon.sheet.thickness", DEFAULTS.sheetThickness()),
                number(p, "cartoon.arrow.width", DEFAULTS.arrowWidth()),
                number(p, "cartoon.coil.radius", DEFAULTS.coilRadius()),
                number(p, "cartoon.nucleic.radius", DEFAULTS.nucleicRadius()),
                (int) number(p, "cartoon.samples", DEFAULTS.samplesPerResidue()));
    }

    private static void positive(String key, float value) {
        if (!(value > 0)) {
            throw new ConfigurationException(key + " must be positive: " + value);
        }
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
