package com.adcworks.jmvs.render;

import com.adcworks.jmvs.scene.CylinderInstances;
import com.adcworks.jmvs.scene.Scene;
import com.adcworks.jmvs.scene.SphereInstances;
import com.adcworks.jmvs.scene.TriangleMesh;
import com.adcworks.jmvs.scene.colour.Rgb;
import com.adcworks.jmvs.scene.density.DensityStyle;
import com.adcworks.jmvs.scene.density.Isosurface;
import com.adcworks.jmvs.scene.surface.Surface;
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
import java.util.Optional;

/**
 * Draws a {@link Scene} with instanced spheres and cylinders and plain triangle meshes, and optionally a
 * {@link Surface} and a density map {@link Isosurface} over it, each opaque or transparent; the isosurface as a
 * surface or as lines. Geometry is uploaded once per scene, surface or isosurface; {@link #setColours(int[])} and
 * {@link #setHighlight(int)} only rewrite colour data. Must be used on the render thread.
 */
public final class StructureRenderer implements Disposable {

    private static final int SPHERE_FLOATS = 8;
    private static final int CYLINDER_FLOATS = 11;
    private static final int MESH_FLOATS = 10;
    /** Most vertices a mesh can have with libGDX's 16-bit index buffers. */
    private static final int MAX_INDEXED_VERTICES = 65_535;
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
    private final List<float[]> surfaceData = new ArrayList<>();
    private final List<int[]> surfaceAtoms = new ArrayList<>();
    private final List<Mesh> surfaceMeshes = new ArrayList<>();
    private float surfaceOpacity = 1f;
    private Isosurface density;
    private DensityStyle densityStyle = DensityStyle.MESH;
    private int densityColour = 0xA0A0A0;
    private final List<float[]> densityData = new ArrayList<>();
    private final List<int[]> densityAtoms = new ArrayList<>();
    private final List<Mesh> densityMeshes = new ArrayList<>();
    private float densityOpacity = 1f;
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
            if (source.vertexCount() <= MAX_INDEXED_VERTICES) {
                addIndexedMesh(source, source.indices(), meshData, meshAtoms, meshes);
                continue;
            }
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
     * Uploads a surface to draw over the scene, or removes it.
     */
    public void setSurface(Optional<Surface> surface) {
        disposeSurface();
        // Surface guarantees at most 65,535 vertices per mesh, within libGDX's 16-bit indices.
        surface.ifPresent(s -> s.meshes().forEach(source ->
                addIndexedMesh(source, source.indices(), surfaceData, surfaceAtoms, surfaceMeshes)));
        uploadSurfaceColours();
    }

    /**
     * Uploads a density map contour to draw over the scene, or removes it. Can be shown without a scene.
     */
    public void setDensity(Optional<Isosurface> isosurface) {
        density = isosurface.orElse(null);
        uploadDensity();
    }

    /**
     * Draws the density contour as a surface or as lines; re-uploads its indices, not a rebuild.
     */
    public void setDensityStyle(DensityStyle style) {
        if (style != densityStyle) {
            densityStyle = style;
            uploadDensity();
        }
    }

    /**
     * Density contour opacity from 0 (invisible) to 1 (opaque).
     */
    public void setDensityOpacity(float opacity) {
        densityOpacity = Math.clamp(opacity, 0f, 1f);
    }

    /**
     * Colour of density contour vertices without a nearby atom, packed {@code 0xRRGGBB}.
     */
    public void setDensityColour(int rgb) {
        densityColour = rgb;
        uploadDensityColours();
    }

    private void uploadDensity() {
        disposeDensity();
        if (density != null) {
            for (int m = 0; m < density.meshes().size(); m++) {
                TriangleMesh source = density.meshes().get(m);
                int[] indices = densityStyle == DensityStyle.MESH ? density.lines().get(m) : source.indices();
                addIndexedMesh(source, indices, densityData, densityAtoms, densityMeshes);
            }
        }
        uploadDensityColours();
    }

