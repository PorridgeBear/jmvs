package com.adcworks.jmvs.scene.surface;

import java.util.Arrays;
import java.util.Optional;

/**
 * Kinds of molecular surface.
 */
public enum SurfaceKind {

    /**
     * Solvent-excluded (Connolly) surface: the boundary of the space a solvent probe cannot reach.
     */
    SES("ses", "Solvent-excluded"),

    /**
     * Solvent-accessible surface: the path of the probe's centre as it rolls over the atoms.
     */
    SAS("sas", "Solvent-accessible");

    private final String id;
    private final String displayName;

    SurfaceKind(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<SurfaceKind> byId(String id) {
        return Arrays.stream(values()).filter(k -> k.id.equals(id)).findFirst();
    }
}
