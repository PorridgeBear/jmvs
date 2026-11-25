package com.adcworks.jmvs.scene.colour;

import com.adcworks.jmvs.core.config.ConfigurationException;

/**
 * Packed {@code 0xRRGGBB} colour helpers.
 */
public final class Rgb {

    private Rgb() {
    }

    public static int of(int r, int g, int b) {
        return (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    /**
     * Parses {@code "r,g,b"} with components 0–255.
     */
    public static int parse(String key, String value) {
        String[] parts = value.split(",");
        if (parts.length != 3) {
            throw new ConfigurationException("Expected r,g,b for " + key + " but was '" + value + "'");
        }
        try {
            int[] c = new int[3];
            for (int i = 0; i < 3; i++) {
                c[i] = Integer.parseInt(parts[i].trim());
                if (c[i] < 0 || c[i] > 255) {
                    throw new ConfigurationException("Colour component out of range in " + key + ": " + value);
                }
            }
            return of(c[0], c[1], c[2]);
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Malformed colour for " + key + ": " + value, e);
        }
    }

    public static float red(int rgb) {
        return ((rgb >> 16) & 0xFF) / 255f;
    }

    public static float green(int rgb) {
        return ((rgb >> 8) & 0xFF) / 255f;
    }

    public static float blue(int rgb) {
        return (rgb & 0xFF) / 255f;
    }

    private static int clamp(int c) {
        return Math.max(0, Math.min(255, c));
    }
}
