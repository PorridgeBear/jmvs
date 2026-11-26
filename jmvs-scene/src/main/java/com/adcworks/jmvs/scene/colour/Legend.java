package com.adcworks.jmvs.scene.colour;

import java.util.List;

/**
 * Renderer-neutral description of what a colour scheme's colours mean, for the UI to draw as a key.
 */
public sealed interface Legend {

    String title();

    /**
     * One colour and its meaning.
     */
    record Entry(String label, int rgb) {
    }

    /**
     * Discrete colours, e.g. confidence bands, in display order.
     */
    record Bands(String title, List<Entry> entries) implements Legend {
        public Bands {
            entries = List.copyOf(entries);
        }
    }

    /**
     * A continuous gradient through {@code stops} (evenly spaced) from {@code min} to {@code max}.
     */
    record Gradient(String title, List<Integer> stops, float min, float max) implements Legend {
        public Gradient {
            stops = List.copyOf(stops);
        }
    }
}
