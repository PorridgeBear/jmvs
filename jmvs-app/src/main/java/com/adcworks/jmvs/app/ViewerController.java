package com.adcworks.jmvs.app;

import com.adcworks.jmvs.core.chem.SecondaryStructures;
import com.adcworks.jmvs.core.io.MapReadException;
import com.adcworks.jmvs.core.io.MrcMapReader;
import com.adcworks.jmvs.core.io.StructureReadException;
import com.adcworks.jmvs.core.map.DensityMap;
import com.adcworks.jmvs.core.io.StructureReaders;
import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.Chain;
import com.adcworks.jmvs.core.model.Residue;
import com.adcworks.jmvs.core.model.SecondaryStructureSource;
import com.adcworks.jmvs.core.model.Structure;
import com.adcworks.jmvs.scene.Registry;
import com.adcworks.jmvs.scene.Scene;
import com.adcworks.jmvs.scene.colour.ColourScheme;
import com.adcworks.jmvs.scene.colour.Legend;
import com.adcworks.jmvs.scene.config.DensitySettings;
import com.adcworks.jmvs.scene.density.ContourLevel;
import com.adcworks.jmvs.scene.density.DensityStyle;
import com.adcworks.jmvs.scene.density.Isosurface;
import com.adcworks.jmvs.scene.density.IsosurfaceBuilder;
import com.adcworks.jmvs.scene.density.ZoneColouring;
import com.adcworks.jmvs.scene.mode.DisplayMode;
import com.adcworks.jmvs.scene.surface.Surface;
import com.adcworks.jmvs.scene.surface.SurfaceBuilder;
import com.adcworks.jmvs.scene.surface.SurfaceKind;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/**
 * Application state and use cases, independent of libGDX: the loaded structure, display mode, colour scheme,
 * selection, surface and density map. Loading and building run on a background executor; all state changes and
 * listener calls happen on the UI executor.
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

        /** The surface to draw over the scene changed; empty while none is chosen or one is being computed. */
        void surfaceChanged(Optional<Surface> surface);

        void surfaceOpacityChanged(float opacity);

        /** A density map was read; its contour follows with {@link #densityChanged}. */
        default void mapShown(DensityMap map) {
        }

        /**
         * The density contour to draw changed: a new map, level or zone colouring, or empty when the map is closed.
         * While a new level is computed the previous contour stays shown.
         */
        default void densityChanged(Optional<Isosurface> isosurface) {
        }

        default void densityStyleChanged(DensityStyle style) {
        }

        default void densityOpacityChanged(float opacity) {
        }
    }

    private record Loaded(Structure read, Structure structure, Scene scene, int[] colours) {
    }

    private final StructureReaders readers;
    private final Registry registry;
    private final Executor background;
    private final Executor ui;
    private final Listener listener;

    /** The structure as read from the file, with the file's secondary structure. */
    private Structure read;
    /** The structure shown: {@link #read}, or a copy with computed secondary structure. */
    private Structure structure;
    private final SecondaryStructures secondaryStructures = new SecondaryStructures();
    private SecondaryStructureSource secondaryStructureSource = SecondaryStructureSource.AUTO;
    private int secondaryStructureGeneration;
    /** The source {@link #structure} was produced with. */
    private SecondaryStructureSource shownSource = SecondaryStructureSource.AUTO;
    private DisplayMode mode;
    private ColourScheme preferredScheme;
    private final Map<String, Scene> scenes = new HashMap<>();
    private OptionalInt selection = OptionalInt.empty();
    private int loadGeneration;
    private boolean loading;
    private final SurfaceBuilder surfaceBuilder;
    private final Map<SurfaceKind, Surface> surfaces = new HashMap<>();
    /** Incremented on every surface request; read by background builds to stop when superseded. */
    private final AtomicInteger surfaceGeneration = new AtomicInteger();
    private Optional<SurfaceKind> surfaceKind = Optional.empty();
    private float surfaceOpacity = 1f;
    private boolean surfaceBuilding;
    private final MrcMapReader mapReader;
    private final DensitySettings densitySettings;
    private DensityMap map;
    private int mapGeneration;
    private boolean mapLoading;
    private float contourLevel;
    /** The contour shown, for {@link #map} at {@link Isosurface#level()}, zone-coloured for {@link #structure}. */
    private Isosurface density;
    /** Incremented on every contour request; read by background builds to stop when superseded. */
    private final AtomicInteger densityGeneration = new AtomicInteger();
    private boolean densityBuilding;
    private DensityStyle densityStyle;
    private float densityOpacity = 1f;

    public ViewerController(StructureReaders readers, Registry registry, Executor background, Executor ui,
                            Listener listener) {
        this.readers = readers;
        this.registry = registry;
        this.background = background;
        this.ui = ui;
        this.listener = listener;
        this.mode = registry.defaultMode();
        this.preferredScheme = registry.defaultScheme();
        this.surfaceBuilder = new SurfaceBuilder(registry.surface());
        this.densitySettings = registry.density();
        this.mapReader = new MrcMapReader(densitySettings.maxVoxels());
        this.densityStyle = densitySettings.style();
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

    /** Where secondary structure comes from; it stays chosen when another file is opened. */
    public SecondaryStructureSource secondaryStructureSource() {
        return secondaryStructureSource;
    }

    /** Whether the shown structure's secondary structure was computed rather than read from the file. */
    public boolean isSecondaryStructureComputed() {
        return structure != null && structure != read;
    }

    public boolean isLoading() {
        return loading;
    }

    /** The chosen surface kind, if any; it stays chosen when another file is opened. */
    public Optional<SurfaceKind> surfaceKind() {
        return surfaceKind;
    }

    public float surfaceOpacity() {
        return surfaceOpacity;
    }

    /** Whether the chosen surface is being computed in the background. */
    public boolean isSurfaceBuilding() {
        return surfaceBuilding;
    }

    public boolean supportsFile(String fileName) {
        return readers.supports(fileName);
    }

    /** Whether the file name is a density map ({@code .map}, {@code .mrc}, {@code .ccp4}, optionally gzipped). */
    public static boolean isMapFile(String fileName) {
        return MrcMapReader.supports(fileName);
    }

    /** The density map shown, if any; it stays when another structure is opened. */
    public Optional<DensityMap> map() {
        return Optional.ofNullable(map);
    }

    public boolean isMapLoading() {
        return mapLoading;
    }

    /** The density map's contour level, in the map's units. */
    public float contourLevel() {
        return contourLevel;
    }

    /** The contour shown, if any. */
    public Optional<Isosurface> density() {
        return Optional.ofNullable(density);
    }

    /** Whether a contour is being computed in the background. */
    public boolean isDensityBuilding() {
        return densityBuilding;
    }

    public DensityStyle densityStyle() {
        return densityStyle;
    }

    public float densityOpacity() {
        return densityOpacity;
    }

    /**
     * Loads a file in the background. On failure the current structure stays visible.
     */
    public void open(Path path) {
        int generation = ++loadGeneration;
        loading = true;
        cancelSurface();
        listener.loadingStarted(path.getFileName().toString());
        DisplayMode modeAtStart = mode;
        ColourScheme schemeAtStart = scheme();
        SecondaryStructureSource sourceAtStart = secondaryStructureSource;
        background.execute(() -> {
            try {
                Structure fromFile = readers.read(path);
                Structure loaded = secondaryStructures.apply(fromFile, sourceAtStart);
                Scene scene = modeAtStart.build(loaded);
                int[] colours = schemeAtStart.colours(loaded);
                ui.execute(() -> shown(generation, new Loaded(fromFile, loaded, scene, colours), modeAtStart,
                        schemeAtStart, sourceAtStart));
            } catch (StructureReadException e) {
                LOG.log(System.Logger.Level.WARNING, "Cannot open " + path, e);
                ui.execute(() -> failed(generation, e.getMessage()));
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.ERROR, "Unexpected error opening " + path, e);
                ui.execute(() -> failed(generation, path.getFileName() + ": unexpected error: " + e));
            }
        });
    }

    /**
     * Reads a density map in the background and contours it at the default level
     * ({@code density.sigma} standard deviations above the mean). On failure the current map stays.
     */
    public void openMap(Path path) {
        openMap(path, Optional.empty());
    }

    /**
     * Reads a density map in the background and contours it at {@code level}, or the default level if empty.
     */
    public void openMap(Path path, Optional<ContourLevel> level) {
        int generation = ++mapGeneration;
        mapLoading = true;
        listener.loadingStarted(path.getFileName().toString());
        background.execute(() -> {
            try {
                DensityMap read = mapReader.read(path);
                ui.execute(() -> mapRead(generation, read, level));
            } catch (MapReadException e) {
                LOG.log(System.Logger.Level.WARNING, "Cannot open map " + path, e);
                ui.execute(() -> mapFailed(generation, e.getMessage()));
            } catch (RuntimeException | OutOfMemoryError e) {
                LOG.log(System.Logger.Level.ERROR, "Unexpected error opening map " + path, e);
                ui.execute(() -> mapFailed(generation, path.getFileName() + ": unexpected error: " + e));
            }
        });
    }

    /** Removes the density map. */
    public void closeMap() {
        mapGeneration++;
        mapLoading = false;
        map = null;
        density = null;
        requestDensity();
    }

    /**
     * Contours the map at another level, computed in the background; the previous contour stays shown meanwhile.
     */
    public void setContourLevel(float level) {
        if (map == null || !Float.isFinite(level) || level == contourLevel) {
            return;
        }
        contourLevel = level;
        requestDensity();
    }

    /** Moves the contour level by {@code steps} tenths of a standard deviation. */
    public void stepContourLevel(int steps) {
        if (map != null) {
            setContourLevel(contourLevel + steps * 0.1f * map.statistics().rms());
        }
    }

    public void setDensityStyle(DensityStyle style) {
        if (style != densityStyle) {
            densityStyle = style;
            listener.densityStyleChanged(style);
        }
    }

    /**
     * Density contour opacity from 0 to 1; values outside are clamped.
     */
    public void setDensityOpacity(float opacity) {
        float clamped = Math.clamp(opacity, 0f, 1f);
        if (clamped != densityOpacity) {
            densityOpacity = clamped;
            listener.densityOpacityChanged(clamped);
        }
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

    /**
     * Chooses where secondary structure comes from. Applying it (computing if needed) and rebuilding the scene run in
     * the background; the listener then gets {@link Listener#sceneChanged}. Atom indices, the selection and surfaces
     * are kept.
     */
    public void setSecondaryStructureSource(SecondaryStructureSource source) {
        if (source == secondaryStructureSource) {
            return;
        }
        secondaryStructureSource = source;
        if (!loading) {
            requestSecondaryStructure();
        }
    }

    /**
     * Shows a surface over the scene, or none. Surfaces are computed in the background, once per kind for each
     * loaded structure.
     */
    public void setSurface(Optional<SurfaceKind> kind) {
        if (kind.equals(surfaceKind)) {
            return;
        }
        surfaceKind = kind;
        requestSurface();
    }

    /**
     * Surface opacity from 0 to 1; values outside are clamped.
     */
    public void setSurfaceOpacity(float opacity) {
        float clamped = Math.clamp(opacity, 0f, 1f);
        if (clamped != surfaceOpacity) {
            surfaceOpacity = clamped;
            listener.surfaceOpacityChanged(clamped);
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

    private void requestSecondaryStructure() {
        int generation = ++secondaryStructureGeneration;
        if (read == null) {
            return;
        }
        Structure target = read;
        SecondaryStructureSource source = secondaryStructureSource;
        DisplayMode modeAtStart = mode;
        background.execute(() -> {
            try {
                Structure applied = secondaryStructures.apply(target, source);
                Scene scene = modeAtStart.build(applied);
                ui.execute(() -> secondaryStructureApplied(generation, target, applied, source, modeAtStart,
                        scene));
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.ERROR, "Cannot assign secondary structure of " + target.name(), e);
                ui.execute(() -> listener.loadFailed("Cannot assign secondary structure: " + e));
            }
        });
    }

    private void secondaryStructureApplied(int generation, Structure appliedTo, Structure applied,
                                           SecondaryStructureSource source, DisplayMode builtFor, Scene scene) {
        if (generation != secondaryStructureGeneration || appliedTo != read) {
            return;
        }
        structure = applied;
        shownSource = source;
        scenes.clear();
        scenes.put(builtFor.id(), scene);
        listener.sceneChanged(scene(), scheme().colours(structure));
    }

    private void requestSurface() {
        int generation = surfaceGeneration.incrementAndGet();
        if (structure == null) {
            surfaceBuilding = false;
            return;
        }
        Optional<Surface> cached = surfaceKind.map(surfaces::get);
        if (surfaceKind.isEmpty() || cached.isPresent()) {
            surfaceBuilding = false;
            listener.surfaceChanged(cached);
            return;
        }
        surfaceBuilding = true;
        listener.surfaceChanged(Optional.empty());
        Structure target = structure;
        SurfaceKind kind = surfaceKind.get();
        background.execute(() -> {
            try {
                long start = System.nanoTime();
                Surface surface = surfaceBuilder.build(target, kind, () -> surfaceGeneration.get() != generation);
                LOG.log(System.Logger.Level.INFO, String.format(Locale.ROOT,
                        "Built %s surface of %s: %d triangles, %.0f Å² in %d ms", kind.id(),
                        target.name(), surface.triangleCount(), surface.area(),
                        (System.nanoTime() - start) / 1_000_000));
                ui.execute(() -> surfaceBuilt(generation, target, surface));
            } catch (CancellationException e) {
                LOG.log(System.Logger.Level.DEBUG, "Surface build superseded");
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.ERROR, "Cannot compute surface of " + target.name(), e);
                ui.execute(() -> surfaceFailed(generation, "Cannot compute surface: " + e));
            }
        });
    }

    /** Stops any surface build, e.g. while another file loads. */
    private void cancelSurface() {
        surfaceGeneration.incrementAndGet();
        surfaceBuilding = false;
    }

    private void surfaceBuilt(int generation, Structure builtFor, Surface surface) {
        // Same atoms: secondary structure may have been re-applied meanwhile, which does not change the surface.
        if (generation != surfaceGeneration.get() || structure == null || builtFor.atoms() != structure.atoms()) {
            return;
        }
        surfaces.put(surface.kind(), surface);
        surfaceBuilding = false;
        listener.surfaceChanged(Optional.of(surface));
    }

    private void surfaceFailed(int generation, String message) {
        if (generation != surfaceGeneration.get()) {
            return;
        }
        surfaceBuilding = false;
        listener.loadFailed(message);
    }

    private void mapRead(int generation, DensityMap read, Optional<ContourLevel> level) {
        if (generation != mapGeneration) {
            return;
        }
        mapLoading = false;
        map = read;
        density = null;
        contourLevel = level.orElse(new ContourLevel.Sigma(densitySettings.defaultSigma()))
                .resolve(read.statistics());
        listener.mapShown(read);
        requestDensity();
    }

    private void mapFailed(int generation, String message) {
        if (generation != mapGeneration) {
            return;
        }
        mapLoading = false;
        listener.loadFailed(message);
    }

    /**
     * Contours the map at the current level and zone-colours it for the current structure, in the background.
     * When only the structure changed, the shown contour's geometry is reused and only its vertex atoms change.
     */
    private void requestDensity() {
        int generation = densityGeneration.incrementAndGet();
        if (map == null) {
            densityBuilding = false;
            listener.densityChanged(Optional.empty());
            return;
        }
        densityBuilding = true;
        DensityMap target = map;
        float level = contourLevel;
        Optional<Structure> model = structure();
        Isosurface reuse = density != null && density.level() == level ? density : null;
        float radius = densitySettings.zoneRadius();
        background.execute(() -> {
            try {
                long start = System.nanoTime();
                BooleanSupplier cancelled = () -> densityGeneration.get() != generation;
                Isosurface contour = reuse != null ? reuse : IsosurfaceBuilder.build(target, level, cancelled);
                Isosurface zoned = ZoneColouring.assign(contour, model, radius, cancelled);
                LOG.log(System.Logger.Level.INFO, String.format(Locale.ROOT,
                        "Contoured %s at %.4g (%.2f σ): %d triangles, %.0f Å³ in %d ms", target.name(), level,
                        target.statistics().sigma(level), zoned.triangleCount(), zoned.volume(),
                        (System.nanoTime() - start) / 1_000_000));
                ui.execute(() -> densityBuilt(generation, target, zoned));
            } catch (CancellationException e) {
                LOG.log(System.Logger.Level.DEBUG, "Contour superseded");
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.ERROR, "Cannot contour " + target.name(), e);
                ui.execute(() -> densityFailed(generation, "Cannot contour map: " + e));
            }
        });
    }

    private void densityBuilt(int generation, DensityMap builtFor, Isosurface contour) {
        if (generation != densityGeneration.get() || builtFor != map) {
            return;
        }
        density = contour;
        densityBuilding = false;
        listener.densityChanged(Optional.of(contour));
    }

    private void densityFailed(int generation, String message) {
        if (generation != densityGeneration.get()) {
            return;
        }
        densityBuilding = false;
        listener.loadFailed(message);
    }

    private void shown(int generation, Loaded loaded, DisplayMode builtFor, ColourScheme colouredWith,
                       SecondaryStructureSource appliedSource) {
        if (generation != loadGeneration) {
            return;
        }
        loading = false;
        read = loaded.read();
        structure = loaded.structure();
        shownSource = appliedSource;
        scenes.clear();
        scenes.put(builtFor.id(), loaded.scene());
        surfaces.clear();
        selection = OptionalInt.empty();
        if (density != null) {
            // The contour's atoms belong to the previous structure: show the map colour until re-zoned.
            density = ZoneColouring.assign(density, Optional.empty(), 0, () -> false);
            listener.densityChanged(Optional.of(density));
        }
        // Mode or scheme may have changed while loading.
        Scene scene = builtFor == mode ? loaded.scene() : scene();
        int[] colours = colouredWith == scheme() ? loaded.colours() : scheme().colours(structure);
        listener.structureShown(scene, colours);
        listener.selectionChanged(selection);
        requestSurface();
        // Zone colouring follows the new structure's atoms; the contour geometry is reused.
        if (map != null) {
            requestDensity();
        }
        // The source may have changed while loading.
        if (shownSource != secondaryStructureSource) {
            requestSecondaryStructure();
        }
    }

    private void failed(int generation, String message) {
        if (generation != loadGeneration) {
            return;
        }
        loading = false;
        listener.loadFailed(message);
        // The source may have changed while loading; apply it to the structure still shown.
        if (read != null && shownSource != secondaryStructureSource) {
            requestSecondaryStructure();
        }
        // The load cancelled the current structure's surface build.
        requestSurface();
    }
}
