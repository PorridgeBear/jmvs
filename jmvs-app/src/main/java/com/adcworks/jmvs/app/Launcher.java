package com.adcworks.jmvs.app;

import com.adcworks.jmvs.core.config.ConfigurationException;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Entry point. The only class allowed to exit the JVM.
 */
public final class Launcher {

    private static final System.Logger LOG = System.getLogger(Launcher.class.getName());

    private Launcher() {
    }

    public static void main(String[] args) {
        LaunchOptions options;
        try {
            options = LaunchOptions.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("jmvs: " + e.getMessage());
            System.err.println(LaunchOptions.USAGE);
            System.exit(2);
            return;
        }
        if (relaunchOnFirstThreadIfNeeded(args)) {
            return;
        }
        try {
            JmvsApplication app = new JmvsApplication(options);
            new Lwjgl3Application(app, configuration(app, options));
        } catch (ConfigurationException e) {
            LOG.log(System.Logger.Level.ERROR, "Invalid configuration", e);
            System.exit(1);
        }
    }

    private static Lwjgl3ApplicationConfiguration configuration(JmvsApplication app, LaunchOptions options) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("JMVS");
        config.disableAudio(true);
        config.setWindowedMode(1280, 800);
        config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2);
        config.setBackBufferConfig(8, 8, 8, 8, 24, 0, 4);
        config.useVsync(options.benchmarkSeconds().isEmpty());
        config.setForegroundFPS(0);
        config.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override
            public void filesDropped(String[] files) {
                if (files.length > 0) {
                    Path path = Path.of(files[0]);
                    Gdx.app.postRunnable(() -> app.open(path));
                }
            }
        });
        return config;
    }

    /**
     * GLFW on macOS must run on the process's first thread ({@code -XstartOnFirstThread}). The Gradle run task and
     * packaged app pass that flag; when started any other way, restart the JVM with it.
     */
    private static boolean relaunchOnFirstThreadIfNeeded(String[] args) {
        boolean mac = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
        long pid = ProcessHandle.current().pid();
        if (!mac || "1".equals(System.getenv("JAVA_STARTED_ON_FIRST_THREAD_" + pid))
                || Boolean.getBoolean("jmvs.relaunched")) {
            return false;
        }
        String java = ProcessHandle.current().info().command()
                .orElse(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        List<String> command = new ArrayList<>(List.of(java, "-XstartOnFirstThread", "-Djmvs.relaunched=true"));
        command.addAll(jvmArgumentsAndMain().orElseGet(() -> List.of(
                "--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow",
                "-cp", System.getProperty("java.class.path"), Launcher.class.getName())));
        command.addAll(List.of(args));
        try {
            Process process = new ProcessBuilder(command).inheritIO().start();
            System.exit(process.waitFor());
        } catch (IOException e) {
            LOG.log(System.Logger.Level.ERROR, "Cannot restart with -XstartOnFirstThread; start java with it", e);
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.exit(1);
        }
        return true;
    }

    /**
     * This JVM's own arguments up to and including the main class (or {@code -jar <file>}), so a restart keeps
     * options such as {@code -Xmx} and the start script's flags.
     */
    private static Optional<List<String>> jvmArgumentsAndMain() {
        return ProcessHandle.current().info().arguments().flatMap(arguments -> {
            List<String> all = List.of(arguments);
            int main = all.indexOf(Launcher.class.getName());
            if (main >= 0) {
                return Optional.of(all.subList(0, main + 1));
            }
            int jar = all.indexOf("-jar");
            return jar >= 0 && jar + 1 < all.size()
                    ? Optional.of(all.subList(0, jar + 2)) : Optional.empty();
        });
    }
}
