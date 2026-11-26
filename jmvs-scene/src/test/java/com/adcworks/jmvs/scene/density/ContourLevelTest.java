package com.adcworks.jmvs.scene.density;

import com.adcworks.jmvs.core.map.DensityMap;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ContourLevelTest {

    private static final DensityMap.Statistics STATS = new DensityMap.Statistics(-1, 1, 0.1f, 0.2f);

    @Test
    void parsesAbsoluteAndSigmaLevels() {
        assertThat(ContourLevel.parse("0.09")).isEqualTo(new ContourLevel.Absolute(0.09f));
        assertThat(ContourLevel.parse("2.5sigma")).isEqualTo(new ContourLevel.Sigma(2.5f));
        assertThat(ContourLevel.parse(" 3 σ ")).isEqualTo(new ContourLevel.Sigma(3f));
        assertThat(ContourLevel.parse("-1SIGMA")).isEqualTo(new ContourLevel.Sigma(-1f));
    }

    @Test
    void resolvesAgainstMapStatistics() {
        assertThat(new ContourLevel.Absolute(0.09f).resolve(STATS)).isEqualTo(0.09f);
        assertThat(new ContourLevel.Sigma(2f).resolve(STATS)).isEqualTo(0.5f);
    }

    @Test
    void rejectsNonNumbers() {
        assertThatIllegalArgumentException().isThrownBy(() -> ContourLevel.parse("high"));
        assertThatIllegalArgumentException().isThrownBy(() -> ContourLevel.parse("sigma"));
        assertThatIllegalArgumentException().isThrownBy(() -> ContourLevel.parse("NaN"));
    }
}
