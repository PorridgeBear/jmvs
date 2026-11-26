package com.adcworks.jmvs.core.io;

import com.adcworks.jmvs.core.chem.ElementTable;
import com.adcworks.jmvs.core.map.DensityMap;
import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.Structure;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Golden values for the sample cryo-EM map {@code emd_3488.map.gz} (haemoglobin at 3.2 Å) and its fitted model
 * {@code 5me2.cif} (see data/README.md).
 */
class SampleMapGoldenTest {

    private static final Path DATA = Path.of(System.getProperty("jmvs.dataDir"));
    /** EMDB's recommended (author) contour level for EMD-3488. */
    private static final float AUTHOR_CONTOUR = 0.09f;

    @Test
    void headerAndStatistics() throws Exception {
        DensityMap map = new MrcMapReader(32_000_000).read(DATA.resolve("emd_3488.map.gz"));

        assertThat(map.name()).isEqualTo("emd_3488.map");
        assertThat(new int[]{map.nx(), map.ny(), map.nz()}).containsExactly(100, 100, 100);
        assertThat(map.axes()[0]).isCloseTo(1.05f, within(1e-5f));
        assertThat(map.axes()[4]).isCloseTo(1.05f, within(1e-5f));
        assertThat(map.axes()[8]).isCloseTo(1.05f, within(1e-5f));
        assertThat(map.origin()).containsExactly(0, 0, 0);
        assertThat(map.statistics().min()).isCloseTo(-0.5414f, within(1e-4f));
        assertThat(map.statistics().max()).isCloseTo(0.7004f, within(1e-4f));
        assertThat(map.statistics().mean()).isCloseTo(0.00152f, within(1e-5f));
        assertThat(map.statistics().rms()).isCloseTo(0.03715f, within(1e-5f));
    }

    @Test
    void fittedModelSitsInTheDensity() throws Exception {
        DensityMap map = new MrcMapReader(32_000_000).read(DATA.resolve("emd_3488.map.gz"));
        Structure model = StructureReaders.defaults(ElementTable.load()).read(DATA.resolve("5me2.cif"));
        List<Atom> alphaCarbons = model.atoms().stream().filter(a -> a.name().equals("CA")).toList();

        long inside = alphaCarbons.stream().filter(a -> nearestValue(map, a) > AUTHOR_CONTOUR).count();
        assertThat(alphaCarbons).hasSize(566);
        assertThat((double) inside / alphaCarbons.size()).isGreaterThan(0.9);
    }

    /** Value at the grid point nearest the atom; the sample map is orthogonal. */
    private static float nearestValue(DensityMap map, Atom atom) {
        int i = Math.round((atom.x() - map.origin()[0]) / map.axes()[0]);
        int j = Math.round((atom.y() - map.origin()[1]) / map.axes()[4]);
        int k = Math.round((atom.z() - map.origin()[2]) / map.axes()[8]);
        boolean inGrid = i >= 0 && j >= 0 && k >= 0 && i < map.nx() && j < map.ny() && k < map.nz();
        return inGrid ? map.value(i, j, k) : Float.NEGATIVE_INFINITY;
    }
}
