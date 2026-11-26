package com.adcworks.jmvs.scene.density;

import com.adcworks.jmvs.core.chem.SpatialGrid;
import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.Structure;
import com.adcworks.jmvs.scene.TriangleMesh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * Zone colouring: each contour vertex takes the colour of the nearest heavy atom of the structure's first model
 * within a radius, so the map shows the current colour scheme where the model explains it and the map's own colour
 * elsewhere. Only vertex atoms change; the geometry is shared (ADR 0007, ADR 0020).
 */
public final class ZoneColouring {

    private ZoneColouring() {
    }

    /**
     * The isosurface with vertex atoms assigned from {@code structure}, or all {@link Isosurface#NO_ATOM} without a
     * structure or with a zero radius.
     */
    public static Isosurface assign(Isosurface isosurface, Optional<Structure> structure, float radius,
                                    BooleanSupplier cancelled) {
        List<Atom> atoms = structure.filter(s -> radius > 0 && !s.models().isEmpty())
                .map(s -> s.atomsOf(s.models().getFirst()).stream().filter(a -> !a.isHydrogen()).toList())
                .orElse(List.of());
        SpatialGrid grid = atoms.isEmpty() ? null : SpatialGrid.of(atoms, Math.max(radius, 1f));
        List<int[]> assigned = new ArrayList<>(isosurface.meshes().size());
        for (TriangleMesh mesh : isosurface.meshes()) {
            if (cancelled.getAsBoolean()) {
                throw new CancellationException("Zone colouring cancelled");
            }
            int[] result = new int[mesh.vertexCount()];
            Arrays.fill(result, Isosurface.NO_ATOM);
            if (grid != null) {
                float[] p = mesh.positions();
                for (int v = 0; v < result.length; v++) {
                    result[v] = grid.nearest(p[v * 3], p[v * 3 + 1], p[v * 3 + 2], radius)
                            .map(Atom::index).orElse(Isosurface.NO_ATOM);
                }
            }
            assigned.add(result);
        }
        return isosurface.withAtoms(assigned);
    }
}
