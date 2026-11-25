package com.adcworks.jmvs.core.model;

/**
 * An atom. Coordinates are in ångströms; {@code index} is the position in {@link Structure#atoms()} and
 * {@code residue} the index of the owning residue in {@link Structure#residues()}.
 */
public record Atom(int index, int serial, String name, Element element, float x, float y, float z,
                   boolean heterogen, int residue) {

    public boolean isHydrogen() {
        return element.isHydrogen();
    }
}
