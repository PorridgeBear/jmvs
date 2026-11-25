package com.adcworks.jmvs.render;

import com.adcworks.jmvs.scene.CylinderInstances;
import com.adcworks.jmvs.scene.Scene;
import com.adcworks.jmvs.scene.SphereInstances;
import com.adcworks.jmvs.scene.TriangleMesh;
import com.adcworks.jmvs.scene.colour.Rgb;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a {@link Scene} with instanced spheres and cylinders and plain triangle meshes. Geometry is uploaded once
 * per scene; {@link #setColours(int[])} and {@link #setHighlight(int)} only rewrite colour data.
 * Must be used on the render thread.
 */
public final class StructureRenderer implements Disposable {

    private static final int SPHERE_FLOATS = 8;
    private static final int CYLINDER_FLOATS = 11;
    private static final int MESH_FLOATS = 10;
    private static final int LARGE_SPHERE_COUNT = 60_000;

    private final ShaderProgram sphereShader;
    private final ShaderProgram cylinderShader;
    private final ShaderProgram meshShader;
    private final Mesh detailedSphere = Primitives.icosphere(2);
    private final Mesh coarseSphere = Primitives.icosphere(1);
    private final Mesh cylinder = Primitives.cylinder(12);
    private final Vector3 lightDir = new Vector3();

    private Scene scene;
    private Mesh sphereTemplate;
    private float[] sphereData = new float[0];
    private float[] cylinderData = new float[0];
    private final List<float[]> meshData = new ArrayList<>();
    private final List<int[]> meshAtoms = new ArrayList<>();
    private final List<Mesh> meshes = new ArrayList<>();
    private int[] atomColours = new int[0];
    private int highlightedAtom = -1;

    public StructureRenderer() {
        sphereShader = shader("sphere.vert");
        cylinderShader = shader("cylinder.vert");
        meshShader = shader("mesh.vert");
    }

    public Scene scene() {
        return scene;
    }

    /**
     * Uploads geometry for a new scene and colours it.
     *
     * @param atomColours packed {@code 0xRRGGBB} per atom index
     */
    public void setScene(Scene scene, int[] atomColours) {
        this.scene = scene;
        this.atomColours = atomColours;
        disableInstancing();
        disposeMeshes();

        SphereInstances spheres = scene.spheres();
        sphereTemplate = spheres.count() > LARGE_SPHERE_COUNT ? coarseSphere : detailedSphere;
        sphereData = new float[spheres.count() * SPHERE_FLOATS];
        for (int i = 0; i < spheres.count(); i++) {
            System.arraycopy(spheres.data(), i * SphereInstances.STRIDE, sphereData, i * SPHERE_FLOATS, 4);
        }
        if (spheres.count() > 0) {
            sphereTemplate.enableInstancedRendering(false, spheres.count(),
                    new VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_center"),
                    new VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_color"));
        }

        CylinderInstances cylinders = scene.cylinders();
        cylinderData = new float[cylinders.count() * CYLINDER_FLOATS];
        for (int i = 0; i < cylinders.count(); i++) {
            int o = i * CYLINDER_FLOATS;
            System.arraycopy(cylinders.data(), i * CylinderInstances.STRIDE, cylinderData, o, 7);
        }
        if (cylinders.count() > 0) {
            cylinder.enableInstancedRendering(false, cylinders.count(),
                    new VertexAttribute(VertexAttributes.Usage.Generic, 3, "a_start"),
                    new VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_endRadius"),
                    new VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_color"));
        }

        for (TriangleMesh source : scene.meshes()) {
            // Expanded to non-indexed triangles: libGDX index buffers are 16-bit.
            int[] indices = source.indices();
            float[] data = new float[indices.length * MESH_FLOATS];
            int[] atoms = new int[indices.length];
            for (int k = 0; k < indices.length; k++) {
                int v = indices[k];
                System.arraycopy(source.positions(), v * 3, data, k * MESH_FLOATS, 3);
                System.arraycopy(source.normals(), v * 3, data, k * MESH_FLOATS + 3, 3);
                atoms[k] = source.atoms()[v];
            }
            meshData.add(data);
            meshAtoms.add(atoms);
            meshes.add(new Mesh(false, indices.length, 0, new VertexAttributes(VertexAttribute.Position(),
                    VertexAttribute.Normal(), new VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_color"))));
        }
        uploadColours();
    }

    /**
     * Recolours the current scene without rebuilding geometry.
     */
    public void setColours(int[] atomColours) {
        this.atomColours = atomColours;
        uploadColours();
    }

    /**
     * Highlights one atom, or none if {@code atom < 0}.
     */
    public void setHighlight(int atom) {
        if (atom != highlightedAtom) {
            highlightedAtom = atom;
            uploadColours();
        }
    }

    public void render(Camera camera) {
        if (scene == null) {
            return;
        }
        // Light from the viewer's upper left.
        lightDir.set(camera.direction).scl(-1).mulAdd(camera.up, 0.5f)
                .mulAdd(new Vector3(camera.direction).crs(camera.up), -0.35f).nor();
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (scene.spheres().count() > 0) {
            draw(sphereShader, sphereTemplate, camera);
        }
        if (scene.cylinders().count() > 0) {
            draw(cylinderShader, cylinder, camera);
        }
        for (Mesh mesh : meshes) {
            draw(meshShader, mesh, camera);
        }
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
    }

    private void draw(ShaderProgram shader, Mesh mesh, Camera camera) {
        shader.bind();
        shader.setUniformMatrix("u_projView", camera.combined);
        shader.setUniformf("u_cameraPos", camera.position);
        shader.setUniformf("u_lightDir", lightDir);
        mesh.render(shader, GL20.GL_TRIANGLES);
    }

    private void uploadColours() {
        if (scene == null) {
            return;
        }
        SphereInstances spheres = scene.spheres();
        for (int i = 0; i < spheres.count(); i++) {
            putColour(sphereData, i * SPHERE_FLOATS + 4, spheres.atoms()[i]);
        }
        if (spheres.count() > 0) {
            sphereTemplate.setInstanceData(sphereData);
        }
        CylinderInstances cylinders = scene.cylinders();
        for (int i = 0; i < cylinders.count(); i++) {
            putColour(cylinderData, i * CYLINDER_FLOATS + 7, cylinders.atoms()[i]);
        }
        if (cylinders.count() > 0) {
            cylinder.setInstanceData(cylinderData);
        }
        for (int m = 0; m < meshes.size(); m++) {
            float[] data = meshData.get(m);
            int[] atoms = meshAtoms.get(m);
            for (int v = 0; v < atoms.length; v++) {
                putColour(data, v * MESH_FLOATS + 6, atoms[v]);
            }
            meshes.get(m).setVertices(data);
        }
    }

    private void putColour(float[] data, int offset, int atom) {
        int rgb = atom < atomColours.length ? atomColours[atom] : 0xFFFFFF;
        data[offset] = Rgb.red(rgb);
        data[offset + 1] = Rgb.green(rgb);
        data[offset + 2] = Rgb.blue(rgb);
        data[offset + 3] = atom == highlightedAtom ? 2f : 1f;
    }

    private void disableInstancing() {
        for (Mesh mesh : List.of(detailedSphere, coarseSphere, cylinder)) {
            if (mesh.isInstanced()) {
                mesh.disableInstancedRendering();
            }
        }
    }

    private void disposeMeshes() {
        meshes.forEach(Mesh::dispose);
        meshes.clear();
        meshData.clear();
        meshAtoms.clear();
    }

    private static ShaderProgram shader(String vertex) {
        String fragmentSource = Gdx.files.classpath("com/adcworks/jmvs/render/shaders/lit.frag").readString("UTF-8");
        String vertexSource = Gdx.files.classpath("com/adcworks/jmvs/render/shaders/" + vertex).readString("UTF-8");
        ShaderProgram program = new ShaderProgram(vertexSource, fragmentSource);
        if (!program.isCompiled()) {
            throw new GdxRuntimeException("Cannot compile " + vertex + ": " + program.getLog());
        }
        return program;
    }

    @Override
    public void dispose() {
        disableInstancing();
        disposeMeshes();
        detailedSphere.dispose();
        coarseSphere.dispose();
        cylinder.dispose();
        sphereShader.dispose();
        cylinderShader.dispose();
        meshShader.dispose();
    }
}
