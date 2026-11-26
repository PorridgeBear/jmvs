package com.adcworks.jmvs.scene.surface;

import com.adcworks.jmvs.scene.TriangleMesh;

import java.util.List;

/**
 * A computed molecular surface, drawn as an overlay on the current display mode. Like {@link
 * com.adcworks.jmvs.scene.Scene} it carries no colour: each vertex refers to the atom whose colour it takes. Each mesh
 * has at most {@link #MAX_MESH_VERTICES} vertices so renderers can use 16-bit indices.
 *
 * @param area surface area in Å²
 */
public record Surface(SurfaceKind kind, List<TriangleMesh> meshes, double area) {

    public static final int MAX_MESH_VERTICES = 65_535;

    public Surface {
        meshes = List.copyOf(meshes);
        for (TriangleMesh mesh : meshes) {
            if (mesh.vertexCount() > MAX_MESH_VERTICES) {
                throw new IllegalArgumentException("Surface mesh has " + mesh.vertexCount() + " vertices");
            }
        }
    }

    public int vertexCount() {
        return meshes.stream().mapToInt(TriangleMesh::vertexCount).sum();
    }

    public int triangleCount() {
        return meshes.stream().mapToInt(m -> m.indices().length / 3).sum();
    }
}
