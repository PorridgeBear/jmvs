package com.adcworks.jmvs.scene.density;

import com.adcworks.jmvs.core.map.DensityMap;
import com.adcworks.jmvs.scene.TriangleMesh;
import com.adcworks.jmvs.scene.mesh.Grid;
import com.adcworks.jmvs.scene.mesh.MeshChunks;
import com.adcworks.jmvs.scene.mesh.SurfaceNets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * Contours a density map with {@link SurfaceNets} (ADR 0020): the map's values are meshed in place in grid index
 * space, with the inside above the level, and the vertices are then moved to Cartesian space with the map's axes,
 * which may be oblique. Normals are transformed by the inverse transpose. Pure computation, safe on a background
 * thread; rebuilding at another level costs one pass over the map.
 */
public final class IsosurfaceBuilder {

    private IsosurfaceBuilder() {
    }

    public static Isosurface build(DensityMap map, float level) {
        return build(map, level, () -> false);
    }

    /**
     * Contours the map at {@code level}; every vertex starts with {@link Isosurface#NO_ATOM}. Throws
     * {@link CancellationException} if {@code cancelled} returns true while meshing.
     */
    public static Isosurface build(DensityMap map, float level, BooleanSupplier cancelled) {
        Grid grid = Grid.ofIndices(map.nx(), map.ny(), map.nz());
        SurfaceNets.Mesh indexed = SurfaceNets.extract(grid, map.values(), level, true, cancelled);
        if (cancelled.getAsBoolean()) {
            throw new CancellationException("Contour cancelled");
        }
        SurfaceNets.Mesh mesh = toCartesian(map, indexed);
        int[] atoms = new int[mesh.vertexCount()];
        Arrays.fill(atoms, Isosurface.NO_ATOM);
        List<TriangleMesh> meshes = MeshChunks.split(mesh, atoms);
        List<int[]> lines = new ArrayList<>(meshes.size());
        for (TriangleMesh chunk : meshes) {
            lines.add(MeshChunks.quadEdges(chunk));
        }
        return new Isosurface(level, meshes, lines, MeshChunks.area(mesh), enclosedVolume(map, level));
    }

    static double enclosedVolume(DensityMap map, float level) {
        long above = 0;
        for (float v : map.values()) {
            if (v > level) {
                above++;
            }
        }
        return above * map.voxelVolume();
    }

    private static SurfaceNets.Mesh toCartesian(DensityMap map, SurfaceNets.Mesh mesh) {
        float[] p = mesh.positions();
        float[] n = mesh.normals();
        float[] positions = new float[p.length];
        float[] normals = new float[n.length];
        float[] normalMatrix = inverseTranspose(map.axes());
        float[] out = new float[3];
        for (int v = 0; v < p.length; v += 3) {
            map.position(p[v], p[v + 1], p[v + 2], out);
            System.arraycopy(out, 0, positions, v, 3);
            float x = 0, y = 0, z = 0;
            for (int c = 0; c < 3; c++) {
                x += normalMatrix[c] * n[v + c];
                y += normalMatrix[3 + c] * n[v + c];
                z += normalMatrix[6 + c] * n[v + c];
            }
            float length = (float) Math.sqrt(x * x + y * y + z * z);
            normals[v] = length > 0 ? x / length : 0;
            normals[v + 1] = length > 0 ? y / length : 0;
            normals[v + 2] = length > 0 ? z / length : 1;
        }
        return new SurfaceNets.Mesh(positions, normals, mesh.triangles());
    }

    /**
     * Row-major inverse transpose of the matrix whose columns are the axes {@code a, b, c}, i.e. the matrix taking
     * index-space gradients to Cartesian ones.
     */
    static float[] inverseTranspose(float[] axes) {
        // Column-major axes: M[row][col] = axes[3 * col + row].
        double m00 = axes[0], m10 = axes[1], m20 = axes[2];
        double m01 = axes[3], m11 = axes[4], m21 = axes[5];
        double m02 = axes[6], m12 = axes[7], m22 = axes[8];
        double c00 = m11 * m22 - m12 * m21, c01 = m12 * m20 - m10 * m22, c02 = m10 * m21 - m11 * m20;
        double c10 = m02 * m21 - m01 * m22, c11 = m00 * m22 - m02 * m20, c12 = m01 * m20 - m00 * m21;
        double c20 = m01 * m12 - m02 * m11, c21 = m02 * m10 - m00 * m12, c22 = m00 * m11 - m01 * m10;
        double det = m00 * c00 + m01 * c01 + m02 * c02;
        if (det == 0) {
            throw new IllegalArgumentException("Map axes are degenerate");
        }
        // (M⁻¹)ᵀ is the cofactor matrix divided by the determinant.
        return new float[]{
                (float) (c00 / det), (float) (c01 / det), (float) (c02 / det),
                (float) (c10 / det), (float) (c11 / det), (float) (c12 / det),
                (float) (c20 / det), (float) (c21 / det), (float) (c22 / det)};
    }
}
