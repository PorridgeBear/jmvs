package com.adcworks.jmvs.scene.config;

import com.adcworks.jmvs.core.config.ConfigurationException;
import com.adcworks.jmvs.core.config.Resources;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Viewer settings from {@code jmvs.properties}: enabled display modes and colour schemes (by id, in menu order),
 * the defaults, molecular surface settings, cartoon dimensions and density map settings.
 */
public record ViewerConfig(List<String> modes, String defaultMode, List<String> schemes, String defaultScheme,
                           SurfaceSettings surface, CartoonSettings cartoon,
                           DensitySettings density) {

    public ViewerConfig {
        modes = List.copyOf(modes);
        schemes = List.copyOf(schemes);
        if (!modes.contains(defaultMode)) {
            throw new ConfigurationException("display.default '" + defaultMode + "' is not in display.modes");
        }
        if (!schemes.contains(defaultScheme)) {
            throw new ConfigurationException("colour.default '" + defaultScheme + "' is not in colour.schemes");
        }
    }

    /**
     * Loads the bundled {@code jmvs.properties}.
     */
    public static Properties loadProperties() {
        return Resources.loadProperties(ViewerConfig.class, "jmvs.properties");
    }

    public static ViewerConfig parse(Properties p) {
        return new ViewerConfig(list(p, "display.modes"), required(p, "display.default"),
                list(p, "colour.schemes"), required(p, "colour.default"), SurfaceSettings.parse(p),
                CartoonSettings.parse(p), DensitySettings.parse(p));
    }

    private static List<String> list(Properties p, String key) {
        List<String> values = Arrays.stream(required(p, key).split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).toList();
        if (values.isEmpty()) {
            throw new ConfigurationException(key + " must not be empty");
        }
        return values;
    }

    private static String required(Properties p, String key) {
        String value = p.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new ConfigurationException("Missing " + key);
        }
        return value.trim();
    }
}
