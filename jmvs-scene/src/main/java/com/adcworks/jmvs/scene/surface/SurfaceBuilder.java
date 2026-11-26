package com.adcworks.jmvs.scene.surface;

import com.adcworks.jmvs.core.chem.SpatialGrid;
import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.Structure;
import com.adcworks.jmvs.scene.AtomRadii;
import com.adcworks.jmvs.scene.TriangleMesh;
import com.adcworks.jmvs.scene.config.SurfaceSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * Computes molecular surfaces on a grid (ADR 0017).
 * <ul>
 *     <li>Atoms: the first model's heavy atoms, without waters, using united-atom radii (hydrogens are implicit).</li>
 *     <li>Solvent-accessible: the zero level of {@code min(|p − c| − (r + probe))} over atoms.</li>
 *     <li>Solvent-excluded: points farther than the probe radius from everywhere the probe centre can reach, found
 *     with an exact distance transform of the grid points outside the solvent-accessible surface and refined by
 *     the field value at the nearest such point.</li>
 * </ul>
 * The level set is meshed with {@link SurfaceNets}, and each vertex takes the colour of the atom whose van der Waals
 * surface is nearest. Builds are pure computation and safe to run on a background thread.
 */
public final class SurfaceBuilder {

    static final Set<String> WATER = Set.of("HOH", "WAT", "DOD", "H2O");

    private final SurfaceSettings settings;

    public SurfaceBuilder(SurfaceSettings settings) {
        this.settings = settings;
    }

    public Surface build(Structure structure, SurfaceKind kind) {
        return build(structure, kind, () -> false);
    }

    /**
     * Builds the surface, checking {@code cancelled} between stages and throwing {@link CancellationException} if
     * it returns true.
     */
    public Surface build(Structure structure, SurfaceKind kind, BooleanSupplier cancelled) {
        List<Atom> atoms = surfaceAtoms(structure);
        if (atoms.isEmpty()) {
            return new Surface(kind, List.of(), 0);
        }
        float[] radii = new float[atoms.size()];
        float maxRadius = 0;
        for (int i = 0; i < atoms.size(); i++) {
            radii[i] = AtomRadii.united(atoms.get(i));
            maxRadius = Math.max(maxRadius, radii[i]);
        }
        float probe = settings.probeRadius();
        Grid grid = grid(atoms, maxRadius + probe);
        float band = 2 * grid.spacing();
        float[] sas = accessibleField(grid, atoms, radii, probe, band, cancelled);
        float[] field = kind == SurfaceKind.SAS ? sas : excludedField(grid, sas, probe, cancelled);
        check(cancelled);
        SurfaceNets.Mesh mesh = SurfaceNets.extract(grid, field, cancelled);
        check(cancelled);
        int[] vertexAtoms = vertexAtoms(mesh, atoms, radii, maxRadius + probe + band);
        return new Surface(kind, chunks(mesh, vertexAtoms), area(mesh));
    }

    static List<Atom> surfaceAtoms(Structure structure) {
        if (structure.models().isEmpty()) {
            return List.of();
        }
        List<Atom> atoms = new ArrayList<>();
        for (Atom atom : structure.atomsOf(structure.models().getFirst())) {
            if (!atom.isHydrogen() && !WATER.contains(structure.residueOf(atom).name())) {
                atoms.add(atom);
            }
        }
        return atoms;
    }

    /**
     * Grid covering the atoms plus {@code reach} and a two-point margin, coarsened to fit {@code maxVoxels}.
     */
    private Grid grid(List<Atom> atoms, float reach) {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        for (Atom a : atoms) {
            minX = Math.min(minX, a.x());
            minY = Math.min(minY, a.y());
            minZ = Math.min(minZ, a.z());
            maxX = Math.max(maxX, a.x());
            maxY = Math.max(maxY, a.y());
            maxZ = Math.max(maxZ, a.z());
        }
        float spacing = settings.spacing();
        while (true) {
            float margin = reach + 2 * spacing;
            int nx = points(maxX - minX + 2 * margin, spacing);
            int ny = points(maxY - minY + 2 * margin, spacing);
            int nz = points(maxZ - minZ + 2 * margin, spacing);
            if ((long) nx * ny * nz <= settings.maxVoxels()) {
                return new Grid(minX - margin, minY - margin, minZ - margin, spacing, nx, ny, nz);
            }
            spacing *= (float) Math.cbrt((double) nx * ny * nz / settings.maxVoxels()) * 1.01f;
        }
    }