    /**
     * Uploads a mesh of at most {@link #MAX_INDEXED_VERTICES} vertices with 16-bit indices (triangles or lines);
     * colours are filled in later from {@code atoms}.
     */
    private static void addIndexedMesh(TriangleMesh source, int[] indices, List<float[]> dataOut,
                                       List<int[]> atomsOut, List<Mesh> meshesOut) {
        int count = source.vertexCount();
        float[] data = new float[count * MESH_FLOATS];
        for (int v = 0; v < count; v++) {
            System.arraycopy(source.positions(), v * 3, data, v * MESH_FLOATS, 3);
            System.arraycopy(source.normals(), v * 3, data, v * MESH_FLOATS + 3, 3);
        }
        short[] shortIndices = new short[indices.length];
        for (int i = 0; i < indices.length; i++) {
            shortIndices[i] = (short) indices[i];
        }
        Mesh mesh = new Mesh(true, count, indices.length, new VertexAttributes(VertexAttribute.Position(),
                VertexAttribute.Normal(), new VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_color")));
        mesh.setIndices(shortIndices);
        dataOut.add(data);
        atomsOut.add(source.atoms());
        meshesOut.add(mesh);
    }

    /**
     * Surface opacity from 0 (invisible) to 1 (opaque).
     */
    public void setSurfaceOpacity(float opacity) {
        surfaceOpacity = Math.clamp(opacity, 0f, 1f);
    }

    /**
     * Recolours the current scene and surface without rebuilding geometry.
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
        if (scene == null && density == null) {
            return;
        }
        // Light from the viewer's upper left.
        lightDir.set(camera.direction).scl(-1).mulAdd(camera.up, 0.5f)
                .mulAdd(new Vector3(camera.direction).crs(camera.up), -0.35f).nor();
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (scene != null) {
            if (scene.spheres().count() > 0) {
                draw(sphereShader, sphereTemplate, camera);
            }
            if (scene.cylinders().count() > 0) {
                draw(cylinderShader, cylinder, camera);
            }
            for (Mesh mesh : meshes) {
                draw(meshShader, mesh, camera, 1f);
            }
            renderOverlay(surfaceMeshes, surfaceOpacity, GL20.GL_TRIANGLES, camera);
        }
        renderOverlay(densityMeshes, densityOpacity,
                densityStyle == DensityStyle.MESH ? GL20.GL_LINES : GL20.GL_TRIANGLES, camera);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
    }

    /**
     * Opaque overlays (surfaces, density contours) draw normally. Transparent ones first write depth only, then
     * blend only their nearest layer, so overlapping folds don't need sorting; blending keeps the framebuffer's
     * alpha for screenshots.
     */
    private void renderOverlay(List<Mesh> overlay, float opacity, int primitive, Camera camera) {
        if (overlay.isEmpty() || opacity <= 0f) {
            return;
        }
        if (opacity >= 1f) {
            for (Mesh mesh : overlay) {
                draw(meshShader, mesh, camera, 1f, primitive);
            }
            return;
        }
        Gdx.gl.glColorMask(false, false, false, false);
        for (Mesh mesh : overlay) {
            draw(meshShader, mesh, camera, opacity, primitive);
        }
        Gdx.gl.glColorMask(true, true, true, true);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFuncSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ZERO, GL20.GL_ONE);
        for (Mesh mesh : overlay) {
            draw(meshShader, mesh, camera, opacity, primitive);
        }
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void draw(ShaderProgram shader, Mesh mesh, Camera camera) {
        draw(shader, mesh, camera, 1f);
    }

    private void draw(ShaderProgram shader, Mesh mesh, Camera camera, float opacity) {
        draw(shader, mesh, camera, opacity, GL20.GL_TRIANGLES);
    }

    private void draw(ShaderProgram shader, Mesh mesh, Camera camera, float opacity, int primitive) {
        shader.bind();
        shader.setUniformMatrix("u_projView", camera.combined);
        shader.setUniformf("u_cameraPos", camera.position);
        shader.setUniformf("u_lightDir", lightDir);
        shader.setUniformf("u_opacity", opacity);
        mesh.render(shader, primitive);
    }

    private void uploadColours() {
        uploadDensityColours();
        if (scene == null) {
            return;
        }
        SphereInstances spheres = scene.spheres();
        for (int i = 0; i < spheres.count(); i++) {
            putColour(sphereData, i * SPHERE_FLOATS + 4, spheres.atoms()[i], 0xFFFFFF);
        }
        if (spheres.count() > 0) {
            sphereTemplate.setInstanceData(sphereData);
        }
        CylinderInstances cylinders = scene.cylinders();
        for (int i = 0; i < cylinders.count(); i++) {
            putColour(cylinderData, i * CYLINDER_FLOATS + 7, cylinders.atoms()[i], 0xFFFFFF);
        }
        if (cylinders.count() > 0) {
            cylinder.setInstanceData(cylinderData);
        }
        for (int m = 0; m < meshes.size(); m++) {
            float[] data = meshData.get(m);
            int[] atoms = meshAtoms.get(m);
            for (int v = 0; v < atoms.length; v++) {
                putColour(data, v * MESH_FLOATS + 6, atoms[v], 0xFFFFFF);
            }
            meshes.get(m).setVertices(data);
        }
        uploadSurfaceColours();
    }

    private void uploadDensityColours() {
        for (int m = 0; m < densityMeshes.size(); m++) {
            float[] data = densityData.get(m);
            int[] atoms = densityAtoms.get(m);
            for (int v = 0; v < atoms.length; v++) {
                putColour(data, v * MESH_FLOATS + 6, atoms[v], densityColour);
            }
            densityMeshes.get(m).setVertices(data);
        }
    }

    private void uploadSurfaceColours() {
        for (int m = 0; m < surfaceMeshes.size(); m++) {
            float[] data = surfaceData.get(m);
            int[] atoms = surfaceAtoms.get(m);
            for (int v = 0; v < atoms.length; v++) {
                putColour(data, v * MESH_FLOATS + 6, atoms[v], 0xFFFFFF);
            }
            surfaceMeshes.get(m).setVertices(data);
        }
    }

    /**
     * Writes the atom's colour, or {@code fallback} for an atom index outside the colours (such as
     * {@link Isosurface#NO_ATOM}).
     */
    private void putColour(float[] data, int offset, int atom, int fallback) {
        int rgb = atom >= 0 && atom < atomColours.length ? atomColours[atom] : fallback;
        data[offset] = Rgb.red(rgb);
        data[offset + 1] = Rgb.green(rgb);
        data[offset + 2] = Rgb.blue(rgb);
        data[offset + 3] = atom >= 0 && atom == highlightedAtom ? 2f : 1f;
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

    private void disposeDensity() {
        densityMeshes.forEach(Mesh::dispose);
        densityMeshes.clear();
        densityData.clear();
        densityAtoms.clear();
    }

    private void disposeSurface() {
        surfaceMeshes.forEach(Mesh::dispose);
        surfaceMeshes.clear();
        surfaceData.clear();
        surfaceAtoms.clear();
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
        disposeSurface();
        disposeDensity();
        detailedSphere.dispose();
        coarseSphere.dispose();
        cylinder.dispose();
        sphereShader.dispose();
        cylinderShader.dispose();
        meshShader.dispose();
    }
}
