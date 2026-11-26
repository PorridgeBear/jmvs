package com.adcworks.jmvs.scene.config;

import com.adcworks.jmvs.core.config.ConfigurationException;
import com.adcworks.jmvs.scene.Registry;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SurfaceSettingsTest {

    @Test
    void bundledPropertiesGiveTheDefaults() {
        assertThat(Registry.load().surface()).isEqualTo(new SurfaceSettings(1.4f, 0.5f, 8_000_000));
    }

    @Test
    void missingKeysFallBackToDefaults() {
        Properties p = new Properties();
        p.setProperty("surface.probe", "1.2");
        assertThat(SurfaceSettings.parse(p)).isEqualTo(new SurfaceSettings(1.2f, 0.5f, 8_000_000));
    }

    @Test
    void rejectsInvalidValues() {
        Properties p = new Properties();
        p.setProperty("surface.spacing", "fine");
        assertThatThrownBy(() -> SurfaceSettings.parse(p)).isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("surface.spacing");
        assertThatThrownBy(() -> new SurfaceSettings(1.4f, 0, 8_000_000))
                .isInstanceOf(ConfigurationException.class);
        assertThatThrownBy(() -> new SurfaceSettings(-1, 0.5f, 8_000_000))
                .isInstanceOf(ConfigurationException.class);
    }
}
