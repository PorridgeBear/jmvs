package com.adcworks.jmvs.model;

import java.util.Properties;

public class Atom {

    private final String id;
    private final String symbol;
    private final String label;
    private final float x, y, z;
    private boolean isHeterogen;
    private float covalentRadius;
    private Residue residue;
    private final Properties properties;

    public Atom(String id, String symbol, String label, float x, float y, float z) {
        this.id = id;
        this.symbol = symbol;
        this.label = label;
        this.x = x;
        this.y = y;
        this.z = z;
        this.properties = new Properties();
    }

    public String getId()
    {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getLabel() {
        return label;
    }

    public float getX() {
        return x;
    }
    public float getY() {
        return y;
    }
    public float getZ() {
        return z;
    }

    public Residue getResidue()
    {
        return residue;
    }

    public void setResidue(Residue residue)
    {
        this.residue = residue;
    }
    
    public void setIsHeterogen(boolean isHeterogen) {
        this.isHeterogen = isHeterogen;
    }
    
    public boolean isHeterogen() {
        return this.isHeterogen;
    }
    
    public void addProperty(String key, String value) {
        if (key != null && value != null) {
            properties.setProperty(key, value);
        }
    }

    public void setCovalentRadius(float covalentRadius)
    {
        this.covalentRadius = covalentRadius;
    }

    public float getCovalentRadius()
    {
        return covalentRadius;
    }

    public String toString() {
        return String.format("%s (%s)", symbol, label);
    }

    public boolean isElement(String[] elements) {
        for (String element : elements) {
            if (this.symbol.equals(element)) {
                return true;
            }
        }

        return false;
    }

    public boolean isHydrogen()
    {
        return symbol.equalsIgnoreCase("H");
    }
}
