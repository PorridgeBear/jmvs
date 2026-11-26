package com.adcworks.jmvs.scene.mode;

import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.Bond;
import com.adcworks.jmvs.core.model.Structure;
import com.adcworks.jmvs.scene.AtomRadii;
import com.adcworks.jmvs.scene.CylinderInstances;
import com.adcworks.jmvs.scene.SphereInstances;

import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Builders for atom spheres and half-bond cylinders shared by the atom-based display modes.
 */
final class AtomGeometry {

    private AtomGeometry() {
    }

    static float vanDerWaalsRadius(Structure structure, Atom atom) {
        return AtomRadii.vanDerWaals(structure, atom);
    }

    static SphereInstances spheres(Structure structure, ToDoubleFunction<Atom> radius) {
        List<Atom> atoms = structure.atoms();
        float[] data = new float[atoms.size() * SphereInstances.STRIDE];
        int[] indices = new int[atoms.size()];
        for (int i = 0; i < atoms.size(); i++) {
            Atom a = atoms.get(i);
            int o = i * SphereInstances.STRIDE;
            data[o] = a.x();
            data[o + 1] = a.y();
            data[o + 2] = a.z();
            data[o + 3] = (float) radius.applyAsDouble(a);
            indices[i] = a.index();
        }
        return new SphereInstances(data, indices);
    }

    /**
     * Two half-cylinders per bond, each meeting at the bond midpoint and coloured by its own atom.
     */
    static CylinderInstances halfBonds(Structure structure, float radius) {
        List<Bond> bonds = structure.bonds();
        float[] data = new float[bonds.size() * 2 * CylinderInstances.STRIDE];
        int[] atoms = new int[bonds.size() * 2];
        int n = 0;
        for (Bond bond : bonds) {
            Atom a = structure.atoms().get(bond.atom1());
            Atom b = structure.atoms().get(bond.atom2());
            float mx = (a.x() + b.x()) / 2, my = (a.y() + b.y()) / 2, mz = (a.z() + b.z()) / 2;
            n = putCylinder(data, atoms, n, a, mx, my, mz, radius);
            n = putCylinder(data, atoms, n, b, mx, my, mz, radius);
        }
        return new CylinderInstances(data, atoms);
    }

    private static int putCylinder(float[] data, int[] atoms, int n, Atom from, float tx, float ty, float tz,
                                   float radius) {
        int o = n * CylinderInstances.STRIDE;
        data[o] = from.x();
        data[o + 1] = from.y();
        data[o + 2] = from.z();
        data[o + 3] = tx;
        data[o + 4] = ty;
        data[o + 5] = tz;
        data[o + 6] = radius;
        atoms[n] = from.index();
        return n + 1;
    }
}
