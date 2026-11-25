package com.adcworks.jmvs.core.chem;

import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.Bond;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Infers covalent bonds with RasMol's distance rules (https://www.umass.edu/microbio/rasmol/rasbonds.htm),
 * searching neighbours with a uniform spatial grid instead of comparing every pair.
 * <ul>
 *     <li>Fast rule (more than {@value #FAST_RULE_ATOM_THRESHOLD} atoms in the structure): bonded when the distance
 *     is between 0.4 Å and 1.9 Å, or 1.2 Å if either atom is a hydrogen.</li>
 *     <li>Otherwise: bonded when the distance is between 0.4 Å and the sum of covalent radii plus 0.56 Å.</li>
 * </ul>
 */
public final class CovalentBonder {

    public static final int FAST_RULE_ATOM_THRESHOLD = 1000;
    static final float MIN_DISTANCE = 0.4f;
    static final float FAST_MAX_DISTANCE = 1.9f;
    static final float FAST_MAX_DISTANCE_HYDROGEN = 1.2f;
    static final float RADII_TOLERANCE = 0.56f;

    public static boolean useFastRule(int totalAtoms) {
        return totalAtoms > FAST_RULE_ATOM_THRESHOLD;
    }

    /**
     * Whether two atoms are bonded under the selected rule. Distance is computed as in the legacy
     * implementation so results match it exactly.
     */
    public static boolean isBonded(Atom a, Atom b, boolean fast) {
        float dx = b.x() - a.x();
        float dy = b.y() - a.y();
        float dz = b.z() - a.z();
        float distance = (float) Math.sqrt((double) dx * dx + (double) dy * dy + (double) dz * dz);
        float max = fast
                ? (a.isHydrogen() || b.isHydrogen() ? FAST_MAX_DISTANCE_HYDROGEN : FAST_MAX_DISTANCE)
                : a.element().covalentRadius() + b.element().covalentRadius() + RADII_TOLERANCE;
        return distance >= MIN_DISTANCE && distance <= max;
    }

    /**
     * Bonds between the given atoms (typically one model), ordered by first then second atom index.
     */
    public List<Bond> bond(List<Atom> atoms, boolean fast) {
        if (atoms.size() < 2) {
            return List.of();
        }
        float cellSize = fast ? FAST_MAX_DISTANCE : maxSlowDistance(atoms);
        Map<Long, List<Atom>> grid = new HashMap<>();
        for (Atom atom : atoms) {
            grid.computeIfAbsent(key(cell(atom.x(), cellSize), cell(atom.y(), cellSize), cell(atom.z(), cellSize)),
                    k -> new ArrayList<>()).add(atom);
        }

        List<Bond> bonds = new ArrayList<>();
        List<Atom> neighbours = new ArrayList<>();
        for (Atom atom : atoms) {
            int cx = cell(atom.x(), cellSize), cy = cell(atom.y(), cellSize), cz = cell(atom.z(), cellSize);
            neighbours.clear();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        List<Atom> cellAtoms = grid.get(key(cx + dx, cy + dy, cz + dz));
                        if (cellAtoms != null) {
                            for (Atom other : cellAtoms) {
                                if (other.index() > atom.index()) {
                                    neighbours.add(other);
                                }
                            }
                        }
                    }
                }
            }
            neighbours.sort((p, q) -> Integer.compare(p.index(), q.index()));
            for (Atom other : neighbours) {
                if (isBonded(atom, other, fast)) {
                    bonds.add(new Bond(atom.index(), other.index()));
                }
            }
        }
        return bonds;
    }

    private static float maxSlowDistance(List<Atom> atoms) {
        float maxRadius = 0;
        for (Atom atom : atoms) {
            maxRadius = Math.max(maxRadius, atom.element().covalentRadius());
        }
        return 2 * maxRadius + RADII_TOLERANCE;
    }

    private static int cell(float coordinate, float cellSize) {
        return (int) Math.floor(coordinate / cellSize);
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
    }
}
