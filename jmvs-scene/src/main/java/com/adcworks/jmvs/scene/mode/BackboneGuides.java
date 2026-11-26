package com.adcworks.jmvs.scene.mode;

import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.Chain;
import com.adcworks.jmvs.core.model.Residue;
import com.adcworks.jmvs.core.model.Structure;

import java.util.ArrayList;
import java.util.List;

/**
 * Protein backbone guides (Cα and carbonyl O of each residue) split into continuous segments, shared by the ribbon
 * and cartoon modes.
 */
final class BackboneGuides {

    /** Consecutive Cα atoms further apart than this (Å) start a new segment. */
    static final float MAX_CA_DISTANCE = 4.5f;

    record Guide(Atom ca, Atom o, Residue residue) {
    }

    private BackboneGuides() {
    }

    /**
     * Continuous runs of polymer residues with both Cα and O in a chain; residues missing either are skipped.
     * May contain empty or short segments.
     */
    static List<List<Guide>> segments(Structure structure, Chain chain) {
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
            if (!current.isEmpty() && Vectors.distance(current.getLast().ca(), guide.ca()) > MAX_CA_DISTANCE) {
                segments.add(current);
                current = new ArrayList<>();
            }
            current.add(guide);
        }
        segments.add(current);
        return segments;
    }

    /**
     * Unit vector in the peptide plane of guide {@code i} (Cα(i), O(i), Cα(i+1)), perpendicular to Cα(i)→Cα(i+1):
     * the Carson–Bugg ribbon width direction.
     */
    static double[] planeNormal(Guide here, Guide next) {
        double[] a = Vectors.sub(next.ca(), here.ca());
        double[] b = Vectors.sub(here.o(), here.ca());
        return Vectors.normalise(Vectors.cross(a, b));
    }

    static double[] widthDirection(Guide here, Guide next) {
        double[] a = Vectors.sub(next.ca(), here.ca());
        return Vectors.normalise(Vectors.cross(planeNormal(here, next), a));
    }
}
