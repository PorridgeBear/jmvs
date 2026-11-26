package com.adcworks.jmvs.scene.mode;

import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.Chain;
import com.adcworks.jmvs.core.model.Residue;
import com.adcworks.jmvs.core.model.SecondaryStructure;
import com.adcworks.jmvs.core.model.Structure;
import com.adcworks.jmvs.scene.CylinderInstances;
import com.adcworks.jmvs.scene.Scene;
import com.adcworks.jmvs.scene.SphereInstances;
import com.adcworks.jmvs.scene.TriangleMesh;
import com.adcworks.jmvs.scene.colour.ColourScheme;
import com.adcworks.jmvs.scene.config.CartoonSettings;
import com.adcworks.jmvs.scene.mode.BackboneGuides.Guide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Cartoon: a cross-section swept along a B-spline through the backbone. Proteins: helices as wide, thick ribbons,
 * strands as flat slabs ending in an arrowhead, coil as a thin tube; the ribbon faces follow the Carson–Bugg
 * peptide-plane width direction. Nucleic acids: a tube through the phosphates with a stick and a block per base,
 * from C1' to the Watson–Crick edge (N1 of purines, N3 of pyrimidines). ADR 0019.
 *
 * <p>Cross-sections are superellipses, {@code |x/w|^e + |y/h|^e = 1}: e = 2 gives the ellipse and circle of
 * helices and coil, a larger e the squarer strands, so one ring topology blends between all of them.
 */
public final class CartoonMode implements DisplayMode {

    /** Vertices per cross-section ring. */
    static final int RING = 16;
    static final int SPLINE_ORDER = 3;
    static final float PICK_RADIUS = 1.5f;
    /** Consecutive nucleic-acid backbone atoms further apart than this (Å) start a new segment. */
    static final float MAX_NUCLEIC_DISTANCE = 8.0f;
    /** Rings per mesh, so each mesh stays within 16-bit indices (65,535 vertices) with its end caps. */
    static final int MAX_RINGS_PER_MESH = (65_535 - 2 * (RING + 1)) / RING;
    static final double ROUND = 2;
    static final double SQUARE = 6;
    /** Half-width of a base block in Å. */
    static final float BASE_HALF_WIDTH = 1.1f;
    /** Vertices of one base block: six faces of four. */
    static final int BLOCK_VERTICES = 24;

    private record Profile(double halfWidth, double halfThickness, double exponent) {

        static Profile lerp(Profile a, Profile b, double f) {
            return new Profile(a.halfWidth + (b.halfWidth - a.halfWidth) * f,
                    a.halfThickness + (b.halfThickness - a.halfThickness) * f,
                    a.exponent + (b.exponent - a.exponent) * f);
        }
    }

    /** One cross-section: centre, frame (width and normal directions) and size. */
    private record Ring(double[] centre, double[] tangent, double[] width, double[] normal, Profile profile,
                        int atom) {
    }

    private final CartoonSettings settings;

    public CartoonMode() {
        this(CartoonSettings.DEFAULTS);
    }

    public CartoonMode(CartoonSettings settings) {
        this.settings = settings;
    }

    @Override
    public String id() {
        return "cartoon";
    }

    @Override
    public String displayName() {
        return "Cartoon";
    }

    @Override
    public boolean supports(ColourScheme scheme) {
        return !scheme.id().equals("cpk");
    }

