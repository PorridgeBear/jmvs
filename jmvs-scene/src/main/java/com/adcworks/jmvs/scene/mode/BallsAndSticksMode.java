package com.adcworks.jmvs.scene.mode;

import com.adcworks.jmvs.core.model.Structure;
import com.adcworks.jmvs.scene.Scene;
import com.adcworks.jmvs.scene.SphereInstances;

import java.util.List;

/**
 * Spheres at a quarter of the van der Waals radius joined by bond cylinders.
 */
public final class BallsAndSticksMode implements DisplayMode {

    static final float BALL_SCALE = 0.25f;
    static final float STICK_RADIUS = 0.15f;

    @Override
    public String id() {
        return "ballsandsticks";
    }

    @Override
    public String displayName() {
        return "Balls and sticks";
    }

    @Override
    public Scene build(Structure structure) {
        SphereInstances balls = AtomGeometry.spheres(structure,
                a -> BALL_SCALE * AtomGeometry.vanDerWaalsRadius(structure, a));
        return new Scene(balls, AtomGeometry.halfBonds(structure, STICK_RADIUS), List.of(), balls,
                structure.boundingSphere());
    }
}
