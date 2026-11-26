package com.adcworks.jmvs.scene.density;

import java.util.Arrays;
import java.util.Optional;

/**
 * How a density map's contour is drawn.
 */
public enum DensityStyle {

    /** A shaded, closed surface. */
    SURFACE("surface", "Surface"),

    /** The surface's grid outline as lines, so the model shows through. */
    MESH("mesh", "Mesh");

    private final String id;
    private final String displayName;

    DensityStyle(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<DensityStyle> byId(String id) {
        return Arrays.stream(values()).filter(s -> s.id.equals(id)).findFirst();
    }
}
