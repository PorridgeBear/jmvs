package com.adcworks.jmvs.core.model;

/**
 * An atom. Coordinates are in ångströms; {@code index} is the position in {@link Structure#atoms()} and
 * {@code residue} the index of the owning residue in {@link Structure#residues()}. {@code bFactor} is
 * {@code _atom_site.B_iso_or_equiv} (predicted models such as AlphaFold store pLDDT there), or {@link Float#NaN}
 * when the file doesn't give one.
 */
public record Atom(int index, int serial, String name, Element element, float x, float y, float z,
                   boolean heterogen, int residue, float bFactor) {

    public boolean isHydrogen() {
        return element.isHydrogen();
    }

    public boolean hasBFactor() {
        return !Float.isNaN(bFactor);
    }
}