    @Override
    public Scene build(Structure structure) {
        List<TriangleMesh> meshes = new ArrayList<>();
        List<Atom> pickAtoms = new ArrayList<>();
        MeshBuilder bases = new MeshBuilder();
        List<float[]> sticks = new ArrayList<>();
        List<Integer> stickAtoms = new ArrayList<>();
        for (Chain chain : structure.chains()) {
            for (List<Guide> segment : BackboneGuides.segments(structure, chain)) {
                if (segment.size() >= 2) {
                    sweep(proteinRings(segment), meshes);
                    segment.forEach(g -> pickAtoms.add(g.ca()));
                }
            }
            for (List<Nucleotide> segment : nucleicSegments(structure, chain)) {
                if (segment.size() >= 2) {
                    sweep(nucleicRings(segment), meshes);
                }
                for (Nucleotide n : segment) {
                    pickAtoms.add(n.backbone);
                    if (n.base.isEmpty()) {
                        continue;
                    }
                    if (bases.vertexCount() + BLOCK_VERTICES > 65_535) {
                        meshes.add(bases.build());
                        bases = new MeshBuilder();
                    }
                    baseBlock(n.base.get(), n.c1, bases);
                    sticks.add(new float[]{n.backbone.x(), n.backbone.y(), n.backbone.z(),
                            n.c1.x(), n.c1.y(), n.c1.z(), settings.coilRadius()});
                    stickAtoms.add(n.c1.index());
                    pickAtoms.add(n.c1);
                }
            }
        }
        if (bases.vertexCount() > 0) {
            meshes.add(bases.build());
        }
        float[] stickData = new float[sticks.size() * CylinderInstances.STRIDE];
        for (int i = 0; i < sticks.size(); i++) {
            System.arraycopy(sticks.get(i), 0, stickData, i * CylinderInstances.STRIDE, CylinderInstances.STRIDE);
        }
        float[] pickData = new float[pickAtoms.size() * SphereInstances.STRIDE];
        int[] pickIndices = new int[pickAtoms.size()];
        for (int i = 0; i < pickAtoms.size(); i++) {
            Atom a = pickAtoms.get(i);
            int o = i * SphereInstances.STRIDE;
            pickData[o] = a.x();
            pickData[o + 1] = a.y();
            pickData[o + 2] = a.z();
            pickData[o + 3] = PICK_RADIUS;
            pickIndices[i] = a.index();
        }
        return new Scene(SphereInstances.EMPTY,
                new CylinderInstances(stickData, stickAtoms.stream().mapToInt(Integer::intValue).toArray()),
                meshes, new SphereInstances(pickData, pickIndices), structure.boundingSphere());
    }

    // ---- Proteins ----

    private List<Ring> proteinRings(List<Guide> guides) {
        int n = guides.size();
        float[] centres = new float[n * 3];
        float[] directions = new float[n * 3];
        double[] previous = null;
        for (int i = 0; i < n; i++) {
            Atom ca = guides.get(i).ca();
            centres[i * 3] = ca.x();
            centres[i * 3 + 1] = ca.y();
            centres[i * 3 + 2] = ca.z();
            double[] d = i < n - 1 ? BackboneGuides.widthDirection(guides.get(i), guides.get(i + 1)) : previous;
            // Keep the ribbon from twisting where the carbonyl oxygen flips side, as in Ribbons.
            if (previous != null && Vectors.dot(previous, d) < 0) {
                d = Vectors.scale(d, -1);
            }
            previous = d;
            for (int k = 0; k < 3; k++) {
                directions[i * 3 + k] = (float) d[k];
            }
        }
        int samplesPerResidue = settings.samplesPerResidue();
        int samples = (n - 1) * samplesPerResidue + 1;
        int order = Math.min(SPLINE_ORDER, n);
        float[] path = BSpline.sample(centres, order, samples);
        float[] widths = BSpline.sample(directions, order, samples);

        Profile[] base = new Profile[n];
        boolean[] arrow = new boolean[n];
        for (int i = 0; i < n; i++) {
            SecondaryStructure ss = guides.get(i).residue().secondaryStructure();
            base[i] = profileOf(ss);
            arrow[i] = ss == SecondaryStructure.SHEET
                    && (i == n - 1 || guides.get(i + 1).residue().secondaryStructure() != SecondaryStructure.SHEET);
        }
        List<Ring> rings = new ArrayList<>(samples);
        for (int s = 0; s < samples; s++) {
            double t = s / (double) samplesPerResidue;
            int k = Math.clamp(Math.round(t), 0, n - 1);
            double u = Math.clamp(t - (k - 0.5), 0, 1);
            Profile profile = profile(base, arrow, k, u);
            rings.add(ring(path, widths, s, samples, profile, guides.get(k).ca().index()));
        }
        return rings;
    }

    private Profile profileOf(SecondaryStructure ss) {
        return switch (ss) {
            case HELIX -> new Profile(settings.helixWidth() / 2, settings.helixThickness() / 2, ROUND);
            case SHEET -> new Profile(settings.sheetWidth() / 2, settings.sheetThickness() / 2, SQUARE);
            case TURN, COIL -> coil();
        };
    }

