package com.adcworks.jmvs.scene.density;

import com.adcworks.jmvs.core.model.BoundingSphere;
import com.adcworks.jmvs.scene.TriangleMesh;
import com.adcworks.jmvs.scene.mesh.MeshChunks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The contour of a density map at one level, drawn over the structure as a surface or as a mesh of lines. Like
 * {@link com.adcworks.jmvs.scene.Scene} it carries no colour: each vertex names the atom whose colour it takes, or
 * {@link #NO_ATOM} for the map's own colour (see {@link ZoneColouring}). Each mesh has at most
 * {@link MeshChunks#MAX_VERTICES} vertices so renderers can use 16-bit indices.
 *
 * @param level  contour level, in the map's units
 * @param meshes triangles of the contour surface
 * @param lines  for each mesh, its quad outlines as pairs of vertex indices, for the mesh style
 * @param area   surface area in Å²
 * @param volume volume enclosed by the contour (points above the level) in Å³
 */
public record Isosurface(float level, List<TriangleMesh> meshes, List<int[]> lines, double area, double volume) {

    /** Vertex atom index meaning "no nearby atom". */
    public static final int NO_ATOM = -1;

    public Isosurface {
        meshes = List.copyOf(meshes);
        lines = List.copyOf(lines);
        if (lines.size() != meshes.size()) {
            throw new IllegalArgumentException(meshes.size() + " meshes but " + lines.size() + " line lists");
        }
        for (TriangleMesh mesh : meshes) {
            if (mesh.vertexCount() > MeshChunks.MAX_VERTICES) {
                throw new IllegalArgumentException("Isosurface mesh has " + mesh.vertexCount() + " vertices");
            }
        }
    }

    public int vertexCount() {
        return meshes.stream().mapToInt(TriangleMesh::vertexCount).sum();
    }

    public int triangleCount() {
        return meshes.stream().mapToInt(m -> m.indices().length / 3).sum();
    }

    /**
     * Sphere centred on the vertices' bounding box that encloses them all, to frame the camera; empty without
     * vertices.
     */
    public Optional<BoundingSphere> boundingSphere() {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        for (TriangleMesh mesh : meshes) {
            float[] p = mesh.positions();
            for (int i = 0; i < p.length; i += 3) {
                minX = Math.min(minX, p[i]);
                minY = Math.min(minY, p[i + 1]);
                minZ = Math.min(minZ, p[i + 2]);
                maxX = Math.max(maxX, p[i]);
                maxY = Math.max(maxY, p[i + 1]);
                maxZ = Math.max(maxZ, p[i + 2]);
            }
        }
        if (minX > maxX) {
            return Optional.empty();
        }
        float cx = (minX + maxX) / 2, cy = (minY + maxY) / 2, cz = (minZ + maxZ) / 2;
        double max = 0;
        for (TriangleMesh mesh : meshes) {
            float[] p = mesh.positions();
            for (int i = 0; i < p.length; i += 3) {
                double dx = p[i] - cx, dy = p[i + 1] - cy, dz = p[i + 2] - cz;
                max = Math.max(max, dx * dx + dy * dy + dz * dz);
            }
        }
        return Optional.of(new BoundingSphere(cx, cy, cz, (float) Math.sqrt(max)));
    }

    /**
     * The same geometry with other vertex atoms, one array per mesh. Geometry arrays are shared, not copied.
     */
    public Isosurface withAtoms(List<int[]> atoms) {
        if (atoms.size() != meshes.size()) {
            throw new IllegalArgumentException(atoms.size() + " atom arrays for " + meshes.size() + " meshes");
        }
        List<TriangleMesh> recoloured = new ArrayList<>(meshes.size());
        for (int m = 0; m < meshes.size(); m++) {
            TriangleMesh mesh = meshes.get(m);
            recoloured.add(new TriangleMesh(mesh.positions(), mesh.normals(), atoms.get(m), mesh.indices()));
        }
        return new Isosurface(level, recoloured, lines, area, volume);
    }
}
