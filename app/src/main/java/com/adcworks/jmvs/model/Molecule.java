package com.adcworks.jmvs.model;

import java.util.*;

/**
 *
 * @author  Christine
 */
public class Molecule {

    public List<Model> models = new ArrayList<Model>();
    private String name;
    public float minX, minY, minZ;
    public float maxX, maxY, maxZ;
    public float midX, midY, midZ;
    private boolean hasHydrogenAtoms;

    public void setName(String name)
    {
        this.name = name;
    }
    public String getName()
    {
        return name;
    }
    public boolean hasHydrogenAtoms() {
        return hasHydrogenAtoms;
    }

    public Model getOrCreateModel(int number)
    {
        int index = number - 1;

        Model model = null;

        if (!models.isEmpty()) {
            model = models.get(index);
        }

        if (model == null) {
            model = new Model(number);
            models.add(model);
        }

        return model;
    }

    public List<Model> getModels() {
        return models;
    }

    public Residue findResidue(String id)
    {
        for (Model model : models) {
            for (Chain chain : model.getChains()) {
                for (Residue residue : chain.getResidues()) {
                    if (residue.getId().equals(id)) {
                        return residue;
                    }
                }
            }
        }

        return null;
    }

    public int totalModels()
    {
        return models.size();
    }

    public int totalBonds()
    {
        int count = 0;

        for (Model model : models) {
            count += model.getBonds().size();
        }

        return count;
    }

    public int totalChains()
    {
        int count = 0;

        for (Model model : models) {
            count += model.totalChains();
        }

        return count;
    }

    public int totalResidues()
    {
        int count = 0;

        for (Model model : models) {
            count += model.totalResidues();
        }

        return count;
    }

    public int totalAtoms()
    {
        int count = 0;

        for (Model model : models) {
            count += model.totalAtoms();
        }

        return count;
    }

    public void addAtom(int modelNum, String chainId, String resId, String resName, Atom atom) {
        Model model = getOrCreateModel(modelNum);
        Chain chain = model.getChain(chainId);
        chain.addAtom(resId, resName, atom);

        if (atom.isHydrogen()) {
            hasHydrogenAtoms = true;
        }
    }

    public void addSheet(String id, int start, int end)
    {
        for (int i = start; i <= end; i++) {
            Residue residue = findResidue(String.valueOf(i));
            if (residue != null) {
                residue.setIsSheetPart(true);
            }
        }
    }

    public void addHelix(String id, int start, int end)
    {
        for (int i = start; i <= end; i++) {
            Residue residue = findResidue(String.valueOf(i));
            if (residue != null) {
                residue.setIsHelixPart(true);
            }
        }
    }

    public void finish() {
        computeBounds();

        midX = (minX + maxX) / 2;
        midY = (minY + maxY) / 2;
        midZ = (minZ + maxZ) / 2;
                        
        computeMid();

        for (Model model : models) {
            model.createCovalentBonds(totalAtoms());
        }

        System.out.printf("Models %d, Chains %d, Residues %d, Atoms %d, Bonds %d\n",
                totalModels(), totalChains(), totalResidues(), totalAtoms(), totalBonds());
    }

    private void computeMid() {
        
        Atom atom;
        int atoms = 0;
        
        Iterator<Model> itModels = models.iterator();
        while(itModels.hasNext()) {
            Iterator<Chain> itChains = ((Model) itModels.next()).getChains().iterator();
            while(itChains.hasNext()) {
                Iterator itResidues = ((Chain) itChains.next()).getResidues().iterator();
                while(itResidues.hasNext()) {
                    Iterator itAtoms = ((Residue) itResidues.next()).getAtoms().iterator();
	                while(itAtoms.hasNext()) {
	                    atom = (Atom) itAtoms.next();
	        
	                    midX += atom.getX();
	                    midY += atom.getY();
	                    midZ += atom.getZ();
	                    
	                    atoms++;
	                }
                }
            }
        }

        midX = ((float)(midX / atoms));
        midY = ((float)(midY / atoms));
        midZ = ((float)(midZ / atoms));
    }
    
    /**
     * Compute the molecule bounds.
     *
     */
    private void computeBounds() {
        
        Iterator itModels = models.iterator();
        while(itModels.hasNext()) {
            Iterator itChains = ((Model) itModels.next()).getChains().iterator();
            while(itChains.hasNext()) {
                Iterator itResidues = ((Chain) itChains.next()).getResidues().iterator();
                while(itResidues.hasNext()) {
                    Iterator itAtoms = ((Residue) itResidues.next()).getAtoms().iterator();
	                while(itAtoms.hasNext()) {
	                    
	                    Atom atom = (Atom) itAtoms.next();
	                    
	                    if(atom.getX() > maxX) {
	                        maxX = atom.getX();
	                    } else if(atom.getX() < minX) {
	                        minX = atom.getX();
	                    }
	                    
	                    if(atom.getY() > maxY) {
	                        maxY = atom.getY();
	                    } else if(atom.getY() < minY) {
	                        minY = atom.getY();
	                    }  
	                    
	                    if(atom.getZ() > maxZ) {
	                        maxZ = atom.getZ();
	                    } else if(atom.getZ() < minZ) {
	                       minZ = atom.getZ();
	                    }     
	                }
                }
            }
        }
    }
}