    private static int points(float extent, float spacing) {
        return (int) Math.ceil(extent / spacing) + 1;
    }

    /**
     * Signed distance to the solvent-accessible surface, exact within {@code band} of it and clamped to
     * {@code band} beyond.
     */
    private static float[] accessibleField(Grid grid, List<Atom> atoms, float[] radii, float probe, float band,
                                           BooleanSupplier cancelled) {
        float[] field = new float[grid.size()];
        Arrays.fill(field, band);
        float s = grid.spacing();
        for (int n = 0; n < atoms.size(); n++) {
            if ((n & 255) == 0) {
                check(cancelled);
            }
            Atom a = atoms.get(n);
            float expanded = radii[n] + probe;
            float reach = expanded + band;
            int x0 = Math.max(0, (int) Math.floor((a.x() - reach - grid.originX()) / s));
            int x1 = Math.min(grid.nx() - 1, (int) Math.ceil((a.x() + reach - grid.originX()) / s));
            int y0 = Math.max(0, (int) Math.floor((a.y() - reach - grid.originY()) / s));
            int y1 = Math.min(grid.ny() - 1, (int) Math.ceil((a.y() + reach - grid.originY()) / s));
            int z0 = Math.max(0, (int) Math.floor((a.z() - reach - grid.originZ()) / s));
            int z1 = Math.min(grid.nz() - 1, (int) Math.ceil((a.z() + reach - grid.originZ()) / s));
            for (int k = z0; k <= z1; k++) {
                float dz = grid.z(k) - a.z();
                for (int j = y0; j <= y1; j++) {
                    float dy = grid.y(j) - a.y();
                    float dyz = dy * dy + dz * dz;
                    int row = grid.index(0, j, k);
                    for (int i = x0; i <= x1; i++) {
                        float dx = grid.x(i) - a.x();
                        float value = (float) Math.sqrt(dx * dx + dyz) - expanded;
                        if (value < field[row + i]) {
                            field[row + i] = value;
                        }
                    }
                }
            }
        }
        return field;
    }

    /**
     * {@code probe − d}, where {@code d} is the distance to the region the probe centre can reach ({@code sas ≥ 0}).
     * The distance to the nearest reachable grid point overestimates {@code d} by up to a grid step, so it is
     * reduced by that point's own distance outside the accessible surface.
     */
    private static float[] excludedField(Grid grid, float[] sas, float probe, BooleanSupplier cancelled) {
        boolean[] reachable = new boolean[sas.length];
        for (int i = 0; i < sas.length; i++) {
            reachable[i] = sas[i] >= 0;
        }
        check(cancelled);
        DistanceTransform.Result transform = DistanceTransform.of(reachable, grid.nx(), grid.ny(), grid.nz());
        check(cancelled);
        float[] field = new float[sas.length];
        float s = grid.spacing();
        for (int i = 0; i < sas.length; i++) {
            int nearest = transform.nearest()[i];
            if (nearest < 0) {
                field[i] = -probe;
                continue;
            }
            float d = (float) Math.sqrt(transform.distanceSquared()[i]) * s - sas[nearest];
            field[i] = probe - d;
        }
        return field;
    }