    private Profile coil() {
        return new Profile(settings.coilRadius(), settings.coilRadius(), ROUND);
    }

    /**
     * Cross-section at fraction {@code u} (0..1) across residue {@code k}'s span. Arrowheads taper from the arrow
     * width to the coil radius over the last strand residue, starting abruptly; other changes blend linearly over
     * the half residues either side of a boundary.
     */
    private Profile profile(Profile[] base, boolean[] arrow, int k, double u) {
        if (arrow[k]) {
            Profile start = new Profile(settings.arrowWidth() / 2, settings.sheetThickness() / 2, SQUARE);
            return Profile.lerp(start, coil(), u);
        }
        if (u > 0.5 && k + 1 < base.length && !arrow[k + 1]) {
            return Profile.lerp(base[k], base[k + 1], (u - 0.5));
        }
        if (u < 0.5 && k > 0 && !arrow[k - 1]) {
            return Profile.lerp(base[k], base[k - 1], (0.5 - u));
        }
        return base[k];
    }

    // ---- Nucleic acids ----

    /**
     * A nucleotide: the backbone atom the tube passes through (P, or C4' at a 5' end without phosphate), C1', and
     * the base's Watson–Crick edge atom if the base is present.
     */
    private record Nucleotide(Atom backbone, Atom c1, Optional<Base> base) {
    }

    private record Base(Atom edge, Atom inPlane) {
    }

    private static List<List<Nucleotide>> nucleicSegments(Structure structure, Chain chain) {
        List<List<Nucleotide>> segments = new ArrayList<>();
        List<Nucleotide> current = new ArrayList<>();
        for (Residue residue : structure.residuesOf(chain)) {
            Optional<Nucleotide> nucleotide = nucleotide(structure, residue);
            if (nucleotide.isEmpty()) {
                continue;
            }
            Nucleotide n = nucleotide.get();
            if (!current.isEmpty()
                    && Vectors.distance(current.getLast().backbone, n.backbone) > MAX_NUCLEIC_DISTANCE) {
                segments.add(current);
                current = new ArrayList<>();
            }
            current.add(n);
        }
        segments.add(current);
        return segments;
    }

    private static Optional<Nucleotide> nucleotide(Structure structure, Residue residue) {
        if (!residue.polymer() || structure.findAtom(residue, "CA").isPresent()) {
            return Optional.empty();
        }
        Optional<Atom> c1 = structure.findAtom(residue, "C1'");
        Optional<Atom> backbone = structure.findAtom(residue, "P").or(() -> structure.findAtom(residue, "C4'"));
        if (c1.isEmpty() || backbone.isEmpty()) {
            return Optional.empty();
        }
        boolean purine = structure.findAtom(residue, "N9").isPresent();
        Optional<Atom> edge = structure.findAtom(residue, purine ? "N1" : "N3");
        Optional<Atom> inPlane = structure.findAtom(residue, "C5");
        Optional<Base> base = edge.isPresent() && inPlane.isPresent()
                ? Optional.of(new Base(edge.get(), inPlane.get())) : Optional.empty();
        return Optional.of(new Nucleotide(backbone.get(), c1.get(), base));
    }

    private List<Ring> nucleicRings(List<Nucleotide> nucleotides) {
        int n = nucleotides.size();
        float[] centres = new float[n * 3];
        for (int i = 0; i < n; i++) {
            Atom a = nucleotides.get(i).backbone;
            centres[i * 3] = a.x();
            centres[i * 3 + 1] = a.y();
            centres[i * 3 + 2] = a.z();
        }
        int samplesPerResidue = settings.samplesPerResidue();
        int samples = (n - 1) * samplesPerResidue + 1;
        float[] path = BSpline.sample(centres, Math.min(SPLINE_ORDER, n), samples);
        // A round tube needs no particular orientation: carry the first frame along (parallel transport).
        float[] widths = new float[samples * 3];
        double[] w = null;
        for (int s = 0; s < samples; s++) {
            double[] tangent = tangent(path, s, samples);
            w = w == null ? Vectors.perpendicular(tangent)
                    : Vectors.normalise(Vectors.sub(w, Vectors.scale(tangent, Vectors.dot(w, tangent))));
            for (int k = 0; k < 3; k++) {
                widths[s * 3 + k] = (float) w[k];
            }
        }
        Profile tube = new Profile(settings.nucleicRadius(), settings.nucleicRadius(), ROUND);
        List<Ring> rings = new ArrayList<>(samples);
        for (int s = 0; s < samples; s++) {
            int k = Math.clamp(Math.round(s / (double) samplesPerResidue), 0, n - 1);
            rings.add(ring(path, widths, s, samples, tube, nucleotides.get(k).backbone.index()));
        }
        return rings;
    }

