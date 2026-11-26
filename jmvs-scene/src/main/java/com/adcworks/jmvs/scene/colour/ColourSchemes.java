package com.adcworks.jmvs.scene.colour;

import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.Structure;

import java.util.List;
import java.util.Optional;

/**
 * Built-in colour schemes: the legacy paint modes plus B-factor and model confidence.
 */
public final class ColourSchemes {

    private ColourSchemes() {
    }

    private record Simple(String id, String displayName, AtomColour colour) implements ColourScheme {
        @Override
        public int colourOf(Structure structure, Atom atom) {
            return colour.of(structure, atom);
        }
    }

    @FunctionalInterface
    private interface AtomColour {
        int of(Structure structure, Atom atom);
    }

    /**
     * By element (RasMol CPK table).
     */
    public static ColourScheme cpk(ColourTables tables) {
        return new Simple("cpk", "CPK", (s, a) -> tables.cpkColour(a.element().cpkIndex()));
    }

    /**
     * By chain, using a fixed palette in order of first appearance so colours are stable between runs.
     */
    public static ColourScheme chain(ColourTables tables) {
        return new Simple("chain", "Chain", (s, a) -> tables.chainColour(s.chainOf(a).order()));
    }

    /**
     * By residue type (RasMol amino colours).
     */
    public static ColourScheme group(ColourTables tables) {
        return new Simple("group", "Group", (s, a) -> tables.residueColour(s.residueOf(a).name()));
    }

    /**
     * By secondary structure: helix, sheet, turn, coil.
     */
    public static ColourScheme structure(ColourTables tables) {
        return new Simple("structure", "Structure",
                (s, a) -> tables.structureColour(s.residueOf(a).secondaryStructure()));
    }

    /**
     * By B-factor, on a low → mid → high gradient scaled to the structure's own range (as RasMol's temperature
     * colouring).
     */
    public static ColourScheme bFactor(ColourTables tables) {
        return new BFactor(tables.bFactor());
    }

    /**
     * By AlphaFold model confidence (pLDDT, read from the B-factor column) in the AlphaFold DB bands. Works on any
     * file; on experimental structures it simply bands the B-factors.
     */
    public static ColourScheme confidence(ColourTables tables) {
        return new Confidence(tables.confidence(), tables.bFactor().missing());
    }

    /** Lowest and highest B-factor of a structure; {@code null} if no atom has one. */
    private record Range(float min, float max) {
        static Range of(Structure structure) {
            float min = Float.POSITIVE_INFINITY;
            float max = Float.NEGATIVE_INFINITY;
            for (Atom atom : structure.atoms()) {
                if (atom.hasBFactor()) {
                    min = Math.min(min, atom.bFactor());
                    max = Math.max(max, atom.bFactor());
                }
            }
            return min <= max ? new Range(min, max) : null;
        }
    }

    private record BFactor(ColourTables.BFactorColours colours) implements ColourScheme {
        @Override
        public String id() {
            return "bfactor";
        }

        @Override
        public String displayName() {
            return "B-factor";
        }

        /** Scans the structure for its range on every call; {@link #colours(Structure)} scans once. */
        @Override
        public int colourOf(Structure structure, Atom atom) {
            return colourOf(Range.of(structure), atom);
        }

        @Override
        public int[] colours(Structure structure) {
            Range range = Range.of(structure);
            int[] result = new int[structure.atoms().size()];
            for (Atom atom : structure.atoms()) {
                result[atom.index()] = colourOf(range, atom);
            }
            return result;
        }

        @Override
        public Optional<Legend> legend(Structure structure) {
            Range range = Range.of(structure);
            if (range == null) {
                return Optional.of(new Legend.Bands("B-factor",
                        List.of(new Legend.Entry("Not in file", colours.missing()))));
            }
            return Optional.of(new Legend.Gradient("B-factor",
                    List.of(colours.low(), colours.mid(), colours.high()), range.min(), range.max()));
        }

        private int colourOf(Range range, Atom atom) {
            if (range == null || !atom.hasBFactor()) {
                return colours.missing();
            }
            float t = range.max() > range.min()
                    ? (atom.bFactor() - range.min()) / (range.max() - range.min()) : 0.5f;
            return t < 0.5f ? lerp(colours.low(), colours.mid(), t * 2)
                    : lerp(colours.mid(), colours.high(), t * 2 - 1);
        }
    }

    private record Confidence(ColourTables.ConfidenceColours colours, int missing) implements ColourScheme {
        @Override
        public String id() {
            return "confidence";
        }

        @Override
        public String displayName() {
            return "Confidence (pLDDT)";
        }

        /** Same boundaries as the AlphaFold DB viewer: a score of exactly 90 is "confident", not "very high". */
        @Override
        public int colourOf(Structure structure, Atom atom) {
            if (!atom.hasBFactor()) {
                return missing;
            }
            float plddt = atom.bFactor();
            return plddt > 90 ? colours.veryHigh()
                    : plddt > 70 ? colours.confident()
                    : plddt > 50 ? colours.low()
                    : colours.veryLow();
        }

        @Override
        public Optional<Legend> legend(Structure structure) {
            return Optional.of(new Legend.Bands("Model confidence (pLDDT)", List.of(
                    new Legend.Entry("Very high (> 90)", colours.veryHigh()),
                    new Legend.Entry("Confident (70-90)", colours.confident()),
                    new Legend.Entry("Low (50-70)", colours.low()),
                    new Legend.Entry("Very low (< 50)", colours.veryLow()))));
        }
    }

    private static int lerp(int from, int to, float t) {
        return Rgb.of(Math.round(channel(from, 16) + (channel(to, 16) - channel(from, 16)) * t),
                Math.round(channel(from, 8) + (channel(to, 8) - channel(from, 8)) * t),
                Math.round(channel(from, 0) + (channel(to, 0) - channel(from, 0)) * t));
    }

    private static int channel(int rgb, int shift) {
        return (rgb >> shift) & 0xFF;
    }
}
