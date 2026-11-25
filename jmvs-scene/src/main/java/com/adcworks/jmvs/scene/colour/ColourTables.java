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
 * secondary-structure colours and the chain palette.
 */
public record ColourTables(Map<Integer, Integer> cpk, Map<String, Integer> residues, int otherResidue,
                           Map<SecondaryStructure, Integer> structure, List<Integer> chainPalette) {

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
        return new ColourTables(cpk, residues, other == null ? WHITE : other, structure, palette);
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
