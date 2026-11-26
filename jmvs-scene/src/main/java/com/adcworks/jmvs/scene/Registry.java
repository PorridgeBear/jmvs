package com.adcworks.jmvs.scene;

import com.adcworks.jmvs.core.config.ConfigurationException;
import com.adcworks.jmvs.scene.colour.ColourScheme;
import com.adcworks.jmvs.scene.colour.ColourSchemes;
import com.adcworks.jmvs.scene.colour.ColourTables;
import com.adcworks.jmvs.scene.config.SurfaceSettings;
import com.adcworks.jmvs.scene.config.ViewerConfig;
import com.adcworks.jmvs.scene.mode.BallsAndSticksMode;
import com.adcworks.jmvs.scene.mode.BallsMode;
import com.adcworks.jmvs.scene.mode.DisplayMode;
import com.adcworks.jmvs.scene.mode.RibbonsMode;
import com.adcworks.jmvs.scene.mode.SticksMode;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The display modes and colour schemes enabled by configuration, in menu order, and the surface settings. To add a
 * mode or scheme, implement the interface and register its id here.
 */
public record Registry(List<DisplayMode> modes, DisplayMode defaultMode, List<ColourScheme> schemes,
                       ColourScheme defaultScheme, SurfaceSettings surface) {

    private static final Map<String, Supplier<DisplayMode>> MODES = Map.of(
            "balls", BallsMode::new,
            "sticks", SticksMode::new,
            "ballsandsticks", BallsAndSticksMode::new,
            "ribbons", RibbonsMode::new);

    private static final Map<String, Function<ColourTables, ColourScheme>> SCHEMES = Map.of(
            "cpk", ColourSchemes::cpk,
            "chain", ColourSchemes::chain,
            "group", ColourSchemes::group,
            "structure", ColourSchemes::structure,
            "bfactor", ColourSchemes::bFactor,
            "confidence", ColourSchemes::confidence);

    public Registry {
        modes = List.copyOf(modes);
        schemes = List.copyOf(schemes);
    }

    /**
     * Registry from the bundled {@code jmvs.properties}.
     */
    public static Registry load() {
        Properties p = ViewerConfig.loadProperties();
        return create(ViewerConfig.parse(p), ColourTables.parse(p));
    }

    public static Registry create(ViewerConfig config, ColourTables tables) {
        List<DisplayMode> modes = config.modes().stream().map(id -> {
            Supplier<DisplayMode> factory = MODES.get(id);
            if (factory == null) {
                throw new ConfigurationException("Unknown display mode '" + id + "'; known: " + MODES.keySet());
            }
            return factory.get();
        }).toList();
        List<ColourScheme> schemes = config.schemes().stream().map(id -> {
            Function<ColourTables, ColourScheme> factory = SCHEMES.get(id);
            if (factory == null) {
                throw new ConfigurationException("Unknown colour scheme '" + id + "'; known: " + SCHEMES.keySet());
            }
            return factory.apply(tables);
        }).toList();
        return new Registry(modes, find(modes, config.defaultMode(), DisplayMode::id),
                schemes, find(schemes, config.defaultScheme(), ColourScheme::id), config.surface());
    }

    public DisplayMode mode(String id) {
        return find(modes, id, DisplayMode::id);
    }

    public ColourScheme scheme(String id) {
        return find(schemes, id, ColourScheme::id);
    }

    /**
     * The scheme to use with a mode: {@code preferred} if the mode supports it, otherwise the first supported one.
     */
    public ColourScheme schemeFor(DisplayMode mode, ColourScheme preferred) {
        if (mode.supports(preferred)) {
            return preferred;
        }
        return schemes.stream().filter(mode::supports).findFirst()
                .orElseThrow(() -> new ConfigurationException("No colour scheme supports mode " + mode.id()));
    }

    private static <T> T find(List<T> items, String id, Function<T, String> idOf) {
        return items.stream().filter(i -> idOf.apply(i).equals(id)).findFirst()
                .orElseThrow(() -> new ConfigurationException("Unknown id '" + id + "'"));
    }
}
