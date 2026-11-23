package com.brendondugan.springfx.processor;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.NoSuchFileException;
import java.util.Collection;
import java.util.List;

/**
 * The primary Controllers recorded in the class output by the last compilation. An IDE's
 * incremental build compiles only the changed files, so without this record a build that
 * recompiles the application class alone would think the module has no primary Controller.
 */
class PrimaryControllerIndex {

    static final String RESOURCE = "META-INF/springfx/primary-controllers";

    private final Filer filer;

    PrimaryControllerIndex(Filer filer) {
        this.filer = filer;
    }

    /** The class names the last compilation recorded, or none. */
    List<String> read() {
        try {
            FileObject file = filer.getResource(StandardLocation.CLASS_OUTPUT, "", RESOURCE);
            return file.getCharContent(true).toString().lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty())
                    .toList();
        } catch (NoSuchFileException | FileNotFoundException | IllegalArgumentException missing) {
            return List.of();
        } catch (IOException e) {
            return List.of();
        }
    }

    /** Records the module's primary Controllers for the next compilation. */
    void write(Collection<String> primaryControllers) throws IOException {
        FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", RESOURCE);
        try (Writer writer = file.openWriter()) {
            for (String name : primaryControllers) {
                writer.write(name);
                writer.write('\n');
            }
        }
    }
}
