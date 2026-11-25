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

import java.util.ArrayList;
import java.util.List;

/**
 * Flat ribbons through protein backbones, after Carson &amp; Bugg, "Algorithm for ribbon models of proteins" (1986),
 * as in the legacy implementation: each peptide plane (Cα(i), O(i), Cα(i+1)) yields a pair of guide points either
 * side of the Cα–Cα midpoint; B-splines through both rows form the ribbon edges.
 */
public final class RibbonsMode implements DisplayMode {

    /** Helix guide points are moved this far (Å) along the peptide-plane normal, away from the helix axis. */
    static final float HELIX_OFFSET = 3.0f;
    /** Half-width (Å) inside a sheet; elsewhere 1 Å, and 0.1 Å at the end of a strand. */
    static final float SHEET_HALF_WIDTH = 2.0f;
    static final float SHEET_END_HALF_WIDTH = 0.1f;
    static final int SAMPLES_PER_RESIDUE = 20;
    static final int SPLINE_ORDER = 3;
    /** Consecutive Cα atoms further apart than this (Å) start a new ribbon segment. */
    static final float MAX_CA_DISTANCE = 4.5f;
    static final float PICK_RADIUS = 1.5f;

    private record Guide(Atom ca, Atom o, Residue residue) {
    }

    @Override
    public String id() {
        return "ribbons";
    }

    @Override
    public String displayName() {
        return "Ribbons";
    }

    @Override
    public boolean supports(ColourScheme scheme) {
        return !scheme.id().equals("cpk");
    }

    @Override
    public Scene build(Structure structure) {
        List<TriangleMesh> meshes = new ArrayList<>();
        List<Atom> pickAtoms = new ArrayList<>();
        for (Chain chain : structure.chains()) {
            for (List<Guide> segment : segments(structure, chain)) {
                if (segment.size() > SPLINE_ORDER) {
                    meshes.add(ribbon(segment));
                    segment.forEach(g -> pickAtoms.add(g.ca()));
                }
            }
        }
        float[] pickData = new float[pickAtoms.size() * SphereInstances.STRIDE];
        int[] pickIndices = new int[pickAtoms.size()];
        for (int i = 0; i < pickAtoms.size(); i++) {
            Atom a = pickAtoms.get(i);
            pickData[i * 4] = a.x();
            pickData[i * 4 + 1] = a.y();
            pickData[i * 4 + 2] = a.z();
            pickData[i * 4 + 3] = PICK_RADIUS;
            pickIndices[i] = a.index();
        }
        return new Scene(SphereInstances.EMPTY, CylinderInstances.EMPTY, meshes,
                new SphereInstances(pickData, pickIndices), structure.boundingSphere());
    }

    private static List<List<Guide>> segments(Structure structure, Chain chain) {
        List<List<Guide>> segments = new ArrayList<>();
        List<Guide> current = new ArrayList<>();
        for (Residue residue : structure.residuesOf(chain)) {
            if (!residue.polymer()) {
                continue;
            }
            var ca = structure.findAtom(residue, "CA");
            var o = structure.findAtom(residue, "O");
            if (ca.isEmpty() || o.isEmpty()) {
                continue;
            }
            Guide guide = new Guide(ca.get(), o.get(), residue);
            if (!current.isEmpty() && distance(current.getLast().ca(), guide.ca()) > MAX_CA_DISTANCE) {
                segments.add(current);
                current = new ArrayList<>();
            }
            current.add(guide);
        }
        segments.add(current);
        return segments;
    }

