package com.adcworks.jmvs.scene.mode;

import com.adcworks.jmvs.core.model.Structure;
import com.adcworks.jmvs.scene.Scene;
import com.adcworks.jmvs.scene.SphereInstances;

import java.util.List;

/**
 * Bonds as thin cylinders, with small spheres at every atom as joints (unbonded atoms such as waters and ions
 * appear as dots).
 */
public final class SticksMode implements DisplayMode {

    static final float STICK_RADIUS = 0.2f;
    static final float PICK_RADIUS = 0.6f;

    @Override
    public String id() {
        return "sticks";
    }

    @Override
    public String displayName() {
        return "Sticks";
    }

    @Override
    public Scene build(Structure structure) {
        SphereInstances joints = AtomGeometry.spheres(structure, a -> STICK_RADIUS);
        SphereInstances pick = AtomGeometry.spheres(structure, a -> PICK_RADIUS);
        return new Scene(joints, AtomGeometry.halfBonds(structure, STICK_RADIUS), List.of(), pick,
                structure.boundingSphere());
    }
}
