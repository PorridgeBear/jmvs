package com.adcworks.jmvs.app;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Native "open file" dialog via LWJGL's tinyfd binding, for structures and density maps.
 */
final class FileOpener {

    private static final String[] PATTERNS = {"*.cif", "*.mmcif", "*.bcif", "*.map", "*.mrc", "*.ccp4", "*.gz"};

    private FileOpener() {
    }

    /**
     * Shows the dialog and blocks until the user chooses a file or cancels.
     */
    static Optional<Path> choose(Path startDirectory) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(PATTERNS.length);
            for (String pattern : PATTERNS) {
                filters.put(stack.UTF8(pattern));
            }
            filters.flip();
            String start = startDirectory.toAbsolutePath() + startDirectory.getFileSystem().getSeparator();
            String result = TinyFileDialogs.tinyfd_openFileDialog("Open structure or map", start, filters,
                    "Structures and density maps (*.cif, *.bcif, *.map, *.mrc, *.ccp4, *.gz)", false);
            return Optional.ofNullable(result).map(Path::of);
        }
    }
}
