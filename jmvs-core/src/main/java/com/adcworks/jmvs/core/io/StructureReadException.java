package com.adcworks.jmvs.core.io;

/**
 * A structure file could not be read or is not valid.
 */
public class StructureReadException extends Exception {

    private final String source;

    public StructureReadException(String source, String message) {
        super(source + ": " + message);
        this.source = source;
    }

    public StructureReadException(String source, String message, Throwable cause) {
        super(source + ": " + message, cause);
        this.source = source;
    }

    /**
     * File name or other description of the input.
     */
    public String source() {
        return source;
    }
}
