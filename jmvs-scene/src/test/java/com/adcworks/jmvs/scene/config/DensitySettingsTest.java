package com.adcworks.jmvs.scene.config;

import com.adcworks.jmvs.core.config.ConfigurationException;
import com.adcworks.jmvs.scene.Registry;
import com.adcworks.jmvs.scene.colour.Rgb;
import com.adcworks.jmvs.scene.density.DensityStyle;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DensitySettingsTest {

    @Test
    void bundledPropertiesGiveTheDefaults() {
        assertThat(Registry.load().density()).isEqualTo(DensitySettings.DEFAULTS);
    }

    @Test
    void missingKeysFallBackToDefaults() {
        Properties p = new Properties();
        p.setProperty("density.sigma", "1.5");
        p.setProperty("density.style", "surface");
        p.setProperty("density.colour", "10, 20, 30");
        assertThat(DensitySettings.parse(p)).isEqualTo(
                new DensitySettings(32_000_000, 1.5f, 3f, Rgb.of(10, 20, 30), DensityStyle.SURFACE));
    }

    @Test
    void rejectsInvalidValues() {
        Properties style = new Properties();
        style.setProperty("density.style", "dots");
        assertThatThrownBy(() -> DensitySettings.parse(style)).isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("density.style");
        Properties radius = new Properties();
        radius.setProperty("density.zoneRadius", "-1");
        assertThatThrownBy(() -> DensitySettings.parse(radius)).isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("density.zoneRadius");
        Properties voxels = new Properties();
        voxels.setProperty("density.maxVoxels", "many");
        assertThatThrownBy(() -> DensitySettings.parse(voxels)).isInstanceOf(ConfigurationException.class);
    }
}
