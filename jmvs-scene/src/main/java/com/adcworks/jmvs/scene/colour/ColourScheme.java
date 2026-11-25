package com.adcworks.jmvs.scene.colour;

import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.Structure;

/**
 * Assigns a colour to every atom. Display modes never colour geometry themselves; renderers combine a mode's
 * {@link com.adcworks.jmvs.scene.Scene} with the colours from a scheme.
 */
public interface ColourScheme {

    String id();

    String displayName();

    /**
     * Packed {@code 0xRRGGBB} colour of an atom.
     */
    int colourOf(Structure structure, Atom atom);

    /**
     * Colours of all atoms, indexed by atom index.
     */
    default int[] colours(Structure structure) {
        int[] colours = new int[structure.atoms().size()];
        for (Atom atom : structure.atoms()) {
            colours[atom.index()] = colourOf(structure, atom);
        }
        return colours;
    }
}
