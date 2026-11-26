package com.adcworks.jmvs.app;

import com.adcworks.jmvs.core.chem.ElementTable;
import com.adcworks.jmvs.core.io.StructureReaders;
import com.adcworks.jmvs.core.model.SecondaryStructure;
import com.adcworks.jmvs.core.model.SecondaryStructureSource;
import com.adcworks.jmvs.scene.Registry;
import com.adcworks.jmvs.scene.Scene;
import com.adcworks.jmvs.scene.surface.Surface;
import com.adcworks.jmvs.scene.surface.SurfaceKind;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Secondary structure is applied on the background executor, which these tests run by hand to control ordering.
 */
class ViewerControllerSecondaryStructureTest {

    private static final Path DATA = Path.of(System.getProperty("jmvs.dataDir"));
    private static final Optional<SurfaceKind> SES = Optional.of(SurfaceKind.SES);

    private final List<Runnable> background = new ArrayList<>();
    private final List<String> events = new ArrayList<>();
    private final List<Optional<Surface>> surfaces = new ArrayList<>();
    private final List<Scene> scenes = new ArrayList<>();

    private final ViewerController controller = new ViewerController(StructureReaders.defaults(ElementTable.load()),
            Registry.load(), background::add, Runnable::run, new ViewerController.Listener() {
        public void loadingStarted(String fileName) {
            events.add("loading " + fileName);
        }

        public void structureShown(Scene scene, int[] colours) {
            events.add("shown");
        }

        public void sceneChanged(Scene scene, int[] colours) {
            scenes.add(scene);
            events.add("scene");
        }

        public void coloursChanged(int[] colours) {
            events.add("colours");
        }

        public void selectionChanged(OptionalInt atom) {
        }

        public void loadFailed(String message) {
            events.add("failed " + message);
        }

        public void surfaceChanged(Optional<Surface> surface) {
            surfaces.add(surface);
            events.add("surface " + surface.map(s -> s.kind().id()).orElse("none"));
        }

        public void surfaceOpacityChanged(float opacity) {
            events.add("opacity " + opacity);
        }
    });

    private void runBackground() {
        while (!background.isEmpty()) {
            background.removeFirst().run();
        }
    }

    private void open(String file) {
        controller.open(DATA.resolve(file));
        runBackground();
    }

    private long helices() {
        return controller.structure().orElseThrow().countResidues(SecondaryStructure.HELIX);
    }

    @Test
    void autoComputesForAFileWithoutAnnotation() {
        open("AF-P69905-F1-model_v6-noss.cif");

        assertThat(controller.secondaryStructureSource()).isEqualTo(SecondaryStructureSource.AUTO);
        assertThat(controller.isSecondaryStructureComputed()).isTrue();
        assertThat(helices()).isEqualTo(106);
    }

    @Test
    void autoKeepsTheFileAnnotation() {
        open("1crn.cif");

        assertThat(controller.isSecondaryStructureComputed()).isFalse();
        assertThat(helices()).isEqualTo(21);
    }

    @Test
    void switchingSourceRebuildsTheSceneInTheBackground() {
        controller.setMode("ribbons");
        open("1crn.cif");
        events.clear();

        controller.setSecondaryStructureSource(SecondaryStructureSource.COMPUTED);
        assertThat(events).isEmpty();
        runBackground();

        assertThat(events).containsExactly("scene");
        assertThat(controller.isSecondaryStructureComputed()).isTrue();
        assertThat(helices()).isEqualTo(22);
        assertThat(scenes.getLast().meshes()).isNotEmpty();

        controller.setSecondaryStructureSource(SecondaryStructureSource.FILE);
        runBackground();
        assertThat(helices()).isEqualTo(21);
        assertThat(controller.isSecondaryStructureComputed()).isFalse();
    }

    @Test
    void sameSourceDoesNothing() {
        open("1crn.cif");
        events.clear();

        controller.setSecondaryStructureSource(SecondaryStructureSource.AUTO);

        assertThat(background).isEmpty();
        assertThat(events).isEmpty();
    }

    @Test
    void sourceChosenBeforeOpeningIsAppliedOnLoad() {
        controller.setSecondaryStructureSource(SecondaryStructureSource.FILE);
        assertThat(background).isEmpty();

        open("AF-P69905-F1-model_v6-noss.cif");

        assertThat(helices()).isZero();
    }

    @Test
    void sourceChangedWhileLoadingIsAppliedAfterwards() {
        controller.open(DATA.resolve("AF-P69905-F1-model_v6-noss.cif"));
        controller.setSecondaryStructureSource(SecondaryStructureSource.FILE);
        runBackground();

        assertThat(controller.secondaryStructureSource()).isEqualTo(SecondaryStructureSource.FILE);
        assertThat(helices()).isZero();
        assertThat(events).endsWith("scene");
    }

    @Test
    void surfaceSurvivesASourceSwitch() {
        open("1crn.cif");
        controller.setSurface(SES);
        controller.setSecondaryStructureSource(SecondaryStructureSource.COMPUTED);
        runBackground();

        assertThat(surfaces.getLast()).isPresent();
        assertThat(controller.isSurfaceBuilding()).isFalse();
    }
}