    /**
     * For each vertex, the atom whose van der Waals surface is closest.
     */
    private static int[] vertexAtoms(SurfaceNets.Mesh mesh, List<Atom> atoms, float[] radii, float reach) {
        int[] slot = new int[atoms.getLast().index() + 1];
        for (int i = 0; i < atoms.size(); i++) {
            slot[atoms.get(i).index()] = i;
        }
        SpatialGrid spatial = SpatialGrid.of(atoms, reach);
        float[] p = mesh.positions();
        int[] result = new int[mesh.vertexCount()];
        var best = new Object() {
            int atom;
            float gap;
        };
        for (int v = 0; v < result.length; v++) {
            float x = p[v * 3], y = p[v * 3 + 1], z = p[v * 3 + 2];
            best.atom = -1;
            best.gap = Float.POSITIVE_INFINITY;
            spatial.forEachCandidate(x, y, z, reach, a -> {
                float dx = a.x() - x, dy = a.y() - y, dz = a.z() - z;
                float gap = (float) Math.sqrt(dx * dx + dy * dy + dz * dz) - radii[slot[a.index()]];
                if (gap < best.gap || gap == best.gap && a.index() < best.atom) {
                    best.gap = gap;
                    best.atom = a.index();
                }
            });
            result[v] = best.atom >= 0 ? best.atom : atoms.getFirst().index();
        }
        return result;
    }

    /**
     * Splits the welded mesh into meshes of at most {@link Surface#MAX_MESH_VERTICES} vertices, duplicating
     * vertices shared across a split.
     */
    static List<TriangleMesh> chunks(SurfaceNets.Mesh mesh, int[] vertexAtoms) {
        int[] tris = mesh.triangles();
        int[] local = new int[mesh.vertexCount()];
        int[] stamp = new int[mesh.vertexCount()];
        Arrays.fill(stamp, -1);
        List<TriangleMesh> chunks = new ArrayList<>();
        int chunk = 0;
        int[] vertices = new int[Surface.MAX_MESH_VERTICES];
        int vertexCount = 0;
        SurfaceNets.IntList indices = new SurfaceNets.IntList();
        for (int t = 0; t < tris.length; t += 3) {
            int fresh = 0;
            for (int c = 0; c < 3; c++) {
                if (stamp[tris[t + c]] != chunk) {
                    fresh++;
                }
            }
            if (vertexCount + fresh > Surface.MAX_MESH_VERTICES) {
                chunks.add(chunk(mesh, vertexAtoms, vertices, vertexCount, indices.toArray()));
                chunk++;
                vertexCount = 0;
                indices = new SurfaceNets.IntList();
            }
            int[] tri = new int[3];
            for (int c = 0; c < 3; c++) {
                int v = tris[t + c];
                if (stamp[v] != chunk) {
                    stamp[v] = chunk;
                    local[v] = vertexCount;
                    vertices[vertexCount++] = v;
                }
                tri[c] = local[v];
            }
            indices.add(tri[0], tri[1], tri[2]);
        }
        if (indices.size() > 0) {
            chunks.add(chunk(mesh, vertexAtoms, vertices, vertexCount, indices.toArray()));
        }
        return chunks;
    }

    private static TriangleMesh chunk(SurfaceNets.Mesh mesh, int[] vertexAtoms, int[] vertices, int count,
                                      int[] indices) {
        float[] positions = new float[count * 3];
        float[] normals = new float[count * 3];
        int[] atoms = new int[count];
        for (int i = 0; i < count; i++) {
            int v = vertices[i];
            System.arraycopy(mesh.positions(), v * 3, positions, i * 3, 3);
            System.arraycopy(mesh.normals(), v * 3, normals, i * 3, 3);
            atoms[i] = vertexAtoms[v];
        }
        return new TriangleMesh(positions, normals, atoms, indices);
    }

    static double area(SurfaceNets.Mesh mesh) {
        float[] p = mesh.positions();
        int[] t = mesh.triangles();
        double area = 0;
        for (int i = 0; i < t.length; i += 3) {
            int a = t[i] * 3, b = t[i + 1] * 3, c = t[i + 2] * 3;
            double ux = p[b] - p[a], uy = p[b + 1] - p[a + 1], uz = p[b + 2] - p[a + 2];
            double vx = p[c] - p[a], vy = p[c + 1] - p[a + 1], vz = p[c + 2] - p[a + 2];
            double cx = uy * vz - uz * vy, cy = uz * vx - ux * vz, cz = ux * vy - uy * vx;
            area += Math.sqrt(cx * cx + cy * cy + cz * cz) / 2;
        }
        return area;
    }

    private static void check(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) {
            throw new CancellationException("Surface build cancelled");
        }
    }
}
