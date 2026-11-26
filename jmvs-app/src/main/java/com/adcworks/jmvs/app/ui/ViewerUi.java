package com.adcworks.jmvs.app.ui;

import com.adcworks.jmvs.app.ViewerController;
import com.adcworks.jmvs.core.map.DensityMap;
import com.adcworks.jmvs.core.model.SecondaryStructureSource;
import com.adcworks.jmvs.scene.colour.ColourScheme;
import com.adcworks.jmvs.scene.colour.Legend;
import com.adcworks.jmvs.scene.colour.Rgb;
import com.adcworks.jmvs.scene.density.DensityStyle;
import com.adcworks.jmvs.scene.mode.DisplayMode;
import com.adcworks.jmvs.scene.surface.SurfaceKind;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.dialog.Dialogs;
import com.kotcrab.vis.ui.widget.Menu;
import com.kotcrab.vis.ui.widget.MenuBar;
import com.kotcrab.vis.ui.widget.MenuItem;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Scene2D overlay: menu bar (File, Display, Colour, Surface, Map, View, Help), a colour legend for schemes that have
 * one, a contour level slider while a density map is shown, and a status bar.
 */
public final class ViewerUi {

    /**
     * Actions the UI triggers that need the application (dialogs, camera, exit).
     */
    public interface Actions {
        void openFile();

        void resetView();

        void quit();
    }

    private static final String CURRENT = "> ";
    private static final String OTHER = "   ";
    private static final String HINT = "Open a structure or density map with File > Open or drop a file on the window";
    /** Contour slider range and step, in standard deviations above the map's mean. */
    private static final float SLIDER_MIN_SIGMA = -1f;
    private static final float SLIDER_MAX_SIGMA = 15f;
    private static final float SLIDER_STEP_SIGMA = 0.05f;
    private static final int GRADIENT_STEPS = 32;
    private static final Map<String, Float> OPACITIES = Map.of("Opaque", 1f, "70%", 0.7f, "40%", 0.4f);
    private static final List<String> OPACITY_ORDER = List.of("Opaque", "70%", "40%");

    private final Stage stage;
    private final ViewerController controller;
    private final Map<String, MenuItem> modeItems = new HashMap<>();
    private final Map<String, MenuItem> schemeItems = new HashMap<>();
    private final Map<SecondaryStructureSource, MenuItem> secondaryStructureItems =
            new EnumMap<>(SecondaryStructureSource.class);
    private final Map<Optional<SurfaceKind>, MenuItem> surfaceItems = new HashMap<>();
    private final Map<String, MenuItem> opacityItems = new HashMap<>();
    private final Map<DensityStyle, MenuItem> densityStyleItems = new EnumMap<>(DensityStyle.class);
    private final Map<String, MenuItem> densityOpacityItems = new HashMap<>();
    private final MenuItem closeMap;
    private final VisTable contourPanel = new VisTable();
    private final VisSlider contourSlider = new VisSlider(SLIDER_MIN_SIGMA, SLIDER_MAX_SIGMA, SLIDER_STEP_SIGMA,
            false);
    private final VisLabel contourLabel = new VisLabel("");
    private DensityMap sliderMap;
    private final VisLabel status = new VisLabel(HINT);
    private final VisLabel selection = new VisLabel("");
    private final VisLabel fps = new VisLabel("");
    private final VisTable legendPanel = new VisTable();
    private Legend shownLegend;
    private String loadingName;

