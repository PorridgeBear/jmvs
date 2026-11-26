package com.adcworks.jmvs.core.chem;

import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.Chain;
import com.adcworks.jmvs.core.model.Model;
import com.adcworks.jmvs.core.model.Residue;
import com.adcworks.jmvs.core.model.SecondaryStructure;
import com.adcworks.jmvs.core.model.Structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Assigns secondary structure from backbone hydrogen bonds, following the core of DSSP (Kabsch &amp; Sander, 1983):
 * electrostatic H-bond energies, n-turns, minimal helices, bridges and ladders (with bulges). ADR 0018 lists what
 * is left out. The eight DSSP states are reduced to {@link SecondaryStructure}: H, G and I become
 * {@link SecondaryStructure#HELIX}, E becomes {@link SecondaryStructure#SHEET}, T becomes
 * {@link SecondaryStructure#TURN} and the rest (B, S, blank) become {@link SecondaryStructure#COIL}.
 *
 * <p>Stateless and thread-safe; models are assigned independently, chains within a model together.
 */
public final class DsspAssigner {

    /** Coupling constant 0.084 e² × 332 (kcal Å / mol) of the DSSP electrostatic model. */
    private static final double COUPLING = 0.084 * 332;
    /** H-bond energy threshold in kcal/mol. */
    private static final double HBOND_ENERGY = -0.5;
    /** DSSP's lower clamp on the energy, in kcal/mol. */
    private static final double MIN_ENERGY = -9.9;
    /** Largest C(i)–N(i+1) distance, in Å, of a peptide bond; longer means a chain break. */
    private static final double MAX_PEPTIDE_BOND = 2.5;
    /** Largest N–O distance, in Å, that can give an energy below {@link #HBOND_ENERGY}. */
    private static final float MAX_N_O_DISTANCE = 5.2f;
    /** Amide N–H bond length in Å. */
    private static final double N_H_LENGTH = 1.0;

    private enum State { NONE, ALPHA, THREE_TEN, PI, STRAND, TURN }

    private enum BridgeType { PARALLEL, ANTIPARALLEL }

    private record Bridge(int i, int j, BridgeType type) {
    }

    /** Backbone atoms of one residue, with a computed amide hydrogen when it has one. */
    private record Backbone(Residue residue, Atom oxygen, double[] n, double[] ca, double[] c, double[] o, double[] h) {
    }

    /**
     * Per-residue secondary structure in {@link Structure#residues()} order. Residues without N, CA, C and O, and
     * non-polymer residues, are {@link SecondaryStructure#COIL}.
     */
    public List<SecondaryStructure> assign(Structure structure) {
        var result = new SecondaryStructure[structure.residues().size()];
        Arrays.fill(result, SecondaryStructure.COIL);
        for (Model model : structure.models()) {
            assignModel(structure, model, result);
        }
        return List.of(result);
    }

    private void assignModel(Structure structure, Model model, SecondaryStructure[] result) {
        List<Backbone> backbone = new ArrayList<>();
        List<Boolean> breakBefore = new ArrayList<>();
        for (Chain chain : structure.chainsOf(model)) {
            boolean newChain = true;
            for (Residue residue : structure.residuesOf(chain)) {
                Optional<Backbone> b = backboneOf(structure, residue);
                if (b.isEmpty()) {
                    newChain = true;
                    continue;
                }
                Backbone current = b.get();
                Backbone previous = newChain ? null : backbone.getLast();
                boolean broken = previous == null || distance(previous.c, current.n) > MAX_PEPTIDE_BOND;
                if (!broken && !residue.name().equals("PRO")) {
                    double[] co = normalise(sub(previous.c, previous.o));
                    current = new Backbone(residue, current.oxygen, current.n, current.ca, current.c, current.o,
                            add(current.n, scale(co, N_H_LENGTH)));
                }
                backbone.add(current);
                breakBefore.add(broken);
                newChain = false;
            }
        }
        int count = backbone.size();
        if (count == 0) {
            return;
        }
        boolean[] breaks = new boolean[count + 1];
        for (int i = 0; i < count; i++) {
            breaks[i] = breakBefore.get(i);
        }
        breaks[count] = true;

        HydrogenBonds bonds = hydrogenBonds(backbone, breaks);
        State[] states = new State[count];
        Arrays.fill(states, State.NONE);

        boolean[][] turns = new boolean[6][count];
        for (int n = 3; n <= 5; n++) {
            for (int i = 0; i + n < count; i++) {
                turns[n][i] = noBreak(breaks, i, i + n) && bonds.has(i, i + n);
            }
        }
        markHelices(turns, 4, State.ALPHA, states, breaks, false);
        markStrands(backbone, bonds, breaks, states);
        markHelices(turns, 3, State.THREE_TEN, states, breaks, true);
        markHelices(turns, 5, State.PI, states, breaks, true);
        for (int n = 3; n <= 5; n++) {
            for (int i = 0; i + n < count; i++) {
                if (turns[n][i]) {
                    for (int k = i + 1; k < i + n; k++) {
                        if (states[k] == State.NONE) {
                            states[k] = State.TURN;
                        }
                    }
                }
            }
        }
        for (int i = 0; i < count; i++) {
            result[backbone.get(i).residue.index()] = switch (states[i]) {
                case ALPHA, THREE_TEN, PI -> SecondaryStructure.HELIX;
                case STRAND -> SecondaryStructure.SHEET;
                case TURN -> SecondaryStructure.TURN;
                case NONE -> SecondaryStructure.COIL;
            };
        }
    }

    /**
     * Minimal helices: two consecutive n-turns at i-1 and i make residues i..i+n-1 helical. Lower-priority helices
     * (3-10, pi) only claim stretches where every residue is still unassigned.
     */
    private static void markHelices(boolean[][] turns, int n, State state, State[] states, boolean[] breaks,
                                    boolean onlyIfFree) {
        int count = states.length;
        for (int i = 1; i + n <= count; i++) {
            if (!turns[n][i - 1] || !turns[n][i] || !noBreak(breaks, i - 1, i + n - 1)) {
                continue;
            }
            boolean free = true;
            for (int k = i; k < i + n && onlyIfFree; k++) {
                free &= states[k] == State.NONE;
            }
            if (!free) {
                continue;
            }
            for (int k = i; k < i + n; k++) {
                if (!onlyIfFree || states[k] == State.NONE) {
                    if (states[k] != State.ALPHA) {
                        states[k] = state;
                    }
                }
            }
        }
    }

    /**
     * Bridges between residues at least three apart, merged into ladders of consecutive bridges of one type and
     * joined across bulges; residues of ladders with two or more bridges, and of bulges, become strand.
     */
    private static void markStrands(List<Backbone> backbone, HydrogenBonds bonds, boolean[] breaks, State[] states) {
        int count = backbone.size();
        List<Bridge> bridges = new ArrayList<>();
        for (int i = 1; i + 1 < count; i++) {
            if (!noBreak(breaks, i - 1, i + 1)) {
                continue;
            }
            for (int j : candidates(bonds, i, count)) {
                if (j <= i + 2 || j + 1 >= count || !noBreak(breaks, j - 1, j + 1)) {
                    continue;
                }
                boolean parallel = bonds.has(i - 1, j) && bonds.has(j, i + 1)
                        || bonds.has(j - 1, i) && bonds.has(i, j + 1);
                boolean antiparallel = bonds.has(i, j) && bonds.has(j, i)
                        || bonds.has(i - 1, j + 1) && bonds.has(j - 1, i + 1);
                if (parallel) {
                    bridges.add(new Bridge(i, j, BridgeType.PARALLEL));
                } else if (antiparallel) {
                    bridges.add(new Bridge(i, j, BridgeType.ANTIPARALLEL));
                }
            }
        }
        bridges = bridges.stream().distinct().sorted((a, b) -> a.i != b.i ? Integer.compare(a.i, b.i)
                : Integer.compare(a.j, b.j)).toList();

        List<List<Bridge>> ladders = new ArrayList<>();
        for (Bridge bridge : bridges) {
            List<Bridge> extended = null;
            for (List<Bridge> ladder : ladders) {
                Bridge last = ladder.getLast();
                int expectedJ = bridge.type == BridgeType.PARALLEL ? last.j + 1 : last.j - 1;
                if (last.type == bridge.type && last.i + 1 == bridge.i && expectedJ == bridge.j) {
                    extended = ladder;
                    break;
                }
            }
            if (extended != null) {
                extended.add(bridge);
            } else {
                ladders.add(new ArrayList<>(List.of(bridge)));
            }
        }

        boolean[] joined = new boolean[ladders.size()];
        for (int a = 0; a < ladders.size(); a++) {
            for (int b = a + 1; b < ladders.size(); b++) {
                if (bulge(ladders.get(a), ladders.get(b), breaks)) {
                    joined[a] = joined[b] = true;
                    markSpan(ladders.get(a), ladders.get(b), states);
                }
            }
        }
        for (int a = 0; a < ladders.size(); a++) {
            List<Bridge> ladder = ladders.get(a);
            if (ladder.size() < 2 && !joined[a]) {
                continue;
            }
            for (Bridge bridge : ladder) {
                markStrand(states, bridge.i);
                markStrand(states, bridge.j);
            }
        }
    }

    /** Residues that could form a bridge with {@code i}: neighbours of anything H-bonded to i-1, i or i+1. */
    private static List<Integer> candidates(HydrogenBonds bonds, int i, int count) {
        var result = new TreeSet<Integer>();
        for (int k = i - 1; k <= i + 1; k++) {
            for (int p : bonds.partners(k)) {
                for (int q = p - 1; q <= p + 1; q++) {
                    if (q >= 0 && q < count) {
                        result.add(q);
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    /** Whether two ladders of one type are linked by a bulge: gaps of at most 1 on one side and 4 on the other. */
    private static boolean bulge(List<Bridge> first, List<Bridge> second, boolean[] breaks) {
        Bridge end = first.getLast(), start = second.getFirst();
        if (end.type != start.type) {
            return false;
        }
        int gapI = start.i - end.i;
        int gapJ = end.type == BridgeType.PARALLEL ? start.j - end.j : end.j - start.j;
        if (gapI <= 0 || gapJ <= 0) {
            return false;
        }
        boolean shape = gapI - 1 <= 1 && gapJ - 1 <= 4 || gapI - 1 <= 4 && gapJ - 1 <= 1;
        return shape && noBreak(breaks, end.i, start.i)
                && noBreak(breaks, Math.min(end.j, start.j), Math.max(end.j, start.j));
    }

    private static void markSpan(List<Bridge> first, List<Bridge> second, State[] states) {
        Bridge end = first.getLast(), start = second.getFirst();
        for (int k = end.i; k <= start.i; k++) {
            markStrand(states, k);
        }
        for (int k = Math.min(end.j, start.j); k <= Math.max(end.j, start.j); k++) {
            markStrand(states, k);
        }
    }

    private static void markStrand(State[] states, int k) {
        if (states[k] != State.ALPHA) {
            states[k] = State.STRAND;
        }
    }

    /** Whether residues {@code from..to} (inclusive) are connected by peptide bonds. */
    private static boolean noBreak(boolean[] breaks, int from, int to) {
        for (int k = from + 1; k <= to; k++) {
            if (breaks[k]) {
                return false;
            }
        }
        return true;
    }

    /**
     * For every donor residue (N–H) keeps the two acceptors (C=O) with the lowest energy below the threshold, as
     * DSSP does, and answers whether C=O of i is bonded to N–H of j.
     */
    private static final class HydrogenBonds {
        private final int[][] acceptorsOf;
        private final List<List<Integer>> partners;

        HydrogenBonds(int[][] acceptorsOf, List<List<Integer>> partners) {
            this.acceptorsOf = acceptorsOf;
            this.partners = partners;
        }

        /** Whether the C=O of residue {@code i} accepts an H-bond from the N–H of residue {@code j}. */
        boolean has(int i, int j) {
            int[] acceptors = acceptorsOf[j];
            return acceptors[0] == i || acceptors[1] == i;
        }

        /** Residues H-bonded to {@code i} in either direction. */
        List<Integer> partners(int i) {
            return partners.get(i);
        }
    }

    private static HydrogenBonds hydrogenBonds(List<Backbone> backbone, boolean[] breaks) {
        int count = backbone.size();
        List<Atom> oxygens = new ArrayList<>(count);
        Map<Integer, Integer> residueOfAtom = new HashMap<>(count * 2);
        for (int k = 0; k < count; k++) {
            Atom o = backbone.get(k).oxygen;
            oxygens.add(o);
            residueOfAtom.put(o.index(), k);
        }
        SpatialGrid grid = SpatialGrid.of(oxygens, MAX_N_O_DISTANCE);

        int[][] acceptorsOf = new int[count][];
        double[][] energiesOf = new double[count][];
        List<List<Integer>> partners = new ArrayList<>(count);
        for (int k = 0; k < count; k++) {
            acceptorsOf[k] = new int[] {-1, -1};
            energiesOf[k] = new double[] {0, 0};
            partners.add(new ArrayList<>());
        }
        for (int j = 0; j < count; j++) {
            Backbone donor = backbone.get(j);
            if (donor.h == null) {
                continue;
            }
            final int donorIndex = j;
            grid.forEachCandidate((float) donor.n[0], (float) donor.n[1], (float) donor.n[2], MAX_N_O_DISTANCE,
                    atom -> {
                        int i = residueOfAtom.get(atom.index());
                        boolean neighbour = i == donorIndex || i == donorIndex - 1 && !breaks[donorIndex];
                        if (neighbour) {
                            return;
                        }
                        double energy = energy(donor, backbone.get(i));
                        if (energy >= HBOND_ENERGY) {
                            return;
                        }
                        int[] acceptors = acceptorsOf[donorIndex];
                        double[] energies = energiesOf[donorIndex];
                        if (energy < energies[0]) {
                            acceptors[1] = acceptors[0];
                            energies[1] = energies[0];
                            acceptors[0] = i;
                            energies[0] = energy;
                        } else if (energy < energies[1]) {
                            acceptors[1] = i;
                            energies[1] = energy;
                        }
                    });
        }
        for (int j = 0; j < count; j++) {
            for (int i : acceptorsOf[j]) {
                if (i >= 0) {
                    partners.get(i).add(j);
                    partners.get(j).add(i);
                }
            }
        }
        partners.replaceAll(list -> Collections.unmodifiableList(list.stream().distinct().sorted().toList()));
        return new HydrogenBonds(acceptorsOf, partners);
    }

    /** DSSP electrostatic energy, in kcal/mol, of the H-bond N–H(donor) ··· O=C(acceptor). */
    private static double energy(Backbone donor, Backbone acceptor) {
        double on = distance(acceptor.o, donor.n);
        double ch = distance(acceptor.c, donor.h);
        double oh = distance(acceptor.o, donor.h);
        double cn = distance(acceptor.c, donor.n);
        if (on < 0.5 || ch < 0.5 || oh < 0.5 || cn < 0.5) {
            return MIN_ENERGY;
        }
        return Math.max(MIN_ENERGY, COUPLING * (1 / on + 1 / ch - 1 / oh - 1 / cn));
    }

    private static Optional<Backbone> backboneOf(Structure structure, Residue residue) {
        if (!residue.polymer()) {
            return Optional.empty();
        }
        double[] n = null, ca = null, c = null, o = null;
        Atom oxygen = null;
        for (Atom atom : structure.atomsOf(residue)) {
            switch (atom.name()) {
                case "N" -> n = position(atom);
                case "CA" -> ca = position(atom);
                case "C" -> c = position(atom);
                case "O" -> {
                    o = position(atom);
                    oxygen = atom;
                }
                default -> {
                }
            }
        }
        if (n == null || ca == null || c == null || o == null) {
            return Optional.empty();
        }
        return Optional.of(new Backbone(residue, oxygen, n, ca, c, o, null));
    }

    private static double[] position(Atom atom) {
        return new double[] {atom.x(), atom.y(), atom.z()};
    }

    private static double distance(double[] a, double[] b) {
        double dx = a[0] - b[0], dy = a[1] - b[1], dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    private static double[] scale(double[] a, double s) {
        return new double[] {a[0] * s, a[1] * s, a[2] * s};
    }

    private static double[] normalise(double[] a) {
        double length = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return length == 0 ? a : scale(a, 1 / length);
    }
}
