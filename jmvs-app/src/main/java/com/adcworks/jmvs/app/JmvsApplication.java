package com.adcworks.jmvs.app;

import com.adcworks.jmvs.app.ui.ViewerUi;
import com.adcworks.jmvs.core.chem.ElementTable;
import com.adcworks.jmvs.core.io.StructureReaders;
import com.adcworks.jmvs.core.map.DensityMap;
import com.adcworks.jmvs.render.GlslCompatibility;
import com.adcworks.jmvs.render.OrbitCameraController;
import com.adcworks.jmvs.render.ScreenPicker;
import com.adcworks.jmvs.render.StructureRenderer;
import com.adcworks.jmvs.scene.Registry;
import com.adcworks.jmvs.scene.Scene;
import com.adcworks.jmvs.scene.density.DensityStyle;
import com.adcworks.jmvs.scene.density.Isosurface;
import com.adcworks.jmvs.scene.surface.Surface;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;

import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * libGDX application: wires the controller, renderer, camera and UI, and runs on the render thread.
 */
public final class JmvsApplication extends ApplicationAdapter implements ViewerController.Listener, ViewerUi.Actions {

    private static final System.Logger LOG = System.getLogger(JmvsApplication.class.getName());

    private final LaunchOptions options;
    private final ExecutorService background = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().daemon().name("jmvs-loader").factory());
    private final ExecutorService dialogs = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().daemon().name("jmvs-dialog").factory());

    private Stage stage;
    private StructureRenderer renderer;
    private OrbitCameraController orbit;
    private ViewerController controller;
    private ViewerUi ui;
    private Path lastDirectory = Path.of("").toAbsolutePath();
    private int framesSinceShown = -1;
    /** The map whose contour the camera was framed on, when shown without a structure. */
    private DensityMap framedMap;
    private long benchmarkStart;
    private int benchmarkFrames;

    public JmvsApplication(LaunchOptions options) {
        this.options = options;
    }

    @Override
    public void create() {
        GlslCompatibility.useCoreProfileShaders();
        // VisUI 1.5.9 targets libGDX 1.14.1; 1.14.2 is a compatible hotfix release.
        VisUI.setSkipGdxVersionCheck(true);
        VisUI.load(VisUI.SkinScale.X1);
        stage = new Stage(new ScreenViewport());
        renderer = new StructureRenderer();
        orbit = new OrbitCameraController(this::clicked);
        orbit.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        Registry registry = Registry.load();
        controller = new ViewerController(StructureReaders.defaults(ElementTable.load()), registry, background,
                Gdx.app::postRunnable, this);
        ui = new ViewerUi(stage, controller, this);
        options.mode().ifPresent(controller::setMode);
        options.scheme().ifPresent(controller::setScheme);
        options.secondaryStructure().ifPresent(controller::setSecondaryStructureSource);
        options.surface().ifPresent(kind -> controller.setSurface(Optional.of(kind)));
        options.surfaceOpacity().ifPresent(controller::setSurfaceOpacity);
        options.mapStyle().ifPresent(controller::setDensityStyle);
        options.mapOpacity().ifPresent(controller::setDensityOpacity);
        renderer.setDensityColour(registry.density().colour());
        renderer.setDensityStyle(controller.densityStyle());
        renderer.setDensityOpacity(controller.densityOpacity());
        ui.refresh();

        Gdx.input.setInputProcessor(new InputMultiplexer(stage, shortcuts(), orbit));
        options.file().ifPresent(this::open);
        options.map().ifPresent(path -> {
            remember(path);
            controller.openMap(path, options.contour());
        });
    }

    /**
     * Opens a file chosen outside the menu (command line, drag-and-drop): a density map by its extension, otherwise
     * a structure. Call on the render thread.
     */
    public void open(Path path) {
        remember(path);
        if (ViewerController.isMapFile(path.getFileName().toString())) {
            controller.openMap(path);
        } else {
            controller.open(path);
        }
    }

    private void remember(Path path) {
        if (path.getParent() != null) {
            lastDirectory = path.toAbsolutePath().getParent();
        }
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.07f, 0.08f, 0.1f, 1f, true);
        renderer.render(orbit.camera());
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
        if (options.screenshot().isEmpty()) {
            ui.setFps(Gdx.graphics.getFramesPerSecond());
        }

        if (options.screenshot().isPresent() && ready() && ++framesSinceShown == 3) {
            writeScreenshot(options.screenshot().get());
            Gdx.app.exit();
        }
        options.benchmarkSeconds().ifPresent(this::benchmarkFrame);
    }

    /** A structure or map is shown, nothing is loading, and its surface and map contour are computed. */
    private boolean ready() {
        return framesSinceShown >= 0 && !controller.isLoading() && !controller.isMapLoading()
                && !controller.isSurfaceBuilding() && !controller.isDensityBuilding();
    }

    private void benchmarkFrame(int seconds) {
        if (!ready()) {
            return;
        }
        orbit.rotate(0.5f, 0.2f);
        if (benchmarkFrames++ == 0) {
            benchmarkStart = System.nanoTime();
            return;
        }
        double elapsed = (System.nanoTime() - benchmarkStart) / 1e9;
        if (elapsed >= seconds) {
            double fps = (benchmarkFrames - 1) / elapsed;
            LOG.log(System.Logger.Level.INFO, String.format(java.util.Locale.ROOT,
                    "Benchmark %s, %s/%s%s: %.1f fps average over %.1f s (%dx%d)",
                    controller.structure().map(s -> s.name() + " " + s.atoms().size() + " atoms").orElse("?"),
                    controller.mode().id(), controller.scheme().id(),
                    controller.surfaceKind().map(k -> " + " + k.id() + " surface").orElse("")
                            + controller.map().map(m -> " + map " + m.name() + " " + controller.densityStyle().id())
                            .orElse(""), fps, elapsed,
                    Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight()));
            Gdx.app.exit();
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        orbit.resize(width, height);
    }

    @Override
    public void dispose() {
        background.shutdownNow();
        dialogs.shutdownNow();
        renderer.dispose();
        stage.dispose();
        VisUI.dispose();
    }

    @Override
    public void loadingStarted(String fileName) {
        ui.loading(fileName);
    }

    @Override
    public void structureShown(Scene scene, int[] colours) {
        renderer.setScene(scene, colours);
        renderer.setHighlight(-1);
        orbit.frame(scene.bounds());
        ui.refresh();
        framesSinceShown = 0;
    }

    @Override
    public void sceneChanged(Scene scene, int[] colours) {
        renderer.setScene(scene, colours);
        ui.refresh();
    }

    @Override
    public void coloursChanged(int[] colours) {
        renderer.setColours(colours);
        ui.refresh();
    }

    @Override
    public void selectionChanged(OptionalInt atom) {
        renderer.setHighlight(atom.orElse(-1));
        ui.refresh();
    }

    @Override
    public void surfaceChanged(Optional<Surface> surface) {
        renderer.setSurface(surface);
        ui.refresh();
    }

    @Override
    public void surfaceOpacityChanged(float opacity) {
        renderer.setSurfaceOpacity(opacity);
        ui.refresh();
    }

    @Override
    public void mapShown(DensityMap map) {
        if (renderer.scene() == null) {
            orbit.frame(map.boundingSphere());
        }
        if (framesSinceShown < 0) {
            framesSinceShown = 0;
        }
        ui.refresh();
    }

    @Override
    public void densityChanged(Optional<Isosurface> isosurface) {
        renderer.setDensity(isosurface);
        // Without a structure, frame the first contour of each map: tighter than the map's box.
        DensityMap map = controller.map().orElse(null);
        if (renderer.scene() == null && map != null && map != framedMap) {
            isosurface.flatMap(Isosurface::boundingSphere).ifPresent(sphere -> {
                orbit.frame(sphere);
                framedMap = map;
            });
        }
        ui.refresh();
    }

    @Override
    public void densityStyleChanged(DensityStyle style) {
        renderer.setDensityStyle(style);
        ui.refresh();
    }

    @Override
    public void densityOpacityChanged(float opacity) {
        renderer.setDensityOpacity(opacity);
        ui.refresh();
    }

    @Override
    public void loadFailed(String message) {
        ui.error(message);
        if (options.screenshot().isPresent() || options.benchmarkSeconds().isPresent()) {
            LOG.log(System.Logger.Level.ERROR, message);
            Gdx.app.exit();
        }
    }

    @Override
    public void openFile() {
        Path start = lastDirectory;
        dialogs.execute(() -> FileOpener.choose(start).ifPresent(path -> Gdx.app.postRunnable(() -> open(path))));
    }

    @Override
    public void resetView() {
        if (renderer.scene() != null) {
            orbit.frame(renderer.scene().bounds());
        } else {
            controller.map().ifPresent(map -> orbit.frame(map.boundingSphere()));
        }
    }

    @Override
    public void quit() {
        Gdx.app.exit();
    }

    private void clicked(int screenX, int screenY) {
        controller.select(ScreenPicker.pick(orbit.camera(), renderer.scene(), screenX, screenY));
    }

    private InputAdapter shortcuts() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                boolean modifier = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
                        || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)
                        || Gdx.input.isKeyPressed(Input.Keys.SYM);
                if (keycode == Input.Keys.O && modifier) {
                    openFile();
                    return true;
                }
                if (keycode == Input.Keys.R && !modifier) {
                    resetView();
                    return true;
                }
                if (keycode == Input.Keys.ESCAPE) {
                    controller.select(OptionalInt.empty());
                    return true;
                }
                if ((keycode == Input.Keys.LEFT_BRACKET || keycode == Input.Keys.RIGHT_BRACKET)
                        && controller.map().isPresent()) {
                    int steps = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                            || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT) ? 5 : 1;
                    controller.stepContourLevel(keycode == Input.Keys.LEFT_BRACKET ? -steps : steps);
                    ui.refresh();
                    return true;
                }
                return false;
            }
        };
    }

    private static void writeScreenshot(Path file) {
        int width = Gdx.graphics.getBackBufferWidth();
        int height = Gdx.graphics.getBackBufferHeight();
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, width, height);
        PixmapIO.writePNG(Gdx.files.absolute(file.toAbsolutePath().toString()), pixmap, -1, true);
        pixmap.dispose();
        LOG.log(System.Logger.Level.INFO, "Wrote screenshot {0}", file.toAbsolutePath());
    }
}
