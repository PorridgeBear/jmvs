package com.adcworks.jmvs.core.io;

import com.adcworks.jmvs.core.model.Structure;

import java.io.InputStream;

/**
 * Reads a {@link Structure} from an (already decompressed) stream.
 */
public interface StructureReader {

    /**
     * Whether this reader handles the file name, ignoring any {@code .gz} suffix.
     */
    boolean supports(String fileName);

    /**
     * @param source file name or description, used for the structure name and error messages
     */
    Structure read(InputStream in, String source) throws StructureReadException;
}
