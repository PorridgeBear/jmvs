package com.adcworks.jmvs.core.model;

/**
 * A covalent bond between two atoms, by index, with {@code atom1 < atom2}.
 */
public record Bond(int atom1, int atom2) {
}
