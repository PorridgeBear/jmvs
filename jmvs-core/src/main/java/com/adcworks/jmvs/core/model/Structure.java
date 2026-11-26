package com.adcworks.jmvs.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * An immutable macromolecular structure: models → chains → residues → atoms, plus covalent bonds.
 * Children are stored as flat lists addressed by index so renderers can map atoms to array slots.
 * Create instances with {@link StructureBuilder}.
 */
public final class Structure {

    private final String name;
    private final List<Model> models;
    private final List<Chain> chains;
    private final List<Residue> residues;
    private final List<Atom> atoms;
    private final List<Bond> bonds;
    private final Bounds bounds;
    private final BoundingSphere boundingSphere;
    private final boolean hasHydrogens;
    private final int distinctChainIds;
    private final boolean secondaryStructureAnnotated;

    Structure(String name, List<Model> models, List<Chain> chains, List<Residue> residues, List<Atom> atoms,
              List<Bond> bonds, int distinctChainIds, boolean secondaryStructureAnnotated) {
        this.name = name;
        this.models = List.copyOf(models);
        this.chains = List.copyOf(chains);
        this.residues = List.copyOf(residues);
        this.atoms = List.copyOf(atoms);
        this.bonds = List.copyOf(bonds);
        this.bounds = Bounds.of(this.atoms);
        this.boundingSphere = BoundingSphere.of(bounds, this.atoms);
        this.hasHydrogens = this.atoms.stream().anyMatch(Atom::isHydrogen);
        this.distinctChainIds = distinctChainIds;
        this.secondaryStructureAnnotated = secondaryStructureAnnotated;
    }

    private Structure(Structure source, List<Residue> residues) {
        this.name = source.name;
        this.models = source.models;
        this.chains = source.chains;
        this.residues = List.copyOf(residues);
        this.atoms = source.atoms;
        this.bonds = source.bonds;
        this.bounds = source.bounds;
        this.boundingSphere = source.boundingSphere;
        this.hasHydrogens = source.hasHydrogens;
        this.distinctChainIds = source.distinctChainIds;
        this.secondaryStructureAnnotated = source.secondaryStructureAnnotated;
    }

    /**
     * A copy of this structure whose residues have the given secondary structure, one entry per residue in
     * {@link #residues()} order. Models, chains, atoms and bonds are shared, so atom indices stay valid.
     */
    public Structure withSecondaryStructure(List<SecondaryStructure> perResidue) {
        if (perResidue.size() != residues.size()) {
            throw new IllegalArgumentException(
                    "Expected %d secondary structure entries, got %d".formatted(residues.size(), perResidue.size()));
        }
        List<Residue> updated = new ArrayList<>(residues.size());
        for (Residue r : residues) {
            updated.add(new Residue(r.index(), r.chain(), r.name(), r.seqId(), r.insertionCode(), r.polymer(),
                    perResidue.get(r.index()), r.firstAtom(), r.atomCount()));
        }
        return new Structure(this, updated);
    }

    /**
     * Whether the file this structure was read from carried any secondary-structure annotation (helices, turns or
     * sheets).
     */
    public boolean secondaryStructureAnnotated() {
        return secondaryStructureAnnotated;
    }

    public String name() {
        return name;
    }

    public List<Model> models() {
        return models;
    }

    public List<Chain> chains() {
        return chains;
    }

    public List<Residue> residues() {
        return residues;
    }

    public List<Atom> atoms() {
        return atoms;
    }

    public List<Bond> bonds() {
        return bonds;
    }

    public Bounds bounds() {
        return bounds;
    }

    public BoundingSphere boundingSphere() {
        return boundingSphere;
    }

    public boolean hasHydrogens() {
        return hasHydrogens;
    }

    /**
     * Number of distinct chain ids across all models (chains with the same id in different models count once).
     */
    public int distinctChainIds() {
        return distinctChainIds;
    }

    public Residue residueOf(Atom atom) {
        return residues.get(atom.residue());
    }

    public Chain chainOf(Residue residue) {
        return chains.get(residue.chain());
    }

    public Chain chainOf(Atom atom) {
        return chainOf(residueOf(atom));
    }

    public Model modelOf(Chain chain) {
        return models.get(chain.model());
    }

    public List<Chain> chainsOf(Model model) {
        return chains.subList(model.firstChain(), model.firstChain() + model.chainCount());
    }

    public List<Residue> residuesOf(Chain chain) {
        return residues.subList(chain.firstResidue(), chain.firstResidue() + chain.residueCount());
    }

    public List<Atom> atomsOf(Residue residue) {
        return atoms.subList(residue.firstAtom(), residue.firstAtom() + residue.atomCount());
    }

    public List<Atom> atomsOf(Model model) {
        return atoms.subList(model.firstAtom(), model.firstAtom() + model.atomCount());
    }

    public Optional<Atom> findAtom(Residue residue, String atomName) {
        return atomsOf(residue).stream().filter(a -> a.name().equals(atomName)).findFirst();
    }

    public long countResidues(SecondaryStructure type) {
        return residues.stream().filter(r -> r.secondaryStructure() == type).count();
    }

    @Override
    public String toString() {
        return "%s: %d models, %d chains, %d residues, %d atoms, %d bonds".formatted(
                name, models.size(), chains.size(), residues.size(), atoms.size(), bonds.size());
    }
}
