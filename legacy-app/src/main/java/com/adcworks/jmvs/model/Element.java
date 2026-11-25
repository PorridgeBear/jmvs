package com.adcworks.jmvs.model;

public class Element {
    
    private String key;
    private String symbol;
    private String name;
    private float covalentRadius;
    private float vanDerWaalsRadius;
    private String cpkColourIndex;
    
    public Element(
        String key,
        String symbol, 
        String name,
        String covalentRadius, 
        String vanDerWaalsRadius, 
        String cpkColourIndex) {
        this.key = key;
        this.symbol = symbol.toLowerCase();
        this.covalentRadius = Float.parseFloat(covalentRadius) / 250f;
        this.vanDerWaalsRadius = Float.parseFloat(vanDerWaalsRadius) / 250f;
        this.cpkColourIndex = cpkColourIndex;
    }
    
    public String getSymbol() {
        return this.symbol;
    }
    
    public String getName() {
        return this.name;
    }
    
    public float getCovalentRadius() {
        return covalentRadius;
    }
    
    public float getVanDerWaalsRadius() {
        return vanDerWaalsRadius;
    }
    
    public String getCPKColourIndex() {
        return this.cpkColourIndex;
    }
}