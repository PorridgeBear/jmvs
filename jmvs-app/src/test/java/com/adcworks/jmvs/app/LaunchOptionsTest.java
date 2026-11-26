package com.adcworks.jmvs.app;

import com.adcworks.jmvs.core.model.SecondaryStructureSource;
import com.adcworks.jmvs.scene.density.ContourLevel;
import com.adcworks.jmvs.scene.density.DensityStyle;
import com.adcworks.jmvs.scene.surface.SurfaceKind;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class LaunchOptionsTest {

    @Test
    void parsesSecondaryStructureAndSurfaceOptions() {
        LaunchOptions options = LaunchOptions.parse(new String[]{"data/4hhb.cif", "--mode", "ribbons", "--ss",
                "computed", "--surface", "ses", "--surface-opacity", "0.5", "--screenshot", "out.png"});

        assertThat(options.file()).contains(Path.of("data/4hhb.cif"));
        assertThat(options.mode()).contains("ribbons");
        assertThat(options.secondaryStructure()).contains(SecondaryStructureSource.COMPUTED);
        assertThat(options.surface()).contains(SurfaceKind.SES);
        assertThat(options.surfaceOpacity()).contains(0.5f);
        assertThat(options.screenshot()).contains(Path.of("out.png"));
    }

    @Test
    void secondaryStructureAndSurfaceAreOptional() {
        LaunchOptions options = LaunchOptions.parse(new String[]{"data/1crn.cif"});
        assertThat(options.secondaryStructure()).isEmpty();
        assertThat(options.surface()).isEmpty();
        assertThat(options.surfaceOpacity()).isEmpty();
    }

    @Test
    void rejectsUnknownSurfaceAndOpacityOutOfRange() {
        assertThatIllegalArgumentException().isThrownBy(() -> LaunchOptions.parse(new String[]{"--surface", "vdw"}))
                .withMessageContaining("ses or sas");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> LaunchOptions.parse(new String[]{"--surface-opacity", "1.5"}));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> LaunchOptions.parse(new String[]{"--surface-opacity", "half"}));
    }

    @Test
    void parsesDensityMapOptions() {
        LaunchOptions options = LaunchOptions.parse(new String[]{"data/5me2.cif", "--map", "data/emd_3488.map.gz",
                "--contour", "0.09", "--map-style", "surface", "--map-opacity", "0.7", "--screenshot", "out.png"});

        assertThat(options.file()).contains(Path.of("data/5me2.cif"));
        assertThat(options.map()).contains(Path.of("data/emd_3488.map.gz"));
        assertThat(options.contour()).contains(new ContourLevel.Absolute(0.09f));
        assertThat(options.mapStyle()).contains(DensityStyle.SURFACE);
        assertThat(options.mapOpacity()).contains(0.7f);
    }

    @Test
    void positionalMapFilesAreMapsAndAreEnoughForAScreenshot() {
        LaunchOptions options = LaunchOptions.parse(new String[]{"emd_3488.map.gz", "5me2.cif", "--contour",
                "3sigma"});
        assertThat(options.map()).contains(Path.of("emd_3488.map.gz"));
        assertThat(options.file()).contains(Path.of("5me2.cif"));
        assertThat(options.contour()).contains(new ContourLevel.Sigma(3f));

        LaunchOptions mapOnly = LaunchOptions.parse(new String[]{"x.mrc", "--screenshot", "out.png"});
        assertThat(mapOnly.file()).isEmpty();
        assertThat(mapOnly.map()).contains(Path.of("x.mrc"));
    }

    @Test
    void rejectsInvalidDensityMapOptions() {
        assertThatIllegalArgumentException().isThrownBy(() -> LaunchOptions.parse(new String[]{"--contour", "high"}))
                .withMessageContaining("--contour");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> LaunchOptions.parse(new String[]{"--map-style", "dots"}))
                .withMessageContaining("surface or mesh");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> LaunchOptions.parse(new String[]{"--map-opacity", "2"}));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> LaunchOptions.parse(new String[]{"--screenshot", "out.png"}));
    }

    @Test
    void rejectsUnknownSecondaryStructureSource() {
        assertThatIllegalArgumentException().isThrownBy(() -> LaunchOptions.parse(new String[]{"--ss", "dssp"}))
                .withMessageContaining("auto, file or computed");
    }
}
