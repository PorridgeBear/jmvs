package com.adcworks.jmvs.scene;

import com.adcworks.jmvs.core.chem.ElementTable;
import com.adcworks.jmvs.core.config.ConfigurationException;
import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.Element;
import com.adcworks.jmvs.core.model.SecondaryStructure;
import com.adcworks.jmvs.core.model.Structure;
import com.adcworks.jmvs.core.model.StructureBuilder;
import com.adcworks.jmvs.scene.colour.ColourScheme;
import com.adcworks.jmvs.scene.colour.ColourTables;
import com.adcworks.jmvs.scene.colour.Legend;
import com.adcworks.jmvs.scene.colour.Rgb;
import com.adcworks.jmvs.scene.config.ViewerConfig;
import com.adcworks.jmvs.scene.mode.DisplayMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistryAndColourTest {

    private static final Structure USC = Samples.read("1usc.cif");
    private final Registry registry = Registry.load();

    @Test
    void registersConfiguredModesAndSchemesInOrderWithDefaults() {
        assertThat(registry.modes()).extracting(DisplayMode::id)
                .containsExactly("balls", "sticks", "ballsandsticks", "ribbons", "cartoon");
        assertThat(registry.schemes()).extracting(ColourScheme::id)
                .containsExactly("cpk", "chain", "group", "structure", "bfactor", "confidence");
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

    /** Carbon atoms in a row, one per B-factor ({@code NaN} for none). */
    private static Structure withBFactors(float... bFactors) {
        StructureBuilder builder = new StructureBuilder("b");
        Element carbon = ElementTable.load().bySymbol("C");
        for (int i = 0; i < bFactors.length; i++) {
            builder.addAtom(1, "A", "A", 1, 1, "", "GLY", i + 1, "C" + i, carbon, i * 10f, 0, 0, false,
                    bFactors[i]);
        }
        return builder.build();
    }

    @Test
    void confidenceUsesAlphaFoldBandsWithExclusiveUpperBounds() {
        Structure s = withBFactors(95, 90, 70.01f, 70, 50, 10, Float.NaN);

        assertThat(registry.scheme("confidence").colours(s)).containsExactly(
                Rgb.of(0, 83, 214), Rgb.of(101, 203, 243), Rgb.of(101, 203, 243), Rgb.of(255, 219, 19),
                Rgb.of(255, 125, 69), Rgb.of(255, 125, 69), Rgb.of(128, 128, 128));
        assertThat(registry.scheme("confidence").legend(s)).get().isInstanceOfSatisfying(Legend.Bands.class,
                b -> assertThat(b.entries()).extracting(Legend.Entry::rgb).containsExactly(
                        Rgb.of(0, 83, 214), Rgb.of(101, 203, 243), Rgb.of(255, 219, 19), Rgb.of(255, 125, 69)));
    }

    @Test
    void bFactorGradientSpansTheStructuresOwnRange() {
        Structure s = withBFactors(20, 40, 60, Float.NaN);
        ColourScheme bFactor = registry.scheme("bfactor");

        assertThat(bFactor.colours(s)).containsExactly(
                Rgb.of(0, 0, 255), Rgb.of(255, 255, 255), Rgb.of(255, 0, 0), Rgb.of(128, 128, 128));
        assertThat(bFactor.colourOf(s, s.atoms().get(1))).isEqualTo(Rgb.of(255, 255, 255));
        assertThat(bFactor.legend(s)).contains(new Legend.Gradient("B-factor",
                List.of(Rgb.of(0, 0, 255), Rgb.of(255, 255, 255), Rgb.of(255, 0, 0)), 20, 60));
    }

    @Test
    void bFactorSchemeHandlesFilesWithoutOrWithConstantBFactors() {
        ColourScheme bFactor = registry.scheme("bfactor");
        Structure none = withBFactors(Float.NaN, Float.NaN);

        assertThat(bFactor.colours(none)).containsOnly(Rgb.of(128, 128, 128));
        assertThat(bFactor.legend(none)).get().isInstanceOf(Legend.Bands.class);
        assertThat(bFactor.colours(withBFactors(30, 30))).containsOnly(Rgb.of(255, 255, 255));
    }

    @Test
    void legacySchemesHaveNoLegend() {
        assertThat(registry.scheme("cpk").legend(USC)).isEmpty();
    }

    @Test
    void rejectsInvalidConfiguration() {
        Properties p = ViewerConfig.loadProperties();
        p.setProperty("display.modes", "balls,spacefill");
        assertThatThrownBy(() -> Registry.create(ViewerConfig.parse(p), ColourTables.parse(p)))
                .isInstanceOf(ConfigurationException.class).hasMessageContaining("spacefill");

        Properties q = ViewerConfig.loadProperties();
        q.setProperty("display.default", "ribbons2");
        assertThatThrownBy(() -> ViewerConfig.parse(q)).isInstanceOf(ConfigurationException.class);

        Properties r = ViewerConfig.loadProperties();
        r.setProperty("cpk.0", "300,0,0");
        assertThatThrownBy(() -> ColourTables.parse(r))
                .isInstanceOf(ConfigurationException.class).hasMessageContaining("cpk.0");

        Properties t = ViewerConfig.loadProperties();
        t.remove("confidence.low");
        assertThatThrownBy(() -> ColourTables.parse(t))
                .isInstanceOf(ConfigurationException.class).hasMessageContaining("confidence.low");
    }
}
