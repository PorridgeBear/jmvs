package com.adcworks.jmvs.core.io;

import com.adcworks.jmvs.core.map.DensityMap;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

/**
 * Reads MRC2014 and CCP4 density maps ({@code .map}, {@code .mrc}, {@code .ccp4}, optionally gzip-compressed), as
 * distributed by EMDB.
 * <ul>
 *     <li>Byte order from the machine stamp, or guessed from the header when the stamp is missing.</li>
 *     <li>Modes 0 (signed 8-bit), 1 (16-bit), 2 (32-bit float), 6 (unsigned 16-bit) and 12 (16-bit float).</li>
 *     <li>Columns, rows and sections are mapped to x, y and z with {@code MAPC/MAPR/MAPS}; the extended header is
 *     skipped.</li>
 *     <li>The origin is the MRC2014 {@code ORIGIN} field when it is set, otherwise the start indices times the
 *     voxel size. Oblique cells are supported.</li>
 *     <li>Maps with more than {@code maxVoxels} points are binned by averaging blocks of {@code b³} points, with
 *     the smallest {@code b} that fits, to bound memory.</li>
 * </ul>
 * Statistics are computed from the values rather than trusted from the header.
 */
public final class MrcMapReader {

    private static final System.Logger LOG = System.getLogger(MrcMapReader.class.getName());
    private static final Set<String> EXTENSIONS = Set.of(".map", ".mrc", ".ccp4");
    private static final Set<Integer> MODES = Set.of(0, 1, 2, 6, 12);
    static final int HEADER_BYTES = 1024;

    private final int maxVoxels;

    /**
     * @param maxVoxels largest map to hold in memory; larger maps are binned
     */
    public MrcMapReader(int maxVoxels) {
        if (maxVoxels < 8) {
            throw new IllegalArgumentException("maxVoxels must be at least 8: " + maxVoxels);
        }
        this.maxVoxels = maxVoxels;
    }

    /**
     * Whether the file name looks like a density map, ignoring any {@code .gz} suffix.
     */
    public static boolean supports(String fileName) {
        String name = fileName.toLowerCase(Locale.ROOT);
        if (name.endsWith(".gz")) {
            name = name.substring(0, name.length() - 3);
        }
        int dot = name.lastIndexOf('.');
        return dot >= 0 && EXTENSIONS.contains(name.substring(dot));
    }

    public DensityMap read(Path path) throws MapReadException {
        String fileName = path.getFileName().toString();
        String name = fileName.toLowerCase(Locale.ROOT).endsWith(".gz")
                ? fileName.substring(0, fileName.length() - 3) : fileName;
        long start = System.nanoTime();
        try (InputStream in = StructureReaders.decompress(new BufferedInputStream(Files.newInputStream(path)))) {
            DensityMap map = read(in, name);
            LOG.log(System.Logger.Level.INFO, "Read map {0} in {1} ms", map, (System.nanoTime() - start) / 1_000_000);
            return map;
        } catch (NoSuchFileException e) {
            throw new MapReadException(fileName, "File not found", e);
        } catch (IOException e) {
            throw new MapReadException(fileName, "Cannot read file: " + e.getMessage(), e);
        }
    }

