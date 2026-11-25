package com.adcworks.jmvs.model;

import java.util.*;

/**
 * In biochemistry and molecular biology, a residue refers to a specific monomer within the polymeric chain of a
 * polysaccharide, protein or nucleic acid, e.g. an amino acid is the monomer for protein chains.
 */
public class Residue {
    
    private String id;
    private String name;
    private List<Atom> atoms = new LinkedList<>();
    private boolean isHeterogen;
    private boolean isSheetPart;
    private boolean isHelixPart;
    private boolean isTurnPart;
    private Chain chain;

    public Residue(Chain chain, String id, String name, boolean isHeterogen) {
        this.chain = chain;
        this.id = id;
        this.name = name;
        this.isHeterogen = isHeterogen;
    }

    public Chain getChain()
    {
        return chain;
    }

    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setIsHeterogen(boolean isHeterogen) {
        this.isHeterogen = isHeterogen;
    }
    
    public boolean isHeterogen() {
        return this.isHeterogen;
    }
    
    public void setIsHelixPart(boolean isHelixPart) {
    	this.isHelixPart = isHelixPart;
    }
    
    public boolean isHelixPart() {
    	return this.isHelixPart;
    }

    public void setIsSheetPart(boolean isSheetPart) {
    	this.isSheetPart = isSheetPart;
    }

    public boolean isSheetPart() {
    	return this.isSheetPart;
    }
    
    public void setIsTurnPart(boolean isTurnPart) {
    	this.isTurnPart = isTurnPart;
    }
    
    public boolean isTurnPart() {
    	return this.isTurnPart;
    }
    
    public void addAtom(Atom atom) {
        atoms.add(atom);
    }
    
    public int totalAtoms() {
        return atoms.size();
    }
    
    public List<Atom> getAtoms() {
        return atoms;
    }
    
    public Object[] getAtomsArray() {
        return atoms.toArray();
    }

    /**
     * A residue is assumed to have just 1 alpha carbon.
     * @return Atom
     */
    public Atom getAlphaCarbon() {
        for (Atom atom : atoms) {
            if (atom.getLabel().equals("CA")) {
                return atom;
            }
        }
        
        return null;
    }

    /**
     * A residue is assumed to have just 1 carboxyl oxygen.
     * @return Atom
     */
    public Atom getCarbonylOxygen() {
        for (Atom atom : atoms) {
            if (atom.getSymbol().equals("O")) {
                return atom;
            }
        }
        
        return null;
    }
}
