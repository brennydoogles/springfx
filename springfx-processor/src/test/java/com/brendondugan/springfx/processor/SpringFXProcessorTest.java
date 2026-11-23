package com.brendondugan.springfx.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SpringFXProcessorTest {

    private static final String APP = """
            package demo;

            import com.brendondugan.springfx.SpringFXApplication;

            public class App extends SpringFXApplication {
            }
            """;

    private static final String MAIN_CONTROLLER = """
            package demo;

            import com.brendondugan.springfx.SpringFXController;

            @SpringFXController(value = "/fxml/main.fxml", primary = true, viewName = "MAIN")
            public class MainController {
            }
            """;

    @TempDir
    Path work;

    private Path resources;
    private Path classOutput;
    private Path generated;

    @BeforeEach
    void folders() throws IOException {
        resources = Files.createDirectories(work.resolve("resources"));
        classOutput = work.resolve("classes");
        generated = work.resolve("generated");
    }

    @Test
    void correctControllersCompileWithoutWarnings() throws IOException {
        fxml("fxml/main.fxml", "demo.MainController");
        fxml("fxml/settings.fxml", null);

        CompilationResult result = compiler()
                .source("demo.App", APP)
                .source("demo.MainController", MAIN_CONTROLLER)
                .source("demo.SettingsController", """
                        package demo;

                        import com.brendondugan.springfx.SpringFXController;

                        @SpringFXController(fxml = "/fxml/settings.fxml", title = "Settings", viewName = "SETTINGS")
                        public class SettingsController {
                        }
                        """)
                .compile();

        assertThat(result.success()).as(result.toString()).isTrue();
        assertThat(result.diagnostics()).as(result.toString()).isEmpty();
    }

    @Test
    void missingFxmlIsAnErrorOnTheAnnotationNamingTheFileAndController() {
        CompilationResult result = compiler().source("demo.MainController", MAIN_CONTROLLER).compile();

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.getMessage(null)).contains("/fxml/main.fxml", "demo.MainController", "not found");
            assertOnAnnotation(error, MAIN_CONTROLLER);
        });
    }

    @Test
    void fxControllerNamingAnotherClassIsAnErrorNamingBothClassesAndTheFile() throws IOException {
        fxml("fxml/main.fxml", "demo.OtherController");

        CompilationResult result = compiler().source("demo.MainController", MAIN_CONTROLLER).compile();

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.getMessage(null))
                    .contains("main.fxml", "demo.OtherController", "demo.MainController");
            assertOnAnnotation(error, MAIN_CONTROLLER);
        });
    }

    @Test
    void moreThanOnePrimaryControllerIsAnErrorOnEachNamingThem() throws IOException {
        fxml("fxml/main.fxml", null);
        String second = """
                package demo;

                import com.brendondugan.springfx.SpringFXController;

                @SpringFXController(value = "/fxml/main.fxml", primary = true, viewName = "SECOND")
                public class SecondController {
                }
                """;

        CompilationResult result = compiler()
                .source("demo.MainController", MAIN_CONTROLLER)
                .source("demo.SecondController", second)
                .compile();

        assertThat(result.errors()).hasSize(2).allSatisfy(error -> assertThat(error.getMessage(null))
                .contains("More than one primary Controller", "demo.MainController", "demo.SecondController"));
        assertThat(result.errors()).allSatisfy(error -> assertThat(error.getLineNumber())
                .isEqualTo(lineOf(MAIN_CONTROLLER, "@SpringFXController")));
    }

    @Test
    void anApplicationWithoutAPrimaryControllerIsAnErrorNamingIt() throws IOException {
        fxml("fxml/settings.fxml", null);

        CompilationResult result = compiler()
                .source("demo.App", APP)
                .source("demo.SettingsController", """
                        package demo;

                        import com.brendondugan.springfx.SpringFXController;

                        @SpringFXController(value = "/fxml/settings.fxml", viewName = "SETTINGS")
                        public class SettingsController {
                        }
                        """)
                .compile();

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.getMessage(null)).contains("demo.App", "no primary Controller");
            assertThat(error.getLineNumber()).isEqualTo(lineOf(APP, "public class App"));
        });
    }

    @Test
    void aModuleWithoutAnApplicationNeedsNoPrimaryController() throws IOException {
        fxml("fxml/settings.fxml", null);

        CompilationResult result = compiler()
                .source("demo.SettingsController", """
                        package demo;

                        import com.brendondugan.springfx.SpringFXController;

                        @SpringFXController(value = "/fxml/settings.fxml", viewName = "SETTINGS")
                        public class SettingsController {
                        }
                        """)
                .compile();

        assertThat(result.success()).as(result.toString()).isTrue();
    }

    @Test
    void recompilingOnlyTheApplicationRemembersThePrimaryController() throws IOException {
        fxml("fxml/main.fxml", null);
        assertThat(compiler().source("demo.App", APP).source("demo.MainController", MAIN_CONTROLLER).compile().success())
                .isTrue();

        CompilationResult incremental = compiler().source("demo.App", APP).compile();

        assertThat(incremental.success()).as(incremental.toString()).isTrue();
    }

    @Test
    void recompilingASecondPrimaryControllerAloneStillFindsTheFirst() throws IOException {
        fxml("fxml/main.fxml", null);
        assertThat(compiler().source("demo.MainController", MAIN_CONTROLLER).compile().success()).isTrue();

        CompilationResult incremental = compiler()
                .source("demo.SecondController", """
                        package demo;

                        import com.brendondugan.springfx.SpringFXController;

                        @SpringFXController(value = "/fxml/main.fxml", primary = true, viewName = "SECOND")
                        public class SecondController {
                        }
                        """)
                .compile();

        assertThat(incremental.errorMessages()).singleElement().asString()
                .contains("More than one primary Controller", "demo.MainController", "demo.SecondController");
    }

    @Test
    void aControllerNoLongerPrimaryIsForgotten() throws IOException {
        fxml("fxml/main.fxml", null);
        assertThat(compiler().source("demo.MainController", MAIN_CONTROLLER).compile().success()).isTrue();

        CompilationResult result = compiler()
                .source("demo.App", APP)
                .source("demo.MainController", MAIN_CONTROLLER.replace(", primary = true", ""))
                .compile();

        assertThat(result.errorMessages()).singleElement().asString().contains("no primary Controller");
    }

    @Test
    void withoutTheResourcesOptionFxmlIsReadFromTheClassOutput() throws IOException {
        Path copied = classOutput.resolve("fxml/main.fxml");
        Files.createDirectories(copied.getParent());
        Files.writeString(copied, fxmlContent("demo.OtherController"));

        CompilationResult result = new TestCompiler(classOutput).viewsPackage("demo").source("demo.MainController", MAIN_CONTROLLER).compile();

        assertThat(result.errorMessages()).singleElement().asString().contains("demo.OtherController");
    }

    @Test
    void aMissingFxmlInTheClassOutputSuggestsTheResourcesOption() {
        CompilationResult result = new TestCompiler(classOutput).viewsPackage("demo").source("demo.MainController", MAIN_CONTROLLER).compile();

        assertThat(result.errorMessages()).singleElement().asString()
                .contains("the class output", "-Aspringfx.resources");
    }

    @Test
    void aRelativeFxmlPathIsResolvedAgainstTheControllersPackage() throws IOException {
        fxml("demo/views/settings.fxml", null);

        CompilationResult result = compiler()
                .source("demo.SettingsController", """
                        package demo;

                        import com.brendondugan.springfx.SpringFXController;

                        @SpringFXController(value = "views/settings.fxml", viewName = "SETTINGS")
                        public class SettingsController {
                        }
                        """)
                .compile();

        assertThat(result.success()).as(result.toString()).isTrue();
    }

    @Test
    void aNestedControllerIsNamedWithItsBinaryName() throws IOException {
        fxml("fxml/inner.fxml", "demo.Outer$Inner");

        CompilationResult result = compiler()
                .source("demo.Outer", """
                        package demo;

                        import com.brendondugan.springfx.SpringFXController;

                        public class Outer {
                            @SpringFXController(value = "/fxml/inner.fxml", viewName = "INNER")
                            public static class Inner {
                            }
                        }
                        """)
                .compile();

        assertThat(result.success()).as(result.toString()).isTrue();
    }

    @Test
    void aControllerThatNamesNoFxmlIsAnError() {
        CompilationResult result = compiler()
                .source("demo.BlankController", """
                        package demo;

                        import com.brendondugan.springfx.SpringFXController;

                        @SpringFXController(viewName = "BLANK")
                        public class BlankController {
                        }
                        """)
                .compile();

        assertThat(result.errorMessages()).singleElement().asString().contains("demo.BlankController", "does not name its FXML");
    }

    @Test
    void malformedFxmlIsAnError() throws IOException {
        Files.createDirectories(resources.resolve("fxml"));
        Files.writeString(resources.resolve("fxml/main.fxml"), "<VBox");

        CompilationResult result = compiler().source("demo.MainController", MAIN_CONTROLLER).compile();

        assertThat(result.errorMessages()).singleElement().asString().contains("main.fxml", "not well-formed");
    }

    @Test
    void doesNothingWithoutSpringFXOnTheClassPath() {
        CompilationResult result = compiler()
                .classPath(work.resolve("empty").toString())
                .source("demo.Plain", "package demo; public class Plain { }")
                .compile();

        assertThat(result.success()).as(result.toString()).isTrue();
        assertThat(result.diagnostics()).isEmpty();
    }

    @Test
    void isFoundOnAProcessorPathThroughItsServiceFile() throws IOException, URISyntaxException {
        Path processorClasses = Path.of(SpringFXProcessor.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        CompilationResult result = compiler()
                .processorPath(processorClasses)
                .source("demo.MainController", MAIN_CONTROLLER)
                .compile();

        assertThat(result.errorMessages()).singleElement().asString().contains("/fxml/main.fxml", "not found");
    }

    /** A compiler for a module whose Views go in package demo, so no application class is needed. */
    private TestCompiler compiler() {
        return new TestCompiler(classOutput).resources(resources).generatedSources(generated).viewsPackage("demo");
    }

    private void fxml(String resource, String fxController) throws IOException {
        Path file = resources.resolve(resource);
        Files.createDirectories(file.getParent());
        Files.writeString(file, fxmlContent(fxController));
    }

    private static String fxmlContent(String fxController) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <?import javafx.scene.layout.VBox?>
                <VBox xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"%s/>
                """.formatted(fxController == null ? "" : " fx:controller=\"" + fxController + "\"");
    }

    private static void assertOnAnnotation(Diagnostic<? extends JavaFileObject> error, String source) {
        assertThat(error.getLineNumber()).isEqualTo(lineOf(source, "@SpringFXController"));
    }

    private static long lineOf(String source, String text) {
        return source.lines().takeWhile(line -> !line.contains(text)).count() + 1;
    }
}
