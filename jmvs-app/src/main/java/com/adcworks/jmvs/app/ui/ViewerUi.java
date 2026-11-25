package com.adcworks.jmvs.app.ui;

import com.adcworks.jmvs.app.ViewerController;
import com.adcworks.jmvs.scene.colour.ColourScheme;
import com.adcworks.jmvs.scene.mode.DisplayMode;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.util.dialog.Dialogs;
import com.kotcrab.vis.ui.widget.Menu;
import com.kotcrab.vis.ui.widget.MenuBar;
import com.kotcrab.vis.ui.widget.MenuItem;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;

import java.util.HashMap;
import java.util.Map;

/**
 * Scene2D overlay: menu bar (File, Display, Colour, View, Help) and a status bar.
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
    private static final String HINT = "Open a structure with File > Open or drop a file on the window";

    private final Stage stage;
    private final ViewerController controller;
    private final Map<String, MenuItem> modeItems = new HashMap<>();
    private final Map<String, MenuItem> schemeItems = new HashMap<>();
    private final VisLabel status = new VisLabel(HINT);
    private final VisLabel selection = new VisLabel("");
    private final VisLabel fps = new VisLabel("");
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
                R: reset view    Esc: clear selection    Ctrl/Cmd+O: open""")));
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

        Table root = new Table();
        root.setFillParent(true);
        root.add(menuBar.getTable()).expandX().fillX().row();
        root.add().expand().fill().row();
        root.add(statusBar).expandX().fillX();
        stage.addActor(root);
        refresh();
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
        if (!controller.isLoading()) {
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

        String text = controller.structure().map(s -> "%s  |  %d atoms, %d chains  |  %s, %s".formatted(
                s.name(), s.atoms().size(), s.distinctChainIds(), controller.mode().displayName(),
                scheme.displayName())).orElse(HINT);
        if (loadingName != null) {
            text = "Loading " + loadingName + "...";
        }
        status.setText(text);
        selection.setText(controller.selection().isPresent()
                ? controller.describe(controller.selection().getAsInt()) : "");
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
