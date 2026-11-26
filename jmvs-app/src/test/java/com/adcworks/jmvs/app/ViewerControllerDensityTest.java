package com.adcworks.jmvs.app;

import com.adcworks.jmvs.core.chem.ElementTable;
import com.adcworks.jmvs.core.io.StructureReaders;
import com.adcworks.jmvs.core.map.DensityMap;
import com.adcworks.jmvs.scene.Registry;
import com.adcworks.jmvs.scene.Scene;
import com.adcworks.jmvs.scene.TriangleMesh;
import com.adcworks.jmvs.scene.density.ContourLevel;
import com.adcworks.jmvs.scene.density.DensityStyle;
import com.adcworks.jmvs.scene.density.Isosurface;
import com.adcworks.jmvs.scene.surface.Surface;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Density maps are read and contoured on the background executor, which these tests run by hand to control ordering.
 */
class ViewerControllerDensityTest {

    private static final Path DATA = Path.of(System.getProperty("jmvs.dataDir"));
    private static final String MAP = "emd_3488.map.gz";

    private final List<Runnable> background = new ArrayList<>();
    private final List<String> events = new ArrayList<>();
    private final List<Optional<Isosurface>> contours = new ArrayList<>();

    private final ViewerController controller = new ViewerController(StructureReaders.defaults(ElementTable.load()),
            Registry.load(), background::add, Runnable::run, new ViewerController.Listener() {
        public void loadingStarted(String fileName) {
            events.add("loading " + fileName);
        }

        public void structureShown(Scene scene, int[] colours) {
            events.add("shown");
        }

        public void sceneChanged(Scene scene, int[] colours) {
        }

        public void coloursChanged(int[] colours) {
        }

        public void selectionChanged(OptionalInt atom) {
        }

        public void loadFailed(String message) {
            events.add("failed " + message);
        }

        public void surfaceChanged(Optional<Surface> surface) {
        }

        public void surfaceOpacityChanged(float opacity) {
        }

        public void mapShown(DensityMap map) {
            events.add("map " + map.name());
        }

        public void densityChanged(Optional<Isosurface> isosurface) {
            contours.add(isosurface);
            events.add("density " + isosurface.map(i -> "" + i.level()).orElse("none"));
        }

        public void densityStyleChanged(DensityStyle style) {
            events.add("style " + style.id());
        }

        public void densityOpacityChanged(float opacity) {
            events.add("opacity " + opacity);
        }
    });

    private void runBackground() {
        while (!background.isEmpty()) {
            background.removeFirst().run();
        }
    }

    private static boolean anyAtom(Isosurface iso) {
        return iso.meshes().stream().map(TriangleMesh::atoms).flatMapToInt(Arrays::stream)
                .anyMatch(a -> a != Isosurface.NO_ATOM);
    }

    @Test
    void mapIsReadAndContouredAtTheDefaultLevel() {
        controller.openMap(DATA.resolve(MAP));
        assertThat(controller.isMapLoading()).isTrue();
        assertThat(controller.map()).isEmpty();
        runBackground();

        DensityMap map = controller.map().orElseThrow();
        assertThat(controller.isMapLoading()).isFalse();
        assertThat(controller.isDensityBuilding()).isFalse();
        assertThat(controller.contourLevel()).isCloseTo(map.statistics().level(2.5f), within(1e-6f));
        assertThat(events).containsExactly("loading " + MAP, "map emd_3488.map",
                "density " + controller.contourLevel());
        Isosurface iso = controller.density().orElseThrow();
        assertThat(iso.triangleCount()).isGreaterThan(10_000);
        // No structure: every vertex has the map colour.
        assertThat(anyAtom(iso)).isFalse();
    }

    @Test
    void requestedContourLevelResolvesWhenTheMapIsRead() {
        controller.openMap(DATA.resolve(MAP), Optional.of(new ContourLevel.Absolute(0.09f)));
        runBackground();
        assertThat(controller.contourLevel()).isEqualTo(0.09f);
        assertThat(controller.density().orElseThrow().level()).isEqualTo(0.09f);
    }

