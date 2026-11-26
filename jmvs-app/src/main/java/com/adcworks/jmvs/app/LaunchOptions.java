package com.adcworks.jmvs.app;

import com.adcworks.jmvs.core.model.SecondaryStructureSource;
import com.adcworks.jmvs.scene.density.ContourLevel;
import com.adcworks.jmvs.scene.density.DensityStyle;
import com.adcworks.jmvs.scene.surface.SurfaceKind;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Command-line options: {@code jmvs [file] [map] [--mode <id>] [--scheme <id>] [--ss auto|file|computed]
 * [--surface ses|sas] [--surface-opacity <0-1>] [--map <file>] [--contour <level>|<n>sigma]
 * [--map-style surface|mesh] [--map-opacity <0-1>] [--screenshot <png>] [--benchmark <seconds>]}. A positional
 * file is a density map if its name ends in {@code .map}, {@code .mrc} or {@code .ccp4} (optionally {@code .gz}),
 * otherwise a structure. {@code --screenshot} renders once (after the surface and map contour, if any, are computed),
 * writes a PNG and exits (visual checks); {@code --benchmark} rotates the view with vsync off, logs the average frame
 * rate and exits. {@code --ss} chooses where secondary structure comes from (default {@code auto}: the file's
 * annotation, else computed).
 */
public record LaunchOptions(Optional<Path> file, Optional<String> mode, Optional<String> scheme,
                            Optional<SecondaryStructureSource> secondaryStructure, Optional<SurfaceKind> surface,
                            Optional<Float> surfaceOpacity, Optional<Path> map, Optional<ContourLevel> contour,
                            Optional<DensityStyle> mapStyle, Optional<Float> mapOpacity,
                            Optional<Path> screenshot, Optional<Integer> benchmarkSeconds) {

    public static final String USAGE = "Usage: jmvs [file] [map] [--mode <id>] [--scheme <id>]"
            + " [--ss auto|file|computed] [--surface ses|sas] [--surface-opacity <0-1>] [--map <file>]"
            + " [--contour <level>|<n>sigma] [--map-style surface|mesh] [--map-opacity <0-1>]"
            + " [--screenshot <png>] [--benchmark <seconds>]";

    public static LaunchOptions parse(String[] args) {
        Path file = null;
        String mode = null;
        String scheme = null;
        Path screenshot = null;
        Integer benchmark = null;
        SecondaryStructureSource secondaryStructure = null;
        SurfaceKind surface = null;
        Float opacity = null;
        Path map = null;
        ContourLevel contour = null;
        DensityStyle mapStyle = null;
        Float mapOpacity = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--mode" -> mode = value(args, ++i, "--mode");
                case "--scheme" -> scheme = value(args, ++i, "--scheme");
                case "--ss" -> {
                    String id = value(args, ++i, "--ss");
                    secondaryStructure = SecondaryStructureSource.byId(id).orElseThrow(
                            () -> new IllegalArgumentException("--ss must be auto, file or computed, not " + id));
                }
                case "--surface" -> {
                    String id = value(args, ++i, "--surface");
                    surface = SurfaceKind.byId(id).orElseThrow(
                            () -> new IllegalArgumentException("--surface must be ses or sas, not " + id));
                }
                case "--surface-opacity" -> opacity = fraction(value(args, ++i, "--surface-opacity"),
                        "--surface-opacity");
                case "--map" -> map = Path.of(value(args, ++i, "--map"));
                case "--contour" -> {
                    String text = value(args, ++i, "--contour");
                    try {
                        contour = ContourLevel.parse(text);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("--contour needs a level or <n>sigma, not " + text, e);
                    }
                }
                case "--map-style" -> {
                    String id = value(args, ++i, "--map-style");
                    mapStyle = DensityStyle.byId(id).orElseThrow(
                            () -> new IllegalArgumentException("--map-style must be surface or mesh, not " + id));
                }
                case "--map-opacity" -> mapOpacity = fraction(value(args, ++i, "--map-opacity"), "--map-opacity");
                case "--screenshot" -> screenshot = Path.of(value(args, ++i, "--screenshot"));
                case "--benchmark" -> {
                    try {
                        benchmark = Integer.parseInt(value(args, ++i, "--benchmark"));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("--benchmark needs a number of seconds", e);
                    }
                }
                default -> {
                    if (args[i].startsWith("--")) {
                        throw new IllegalArgumentException("Unknown option " + args[i]);
                    }
                    if (ViewerController.isMapFile(args[i])) {
                        map = Path.of(args[i]);
                    } else {
                        file = Path.of(args[i]);
                    }
                }
            }
        }
        if ((screenshot != null || benchmark != null) && file == null && map == null) {
            throw new IllegalArgumentException("--screenshot and --benchmark need a structure or map file");
        }
        return new LaunchOptions(Optional.ofNullable(file), Optional.ofNullable(mode), Optional.ofNullable(scheme),
                Optional.ofNullable(secondaryStructure), Optional.ofNullable(surface), Optional.ofNullable(opacity),
                Optional.ofNullable(map), Optional.ofNullable(contour), Optional.ofNullable(mapStyle),
                Optional.ofNullable(mapOpacity), Optional.ofNullable(screenshot), Optional.ofNullable(benchmark));
    }

    private static float fraction(String text, String option) {
        float value;
        try {
            value = Float.parseFloat(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(option + " needs a number from 0 to 1", e);
        }
        if (!(value >= 0 && value <= 1)) {
            throw new IllegalArgumentException(option + " needs a number from 0 to 1");
        }
        return value;
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException(option + " needs a value");
        }
        return args[index];
    }
}
