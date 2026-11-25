package com.adcworks.jmvs.core.model;

/**
 * A chemical element with RasMol-derived radii (in ångströms) and CPK colour index.
 */
public record Element(int number, String symbol, String name, float covalentRadius, float vanDerWaalsRadius,
                      int cpkIndex) {

    public boolean isHydrogen() {
        return number == 1;
    }
}
