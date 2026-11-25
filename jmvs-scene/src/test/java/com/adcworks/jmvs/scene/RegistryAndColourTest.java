package com.adcworks.jmvs.scene;

import com.adcworks.jmvs.core.config.ConfigurationException;
import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.SecondaryStructure;
import com.adcworks.jmvs.core.model.Structure;
import com.adcworks.jmvs.scene.colour.ColourScheme;
import com.adcworks.jmvs.scene.colour.ColourTables;
import com.adcworks.jmvs.scene.colour.Rgb;
import com.adcworks.jmvs.scene.config.ViewerConfig;
import com.adcworks.jmvs.scene.mode.DisplayMode;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistryAndColourTest {

    private static final Structure USC = Samples.read("1usc.cif");
    private final Registry registry = Registry.load();

    @Test
    void registersConfiguredModesAndSchemesInOrderWithDefaults() {
        assertThat(registry.modes()).extracting(DisplayMode::id)
                .containsExactly("balls", "sticks", "ballsandsticks", "ribbons");
        assertThat(registry.schemes()).extracting(ColourScheme::id)
                .containsExactly("cpk", "chain", "group", "structure");
        assertThat(registry.defaultMode().id()).isEqualTo("balls");
        assertThat(registry.defaultScheme().id()).isEqualTo("cpk");
    }

    @Test
    void cpkColoursByElement() {
        Atom carbon = USC.atoms().stream().filter(a -> a.element().symbol().equals("C")).findFirst().orElseThrow();

        assertThat(registry.scheme("cpk").colourOf(USC, carbon)).isEqualTo(Rgb.of(200, 200, 200));
    }

    @Test
    void chainColoursAreStableAndDistinctPerChain() {
        int[] first = registry.scheme("chain").colours(USC);
        int[] second = Registry.load().scheme("chain").colours(USC);

        assertThat(first).isEqualTo(second);
        int chainA = first[USC.residuesOf(USC.chains().get(0)).getFirst().firstAtom()];
        int chainB = first[USC.residuesOf(USC.chains().get(1)).getFirst().firstAtom()];
        assertThat(chainA).isNotEqualTo(chainB);
    }

    @Test
    void structureAndGroupColours() {
        Atom helixAtom = USC.atoms().stream()
                .filter(a -> USC.residueOf(a).secondaryStructure() == SecondaryStructure.HELIX)
                .findFirst().orElseThrow();
        Atom lysine = USC.atoms().stream().filter(a -> USC.residueOf(a).name().equals("LYS"))
                .findFirst().orElseThrow();

        assertThat(registry.scheme("structure").colourOf(USC, helixAtom)).isEqualTo(Rgb.of(240, 0, 128));
        assertThat(registry.scheme("group").colourOf(USC, lysine)).isEqualTo(Rgb.of(20, 90, 255));
    }

    @Test
    void rejectsInvalidConfiguration() {
        Properties p = ViewerConfig.loadProperties();
        p.setProperty("display.modes", "balls,cartoon");
        assertThatThrownBy(() -> Registry.create(ViewerConfig.parse(p), ColourTables.parse(p)))
                .isInstanceOf(ConfigurationException.class).hasMessageContaining("cartoon");

        Properties q = ViewerConfig.loadProperties();
        q.setProperty("display.default", "ribbons2");
        assertThatThrownBy(() -> ViewerConfig.parse(q)).isInstanceOf(ConfigurationException.class);

        Properties r = ViewerConfig.loadProperties();
        r.setProperty("cpk.0", "300,0,0");
        assertThatThrownBy(() -> ColourTables.parse(r))
                .isInstanceOf(ConfigurationException.class).hasMessageContaining("cpk.0");
    }
}
