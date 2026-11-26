package com.adcworks.jmvs.core.chem;

import com.adcworks.jmvs.core.io.StructureReaders;
import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.Structure;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SpatialGridTest {

    private static final Structure CRAMBIN = read("1crn.cif");

    @Test
    void candidatesIncludeEveryAtomWithinRadius() {
        SpatialGrid grid = SpatialGrid.of(CRAMBIN.atoms(), 2.0f);
        Random random = new Random(11);
        for (int i = 0; i < 200; i++) {
            float x = point(random, CRAMBIN.bounds().minX(), CRAMBIN.bounds().maxX());
            float y = point(random, CRAMBIN.bounds().minY(), CRAMBIN.bounds().maxY());
            float z = point(random, CRAMBIN.bounds().minZ(), CRAMBIN.bounds().maxZ());
            float radius = 1 + random.nextFloat() * 5;
            Set<Atom> candidates = new HashSet<>();
            grid.forEachCandidate(x, y, z, radius, candidates::add);
            for (Atom atom : within(x, y, z, radius)) {
                assertThat(candidates).contains(atom);
            }
        }
    }

    @Test
    void nearestMatchesBruteForce() {
        SpatialGrid grid = SpatialGrid.of(CRAMBIN.atoms(), 3.0f);
        Random random = new Random(7);
        for (int i = 0; i < 200; i++) {
            Atom anchor = CRAMBIN.atoms().get(random.nextInt(CRAMBIN.atoms().size()));
            float x = anchor.x() + random.nextFloat() * 4 - 2;
            float y = anchor.y() + random.nextFloat() * 4 - 2;
            float z = anchor.z() + random.nextFloat() * 4 - 2;
            assertThat(grid.nearest(x, y, z, 5f)).isEqualTo(bruteNearest(x, y, z, 5f));
        }
    }

    @Test
    void nearestIsEmptyBeyondMaxDistance() {
        SpatialGrid grid = SpatialGrid.of(CRAMBIN.atoms(), 3.0f);
        assertThat(grid.nearest(1000, 1000, 1000, 5f)).isEmpty();
    }

    @Test
    void rejectsNonPositiveCellSize() {
        assertThatIllegalArgumentException().isThrownBy(() -> SpatialGrid.of(CRAMBIN.atoms(), 0));
    }

    private static List<Atom> within(float x, float y, float z, float radius) {
        List<Atom> result = new ArrayList<>();
        for (Atom atom : CRAMBIN.atoms()) {
            if (distanceSquared(atom, x, y, z) <= (double) radius * radius) {
                result.add(atom);
            }
        }
        return result;
    }

    private static Optional<Atom> bruteNearest(float x, float y, float z, float maxDistance) {
        Atom best = null;
        double bestDistance = (double) maxDistance * maxDistance;
        for (Atom atom : CRAMBIN.atoms()) {
            double d = distanceSquared(atom, x, y, z);
            if (d < bestDistance || d == bestDistance && best == null) {
                best = atom;
                bestDistance = d;
            }
        }
        return Optional.ofNullable(best);
    }

    private static double distanceSquared(Atom atom, float x, float y, float z) {
        double dx = atom.x() - x, dy = atom.y() - y, dz = atom.z() - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static float point(Random random, float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private static Structure read(String file) {
        try {
            return StructureReaders.defaults(ElementTable.load())
                    .read(Path.of(System.getProperty("jmvs.dataDir"), file));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
