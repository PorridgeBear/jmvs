package com.adcworks.jmvs.scene.mode;

import com.adcworks.jmvs.core.model.Structure;
import com.adcworks.jmvs.scene.CylinderInstances;
import com.adcworks.jmvs.scene.Scene;
import com.adcworks.jmvs.scene.SphereInstances;

import java.util.List;

/**
 * Space-filling spheres at van der Waals radii (RasMol united-atom radii when there are no hydrogens).
 */
public final class BallsMode implements DisplayMode {

    @Override
    public String id() {
        return "balls";
    }

    @Override
    public String displayName() {
        return "Balls";
    }

    @Override
    public Scene build(Structure structure) {
        SphereInstances spheres = AtomGeometry.spheres(structure, a -> AtomGeometry.vanDerWaalsRadius(structure, a));
        return new Scene(spheres, CylinderInstances.EMPTY, List.of(), spheres, structure.boundingSphere());
    }
}
