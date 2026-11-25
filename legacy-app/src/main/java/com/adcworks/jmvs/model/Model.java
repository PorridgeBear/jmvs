package com.adcworks.jmvs.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Model. Top-level container.
 * 
 * @author Ali
 *
 */
public class Model {

    private int number;
    private final List<Chain> chains = new ArrayList<Chain>();
    private final List<Bond> bonds = new ArrayList<Bond>();

    public Model(int number) {
        this.number = number;
    }

    public int totalChains()
    {
        return chains.size();
    }

    public int totalResidues()
    {
        int count = 0;
        for (Chain chain : chains) {
            count += chain.totalResidues();
        }

        return count;
    }

    public int totalAtoms()
    {
        int count = 0;
        for (Chain chain : chains) {
            count += chain.totalAtoms();
        }

        return count;
    }
    
    public List<Chain> getChains() {
        return chains;
    }

    public Chain getChain(String chainId) {

        for (Chain chain : chains) {
            if (chain.getId().equals(chainId)) {
                return chain;
            }
        }

        Chain chain = new Chain(chainId);
        chains.add(chain);
        return chain;
    }

    /**
     * Uses RasMol's method of finding covalent bonds using a distance testing algorithm.
     * https://www.umass.edu/microbio/rasmol/rasbonds.htm
     */
    public void createCovalentBonds(int totalAtoms)
    {
        // RasMol fast bonding when total atoms + hetatoms > 255
        //  Two atoms are considered bonded when the distance between them is between 0.4 and 1.9 Angstroms,
        //  unless one or both atoms are hydrogens, in which case the bonded range is between 0.4 and 1.2 Angstroms.

        // for less than 255
        // bonded when the distance between them is between 0.4 Angstroms and
        // (the sum of their covalent radii plus 0.56 Angstroms)

        boolean useFastBonding = totalAtoms > 1000;

        float fastBondingMinDist = 0.4f;
        float fastBondingMaxDist = 1.9f;
        float fastBondingMaxDistHydrogen = 1.2f;

        float slowBondingMinDist = 0.4f;
        float slowBondingCoefficient = 0.56f;

        List<Atom> atoms = new ArrayList<>();

        for (Chain chain : chains) {
            for (Residue residue : chain.getResidues()) {
                atoms.addAll(residue.getAtoms());
            }
        }

        for (int s = 0; s < atoms.size() - 1; s++) {
            Atom sAtom = atoms.get(s);
            for (int d = s + 1; d < atoms.size(); d++) {
                Atom dAtom = atoms.get(d);

                float dx = dAtom.getX() - sAtom.getX();
                float dy = dAtom.getY() - sAtom.getY();
                float dz = dAtom.getZ() - sAtom.getZ();

                float dist = (float) Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2) + Math.pow(dz, 2));

                boolean hasHydrogen = sAtom.getSymbol().equals("H") || dAtom.getSymbol().equals("H");

                boolean createBond;
                if (useFastBonding) {
                    createBond = dist >= fastBondingMinDist &&
                            dist <= (hasHydrogen ? fastBondingMaxDistHydrogen : fastBondingMaxDist);
                } else {
                    float slowBondingMaxDist = sAtom.getCovalentRadius() + dAtom.getCovalentRadius() +
                            slowBondingCoefficient;
                    createBond = dist >= slowBondingMinDist && dist <= slowBondingMaxDist;
                }

                if (createBond) {
                    System.out.printf("Create bond: %s (%s) - %s (%s) %f %n",
                            sAtom.getId(), sAtom.getLabel(), dAtom.getId(), dAtom.getLabel(), dist);
                    bonds.add(new Bond(sAtom, dAtom));
                }
            }
        }
    }

    public List<Bond> getBonds()
    {
        return bonds;
    }
}
