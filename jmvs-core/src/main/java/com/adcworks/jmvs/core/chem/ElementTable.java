package com.adcworks.jmvs.core.chem;

import com.adcworks.jmvs.core.config.ConfigurationException;
import com.adcworks.jmvs.core.config.Resources;
import com.adcworks.jmvs.core.model.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Element data from RasMol's {@code abstree.h}, loaded from {@code elements.properties}. Each entry is
 * {@code element.<number>=<symbol>,<covalent radius>,<vdW radius>,<CPK index>,<name>} with radii in RasMol units
 * (250 per ångström); radii are converted to ångströms on load.
 */
public final class ElementTable {

    private static final float RASMOL_UNITS_PER_ANGSTROM = 250f;

    private final Map<String, Element> bySymbol;
    private final List<Element> elements;
    private final Element unknown;

    private ElementTable(List<Element> elements) {
        this.elements = List.copyOf(elements);
        this.bySymbol = new HashMap<>();
        for (Element e : elements) {
            bySymbol.put(e.symbol().toUpperCase(Locale.ROOT), e);
        }
        this.unknown = elements.stream().filter(e -> e.number() == 0).findFirst()
                .orElseThrow(() -> new ConfigurationException("element.0 (unknown element) is missing"));
    }

    /**
     * Loads the bundled table.
     */
    public static ElementTable load() {
        return parse(Resources.loadProperties(ElementTable.class, "elements.properties"));
    }

    public static ElementTable parse(Properties properties) {
        Map<Integer, Element> byNumber = new TreeMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith("element.")) {
                throw new ConfigurationException("Unexpected key in element table: " + key);
            }
            String[] values = properties.getProperty(key).split(",");
            if (values.length != 5) {
                throw new ConfigurationException("Expected 5 values for " + key);
            }
            try {
                int number = Integer.parseInt(key.substring("element.".length()));
                String symbol = normaliseSymbol(values[0].trim());
                byNumber.put(number, new Element(number, symbol, normaliseName(values[4].trim()),
                        Float.parseFloat(values[1].trim()) / RASMOL_UNITS_PER_ANGSTROM,
                        Float.parseFloat(values[2].trim()) / RASMOL_UNITS_PER_ANGSTROM,
                        Integer.parseInt(values[3].trim())));
            } catch (NumberFormatException e) {
                throw new ConfigurationException("Malformed number in " + key, e);
            }
        }
        return new ElementTable(List.copyOf(byNumber.values()));
    }

    /**
     * Element for an mmCIF {@code type_symbol} (case-insensitive), or the unknown element.
     */
    public Element bySymbol(String symbol) {
        if (symbol == null) {
            return unknown;
        }
        return bySymbol.getOrDefault(symbol.trim().toUpperCase(Locale.ROOT), unknown);
    }

    public Element unknown() {
        return unknown;
    }

    public List<Element> elements() {
        return elements;
    }

    private static String normaliseSymbol(String symbol) {
        return symbol.length() <= 1 ? symbol : symbol.charAt(0) + symbol.substring(1).toLowerCase(Locale.ROOT);
    }

    private static String normaliseName(String name) {
        return name.isEmpty() ? name : name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }
}
