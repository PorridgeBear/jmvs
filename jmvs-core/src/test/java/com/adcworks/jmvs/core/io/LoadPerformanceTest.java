package com.adcworks.jmvs.core.io;

import com.adcworks.jmvs.core.chem.ElementTable;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Quality scenario Q9: reading and bonding a ~20k-atom structure takes well under a second. The budget is
 * generous so slow CI runners do not fail spuriously.
 */
@Tag("performance")
class LoadPerformanceTest {

    @Test
    void readsLargeStructureQuickly() throws Exception {
        StructureReaders readers = StructureReaders.defaults(ElementTable.load());
        Path file = Path.of(System.getProperty("jmvs.dataDir"), "1zpd.cif");
        readers.read(file);

        long start = System.nanoTime();
        readers.read(file);
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertThat(millis).isLessThan(3_000);
    }
}
