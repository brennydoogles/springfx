package com.brendondugan.springfx.processor;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The Controllers and SpringFX Application classes the last compilation of this module saw,
 * recorded in the class output. An IDE's incremental build compiles only the changed files, so
 * without this record the processor would think the module's other Controllers were gone: the
 * primary-Controller checks would be wrong and {@code Views} would lose constants. Only class names
 * are recorded; whether each is primary, and its View Name, are read again from its class file.
 */
class ModuleIndex {

    static final String RESOURCE = "META-INF/springfx/module-index";

    private static final String CONTROLLER = "controller ";
    private static final String APPLICATION = "application ";

    private final Filer filer;
    private List<String> controllers;
    private List<String> applications;

    ModuleIndex(Filer filer) {
        this.filer = filer;
    }

    /** The Controller class names last recorded. */
    List<String> controllers() {
        load();
        return controllers;
    }

    /** The SpringFX Application class names last recorded. */
    List<String> applications() {
        load();
        return applications;
    }

    boolean isEmpty() {
        return controllers().isEmpty() && applications().isEmpty();
    }

    /** Records the module's Controllers and SpringFX Application classes for the next compilation. */
    void write(Collection<String> controllerNames, Collection<String> applicationNames) throws IOException {
        FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", RESOURCE);
        try (Writer writer = file.openWriter()) {
            for (String name : controllerNames) {
                writer.write(CONTROLLER + name + "\n");
            }
            for (String name : applicationNames) {
                writer.write(APPLICATION + name + "\n");
            }
        }
    }

    private void load() {
        if (controllers != null) {
            return;
        }
        controllers = new ArrayList<>();
        applications = new ArrayList<>();
        List<String> lines;
        try {
            FileObject file = filer.getResource(StandardLocation.CLASS_OUTPUT, "", RESOURCE);
            lines = file.getCharContent(true).toString().lines().map(String::strip).toList();
        } catch (IOException | IllegalArgumentException missing) {
            return;
        }
        for (String line : lines) {
            if (line.startsWith(CONTROLLER)) {
                controllers.add(line.substring(CONTROLLER.length()).strip());
            } else if (line.startsWith(APPLICATION)) {
                applications.add(line.substring(APPLICATION.length()).strip());
            }
        }
    }
}
