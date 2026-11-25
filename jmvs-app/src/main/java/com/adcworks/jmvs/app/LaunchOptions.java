package com.adcworks.jmvs.app;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Command-line options: {@code jmvs [file] [--mode <id>] [--scheme <id>] [--screenshot <png>]
 * [--benchmark <seconds>]}. {@code --screenshot} renders the file once, writes a PNG and exits (visual checks);
 * {@code --benchmark} rotates the structure with vsync off, logs the average frame rate and exits.
 */
public record LaunchOptions(Optional<Path> file, Optional<String> mode, Optional<String> scheme,
                            Optional<Path> screenshot, Optional<Integer> benchmarkSeconds) {

    public static final String USAGE =
            "Usage: jmvs [file] [--mode <id>] [--scheme <id>] [--screenshot <png>] [--benchmark <seconds>]";

    public static LaunchOptions parse(String[] args) {
        Path file = null;
        String mode = null;
        String scheme = null;
        Path screenshot = null;
        Integer benchmark = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--mode" -> mode = value(args, ++i, "--mode");
                case "--scheme" -> scheme = value(args, ++i, "--scheme");
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
                    file = Path.of(args[i]);
                }
            }
        }
        if ((screenshot != null || benchmark != null) && file == null) {
            throw new IllegalArgumentException("--screenshot and --benchmark need a structure file");
        }
        return new LaunchOptions(Optional.ofNullable(file), Optional.ofNullable(mode), Optional.ofNullable(scheme),
                Optional.ofNullable(screenshot), Optional.ofNullable(benchmark));
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException(option + " needs a value");
        }
        return args[index];
    }
}