    /** A flat box from C1' to the base's Watson–Crick edge, lying in the base plane. */
    private void baseBlock(Base base, Atom c1, MeshBuilder out) {
        double[] start = Vectors.position(c1);
        double[] end = Vectors.position(base.edge);
        double[] axis = Vectors.normalise(Vectors.sub(end, start));
        double[] toC5 = Vectors.sub(Vectors.position(base.inPlane), start);
        double[] width = Vectors.normalise(Vectors.sub(toC5, Vectors.scale(axis, Vectors.dot(toC5, axis))));
        double[] normal = Vectors.cross(axis, width);
        double halfThickness = settings.sheetThickness() / 2;
        double[] w = Vectors.scale(width, BASE_HALF_WIDTH);
        double[] h = Vectors.scale(normal, halfThickness);
        double[][] corners = new double[8][];
        for (int i = 0; i < 8; i++) {
            double[] p = (i & 4) == 0 ? start : end;
            p = Vectors.add(p, (i & 1) == 0 ? Vectors.scale(w, -1) : w);
            corners[i] = Vectors.add(p, (i & 2) == 0 ? Vectors.scale(h, -1) : h);
        }
        int atom = c1.index();
        // Faces as corner indices, counter-clockwise seen from outside, with their normals.
        out.quad(corners[0], corners[1], corners[3], corners[2], Vectors.scale(axis, -1), atom);
        out.quad(corners[4], corners[6], corners[7], corners[5], axis, atom);
        out.quad(corners[0], corners[4], corners[5], corners[1], Vectors.scale(normal, -1), atom);
        out.quad(corners[2], corners[3], corners[7], corners[6], normal, atom);
        out.quad(corners[0], corners[2], corners[6], corners[4], Vectors.scale(width, -1), atom);
        out.quad(corners[1], corners[5], corners[7], corners[3], width, atom);
    }

    // ---- Sweeping ----

    private static Ring ring(float[] path, float[] widths, int s, int samples, Profile profile, int atom) {
        double[] centre = {path[s * 3], path[s * 3 + 1], path[s * 3 + 2]};
        double[] tangent = tangent(path, s, samples);
        double[] d = {widths[s * 3], widths[s * 3 + 1], widths[s * 3 + 2]};
        double[] width = Vectors.sub(d, Vectors.scale(tangent, Vectors.dot(d, tangent)));
        width = Vectors.dot(width, width) < 1e-12 ? Vectors.perpendicular(tangent) : Vectors.normalise(width);
        double[] normal = Vectors.cross(tangent, width);
        return new Ring(centre, tangent, width, normal, profile, atom);
    }

    private static double[] tangent(float[] path, int s, int samples) {
        int a = Math.max(0, s - 1), b = Math.min(samples - 1, s + 1);
        double[] t = {path[b * 3] - path[a * 3], path[b * 3 + 1] - path[a * 3 + 1], path[b * 3 + 2] - path[a * 3 + 2]};
        return Vectors.normalise(t);
    }

    /** Emits the swept surface of the rings, split into meshes of at most {@link #MAX_RINGS_PER_MESH} rings. */
    private static void sweep(List<Ring> rings, List<TriangleMesh> meshes) {
        for (int from = 0; from < rings.size() - 1; from += MAX_RINGS_PER_MESH - 1) {
            int to = Math.min(rings.size(), from + MAX_RINGS_PER_MESH);
            MeshBuilder mesh = new MeshBuilder();
            List<Ring> chunk = rings.subList(from, to);
            int first = mesh.vertexCount();
            for (Ring ring : chunk) {
                addRing(ring, mesh);
            }
            for (int r = 0; r < chunk.size() - 1; r++) {
                int a = first + r * RING, b = a + RING;
                for (int j = 0; j < RING; j++) {
                    int j1 = (j + 1) % RING;
                    mesh.triangle(a + j, a + j1, b + j);
                    mesh.triangle(b + j, a + j1, b + j1);
                }
            }
            if (from == 0) {
                cap(chunk.getFirst(), -1, mesh);
            }
            if (to == rings.size()) {
                cap(chunk.getLast(), 1, mesh);
            }
            meshes.add(mesh.build());
        }
    }

