package com.adcworks.jmvs.core.model;

/**
 * Secondary structure of a residue, read from {@code _struct_conf} and {@code _struct_sheet_range} or computed by
 * {@link com.adcworks.jmvs.core.chem.DsspAssigner}.
 */
public enum SecondaryStructure {
    HELIX, SHEET, TURN, COIL
}
