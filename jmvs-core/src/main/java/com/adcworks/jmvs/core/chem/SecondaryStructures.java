package com.adcworks.jmvs.core.chem;

import com.adcworks.jmvs.core.model.SecondaryStructureSource;
import com.adcworks.jmvs.core.model.Structure;

/**
 * Applies a {@link SecondaryStructureSource} to a structure as read from a file.
 */
public final class SecondaryStructures {

    private final DsspAssigner assigner;

    public SecondaryStructures() {
        this(new DsspAssigner());
    }

    public SecondaryStructures(DsspAssigner assigner) {
        this.assigner = assigner;
    }

    /**
     * The structure with secondary structure from the given source: {@code read} itself for the file's annotation,
     * or a copy with computed assignment. {@code read} must carry the file's annotation, i.e. come from a reader.
     */
    public Structure apply(Structure read, SecondaryStructureSource source) {
        boolean compute = switch (source) {
            case AUTO -> !read.secondaryStructureAnnotated();
            case FILE -> false;
            case COMPUTED -> true;
        };
        return compute ? read.withSecondaryStructure(assigner.assign(read)) : read;
    }
}
