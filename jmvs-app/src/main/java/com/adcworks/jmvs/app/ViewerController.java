package com.adcworks.jmvs.app;

import com.adcworks.jmvs.core.io.StructureReadException;
import com.adcworks.jmvs.core.io.StructureReaders;
import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.Chain;
import com.adcworks.jmvs.core.model.Residue;
import com.adcworks.jmvs.core.model.Structure;
import com.adcworks.jmvs.scene.Registry;
import com.adcworks.jmvs.scene.Scene;
import com.adcworks.jmvs.scene.colour.ColourScheme;
import com.adcworks.jmvs.scene.colour.Legend;
import com.adcworks.jmvs.scene.mode.DisplayMode;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.Executor;

/**
 * Application state and use cases, independent of libGDX: the loaded structure, display mode, colour scheme and
 * selection. Loading runs on a background executor; all state changes and listener calls happen on the UI
 * executor.
 */
public final class ViewerController {

    private static final System.Logger LOG = System.getLogger(ViewerController.class.getName());

    /**
     * Receives state changes on the UI thread.
     */
    public interface Listener {

        void loadingStarted(String fileName);

        /** A new structure is shown; the view should be re-framed. */
        void structureShown(Scene scene, int[] colours);

        /** The display mode changed for the same structure. */
        void sceneChanged(Scene scene, int[] colours);

        /** Only the colours changed. */
        void coloursChanged(int[] colours);

        void selectionChanged(OptionalInt atom);

        void loadFailed(String message);
    }

    private record Loaded(Structure structure, Scene scene, int[] colours) {
    }

    private final StructureReaders readers;
    private final Registry registry;
    private final Executor background;
    private final Executor ui;
    private final Listener listener;

    private Structure structure;
    private DisplayMode mode;
    private ColourScheme preferredScheme;
    private final Map<String, Scene> scenes = new HashMap<>();
    private OptionalInt selection = OptionalInt.empty();
    private int loadGeneration;
    private boolean loading;

    public ViewerController(StructureReaders readers, Registry registry, Executor background, Executor ui,
                            Listener listener) {
        this.readers = readers;
        this.registry = registry;
        this.background = background;
        this.ui = ui;
        this.listener = listener;
        this.mode = registry.defaultMode();
        this.preferredScheme = registry.defaultScheme();
    }

    public Registry registry() {
        return registry;
    }

    public Optional<Structure> structure() {
        return Optional.ofNullable(structure);
    }

    public DisplayMode mode() {
        return mode;
    }

    /** The scheme in effect: the preferred one, or a fallback if the current mode does not support it. */
    public ColourScheme scheme() {
        return registry.schemeFor(mode, preferredScheme);
    }

    public OptionalInt selection() {
        return selection;
    }

    public boolean isLoading() {
        return loading;
    }

    public boolean supportsFile(String fileName) {
        return readers.supports(fileName);
    }

    /**
     * Loads a file in the background. On failure the current structure stays visible.
     */
    public void open(Path path) {
        int generation = ++loadGeneration;
        loading = true;
        listener.loadingStarted(path.getFileName().toString());
        DisplayMode modeAtStart = mode;
        ColourScheme schemeAtStart = scheme();
        background.execute(() -> {
            try {
                Structure loaded = readers.read(path);
                Scene scene = modeAtStart.build(loaded);
                int[] colours = schemeAtStart.colours(loaded);
                ui.execute(() -> shown(generation, new Loaded(loaded, scene, colours), modeAtStart, schemeAtStart));
            } catch (StructureReadException e) {
                LOG.log(System.Logger.Level.WARNING, "Cannot open " + path, e);
                ui.execute(() -> failed(generation, e.getMessage()));
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.ERROR, "Unexpected error opening " + path, e);
                ui.execute(() -> failed(generation, path.getFileName() + ": unexpected error: " + e));
            }
        });
    }

    public void setMode(String id) {
        DisplayMode next = registry.mode(id);
        if (next == mode) {
            return;
        }
        mode = next;
        if (structure != null) {
            listener.sceneChanged(scene(), scheme().colours(structure));
        }
    }

    public void setScheme(String id) {
        preferredScheme = registry.scheme(id);
        if (structure != null) {
            listener.coloursChanged(scheme().colours(structure));
        }
    }

    public void select(OptionalInt atom) {
        if (!atom.equals(selection)) {
            selection = atom;
            listener.selectionChanged(atom);
        }
    }

    /**
     * Human-readable description of an atom, e.g. {@code "CA (Carbon)  ALA 12  chain A  (1.20, 3.40, -5.60)"},
     * followed by its B-factor, shown as pLDDT while colouring by confidence.
     */
    public String describe(int atomIndex) {
        Atom atom = structure.atoms().get(atomIndex);
        Residue residue = structure.residueOf(atom);
        Chain chain = structure.chainOf(residue);
        String text = String.format(Locale.ROOT, "%s (%s)  %s %d%s  chain %s  (%.2f, %.2f, %.2f)", atom.name(),
                atom.element().name(), residue.name(), residue.seqId(), residue.insertionCode(), chain.authId(),
                atom.x(), atom.y(), atom.z());
        if (!atom.hasBFactor()) {
            return text;
        }
        return scheme().id().equals("confidence")
                ? text + String.format(Locale.ROOT, "  pLDDT %.1f", atom.bFactor())
                : text + String.format(Locale.ROOT, "  B-factor %.2f", atom.bFactor());
    }

    /**
     * Key to the current colours, if the scheme has one and a structure is shown.
     */
    public Optional<Legend> legend() {
        return structure().flatMap(scheme()::legend);
    }

    private Scene scene() {
        return scenes.computeIfAbsent(mode.id(), id -> mode.build(structure));
    }

    private void shown(int generation, Loaded loaded, DisplayMode builtFor, ColourScheme colouredWith) {
        if (generation != loadGeneration) {
            return;
        }
        loading = false;
        structure = loaded.structure();
        scenes.clear();
        scenes.put(builtFor.id(), loaded.scene());
        selection = OptionalInt.empty();
        // Mode or scheme may have changed while loading.
        Scene scene = builtFor == mode ? loaded.scene() : scene();
        int[] colours = colouredWith == scheme() ? loaded.colours() : scheme().colours(structure);
        listener.structureShown(scene, colours);
        listener.selectionChanged(selection);
    }

    private void failed(int generation, String message) {
        if (generation != loadGeneration) {
            return;
        }
        loading = false;
        listener.loadFailed(message);
    }
}