    @Test
    void newLevelSupersedesTheBuildInProgressAndKeepsTheOldContourMeanwhile() {
        controller.openMap(DATA.resolve(MAP));
        runBackground();
        Isosurface first = controller.density().orElseThrow();
        events.clear();

        controller.setContourLevel(0.1f);
        controller.setContourLevel(0.12f);
        assertThat(controller.isDensityBuilding()).isTrue();
        assertThat(controller.density()).containsSame(first);
        runBackground();

        assertThat(controller.isDensityBuilding()).isFalse();
        assertThat(controller.density().orElseThrow().level()).isEqualTo(0.12f);
        assertThat(events).containsExactly("density 0.12");
    }

    @Test
    void stepsAreTenthsOfASigma() {
        controller.openMap(DATA.resolve(MAP));
        runBackground();
        float level = controller.contourLevel();
        float rms = controller.map().orElseThrow().statistics().rms();
        controller.stepContourLevel(3);
        assertThat(controller.contourLevel()).isCloseTo(level + 0.3f * rms, within(1e-6f));
    }

    @Test
    void contourIsZoneColouredByTheModelAndKeptWhenAModelOpens() {
        controller.openMap(DATA.resolve(MAP));
        runBackground();
        Isosurface grey = controller.density().orElseThrow();
        events.clear();

        controller.open(DATA.resolve("5me2.cif"));
        runBackground();

        assertThat(controller.map()).isPresent();
        Isosurface zoned = controller.density().orElseThrow();
        assertThat(anyAtom(zoned)).isTrue();
        // Same geometry: only the vertex atoms changed.
        assertThat(zoned.meshes().getFirst().positions()).isSameAs(grey.meshes().getFirst().positions());
        assertThat(events).containsExactly("loading 5me2.cif", "density " + grey.level(), "shown",
                "density " + grey.level());
    }

    @Test
    void openingAnotherModelResetsZonesBeforeShowingIt() {
        controller.open(DATA.resolve("5me2.cif"));
        runBackground();
        controller.openMap(DATA.resolve(MAP));
        runBackground();
        assertThat(anyAtom(controller.density().orElseThrow())).isTrue();
        contours.clear();

        controller.open(DATA.resolve("1crn.cif"));
        runBackground();

        // First the map colour (old atom indices are meaningless), then zones for crambin, far from the map.
        assertThat(contours).hasSize(2);
        assertThat(anyAtom(contours.get(0).orElseThrow())).isFalse();
    }

    @Test
    void closingTheMapRemovesTheContourAndCancelsBuilds() {
        controller.openMap(DATA.resolve(MAP));
        runBackground();
        controller.setContourLevel(0.2f);
        controller.closeMap();
        runBackground();

        assertThat(controller.map()).isEmpty();
        assertThat(controller.density()).isEmpty();
        assertThat(controller.isDensityBuilding()).isFalse();
        assertThat(contours.getLast()).isEmpty();
    }

    @Test
    void missingMapReportsAndKeepsTheCurrentOne() {
        controller.openMap(DATA.resolve(MAP));
        runBackground();
        DensityMap shown = controller.map().orElseThrow();

        controller.openMap(DATA.resolve("missing.map"));
        runBackground();

        assertThat(controller.map()).containsSame(shown);
        assertThat(controller.isMapLoading()).isFalse();
        assertThat(events.getLast()).startsWith("failed missing.map").contains("not found");
    }

    @Test
    void styleAndOpacity() {
        assertThat(controller.densityStyle()).isEqualTo(DensityStyle.MESH);
        controller.setDensityStyle(DensityStyle.SURFACE);
        controller.setDensityStyle(DensityStyle.SURFACE);
        controller.setDensityOpacity(1.5f);
        controller.setDensityOpacity(0.4f);
        assertThat(controller.densityStyle()).isEqualTo(DensityStyle.SURFACE);
        assertThat(controller.densityOpacity()).isEqualTo(0.4f);
        assertThat(events).containsExactly("style surface", "opacity 0.4");
    }

    @Test
    void recognisesMapFiles() {
        assertThat(ViewerController.isMapFile("emd_3488.map.gz")).isTrue();
        assertThat(ViewerController.isMapFile("5me2.cif")).isFalse();
    }
}
