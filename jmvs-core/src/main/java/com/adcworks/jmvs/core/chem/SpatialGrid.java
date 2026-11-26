package com.adcworks.jmvs.core.chem;

import com.adcworks.jmvs.core.model.Atom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Uniform spatial hash of atoms for neighbour searches, used for covalent bonding (ADR 0009) and for mapping surface
 * vertices to atoms. Immutable once built; cells are keyed by packed 21-bit cell coordinates.
 */
public final class SpatialGrid {

    private final float cellSize;
    private final Map<Long, List<Atom>> cells;

    private SpatialGrid(float cellSize, Map<Long, List<Atom>> cells) {
        this.cellSize = cellSize;
        this.cells = cells;
    }

    /**
     * Grid of the given atoms with cubic cells of {@code cellSize} Å; searches are cheapest when the search radius
     * is at most the cell size.
     */
    public static SpatialGrid of(List<Atom> atoms, float cellSize) {
        if (!(cellSize > 0)) {
            throw new IllegalArgumentException("Cell size must be positive: " + cellSize);
        }
        Map<Long, List<Atom>> cells = new HashMap<>();
        for (Atom atom : atoms) {
            cells.computeIfAbsent(key(cell(atom.x(), cellSize), cell(atom.y(), cellSize), cell(atom.z(), cellSize)),
                    k -> new ArrayList<>()).add(atom);
        }
        cells.replaceAll((k, v) -> List.copyOf(v));
        return new SpatialGrid(cellSize, Map.copyOf(cells));
    }

    public float cellSize() {
        return cellSize;
    }

    /**
     * Visits every atom in the cells within {@code ceil(radius / cellSize)} cells of the point's cell. This is a
     * superset of the atoms within {@code radius}: callers filter by distance. Order is unspecified.
     */
    public void forEachCandidate(float x, float y, float z, float radius, Consumer<Atom> action) {
        int reach = (int) Math.ceil(radius / cellSize);
        int cx = cell(x, cellSize), cy = cell(y, cellSize), cz = cell(z, cellSize);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dy = -reach; dy <= reach; dy++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    List<Atom> atoms = cells.get(key(cx + dx, cy + dy, cz + dz));
                    if (atoms != null) {
                        atoms.forEach(action);
                    }
                }
            }
        }
    }

    /**
     * The atom closest to the point within {@code maxDistance} Å, if any; ties go to the lower atom index.
     */
    public Optional<Atom> nearest(float x, float y, float z, float maxDistance) {
        var best = new Object() {
            Atom atom;
            double distanceSquared = (double) maxDistance * maxDistance;
        };
        forEachCandidate(x, y, z, maxDistance, atom -> {
            double dx = atom.x() - x, dy = atom.y() - y, dz = atom.z() - z;
            double d = dx * dx + dy * dy + dz * dz;
            boolean closer = d < best.distanceSquared
                    || d == best.distanceSquared && (best.atom == null || atom.index() < best.atom.index());
            if (closer) {
                best.atom = atom;
                best.distanceSquared = d;
            }
        });
        return Optional.ofNullable(best.atom);
    }

    private static int cell(float coordinate, float cellSize) {
        return (int) Math.floor(coordinate / cellSize);
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
    }
}
