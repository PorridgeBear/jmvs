package com.adcworks.jmvs.scene.colour;

import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.Structure;

/**
 * Built-in colour schemes, equivalent to the legacy paint modes.
 */
public final class ColourSchemes {

    private ColourSchemes() {
    }

    private record Simple(String id, String displayName, AtomColour colour) implements ColourScheme {
        @Override
        public int colourOf(Structure structure, Atom atom) {
            return colour.of(structure, atom);
        }
    }

    @FunctionalInterface
    private interface AtomColour {
        int of(Structure structure, Atom atom);
    }

    /**
     * By element (RasMol CPK table).
     */
    public static ColourScheme cpk(ColourTables tables) {
        return new Simple("cpk", "CPK", (s, a) -> tables.cpkColour(a.element().cpkIndex()));
    }

    /**
     * By chain, using a fixed palette in order of first appearance so colours are stable between runs.
     */
    public static ColourScheme chain(ColourTables tables) {
        return new Simple("chain", "Chain", (s, a) -> tables.chainColour(s.chainOf(a).order()));
    }

    /**
     * By residue type (RasMol amino colours).
     */
    public static ColourScheme group(ColourTables tables) {
        return new Simple("group", "Group", (s, a) -> tables.residueColour(s.residueOf(a).name()));
    }

    /**
     * By secondary structure: helix, sheet, turn, coil.
     */
    public static ColourScheme structure(ColourTables tables) {
        return new Simple("structure", "Structure",
                (s, a) -> tables.structureColour(s.residueOf(a).secondaryStructure()));
    }
}