    /**
     * Reads a map from an (already decompressed) stream.
     *
     * @param source file name or description, used for the map name and error messages
     */
    public DensityMap read(InputStream in, String source) throws MapReadException, IOException {
        byte[] headerBytes = in.readNBytes(HEADER_BYTES);
        if (headerBytes.length < HEADER_BYTES) {
            throw new MapReadException(source, "Not an MRC/CCP4 map: shorter than the 1024-byte header");
        }
        ByteBuffer header = ByteBuffer.wrap(headerBytes).order(byteOrder(headerBytes));
        int[] n = {word(header, 1), word(header, 2), word(header, 3)};
        int mode = word(header, 4);
        int[] start = {word(header, 5), word(header, 6), word(header, 7)};
        int[] sampling = {word(header, 8), word(header, 9), word(header, 10)};
        float[] cell = {real(header, 11), real(header, 12), real(header, 13)};
        float[] angles = {real(header, 14), real(header, 15), real(header, 16)};
        int[] axisOf = {word(header, 17) - 1, word(header, 18) - 1, word(header, 19) - 1};
        int extendedHeader = word(header, 24);
        float[] originField = {real(header, 50), real(header, 51), real(header, 52)};

        if (n[0] <= 0 || n[1] <= 0 || n[2] <= 0) {
            throw new MapReadException(source, "Not an MRC/CCP4 map: grid size " + n[0] + "×" + n[1] + "×" + n[2]);
        }
        if (!MODES.contains(mode)) {
            throw new MapReadException(source, "Unsupported data mode " + mode
                    + "; supported: 0, 1, 2, 6 and 12 (real values)");
        }
        if (!isPermutation(axisOf)) {
            throw new MapReadException(source, "Invalid axis order MAPC/MAPR/MAPS " + (axisOf[0] + 1) + ","
                    + (axisOf[1] + 1) + "," + (axisOf[2] + 1));
        }
        if (extendedHeader < 0) {
            throw new MapReadException(source, "Invalid extended header length " + extendedHeader);
        }
        in.skipNBytes(extendedHeader);

        // Sizes and start indices in x, y, z order.
        int[] size = new int[3];
        int[] startXyz = new int[3];
        for (int file = 0; file < 3; file++) {
            size[axisOf[file]] = n[file];
            startXyz[axisOf[file]] = start[file];
        }
        int bin = binning(size);
        int[] binned = new int[3];
        for (int d = 0; d < 3; d++) {
            binned[d] = (size[d] + bin - 1) / bin;
            if (binned[d] < 2) {
                throw new MapReadException(source, "Map " + size[0] + "×" + size[1] + "×" + size[2]
                        + " is too large to bin to " + maxVoxels + " points");
            }
        }
        float[] values = new float[binned[0] * binned[1] * binned[2]];
        try {
            readValues(in, header.order(), mode, n, axisOf, bin, binned, values);
        } catch (EOFException e) {
            throw new MapReadException(source, "File is truncated: fewer values than the " + n[0] + "×" + n[1]
                    + "×" + n[2] + " grid", e);
        }
        if (bin > 1) {
            divideByBinCounts(values, size, bin, binned);
            LOG.log(System.Logger.Level.INFO, "Binned {0} {1}×{2}×{3} by {4} to fit {5} points", source, size[0],
                    size[1], size[2], bin, maxVoxels);
        }

        float[] axes = axes(source, cell, angles, sampling, size);
        float[] origin = new float[3];
        boolean hasOrigin = (originField[0] != 0 || originField[1] != 0 || originField[2] != 0)
                && Float.isFinite(originField[0]) && Float.isFinite(originField[1]) && Float.isFinite(originField[2]);
        for (int d = 0; d < 3; d++) {
            origin[d] = hasOrigin ? originField[d]
                    : startXyz[0] * axes[d] + startXyz[1] * axes[3 + d] + startXyz[2] * axes[6 + d];
            // A binned point is the mean of its block, so it sits at the block's centre.
            origin[d] += (bin - 1) / 2f * (axes[d] + axes[3 + d] + axes[6 + d]);
        }
        for (int i = 0; i < 9; i++) {
            axes[i] *= bin;
        }
        return new DensityMap(source, binned[0], binned[1], binned[2], values, origin, axes,
                DensityMap.Statistics.of(values));
    }

    /**
     * Little-endian if the machine stamp says so ({@code 0x44} first), big-endian for {@code 0x11}; otherwise the
     * order in which the mode and grid size are plausible.
     */
    static ByteOrder byteOrder(byte[] header) {
        byte stamp = header[212];
        if (stamp == 0x44) {
            return ByteOrder.LITTLE_ENDIAN;
        }
        if (stamp == 0x11) {
            return ByteOrder.BIG_ENDIAN;
        }
        ByteBuffer little = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        boolean plausible = MODES.contains(little.getInt(12)) && little.getInt(0) > 0 && little.getInt(0) < 65_536;
        return plausible ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
    }

    private int binning(int[] size) {
        int bin = 1;
        while (blocks(size[0], bin) * blocks(size[1], bin) * blocks(size[2], bin) > maxVoxels) {
            bin++;
        }
        return bin;
    }

    private static long blocks(int size, int bin) {
        return (size + bin - 1) / bin;
    }