    public ViewerUi(Stage stage, ViewerController controller, Actions actions) {
        this.stage = stage;
        this.controller = controller;

        MenuBar menuBar = new MenuBar();
        Menu file = new Menu("File");
        file.addItem(item("Open...", actions::openFile).setShortcut("Ctrl+O"));
        file.addItem(item("Quit", actions::quit));
        menuBar.addMenu(file);

        Menu display = new Menu("Display");
        for (DisplayMode mode : controller.registry().modes()) {
            MenuItem item = item(mode.displayName(), () -> {
                controller.setMode(mode.id());
                refresh();
            });
            modeItems.put(mode.id(), item);
            display.addItem(item);
        }
        display.addSeparator();
        for (SecondaryStructureSource source : SecondaryStructureSource.values()) {
            MenuItem item = item(secondaryStructureName(source), () -> {
                controller.setSecondaryStructureSource(source);
                refresh();
            });
            secondaryStructureItems.put(source, item);
            display.addItem(item);
        }
        menuBar.addMenu(display);

        Menu colour = new Menu("Colour");
        for (ColourScheme scheme : controller.registry().schemes()) {
            MenuItem item = item(scheme.displayName(), () -> {
                controller.setScheme(scheme.id());
                refresh();
            });
            schemeItems.put(scheme.id(), item);
            colour.addItem(item);
        }
        menuBar.addMenu(colour);

        Menu surface = new Menu("Surface");
        addSurfaceItem(surface, Optional.empty());
        for (SurfaceKind kind : SurfaceKind.values()) {
            addSurfaceItem(surface, Optional.of(kind));
        }
        surface.addSeparator();
        for (String label : OPACITY_ORDER) {
            MenuItem item = item(label, () -> {
                controller.setSurfaceOpacity(OPACITIES.get(label));
                refresh();
            });
            opacityItems.put(label, item);
            surface.addItem(item);
        }
        menuBar.addMenu(surface);

        Menu density = new Menu("Map");
        density.addItem(item("Open map...", actions::openFile));
        closeMap = item("Close map", () -> {
            controller.closeMap();
            refresh();
        });
        density.addItem(closeMap);
        density.addSeparator();
        for (DensityStyle style : DensityStyle.values()) {
            MenuItem item = item(style.displayName(), () -> {
                controller.setDensityStyle(style);
                refresh();
            });
            densityStyleItems.put(style, item);
            density.addItem(item);
        }
        density.addSeparator();
        for (String label : OPACITY_ORDER) {
            MenuItem item = item(label, () -> {
                controller.setDensityOpacity(OPACITIES.get(label));
                refresh();
            });
            densityOpacityItems.put(label, item);
            density.addItem(item);
        }
        density.addSeparator();
        density.addItem(item("Raise contour", () -> {
            controller.stepContourLevel(1);
            refresh();
        }).setShortcut("]"));
        density.addItem(item("Lower contour", () -> {
            controller.stepContourLevel(-1);
            refresh();
        }).setShortcut("["));
        menuBar.addMenu(density);

        Menu view = new Menu("View");
        view.addItem(item("Reset view", actions::resetView).setShortcut("R"));
        view.addItem(item("Clear selection", () -> controller.select(java.util.OptionalInt.empty()))
                .setShortcut("Esc"));
        menuBar.addMenu(view);

        Menu help = new Menu("Help");
        help.addItem(item("Controls", () -> Dialogs.showOKDialog(stage, "Controls", """
                Left-drag: rotate
                Right-drag or scroll: zoom
                Middle-drag or Shift + left-drag: pan
                Click: select atom
                R: reset view    Esc: clear selection    Ctrl/Cmd+O: open
                [ / ]: lower / raise the map contour (Shift: 5 steps)""")));
        help.addItem(item("About JMVS", () -> Dialogs.showOKDialog(stage, "About JMVS", """
                JMVS - Java Molecular Visualisation System
                A RasMol-like 3D molecular viewer.
                1999 dissertation, modernised with libGDX.
                https://github.com/PorridgeBear/jmvs""")));
        menuBar.addMenu(help);

        VisTable statusBar = new VisTable();
        statusBar.setBackground("window-bg");
        statusBar.add(status).left().padLeft(6);
        statusBar.add(selection).expandX().right().padRight(12);
        statusBar.add(fps).right().padRight(6).width(60);
        fps.setAlignment(Align.right);
        legendPanel.setBackground("window-bg");
        legendPanel.pad(6);
        legendPanel.setVisible(false);
        contourPanel.setBackground("window-bg");
        contourPanel.pad(6);
        contourPanel.add(new VisLabel("Contour")).padRight(8);
        contourPanel.add(contourSlider).width(220).padRight(8);
        contourPanel.add(contourLabel).width(150).left();
        contourPanel.setVisible(false);
        contourSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                controller.map().ifPresent(map -> {
                    controller.setContourLevel(map.statistics().level(contourSlider.getValue()));
                    refresh();
                });
            }
        });

        Table root = new Table();
        root.setFillParent(true);
        root.add(menuBar.getTable()).colspan(2).expandX().fillX().row();
        root.add(legendPanel).expand().bottom().left().pad(8);
        root.add(contourPanel).bottom().right().pad(8).row();
        root.add(statusBar).colspan(2).expandX().fillX();
        stage.addActor(root);
        refresh();
    }

    private void addSurfaceItem(Menu menu, Optional<SurfaceKind> kind) {
        MenuItem item = item(surfaceName(kind), () -> {
            controller.setSurface(kind);
            refresh();
        });
        surfaceItems.put(kind, item);
        menu.addItem(item);
    }

    private static String secondaryStructureName(SecondaryStructureSource source) {
        return "Secondary structure: " + source.displayName();
    }

    private static String surfaceName(Optional<SurfaceKind> kind) {
        return kind.map(SurfaceKind::displayName).orElse("None");
    }

    public void loading(String fileName) {
        loadingName = fileName;
        refresh();
    }

    public void error(String message) {
        loadingName = null;
        refresh();
        Dialogs.showErrorDialog(stage, message);
    }

    public void setFps(int framesPerSecond) {
        fps.setText(framesPerSecond + " fps");
    }

    /**
     * Updates menu markers and status text from the controller state.
     */
    public void refresh() {
        if (!controller.isLoading() && !controller.isMapLoading()) {
            loadingName = null;
        }
        String modeId = controller.mode().id();
        ColourScheme scheme = controller.scheme();
        controller.registry().modes().forEach(m ->
                modeItems.get(m.id()).setText((m.id().equals(modeId) ? CURRENT : OTHER) + m.displayName()));
        controller.registry().schemes().forEach(s -> {
            MenuItem item = schemeItems.get(s.id());
            item.setText((s.id().equals(scheme.id()) ? CURRENT : OTHER) + s.displayName());
            item.setDisabled(!controller.mode().supports(s));
        });
        secondaryStructureItems.forEach((source, item) -> item.setText(
                (source == controller.secondaryStructureSource() ? CURRENT : OTHER) + secondaryStructureName(source)));
        surfaceItems.forEach((kind, item) ->
                item.setText((kind.equals(controller.surfaceKind()) ? CURRENT : OTHER) + surfaceName(kind)));
        OPACITY_ORDER.forEach(label -> {
            MenuItem item = opacityItems.get(label);
            item.setText((OPACITIES.get(label) == controller.surfaceOpacity() ? CURRENT : OTHER) + label);
            item.setDisabled(controller.surfaceKind().isEmpty());
        });
        boolean hasMap = controller.map().isPresent();
        closeMap.setDisabled(!hasMap);
        densityStyleItems.forEach((style, item) ->
                item.setText((style == controller.densityStyle() ? CURRENT : OTHER) + style.displayName()));
        OPACITY_ORDER.forEach(label -> densityOpacityItems.get(label).setText(
                (OPACITIES.get(label) == controller.densityOpacity() ? CURRENT : OTHER) + label));
        refreshContourPanel();

        String text = controller.structure().map(s -> "%s  |  %d atoms, %d chains  |  %s, %s".formatted(
                s.name(), s.atoms().size(), s.distinctChainIds(), controller.mode().displayName(),
                scheme.displayName())).orElse(HINT);
        if (controller.isSecondaryStructureComputed()) {
            text += ", computed secondary structure";
        }
        if (controller.structure().isPresent() && controller.surfaceKind().isPresent()) {
            text += controller.isSurfaceBuilding() ? "  |  Computing surface..."
                    : "  |  " + controller.surfaceKind().get().displayName() + " surface";
        }
        if (controller.map().isPresent()) {
            DensityMap map = controller.map().get();
            text = (controller.structure().isPresent() ? text + "  |  " : "") + map.name() + "  "
                    + (controller.isDensityBuilding() && controller.density().isEmpty() ? "contouring..."
                    : contourText(map, controller.contourLevel()));
        }
        if (loadingName != null) {
            text = "Loading " + loadingName + "...";
        }
        status.setText(text);
        selection.setText(controller.selection().isPresent()
                ? controller.describe(controller.selection().getAsInt()) : "");
        showLegend(controller.legend().orElse(null));
    }

    private void refreshContourPanel() {
        Optional<DensityMap> map = controller.map();
        contourPanel.setVisible(map.isPresent());
        if (map.isEmpty()) {
            sliderMap = null;
            return;
        }
        DensityMap.Statistics stats = map.get().statistics();
        // Moving the slider from code must not set the level.
        contourSlider.setProgrammaticChangeEvents(false);
        if (sliderMap != map.get()) {
            sliderMap = map.get();
            // Up to the map's maximum, but at least a little above the current level.
            float top = Math.max(Math.min(SLIDER_MAX_SIGMA, stats.sigma(stats.max())),
                    stats.sigma(controller.contourLevel()) + 1);
            contourSlider.setRange(SLIDER_MIN_SIGMA, top);
        }
        contourSlider.setValue(stats.sigma(controller.contourLevel()));
        contourSlider.setProgrammaticChangeEvents(true);
        contourLabel.setText(contourText(map.get(), controller.contourLevel()));
    }

    /** Level and its standard deviations above the mean; "sigma" in words, as the UI font has no σ. */
    private static String contourText(DensityMap map, float level) {
        return String.format(Locale.ROOT, "%.4g (%.2f sigma)", level, map.statistics().sigma(level));
    }

    private void showLegend(Legend legend) {
        if (Objects.equals(legend, shownLegend)) {
            return;
        }
        shownLegend = legend;
        legendPanel.clearChildren();
        legendPanel.setVisible(legend != null);
        if (legend == null) {
            return;
        }
        legendPanel.add(new VisLabel(legend.title())).left().colspan(2).padBottom(4).row();
        switch (legend) {
            case Legend.Bands bands -> bands.entries().forEach(e -> {
                legendPanel.add(new Image(swatch(e.rgb()))).size(14).padRight(6);
                legendPanel.add(new VisLabel(e.label())).left().row();
            });
            case Legend.Gradient gradient -> {
                VisTable bar = new VisTable();
                for (int i = 0; i < GRADIENT_STEPS; i++) {
                    bar.add(new Image(swatch(sample(gradient.stops(), i / (GRADIENT_STEPS - 1f))))).size(5, 14);
                }
                legendPanel.add(bar).colspan(2).left().row();
                VisTable labels = new VisTable();
                labels.add(new VisLabel(String.format(Locale.ROOT, "%.1f", gradient.min()))).left().expandX();
                labels.add(new VisLabel(String.format(Locale.ROOT, "%.1f", gradient.max()))).right();
                legendPanel.add(labels).colspan(2).fillX();
            }
        }
    }

    private static Drawable swatch(int rgb) {
        return VisUI.getSkin().newDrawable("white", new Color(Rgb.red(rgb), Rgb.green(rgb), Rgb.blue(rgb), 1));
    }

    /** Colour at {@code t} (0–1) along evenly spaced gradient stops. */
    private static int sample(List<Integer> stops, float t) {
        float position = t * (stops.size() - 1);
        int i = Math.min((int) position, stops.size() - 2);
        float f = position - i;
        int a = stops.get(i);
        int b = stops.get(i + 1);
        return Rgb.of(Math.round(((a >> 16) & 0xFF) * (1 - f) + ((b >> 16) & 0xFF) * f),
                Math.round(((a >> 8) & 0xFF) * (1 - f) + ((b >> 8) & 0xFF) * f),
                Math.round((a & 0xFF) * (1 - f) + (b & 0xFF) * f));
    }

    private static MenuItem item(String text, Runnable action) {
        return new MenuItem(text, new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
    }
}
