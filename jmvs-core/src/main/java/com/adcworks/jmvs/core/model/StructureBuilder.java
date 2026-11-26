package com.adcworks.jmvs.core.model;

import com.adcworks.jmvs.core.chem.CovalentBonder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects atoms and secondary-structure ranges in any order and builds an immutable {@link Structure},
 * grouping atoms by model number, chain id and residue key, then inferring covalent bonds.
 */
public final class StructureBuilder {

    private record PendingAtom(int serial, String name, Element element, float x, float y, float z,
                               boolean heterogen, float bFactor) {
    }

    private static final class PendingResidue {
        final String name;
        final int seqId;
        final String insertionCode;
        final boolean polymer;
        final List<PendingAtom> atoms = new ArrayList<>();

        PendingResidue(String name, int seqId, String insertionCode, boolean polymer) {
            this.name = name;
            this.seqId = seqId;
            this.insertionCode = insertionCode;
            this.polymer = polymer;
        }
    }

    private static final class PendingChain {
        final String authId;
        final Map<String, PendingResidue> residues = new LinkedHashMap<>();

        PendingChain(String authId) {
            this.authId = authId;
        }
    }

    private record Range(String chainId, int start, int end, SecondaryStructure type) {
    }

    private final String name;
    private final CovalentBonder bonder;
    private final Map<Integer, Map<String, PendingChain>> models = new LinkedHashMap<>();
    private final List<Range> ranges = new ArrayList<>();

    public StructureBuilder(String name) {
        this(name, new CovalentBonder());
    }

    public StructureBuilder(String name, CovalentBonder bonder) {
        this.name = name;
        this.bonder = bonder;
    }

    /**
     * Adds an atom.
     *
     * @param labelSeqId {@code label_seq_id}, or {@code null} for non-polymer residues
     * @param authSeqId  {@code auth_seq_id}, used to separate non-polymer residues
     * @param bFactor    {@code B_iso_or_equiv}, or {@link Float#NaN} if absent
     */
    public StructureBuilder addAtom(int modelNumber, String chainId, String authChainId, Integer labelSeqId,
                                    int authSeqId, String insertionCode, String residueName, int serial,
                                    String atomName, Element element, float x, float y, float z,
                                    boolean heterogen, float bFactor) {
        boolean polymer = labelSeqId != null;
        int seqId = polymer ? labelSeqId : authSeqId;
        String key = (polymer ? "L" : "A") + seqId + insertionCode + ":" + residueName;
        models.computeIfAbsent(modelNumber, n -> new LinkedHashMap<>())
                .computeIfAbsent(chainId, id -> new PendingChain(authChainId))
                .residues.computeIfAbsent(key, k -> new PendingResidue(residueName, seqId, insertionCode, polymer))
                .atoms.add(new PendingAtom(serial, atomName, element, x, y, z, heterogen, bFactor));
        return this;
    }

    /**
     * Assigns secondary structure to polymer residues {@code start..end} (by {@code label_seq_id}, inclusive)
     * of the given chain in every model. Later assignments override earlier ones.
     */
    public StructureBuilder addSecondaryStructure(String chainId, int start, int end, SecondaryStructure type) {
        ranges.add(new Range(chainId, start, end, type));
        return this;
    }

    public Structure build() {
        List<Model> modelList = new ArrayList<>();
        List<Chain> chains = new ArrayList<>();
        List<Residue> residues = new ArrayList<>();
        List<Atom> atoms = new ArrayList<>();
        Map<String, Integer> chainOrder = new LinkedHashMap<>();

        for (var modelEntry : models.entrySet()) {
            int firstChain = chains.size();
            int firstModelAtom = atoms.size();
            for (var chainEntry : modelEntry.getValue().entrySet()) {
                String chainId = chainEntry.getKey();
                int order = chainOrder.computeIfAbsent(chainId, id -> chainOrder.size());
                int chainIndex = chains.size();
                int firstResidue = residues.size();
                for (PendingResidue r : chainEntry.getValue().residues.values()) {
                    int residueIndex = residues.size();
                    int firstAtom = atoms.size();
                    for (PendingAtom a : r.atoms) {
                        atoms.add(new Atom(atoms.size(), a.serial, a.name, a.element, a.x, a.y, a.z, a.heterogen,
                                residueIndex, a.bFactor));
                    }
                    residues.add(new Residue(residueIndex, chainIndex, r.name, r.seqId, r.insertionCode, r.polymer,
                            secondaryStructureOf(chainId, r), firstAtom, r.atoms.size()));
                }
                chains.add(new Chain(chainIndex, modelList.size(), chainId, chainEntry.getValue().authId, order,
                        firstResidue, residues.size() - firstResidue));
            }
            modelList.add(new Model(modelList.size(), modelEntry.getKey(), firstChain, chains.size() - firstChain,
                    firstModelAtom, atoms.size() - firstModelAtom));
        }

        List<Bond> bonds = new ArrayList<>();
        boolean fast = CovalentBonder.useFastRule(atoms.size());
        for (Model model : modelList) {
            bonds.addAll(bonder.bond(atoms.subList(model.firstAtom(), model.firstAtom() + model.atomCount()), fast));
        }
        return new Structure(name, modelList, chains, residues, atoms, bonds, chainOrder.size());
    }

    private SecondaryStructure secondaryStructureOf(String chainId, PendingResidue residue) {
        SecondaryStructure result = SecondaryStructure.COIL;
        if (!residue.polymer) {
            return result;
        }
        for (Range range : ranges) {
            if (range.chainId.equals(chainId) && residue.seqId >= range.start && residue.seqId <= range.end) {
                result = range.type;
            }
        }
        return result;
    }
}
