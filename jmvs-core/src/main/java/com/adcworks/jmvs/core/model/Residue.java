package com.adcworks.jmvs.core.model;

/**
 * A residue (monomer, ligand or water). Atoms are the contiguous range
 * {@code [firstAtom, firstAtom + atomCount)} in {@link Structure#atoms()}.
 *
 * @param seqId   {@code label_seq_id} for polymer residues, otherwise {@code auth_seq_id}
 * @param polymer whether the residue has a {@code label_seq_id}, i.e. is part of a polymer sequence
 */
public record Residue(int index, int chain, String name, int seqId, String insertionCode, boolean polymer,
                      SecondaryStructure secondaryStructure, int firstAtom, int atomCount) {
}