    private static TriangleMesh ribbon(List<Guide> guides) {
        int controls = guides.size() - 1;
        float[] edgeN = new float[controls * 3];
        float[] edgeP = new float[controls * 3];
        double[] previousD = null;

        for (int r = 0; r < controls; r++) {
            Guide here = guides.get(r);
            Guide next = guides.get(r + 1);
            double[] a = sub(next.ca(), here.ca());
            double[] b = sub(here.o(), here.ca());
            double[] c = normalise(cross(a, b));
            double[] d = normalise(cross(c, a));
            double[] p = {(here.ca().x() + next.ca().x()) / 2.0, (here.ca().y() + next.ca().y()) / 2.0,
                    (here.ca().z() + next.ca().z()) / 2.0};

            SecondaryStructure ss = here.residue().secondaryStructure();
            if (ss == SecondaryStructure.HELIX) {
                for (int i = 0; i < 3; i++) {
                    p[i] += c[i] * HELIX_OFFSET;
                }
            } else if (ss == SecondaryStructure.SHEET) {
                double width = next.residue().secondaryStructure() == SecondaryStructure.SHEET
                        ? SHEET_HALF_WIDTH : SHEET_END_HALF_WIDTH;
                for (int i = 0; i < 3; i++) {
                    d[i] *= width;
                }
            }
            // Keep the ribbon from twisting where the carbonyl oxygen flips side.
            if (previousD != null && dot(previousD, d) < 0) {
                d = new double[]{-d[0], -d[1], -d[2]};
            }
            previousD = d;
            for (int i = 0; i < 3; i++) {
                edgeN[r * 3 + i] = (float) (p[i] - d[i]);
                edgeP[r * 3 + i] = (float) (p[i] + d[i]);
            }
        }

        int samples = controls * SAMPLES_PER_RESIDUE;
        float[] splineN = BSpline.sample(edgeN, SPLINE_ORDER, samples);
        float[] splineP = BSpline.sample(edgeP, SPLINE_ORDER, samples);
        return strip(splineN, splineP, samples, guides);
    }

    private static TriangleMesh strip(float[] n, float[] p, int samples, List<Guide> guides) {
        float[] positions = new float[samples * 2 * 3];
        float[] normals = new float[positions.length];
        int[] atoms = new int[samples * 2];
        int controls = guides.size() - 1;
        for (int i = 0; i < samples; i++) {
            System.arraycopy(n, i * 3, positions, i * 6, 3);
            System.arraycopy(p, i * 3, positions, i * 6 + 3, 3);
            int prev = Math.max(0, i - 1), next = Math.min(samples - 1, i + 1);
            double[] along = {
                    (n[next * 3] + p[next * 3] - n[prev * 3] - p[prev * 3]) / 2.0,
                    (n[next * 3 + 1] + p[next * 3 + 1] - n[prev * 3 + 1] - p[prev * 3 + 1]) / 2.0,
                    (n[next * 3 + 2] + p[next * 3 + 2] - n[prev * 3 + 2] - p[prev * 3 + 2]) / 2.0};
            double[] across = {p[i * 3] - n[i * 3], p[i * 3 + 1] - n[i * 3 + 1], p[i * 3 + 2] - n[i * 3 + 2]};
            double[] normal = normalise(cross(along, across));
            for (int k = 0; k < 3; k++) {
                normals[i * 6 + k] = (float) normal[k];
                normals[i * 6 + 3 + k] = (float) normal[k];
            }
            // Same colour assignment as the legacy spline: proportional position along the control points.
            int guide = Math.min(controls - 1, (int) ((long) i * controls / samples));
            int atom = guides.get(guide).ca().index();
            atoms[i * 2] = atom;
            atoms[i * 2 + 1] = atom;
        }
        int[] indices = new int[(samples - 1) * 6];
        for (int i = 0; i < samples - 1; i++) {
            int v = i * 2;
            int o = i * 6;
            indices[o] = v;
            indices[o + 1] = v + 2;
            indices[o + 2] = v + 1;
            indices[o + 3] = v + 1;
            indices[o + 4] = v + 2;
            indices[o + 5] = v + 3;
        }
        return new TriangleMesh(positions, normals, atoms, indices);
    }

    private static double distance(Atom a, Atom b) {
        double dx = a.x() - b.x(), dy = a.y() - b.y(), dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double[] sub(Atom a, Atom b) {
        return new double[]{a.x() - b.x(), a.y() - b.y(), a.z() - b.z()};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] normalise(double[] v) {
        double length = Math.sqrt(dot(v, v));
        return length == 0 ? new double[]{0, 0, 1} : new double[]{v[0] / length, v[1] / length, v[2] / length};
    }
}
