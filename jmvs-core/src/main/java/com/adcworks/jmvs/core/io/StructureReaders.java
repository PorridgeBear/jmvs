package com.adcworks.jmvs.core.io;

import com.adcworks.jmvs.core.chem.ElementTable;
import com.adcworks.jmvs.core.model.Structure;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

/**
 * Selects a {@link StructureReader} by file name and transparently decompresses gzip input.
 */
public final class StructureReaders {

    private static final System.Logger LOG = System.getLogger(StructureReaders.class.getName());

    private final List<StructureReader> readers;

    public StructureReaders(List<StructureReader> readers) {
        this.readers = List.copyOf(readers);
    }

    public static StructureReaders defaults(ElementTable elements) {
        return new StructureReaders(List.of(new CifStructureReader(elements)));
    }

    public boolean supports(String fileName) {
        String name = stripGzip(fileName);
        return readers.stream().anyMatch(r -> r.supports(name));
    }

    public Structure read(Path path) throws StructureReadException {
        String fileName = path.getFileName().toString();
        String name = stripGzip(fileName);
        StructureReader reader = readers.stream().filter(r -> r.supports(name)).findFirst()
                .orElseThrow(() -> new StructureReadException(fileName, "Unsupported file type"));
        long start = System.nanoTime();
        try (InputStream in = decompress(new BufferedInputStream(Files.newInputStream(path)))) {
            Structure structure = reader.read(in, name);
            LOG.log(System.Logger.Level.INFO, "Read {0} in {1} ms", structure,
                    (System.nanoTime() - start) / 1_000_000);
            return structure;
        } catch (NoSuchFileException e) {
            throw new StructureReadException(fileName, "File not found", e);
        } catch (IOException e) {
            throw new StructureReadException(fileName, "Cannot read file: " + e.getMessage(), e);
        }
    }

    static InputStream decompress(BufferedInputStream in) throws IOException {
        in.mark(2);
        int b1 = in.read();
        int b2 = in.read();
        in.reset();
        boolean gzip = b1 == 0x1f && b2 == 0x8b;
        return gzip ? new BufferedInputStream(new GZIPInputStream(in)) : in;
    }

    private static String stripGzip(String fileName) {
        return fileName.toLowerCase(Locale.ROOT).endsWith(".gz")
                ? fileName.substring(0, fileName.length() - 3) : fileName;
    }
}
