package com.adcworks.jmvs.app;

import com.adcworks.jmvs.core.chem.ElementTable;
import com.adcworks.jmvs.core.io.StructureReaders;
import com.adcworks.jmvs.scene.Registry;
import com.adcworks.jmvs.scene.Scene;
import com.adcworks.jmvs.scene.surface.Surface;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

class ViewerControllerTest {

    private static final Path DATA = Path.of(System.getProperty("jmvs.dataDir"));

    private final List<String> events = new ArrayList<>();
    private Scene lastScene;

    private final ViewerController controller = new ViewerController(
            StructureReaders.defaults(ElementTable.load()), Registry.load(), Runnable::run, Runnable::run,
            new ViewerController.Listener() {
                @Override
                public void loadingStarted(String fileName) {
                    events.add("loading " + fileName);
                }

                @Override
                public void structureShown(Scene scene, int[] colours) {
                    lastScene = scene;
                    events.add("shown");
                }

                @Override
                public void sceneChanged(Scene scene, int[] colours) {
                    lastScene = scene;
                    events.add("scene");
                }

                @Override
                public void coloursChanged(int[] colours) {
                    events.add("colours");
                }

                @Override
                public void selectionChanged(OptionalInt atom) {
                    events.add("selection " + atom);
                }

                @Override
                public void loadFailed(String message) {
                    events.add("failed " + message);
                }

                @Override
                public void surfaceChanged(Optional<Surface> surface) {
                }

                @Override
                public void surfaceOpacityChanged(float opacity) {
                }
            });

    @Test
    void opensStructureWithDefaultModeAndScheme() {
        controller.open(DATA.resolve("1crn.cif"));

        assertThat(events).containsExactly("loading 1crn.cif", "shown", "selection OptionalInt.empty");
        assertThat(controller.structure()).hasValueSatisfying(s -> assertThat(s.atoms()).hasSize(327));
        assertThat(controller.mode().id()).isEqualTo("balls");
        assertThat(controller.scheme().id()).isEqualTo("cpk");
        assertThat(lastScene.spheres().count()).isEqualTo(327);
    }

    @Test
    void changingSchemeOnlyRecoloursAndChangingModeRebuildsScene() {
        controller.open(DATA.resolve("1crn.cif"));
        events.clear();

        controller.setScheme("chain");
        controller.setMode("sticks");

        assertThat(events).containsExactly("colours", "scene");
        assertThat(lastScene.cylinders().count()).isEqualTo(2 * 337);
    }

    @Test
    void fallsBackToSupportedSchemeAndRestoresPreference() {
        controller.open(DATA.resolve("1crn.cif"));

        controller.setMode("ribbons");
        assertThat(controller.scheme().id()).isEqualTo("chain");

        controller.setMode("balls");
        assertThat(controller.scheme().id()).isEqualTo("cpk");
    }

    @Test
    void invalidFileReportsErrorAndKeepsCurrentStructure(@TempDir Path dir) throws IOException {
        controller.open(DATA.resolve("1crn.cif"));
        Path broken = Files.writeString(dir.resolve("broken.cif"), "data_X\n_struct.title nothing\n");
        events.clear();

        controller.open(broken);

        assertThat(events).hasSize(2);
        assertThat(events.get(1)).startsWith("failed broken.cif: ");
        assertThat(controller.structure()).hasValueSatisfying(s -> assertThat(s.name()).isEqualTo("1crn.cif"));
        assertThat(controller.isLoading()).isFalse();
    }

    @Test
    void appliesModeAndSchemeChosenWhileLoading() {
        List<Runnable> pending = new ArrayList<>();
        List<int[]> shownColours = new ArrayList<>();
        List<Scene> shownScenes = new ArrayList<>();
        ViewerController deferred = new ViewerController(StructureReaders.defaults(ElementTable.load()),
                Registry.load(), pending::add, Runnable::run, new ViewerController.Listener() {
            public void loadingStarted(String fileName) { }
            public void structureShown(Scene scene, int[] colours) {
                shownScenes.add(scene);
                shownColours.add(colours);
            }
            public void sceneChanged(Scene scene, int[] colours) { }
            public void coloursChanged(int[] colours) { }
            public void selectionChanged(OptionalInt atom) { }
            public void loadFailed(String message) { }
            public void surfaceChanged(Optional<Surface> surface) { }
            public void surfaceOpacityChanged(float opacity) { }
        });

        deferred.open(DATA.resolve("1crn.cif"));
        deferred.setMode("sticks");
        deferred.setScheme("structure");
        pending.forEach(Runnable::run);

        var structure = deferred.structure().orElseThrow();
        assertThat(shownScenes.getFirst().cylinders().count()).isEqualTo(2 * 337);
        assertThat(shownColours.getFirst()).isEqualTo(Registry.load().scheme("structure").colours(structure));
    }

    @Test
    void selectionIsDescribed() {
        controller.open(DATA.resolve("1crn.cif"));

        controller.select(OptionalInt.of(1));

        assertThat(controller.selection()).hasValue(1);
        assertThat(controller.describe(1)).startsWith("CA (Carbon)  THR 1  chain A  (").endsWith("  B-factor 10.80");

        controller.setScheme("confidence");
        assertThat(controller.describe(1)).endsWith("  pLDDT 10.8");
    }

    @Test
    void legendFollowsTheSchemeAndStructure() {
        assertThat(controller.legend()).isEmpty();
        controller.open(DATA.resolve("AF-P69905-F1-model_v6.cif"));
        assertThat(controller.legend()).isEmpty();

        controller.setScheme("confidence");

        assertThat(controller.legend()).hasValueSatisfying(l -> assertThat(l.title()).contains("pLDDT"));
    }
}
