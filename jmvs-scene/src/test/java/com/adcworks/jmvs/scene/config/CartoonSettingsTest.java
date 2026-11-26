package com.adcworks.jmvs.scene.config;

import com.adcworks.jmvs.core.config.ConfigurationException;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartoonSettingsTest {

    @Test
    void bundledPropertiesGiveTheDefaults() {
        assertThat(ViewerConfig.parse(ViewerConfig.loadProperties()).cartoon()).isEqualTo(CartoonSettings.DEFAULTS);
    }

    @Test
    void missingKeysFallBackToDefaults() {
        Properties p = new Properties();
        p.setProperty("cartoon.coil.radius", "0.4");
        CartoonSettings settings = CartoonSettings.parse(p);

        assertThat(settings.coilRadius()).isEqualTo(0.4f);
        assertThat(settings.helixWidth()).isEqualTo(CartoonSettings.DEFAULTS.helixWidth());
    }

    @Test
    void rejectsInvalidValues() {
        Properties p = new Properties();
        p.setProperty("cartoon.helix.width", "wide");
        assertThatThrownBy(() -> CartoonSettings.parse(p)).isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("cartoon.helix.width");
        assertThatThrownBy(() -> new CartoonSettings(2.4f, 0, 2, 0.5f, 3.4f, 0.3f, 0.8f, 8))
                .isInstanceOf(ConfigurationException.class);
        assertThatThrownBy(() -> new CartoonSettings(2.4f, 0.5f, 2, 0.5f, 3.4f, 0.3f, 0.8f, 1))
                .isInstanceOf(ConfigurationException.class);
    }
}
