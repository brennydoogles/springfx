package com.brendondugan.springfx.processor;

import javax.annotation.processing.Processor;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compiles sample sources with the JDK compiler, the way a consumer's build would, into a class
 * output folder that persists between compilations so incremental builds can be imitated.
 */
class TestCompiler {

    private final Path classOutput;
    private final Map<String, String> sources = new LinkedHashMap<>();
    private final List<String> options = new ArrayList<>();
    private String classPath = System.getProperty("java.class.path");
    private Path processorPath;
    private Path generatedSources;

    TestCompiler(Path classOutput) {
        this.classOutput = classOutput;
    }

    TestCompiler source(String className, String code) {
        sources.put(className, code);
        return this;
    }

    TestCompiler resources(Path folder) {
        options.add("-A" + SpringFXProcessor.RESOURCES_OPTION + "=" + folder);
        return this;
    }

    TestCompiler viewsPackage(String pkg) {
        options.add("-A" + SpringFXProcessor.VIEWS_PACKAGE_OPTION + "=" + pkg);
        return this;
    }

    /** Where generated sources such as Views are written, so tests can read them. */
    TestCompiler generatedSources(Path folder) {
        this.generatedSources = folder;
        return this;
    }

    TestCompiler classPath(String classPath) {
        this.classPath = classPath;
        return this;
    }

    /** Finds the processor on this path through its service file instead of passing it in. */
    TestCompiler processorPath(Path processorPath) {
        this.processorPath = processorPath;
        return this;
    }

    CompilationResult compile() {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            Files.createDirectories(classOutput);
            List<String> args = new ArrayList<>(options);
            args.addAll(List.of("-d", classOutput.toString(),
                    "-classpath", classPath + File.pathSeparator + classOutput));
            if (generatedSources != null) {
                Files.createDirectories(generatedSources);
                args.addAll(List.of("-s", generatedSources.toString()));
            }
            if (processorPath != null) {
                args.addAll(List.of("--processor-path", processorPath.toString()));
            }
            List<SourceFile> units = sources.entrySet().stream()
                    .map(source -> new SourceFile(source.getKey(), source.getValue()))
                    .toList();
            JavaCompiler.CompilationTask task = compiler.getTask(null, files, diagnostics, args, null, units);
            if (processorPath == null) {
                task.setProcessors(List.<Processor>of(new SpringFXProcessor()));
            }
            return new CompilationResult(task.call(), diagnostics.getDiagnostics());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
