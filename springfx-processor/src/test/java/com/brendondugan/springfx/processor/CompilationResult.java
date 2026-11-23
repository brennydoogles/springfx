package com.brendondugan.springfx.processor;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.List;
import java.util.Locale;

/** What javac reported for one compilation. */
record CompilationResult(boolean success, List<Diagnostic<? extends JavaFileObject>> diagnostics) {

    List<Diagnostic<? extends JavaFileObject>> errors() {
        return ofKind(Diagnostic.Kind.ERROR);
    }

    List<Diagnostic<? extends JavaFileObject>> warnings() {
        return diagnostics.stream()
                .filter(d -> d.getKind() == Diagnostic.Kind.WARNING || d.getKind() == Diagnostic.Kind.MANDATORY_WARNING)
                .toList();
    }

    List<String> errorMessages() {
        return errors().stream().map(d -> d.getMessage(Locale.ROOT)).toList();
    }

    private List<Diagnostic<? extends JavaFileObject>> ofKind(Diagnostic.Kind kind) {
        return diagnostics.stream().filter(d -> d.getKind() == kind).toList();
    }

    @Override
    public String toString() {
        return diagnostics.stream().map(d -> d.getKind() + ": " + d.getMessage(Locale.ROOT)).toList().toString();
    }
}
