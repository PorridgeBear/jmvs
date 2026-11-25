package com.adcworks.jmvs.core.model;

/**
 * A chain within a model, identified by {@code label_asym_id}. Residues are the contiguous range
 * {@code [firstResidue, firstResidue + residueCount)} in {@link Structure#residues()}.
 *
 * @param order position of this chain id among the distinct chain ids of the structure; stable across models
 */
public record Chain(int index, int model, String id, String authId, int order, int firstResidue,
                    int residueCount) {
}