    /**
     * Reads the values section by section, adding each to its (binned) x, y, z point.
     */
    private static void readValues(InputStream in, ByteOrder order, int mode, int[] n, int[] axisOf, int bin,
                                   int[] binned, float[] values) throws IOException {
        int bytes = switch (mode) {
            case 0 -> 1;
            case 1, 6, 12 -> 2;
            default -> 4;
        };
        // Strides in the binned x, y, z array for one step along a column, row and section.
        int[] stride = {1, binned[0], binned[0] * binned[1]};
        int cStride = stride[axisOf[0]], rStride = stride[axisOf[1]], sStride = stride[axisOf[2]];
        byte[] section = new byte[n[0] * n[1] * bytes];
        ByteBuffer buffer = ByteBuffer.wrap(section).order(order);
        for (int s = 0; s < n[2]; s++) {
            if (in.readNBytes(section, 0, section.length) < section.length) {
                throw new EOFException();
            }
            int sOffset = (s / bin) * sStride;
            for (int r = 0; r < n[1]; r++) {
                int rOffset = sOffset + (r / bin) * rStride;
                int base = r * n[0];
                for (int c = 0; c < n[0]; c++) {
                    int at = (base + c) * bytes;
                    float v = switch (mode) {
                        case 0 -> section[at];
                        case 1 -> buffer.getShort(at);
                        case 6 -> buffer.getShort(at) & 0xFFFF;
                        case 12 -> Float.float16ToFloat(buffer.getShort(at));
                        default -> buffer.getFloat(at);
                    };
                    values[rOffset + (c / bin) * cStride] += v;
                }
            }
        }
    }

    /** Turns block sums into means; blocks at the far edges may be smaller than {@code bin³}. */
    private static void divideByBinCounts(float[] values, int[] size, int bin, int[] binned) {
        for (int z = 0; z < binned[2]; z++) {
            int cz = Math.min(bin, size[2] - z * bin);
            for (int y = 0; y < binned[1]; y++) {
                int cy = Math.min(bin, size[1] - y * bin);
                for (int x = 0; x < binned[0]; x++) {
                    int cx = Math.min(bin, size[0] - x * bin);
                    values[x + binned[0] * (y + binned[1] * z)] /= cx * cy * cz;
                }
            }
        }
    }

    /**
     * Grid steps along x, y and z in Å: the cell vectors (a along x, b in the xy plane, the usual crystallographic
     * convention) divided by the sampling. A missing cell gives 1 Å voxels.
     */
    private static float[] axes(String source, float[] cell, float[] angles, int[] sampling, int[] size) {
        float[] steps = new float[3];
        for (int d = 0; d < 3; d++) {
            int m = sampling[d] > 0 ? sampling[d] : size[d];
            steps[d] = cell[d] > 0 && Float.isFinite(cell[d]) ? cell[d] / m : 1f;
        }
        if (!(cell[0] > 0 && cell[1] > 0 && cell[2] > 0)) {
            LOG.log(System.Logger.Level.WARNING, "{0} has no unit cell; assuming 1 Å voxels", source);
        }
        boolean orthogonal = true;
        for (float angle : angles) {
            orthogonal &= !(angle > 0) || Math.abs(angle - 90) < 1e-3f;
        }
        if (orthogonal) {
            return new float[]{steps[0], 0, 0, 0, steps[1], 0, 0, 0, steps[2]};
        }
        double alpha = Math.toRadians(angles[0]), beta = Math.toRadians(angles[1]), gamma = Math.toRadians(angles[2]);
        double cosA = Math.cos(alpha), cosB = Math.cos(beta), cosG = Math.cos(gamma), sinG = Math.sin(gamma);
        double cy = (cosA - cosB * cosG) / sinG;
        double cz = Math.sqrt(Math.max(0, 1 - cosB * cosB - cy * cy));
        return new float[]{
                steps[0], 0, 0,
                (float) (steps[1] * cosG), (float) (steps[1] * sinG), 0,
                (float) (steps[2] * cosB), (float) (steps[2] * cy), (float) (steps[2] * cz)};
    }

    private static boolean isPermutation(int[] axisOf) {
        boolean[] seen = new boolean[3];
        for (int a : axisOf) {
            if (a < 0 || a > 2 || seen[a]) {
                return false;
            }
            seen[a] = true;
        }
        return true;
    }

    /** Header word {@code w}, numbered from 1 as in the MRC2014 specification. */
    private static int word(ByteBuffer header, int w) {
        return header.getInt((w - 1) * 4);
    }

    private static float real(ByteBuffer header, int w) {
        return header.getFloat((w - 1) * 4);
    }
}
