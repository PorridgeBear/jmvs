package com.adcworks.jmvs.app;

import com.adcworks.jmvs.core.chem.ElementTable;
import com.adcworks.jmvs.core.io.StructureReaders;
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
 * Surfaces are built on the background executor, which these tests run by hand to control ordering.
 */
class ViewerControllerSurfaceTest {

    private static final Path DATA = Path.of(System.getProperty("jmvs.dataDir"));
    private static final Optional<SurfaceKind> SES = Optional.of(SurfaceKind.SES);
    private static final Optional<SurfaceKind> SAS = Optional.of(SurfaceKind.SAS);

    private final List<Runnable> background = new ArrayList<>();
    private final List<String> events = new ArrayList<>();
    private final List<Optional<Surface>> surfaces = new ArrayList<>();

    private final ViewerController controller = new ViewerController(StructureReaders.defaults(ElementTable.load()),
            Registry.load(), background::add, Runnable::run, new ViewerController.Listener() {
        public void loadingStarted(String fileName) {
            events.add("loading " + fileName);
        }

        public void structureShown(Scene scene, int[] colours) {
            events.add("shown");
        }

        public void sceneChanged(Scene scene, int[] colours) {
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

    @Test
    void surfaceIsBuiltInTheBackground() {
        open("1crn.cif");
        events.clear();

        controller.setSurface(SES);

        assertThat(controller.isSurfaceBuilding()).isTrue();
        assertThat(events).containsExactly("surface none");
        assertThat(background).hasSize(1);
        runBackground();
        assertThat(controller.isSurfaceBuilding()).isFalse();
        assertThat(events).containsExactly("surface none", "surface ses");
        assertThat(surfaces.getLast()).hasValueSatisfying(s -> assertThat(s.triangleCount()).isPositive());
    }

    @Test
    void surfacesAreCachedPerKindAndSchemeChangesOnlyRecolour() {
        open("1crn.cif");
        controller.setSurface(SES);
        runBackground();
        Surface ses = surfaces.getLast().orElseThrow();
        controller.setSurface(SAS);
        runBackground();
        events.clear();

        controller.setSurface(SES);
        controller.setScheme("chain");
        controller.setMode("sticks");

        assertThat(background).isEmpty();
        assertThat(events).containsExactly("surface ses", "colours", "scene");
        assertThat(surfaces.getLast()).containsSame(ses);
    }

    @Test
    void supersededBuildIsDropped() {
        open("1crn.cif");
        controller.setSurface(SES);
        controller.setSurface(SAS);
        events.clear();

        runBackground();

        assertThat(events).containsExactly("surface sas");
    }

    @Test
    void turningTheSurfaceOffWhileBuildingCancelsIt() {
        open("1crn.cif");
        controller.setSurface(SES);
        controller.setSurface(Optional.empty());
        events.clear();

        runBackground();

        assertThat(events).isEmpty();
        assertThat(controller.isSurfaceBuilding()).isFalse();
    }

    @Test
    void chosenSurfaceIsRebuiltForTheNextFile() {
        open("1crn.cif");
        controller.setSurface(SES);
        runBackground();
        Surface crambin = surfaces.getLast().orElseThrow();
        events.clear();

        controller.open(DATA.resolve("1usc.cif"));
        assertThat(controller.isSurfaceBuilding()).isFalse();
        runBackground();

        assertThat(events).containsExactly("loading 1usc.cif", "shown", "surface none", "surface ses");
        assertThat(surfaces.getLast()).hasValueSatisfying(s -> assertThat(s).isNotSameAs(crambin));
    }

    @Test
    void surfaceChosenBeforeOpeningIsBuiltAfterLoad() {
        controller.setSurface(SAS);
        assertThat(events).isEmpty();

        open("1crn.cif");

        assertThat(events).containsExactly("loading 1crn.cif", "shown", "surface none", "surface sas");
    }

    @Test
    void opacityIsClampedAndReported() {
        controller.setSurfaceOpacity(0.4f);
        controller.setSurfaceOpacity(0.4f);
        controller.setSurfaceOpacity(3f);

        assertThat(events).containsExactly("opacity 0.4", "opacity 1.0");
        assertThat(controller.surfaceOpacity()).isEqualTo(1f);
    }
}