    private static void addRing(Ring ring, MeshBuilder mesh) {
        Profile p = ring.profile;
        double e = p.exponent;
        for (int j = 0; j < RING; j++) {
            double theta = 2 * Math.PI * j / RING;
            double c = Math.cos(theta), s = Math.sin(theta);
            double x = p.halfWidth * signedPower(c, 2 / e);
            double y = p.halfThickness * signedPower(s, 2 / e);
            double nx = signedPower(c, 2 - 2 / e) / p.halfWidth;
            double ny = signedPower(s, 2 - 2 / e) / p.halfThickness;
            double[] position = Vectors.add(ring.centre,
                    Vectors.add(Vectors.scale(ring.width, x), Vectors.scale(ring.normal, y)));
            double[] normal = Vectors.normalise(
                    Vectors.add(Vectors.scale(ring.width, nx), Vectors.scale(ring.normal, ny)));
            mesh.vertex(position, normal, ring.atom);
        }
    }

    /** Closes an end of the tube with a fan facing {@code direction} (-1 backwards, +1 forwards). */
    private static void cap(Ring ring, int direction, MeshBuilder mesh) {
        double[] normal = Vectors.scale(ring.tangent, direction);
        int centre = mesh.vertexCount();
        mesh.vertex(ring.centre, normal, ring.atom);
        MeshBuilder ringOnly = new MeshBuilder();
        addRing(ring, ringOnly);
        for (int j = 0; j < RING; j++) {
            mesh.vertex(ringOnly.position(j), normal, ring.atom);
        }
        for (int j = 0; j < RING; j++) {
            int a = centre + 1 + j, b = centre + 1 + (j + 1) % RING;
            if (direction > 0) {
                mesh.triangle(centre, a, b);
            } else {
                mesh.triangle(centre, b, a);
            }
        }
    }

    private static double signedPower(double v, double power) {
        return Math.copySign(Math.pow(Math.abs(v), power), v);
    }

    /** Growable vertex and index arrays for one {@link TriangleMesh}. */
    private static final class MeshBuilder {
        private float[] positions = new float[3 * 256];
        private float[] normals = new float[3 * 256];
        private int[] atoms = new int[256];
        private int[] indices = new int[3 * 256];
        private int vertices;
        private int indexCount;

        int vertexCount() {
            return vertices;
        }

        double[] position(int v) {
            return new double[]{positions[v * 3], positions[v * 3 + 1], positions[v * 3 + 2]};
        }

        void vertex(double[] position, double[] normal, int atom) {
            if (vertices == atoms.length) {
                positions = Arrays.copyOf(positions, positions.length * 2);
                normals = Arrays.copyOf(normals, normals.length * 2);
                atoms = Arrays.copyOf(atoms, atoms.length * 2);
            }
            for (int k = 0; k < 3; k++) {
                positions[vertices * 3 + k] = (float) position[k];
                normals[vertices * 3 + k] = (float) normal[k];
            }
            atoms[vertices++] = atom;
        }

        void triangle(int a, int b, int c) {
            if (indexCount + 3 > indices.length) {
                indices = Arrays.copyOf(indices, indices.length * 2);
            }
            indices[indexCount++] = a;
            indices[indexCount++] = b;
            indices[indexCount++] = c;
        }

        /** A flat quad with its own four vertices. */
        void quad(double[] a, double[] b, double[] c, double[] d, double[] normal, int atom) {
            int first = vertices;
            vertex(a, normal, atom);
            vertex(b, normal, atom);
            vertex(c, normal, atom);
            vertex(d, normal, atom);
            triangle(first, first + 1, first + 2);
            triangle(first, first + 2, first + 3);
        }

        TriangleMesh build() {
            return new TriangleMesh(Arrays.copyOf(positions, vertices * 3), Arrays.copyOf(normals, vertices * 3),
                    Arrays.copyOf(atoms, vertices), Arrays.copyOf(indices, indexCount));
        }
    }
}
