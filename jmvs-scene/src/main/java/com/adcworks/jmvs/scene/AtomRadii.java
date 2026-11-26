package com.adcworks.jmvs.scene;

import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.Structure;

import java.util.Map;

/**
 * Atom radii shared by display modes and surfaces.
 */
public final class AtomRadii {

    /**
     * RasMol "united atom" radii for C, N, O and S, which include the atom's implicit hydrogens.
     */
    public static final Map<String, Float> UNITED = Map.of("C", 1.872f, "N", 1.507f, "O", 1.400f, "S", 1.848f);

    private AtomRadii() {
    }

    /**
     * Van der Waals radius as RasMol draws it: united-atom radii when the structure has no hydrogens.
     */
    public static float vanDerWaals(Structure structure, Atom atom) {
        return structure.hasHydrogens() ? atom.element().vanDerWaalsRadius() : united(atom);
    }

    /**
     * United-atom radius for C, N, O and S, otherwise the element's van der Waals radius.
     */
    public static float united(Atom atom) {
        Float united = UNITED.get(atom.element().symbol());
        return united != null ? united : atom.element().vanDerWaalsRadius();
    }
}
