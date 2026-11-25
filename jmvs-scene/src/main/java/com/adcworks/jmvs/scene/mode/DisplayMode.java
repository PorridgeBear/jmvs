package com.adcworks.jmvs.scene.mode;

import com.adcworks.jmvs.core.model.Structure;
import com.adcworks.jmvs.scene.Scene;
import com.adcworks.jmvs.scene.colour.ColourScheme;

/**
 * How a structure is drawn (balls, sticks, ribbons...). Implementations produce geometry only; colours come from a
 * {@link ColourScheme}.
 */
public interface DisplayMode {

    String id();

    String displayName();

    Scene build(Structure structure);

    /**
     * Whether colouring this mode with the scheme is meaningful (e.g. ribbons cannot show per-element colours).
     */
    default boolean supports(ColourScheme scheme) {
        return true;
    }
}
