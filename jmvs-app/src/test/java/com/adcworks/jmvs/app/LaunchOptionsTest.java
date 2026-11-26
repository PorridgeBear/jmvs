package com.adcworks.jmvs.app;

import com.adcworks.jmvs.core.model.SecondaryStructureSource;
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
    void rejectsUnknownSecondaryStructureSource() {
        assertThatIllegalArgumentException().isThrownBy(() -> LaunchOptions.parse(new String[]{"--ss", "dssp"}))
                .withMessageContaining("auto, file or computed");
    }
}
