package com.adcworks.jmvs.scene;

/**
 * Indexed triangle mesh. {@code positions} and {@code normals} hold 3 floats per vertex; {@code atoms[v]} is the
 * atom whose colour vertex {@code v} takes. Meshes are two-sided.
 */
public record TriangleMesh(float[] positions, float[] normals, int[] atoms, int[] indices) {

    public TriangleMesh {
        if (positions.length != atoms.length * 3 || normals.length != positions.length) {
            throw new IllegalArgumentException("positions and normals must hold 3 floats per vertex");
        }
        if (indices.length % 3 != 0) {
            throw new IllegalArgumentException("indices must describe whole triangles");
        }
    }

    public int vertexCount() {
        return atoms.length;
    }
}
