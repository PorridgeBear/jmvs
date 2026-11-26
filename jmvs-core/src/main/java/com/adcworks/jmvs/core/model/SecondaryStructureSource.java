package com.adcworks.jmvs.core.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Where residue secondary structure comes from.
 */
public enum SecondaryStructureSource {

    /**
     * The file's annotation when it has any, otherwise computed from the backbone.
     */
    AUTO("auto", "Auto"),

    /**
     * The file's {@code _struct_conf} and {@code _struct_sheet_range} annotation, even if empty.
     */
    FILE("file", "From file"),

    /**
     * Computed from backbone hydrogen bonds (DSSP subset), ignoring the file's annotation.
     */
    COMPUTED("computed", "Computed");

    private final String id;
    private final String displayName;

    SecondaryStructureSource(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<SecondaryStructureSource> byId(String id) {
        return Arrays.stream(values()).filter(s -> s.id.equals(id)).findFirst();
    }
}
