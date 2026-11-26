package com.adcworks.jmvs.core.io;

import com.adcworks.jmvs.core.map.DensityMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class MrcMapReaderTest {

    private static final MrcMapReader READER = new MrcMapReader(1_000_000);

    /**
     * A synthetic map file: header fields settable, values written in file (column, row, section) order.
     */
    static final class MrcFile {
        ByteOrder order = ByteOrder.LITTLE_ENDIAN;
        boolean stamp = true;
        int[] n = {4, 3, 2};
        int mode = 2;
        int[] start = {0, 0, 0};
        int[] sampling = {4, 3, 2};
        float[] cell = {8, 6, 4};
        float[] angles = {90, 90, 90};
        int[] axes = {1, 2, 3};
        int extended = 0;
        float[] origin = {0, 0, 0};
        /** Value at file position {@code (c, r, s)}. */
        java.util.function.IntUnaryOperator value = i -> i;

        byte[] bytes() {
            int count = n[0] * n[1] * n[2];
            int size = switch (mode) {
                case 0 -> 1;
                case 1, 6, 12 -> 2;
                default -> 4;
            };
            ByteBuffer b = ByteBuffer.allocate(MrcMapReader.HEADER_BYTES + extended + count * size).order(order);
            b.putInt(0, n[0]).putInt(4, n[1]).putInt(8, n[2]).putInt(12, mode);
            b.putInt(16, start[0]).putInt(20, start[1]).putInt(24, start[2]);
            b.putInt(28, sampling[0]).putInt(32, sampling[1]).putInt(36, sampling[2]);
            b.putFloat(40, cell[0]).putFloat(44, cell[1]).putFloat(48, cell[2]);
            b.putFloat(52, angles[0]).putFloat(56, angles[1]).putFloat(60, angles[2]);
            b.putInt(64, axes[0]).putInt(68, axes[1]).putInt(72, axes[2]);
            b.putInt(92, extended);
            b.putFloat(196, origin[0]).putFloat(200, origin[1]).putFloat(204, origin[2]);
            b.put(208, (byte) 'M').put(209, (byte) 'A').put(210, (byte) 'P').put(211, (byte) ' ');
            if (stamp) {
                byte s = order == ByteOrder.LITTLE_ENDIAN ? (byte) 0x44 : (byte) 0x11;
                b.put(212, s).put(213, order == ByteOrder.LITTLE_ENDIAN ? (byte) 0x41 : (byte) 0x11);
            }
            int at = MrcMapReader.HEADER_BYTES + extended;
            for (int i = 0; i < count; i++) {
                int v = value.applyAsInt(i);
                switch (mode) {
                    case 0 -> b.put(at, (byte) v);
                    case 1, 6 -> b.putShort(at, (short) v);
                    case 12 -> b.putShort(at, Float.floatToFloat16(v));
                    default -> b.putFloat(at, v);
                }
                at += size;
            }
            return b.array();
        }

        DensityMap read() throws Exception {
            return READER.read(new ByteArrayInputStream(bytes()), "test.map");
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 6, 12})
    void readsEveryRealModeInBothByteOrders(int mode) throws Exception {
        for (ByteOrder order : new ByteOrder[]{ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN}) {
            MrcFile file = new MrcFile();
            file.mode = mode;
            file.order = order;
            DensityMap map = file.read();

            assertThat(map.nx()).isEqualTo(4);
            assertThat(map.ny()).isEqualTo(3);
            assertThat(map.nz()).isEqualTo(2);
            for (int i = 0; i < 24; i++) {
                assertThat(map.values()[i]).isEqualTo(i);
            }
            assertThat(map.axes()).containsExactly(2, 0, 0, 0, 2, 0, 0, 0, 2);
            assertThat(map.statistics().min()).isZero();
            assertThat(map.statistics().max()).isEqualTo(23);
            assertThat(map.statistics().mean()).isEqualTo(11.5f);
        }
    }

    @Test
    void signedAndUnsignedIntegerModes() throws Exception {
        MrcFile signed = new MrcFile();
        signed.mode = 0;
        signed.value = i -> -i;
        assertThat(signed.read().values()[5]).isEqualTo(-5);
        MrcFile unsigned = new MrcFile();
        unsigned.mode = 6;
        unsigned.value = i -> 40_000 + i;
        assertThat(unsigned.read().values()[1]).isEqualTo(40_001);
    }

    @Test
    void guessesByteOrderWithoutMachineStamp() throws Exception {
        for (ByteOrder order : new ByteOrder[]{ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN}) {
            MrcFile file = new MrcFile();
            file.order = order;
            file.stamp = false;
            assertThat(file.read().nx()).isEqualTo(4);
        }
    }

    @Test
    void mapsColumnsRowsAndSectionsToXyz() throws Exception {
        // Columns along z, rows along x, sections along y.
        MrcFile file = new MrcFile();
        file.axes = new int[]{3, 1, 2};
        file.sampling = new int[]{3, 2, 4};
        file.cell = new float[]{3, 2, 4};
        DensityMap map = file.read();

        assertThat(new int[]{map.nx(), map.ny(), map.nz()}).containsExactly(3, 2, 4);
        for (int s = 0; s < 2; s++) {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 4; c++) {
                    assertThat(map.value(r, s, c)).isEqualTo(c + 4 * (r + 3 * s));
                }
            }
        }
    }

    @Test
    void skipsTheExtendedHeader() throws Exception {
        MrcFile file = new MrcFile();
        file.extended = 80;
        assertThat(file.read().values()[23]).isEqualTo(23);
    }

    @Test
    void originFieldTakesPrecedenceOverStartIndices() throws Exception {
        MrcFile file = new MrcFile();
        file.start = new int[]{-2, 1, 3};
        float[] p = new float[3];
        file.read().position(0, 0, 0, p);
        assertThat(p).containsExactly(-4, 2, 6);

        file.origin = new float[]{10, 20, 30};
        file.read().position(1, 0, 0, p);
        assertThat(p).containsExactly(12, 20, 30);
    }

    @Test
    void obliqueCellsGiveObliqueAxes() throws Exception {
        MrcFile file = new MrcFile();
        file.angles = new float[]{90, 90, 120};
        DensityMap map = file.read();
        float[] p = new float[3];
        map.position(0, 1, 0, p);
        assertThat(p[0]).isCloseTo(-1f, within(1e-5f));
        assertThat(p[1]).isCloseTo((float) Math.sqrt(3), within(1e-5f));
        assertThat(map.voxelVolume()).isCloseTo(8 * Math.sin(Math.toRadians(120)), within(1e-5));
    }

    @Test
    void binsLargeMapsByAveraging() throws Exception {
        MrcFile file = new MrcFile();
        file.n = new int[]{5, 4, 4};
        file.sampling = new int[]{5, 4, 4};
        file.cell = new float[]{5, 4, 4};
        file.value = i -> i % 5;
        DensityMap map = new MrcMapReader(20).read(new ByteArrayInputStream(file.bytes()), "big.map");

        assertThat(new int[]{map.nx(), map.ny(), map.nz()}).containsExactly(3, 2, 2);
        // Blocks along x hold columns {0, 1}, {2, 3} and {4} alone at the edge.
        assertThat(map.value(0, 0, 0)).isEqualTo(0.5f);
        assertThat(map.value(1, 1, 1)).isEqualTo(2.5f);
        assertThat(map.value(2, 0, 1)).isEqualTo(4f);
        assertThat(map.axes()[0]).isEqualTo(2f);
        // The first block's centre is half a voxel in from the first point.
        assertThat(map.origin()).containsExactly(0.5f, 0.5f, 0.5f);
    }

    @Test
    void rejectsUnsupportedModesBadAxesAndTruncatedFiles() {
        MrcFile complex = new MrcFile();
        complex.mode = 4;
        assertThatThrownBy(complex::read).isInstanceOf(MapReadException.class).hasMessageContaining("mode 4");

        MrcFile axes = new MrcFile();
        axes.axes = new int[]{1, 1, 3};
        assertThatThrownBy(axes::read).isInstanceOf(MapReadException.class).hasMessageContaining("axis order");

        byte[] bytes = new MrcFile().bytes();
        assertThatThrownBy(() -> READER.read(new ByteArrayInputStream(Arrays.copyOf(bytes, bytes.length - 4)), "t.map"))
                .isInstanceOf(MapReadException.class).hasMessageContaining("truncated");
        assertThatThrownBy(() -> READER.read(new ByteArrayInputStream(new byte[100]), "t.map"))
                .isInstanceOf(MapReadException.class).hasMessageContaining("header");
    }

    @Test
    void readsGzipFilesAndRecognisesMapExtensions(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("test.map.gz");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(new MrcFile().bytes());
        }
        Files.write(file, bytes.toByteArray());
        DensityMap map = READER.read(file);

        assertThat(map.name()).isEqualTo("test.map");
        assertThat(map.values()[7]).isEqualTo(7);
        assertThat(MrcMapReader.supports("emd_3488.map.gz")).isTrue();
        assertThat(MrcMapReader.supports("x.MRC")).isTrue();
        assertThat(MrcMapReader.supports("x.ccp4")).isTrue();
        assertThat(MrcMapReader.supports("1crn.cif")).isFalse();
        assertThat(MrcMapReader.supports("map")).isFalse();
    }

    @Test
    void missingFileIsAReadError(@TempDir Path dir) {
        assertThatThrownBy(() -> READER.read(dir.resolve("none.map"))).isInstanceOf(MapReadException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void statisticsConvertBetweenLevelsAndSigma() {
        DensityMap.Statistics stats = DensityMap.Statistics.of(new float[]{-1, 1, -1, 1});
        assertThat(stats.mean()).isZero();
        assertThat(stats.rms()).isEqualTo(1f);
        assertThat(stats.level(2.5f)).isEqualTo(2.5f);
        assertThat(stats.sigma(0.5f)).isEqualTo(0.5f);
    }
}
