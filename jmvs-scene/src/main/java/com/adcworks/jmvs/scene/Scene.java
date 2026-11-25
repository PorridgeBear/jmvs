package com.adcworks.jmvs.scene;

import com.adcworks.jmvs.core.model.BoundingSphere;

import java.util.List;

/**
 * Renderer-neutral description of what to draw for a structure in one display mode. Geometry refers to atoms by
 * index; colours are supplied separately so a colour change never rebuilds geometry.
 *
 * @param pickTargets spheres used for ray picking (may differ from the drawn spheres, e.g. Cα atoms for ribbons)
 */
public record Scene(SphereInstances spheres, CylinderInstances cylinders, List<TriangleMesh> meshes,
                    SphereInstances pickTargets, BoundingSphere bounds) {

    public Scene {
        meshes = List.copyOf(meshes);
    }
}
