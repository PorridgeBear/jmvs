package com.adcworks.jmvs.model;

import java.util.*;

/**
 * Protein polypeptide chains are linear polymers that are assembled from a repertoire of 20 different
 * standard amino acids joined together through peptide bonds from N-terminus to C-terminus.
 * The identity of each amino acid is determined by its side chain, known as an R group.
 */
public class Chain {

    private String id;
    private final List<Residue> residues = new ArrayList<>();

    public Chain(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
    public List<Residue> getResidues() {
        return residues;
    }

    public void addAtom(String resId, String resName, Atom atom) {
        Residue residue = getResidue(resId, resName, atom.isHeterogen());
        atom.setResidue(residue);
        residue.addAtom(atom);
    }

    public boolean hasResidue(String resId) {
        for (Residue residue : residues) {
            if (residue.getId().equals(resId)) {
                return true;
            }
        }
        
        return false;
    }

    public Residue getResidue(String resId, String resName, boolean isHeterogen) {
        for (Residue residue : residues) {
            if (residue.getId().equals(resId)) {
                return residue;
            }
        }
        
        Residue residue = new Residue(this, resId, resName, isHeterogen);
        residues.add(residue);

        return residue;
    }

    public int totalResidues()
    {
        return residues.size();
    }

    public int totalAtoms() {
        int count = 0;

        for (Residue residue : residues) {
            count += residue.totalAtoms();
        } 
        
        return count;
    }    

    public boolean containsHeterogens() {
        for (Residue residue : residues) {
            if (residue.isHeterogen()) {
                return true;
            }
        } 
        
        return false;
    }
}
