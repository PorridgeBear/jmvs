package com.adcworks.jmvs.core.model;

/**
 * A model (e.g. an NMR conformer). Chains and atoms are contiguous ranges in the {@link Structure} lists.
 */
public record Model(int index, int number, int firstChain, int chainCount, int firstAtom, int atomCount) {
}
