package com.adcworks.jmvs.core.chem;

import com.adcworks.jmvs.core.config.ConfigurationException;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ElementTableTest {

    private final ElementTable table = ElementTable.load();

    @Test
    void convertsRasMolUnitsToAngstroms() {
        var carbon = table.bySymbol("C");

        assertThat(carbon.covalentRadius()).isEqualTo(170 / 250f);
        assertThat(carbon.vanDerWaalsRadius()).isEqualTo(387 / 250f);
        assertThat(carbon.cpkIndex()).isZero();
        assertThat(carbon.name()).isEqualTo("Carbon");
    }

    @Test
    void looksUpSymbolsCaseInsensitivelyAndFallsBackToUnknown() {
        assertThat(table.bySymbol("FE").symbol()).isEqualTo("Fe");
        assertThat(table.bySymbol("br").symbol()).isEqualTo("Br");
        assertThat(table.bySymbol("Xx")).isSameAs(table.unknown());
        assertThat(table.bySymbol(null)).isSameAs(table.unknown());
    }

    @Test
    void rejectsMalformedEntries() {
        Properties p = new Properties();
        p.setProperty("element.0", "?,170,360");

        assertThatThrownBy(() -> ElementTable.parse(p))
                .isInstanceOf(ConfigurationException.class).hasMessageContaining("element.0");
    }
}
