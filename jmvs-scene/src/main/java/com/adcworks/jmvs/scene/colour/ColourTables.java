package com.adcworks.jmvs.scene.colour;

import com.adcworks.jmvs.core.config.ConfigurationException;
import com.adcworks.jmvs.core.model.SecondaryStructure;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Colour tables parsed once from {@code jmvs.properties}: CPK colours by index, residue colours by name,
 * secondary-structure colours, the chain palette, the B-factor gradient and the pLDDT confidence bands.
 */
public record ColourTables(Map<Integer, Integer> cpk, Map<String, Integer> residues, int otherResidue,
                           Map<SecondaryStructure, Integer> structure, List<Integer> chainPalette,
                           BFactorColours bFactor, ConfidenceColours confidence) {

    /**
     * Gradient from {@code low} through {@code mid} to {@code high}; {@code missing} for atoms without a B-factor.
     */
    public record BFactorColours(int low, int mid, int high, int missing) {
    }

    /**
     * AlphaFold DB pLDDT bands: very high (&gt; 90), confident (70–90), low (50–70), very low (&lt; 50).
     */
    public record ConfidenceColours(int veryHigh, int confident, int low, int veryLow) {
    }

    private static final int WHITE = 0xFFFFFF;

    public ColourTables {
        cpk = Map.copyOf(cpk);
        residues = Map.copyOf(residues);
        structure = Map.copyOf(structure);
        chainPalette = List.copyOf(chainPalette);
        if (chainPalette.isEmpty()) {
            throw new ConfigurationException("chain.palette must contain at least one colour");
        }
    }

    public static ColourTables parse(Properties p) {
        Map<Integer, Integer> cpk = new HashMap<>();
        Map<String, Integer> residues = new HashMap<>();
        Map<SecondaryStructure, Integer> structure = new EnumMap<>(SecondaryStructure.class);
        for (String key : p.stringPropertyNames()) {
            String value = p.getProperty(key);
            if (key.startsWith("cpk.")) {
                try {
                    cpk.put(Integer.parseInt(key.substring(4)), Rgb.parse(key, value));
                } catch (NumberFormatException e) {
                    throw new ConfigurationException("CPK index must be a number: " + key, e);
                }
            } else if (key.startsWith("residue.")) {
                residues.put(key.substring(8).toUpperCase(Locale.ROOT), Rgb.parse(key, value));
            } else if (key.startsWith("structure.")) {
                try {
                    structure.put(SecondaryStructure.valueOf(key.substring(10).toUpperCase(Locale.ROOT)),
                            Rgb.parse(key, value));
                } catch (IllegalArgumentException e) {
                    throw new ConfigurationException("Unknown secondary structure in " + key, e);
                }
            }
        }
        for (SecondaryStructure ss : SecondaryStructure.values()) {
            if (!structure.containsKey(ss)) {
                throw new ConfigurationException("Missing structure." + ss.name().toLowerCase(Locale.ROOT));
            }
        }
        Integer other = residues.remove("OTHER");
        List<Integer> palette = new ArrayList<>();
        for (String entry : p.getProperty("chain.palette", "").split(";")) {
            if (!entry.isBlank()) {
                palette.add(Rgb.parse("chain.palette", entry));
            }
        }
        return new ColourTables(cpk, residues, other == null ? WHITE : other, structure, palette,
                new BFactorColours(required(p, "bfactor.low"), required(p, "bfactor.mid"),
                        required(p, "bfactor.high"), required(p, "bfactor.missing")),
                new ConfidenceColours(required(p, "confidence.veryhigh"), required(p, "confidence.confident"),
                        required(p, "confidence.low"), required(p, "confidence.verylow")));
    }

    private static int required(Properties p, String key) {
        String value = p.getProperty(key);
        if (value == null) {
            throw new ConfigurationException("Missing " + key);
        }
        return Rgb.parse(key, value);
    }

    public int cpkColour(int index) {
        return cpk.getOrDefault(index, WHITE);
    }

    public int residueColour(String residueName) {
        return residues.getOrDefault(residueName.toUpperCase(Locale.ROOT), otherResidue);
    }

    public int structureColour(SecondaryStructure ss) {
        return structure.get(ss);
    }

    public int chainColour(int chainOrder) {
        return chainPalette.get(chainOrder % chainPalette.size());
    }
}
