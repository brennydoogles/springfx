package com.brendondugan.springfx.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** The View Name checks and the generated Views enum (ADR-0004). */
class ViewsGenerationTest {

    private static final String APP = """
            package demo;

            import com.brendondugan.springfx.SpringFXApplication;

            public class App extends SpringFXApplication {
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
        Path fxml = Files.createDirectories(resources.resolve("fxml")).resolve("view.fxml");
        Files.writeString(fxml, """
                <?xml version="1.0" encoding="UTF-8"?>
                <?import javafx.scene.layout.VBox?>
                <VBox xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"/>
                """);
    }

    @Test
    void generatesViewsInTheApplicationsPackageSortedByViewName() throws IOException {
        CompilationResult result = compiler()
                .source("demo.App", APP)
                .source("demo.MainController", controller("demo", "MainController", "MAIN", true))
                .source("demo.LoginController", controller("demo", "LoginController", "LOGIN_PAGE", false))
                .compile();

        assertThat(result.success()).as(result.toString()).isTrue();
        assertThat(result.diagnostics()).as(result.toString()).isEmpty();
        String views = views("demo");
        assertThat(views)
                .contains("package demo;")
                .contains("@javax.annotation.processing.Generated(\"" + SpringFXProcessor.class.getName() + "\")")
                .contains("public enum Views implements com.brendondugan.springfx.ViewName")
                .contains("/** {@link demo.LoginController} */")
                .contains("LOGIN_PAGE(demo.LoginController.class)")
                .contains("MAIN(demo.MainController.class)");
        assertThat(views.indexOf("LOGIN_PAGE(")).isLessThan(views.indexOf("MAIN("));
    }

    @Test
    void generatedViewsAreViewNamesThatCodeCanUse() {
        CompilationResult result = compiler()
                .source("demo.App", APP)
                .source("demo.MainController", controller("demo", "MainController", "MAIN", true))
                .source("demo.Opener", """
                        package demo;

                        import com.brendondugan.springfx.ViewName;
                        import com.brendondugan.springfx.WindowManager;

                        class Opener {
                            void open(WindowManager windows) {
                                ViewName view = Views.MAIN;
                                Class<?> type = Views.MAIN.controllerType();
                                windows.open(Views.MAIN);
                            }
                        }
                        """)
                .compile();

        assertThat(result.success()).as(result.toString()).isTrue();
    }

    @Test
    void theViewsPackageOptionOverridesTheApplicationsPackage() throws IOException {
        CompilationResult result = compiler()
                .viewsPackage("demo.ui")
                .source("demo.App", APP)
                .source("demo.MainController", controller("demo", "MainController", "MAIN", true))
                .compile();

        assertThat(result.success()).as(result.toString()).isTrue();
        assertThat(views("demo.ui")).contains("package demo.ui;", "MAIN(demo.MainController.class)");
    }

    @Test
    void aViewNameThatIsNotUpperSnakeCaseIsAnErrorStatingTheRule() {
        for (String bad : new String[] {"loginPage", "Login", "LOGIN__PAGE", "_LOGIN", "LOGIN_", "1LOGIN", ""}) {
            CompilationResult result = compiler()
                    .source("demo.App", APP)
                    .source("demo.MainController", controller("demo", "MainController", bad, true))
                    .compile();

            assertThat(result.errorMessages()).as(bad).singleElement().asString()
                    .contains("\"" + bad + "\"", "demo.MainController", "UPPER_SNAKE_CASE");
        }
    }

    @Test
    void aViewNameUsedTwiceIsAnErrorOnEachNamingBoth() {
        CompilationResult result = compiler()
                .source("demo.App", APP)
                .source("demo.MainController", controller("demo", "MainController", "SETTINGS", true))
                .source("demo.SettingsController", controller("demo", "SettingsController", "SETTINGS", false))
                .compile();

        assertThat(result.errors()).hasSize(2).allSatisfy(error -> {
            assertThat(error.getMessage(null)).contains("SETTINGS", "demo.MainController", "demo.SettingsController");
            assertThat(error.getLineNumber()).isEqualTo(5);
        });
    }

    @Test
    void aControllerViewsCannotReachIsAnErrorOnItsAnnotation() {
        CompilationResult result = compiler()
                .source("demo.App", APP)
                .source("demo.MainController", controller("demo", "MainController", "MAIN", true))
                .source("demo.settings.SettingsController", """
                        package demo.settings;

                        import com.brendondugan.springfx.SpringFXController;

                        @SpringFXController(value = "/fxml/view.fxml", viewName = "SETTINGS")
                        class SettingsController {
                        }
                        """)
                .compile();

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.getMessage(null))
                    .contains("demo.settings.SettingsController", "demo.Views", "public", "-Aspringfx.viewsPackage");
            assertThat(error.getLineNumber()).isEqualTo(5);
        });
    }

    @Test
    void aPackagePrivateControllerInTheViewsPackageIsFine() {
        CompilationResult result = compiler()
                .source("demo.App", APP)
                .source("demo.MainController", """
                        package demo;

                        import com.brendondugan.springfx.SpringFXController;

                        @SpringFXController(value = "/fxml/view.fxml", viewName = "MAIN", primary = true)
                        class MainController {
                        }
                        """)
                .compile();

        assertThat(result.success()).as(result.toString()).isTrue();
    }

    @Test
    void aPublicNestedControllerIsNamedByItsCanonicalName() throws IOException {
        CompilationResult result = compiler()
                .source("demo.App", APP)
                .source("demo.Screens", """
                        package demo;

                        import com.brendondugan.springfx.SpringFXController;

                        public class Screens {
                            @SpringFXController(value = "/fxml/view.fxml", viewName = "MAIN", primary = true)
                            public static class Main {
                            }
                        }
                        """)
                .compile();

        assertThat(result.success()).as(result.toString()).isTrue();
        assertThat(views("demo")).contains("MAIN(demo.Screens.Main.class)");
    }

    @Test
    void aModuleWithoutAnApplicationOrTheOptionIsAnErrorSayingToSetIt() {
        CompilationResult result = new TestCompiler(classOutput).resources(resources).generatedSources(generated)
                .source("demo.SettingsController", controller("demo", "SettingsController", "SETTINGS", false))
                .compile();

        assertThat(result.errorMessages()).singleElement().asString()
                .contains("no SpringFX Application", "-Aspringfx.viewsPackage");
    }

    @Test
    void applicationsInDifferentPackagesNeedTheOption() {
        CompilationResult result = new TestCompiler(classOutput).resources(resources).generatedSources(generated)
                .source("demo.App", APP)
                .source("demo.other.OtherApp", """
                        package demo.other;

                        public class OtherApp extends com.brendondugan.springfx.SpringFXApplication {
                        }
                        """)
                .source("demo.MainController", controller("demo", "MainController", "MAIN", true))
                .compile();

        assertThat(result.errorMessages()).singleElement().asString()
                .contains("demo.App", "demo.other.OtherApp", "different packages", "-Aspringfx.viewsPackage");
    }

    @Test
    void recompilingOneControllerStillGeneratesEveryView() throws IOException {
        assertThat(compiler()
                .source("demo.App", APP)
                .source("demo.MainController", controller("demo", "MainController", "MAIN", true))
                .source("demo.LoginController", controller("demo", "LoginController", "LOGIN_PAGE", false))
                .compile().success()).isTrue();
        // The IDE would keep the old file; delete it to prove the incremental build regenerates it.
        Files.delete(generated.resolve("demo/Views.java"));

        CompilationResult incremental = new TestCompiler(classOutput).resources(resources).generatedSources(generated)
                .source("demo.LoginController", controller("demo", "LoginController", "LOGIN_PAGE", false))
                .compile();

        assertThat(incremental.success()).as(incremental.toString()).isTrue();
        assertThat(views("demo")).contains("LOGIN_PAGE(demo.LoginController.class)", "MAIN(demo.MainController.class)");
    }

    @Test
    void renamingAViewNameInAnIncrementalBuildDropsTheOldConstant() throws IOException {
        assertThat(compiler()
                .source("demo.App", APP)
                .source("demo.MainController", controller("demo", "MainController", "MAIN", true))
                .source("demo.LoginController", controller("demo", "LoginController", "LOGIN_PAGE", false))
                .compile().success()).isTrue();

        CompilationResult incremental = new TestCompiler(classOutput).resources(resources).generatedSources(generated)
                .source("demo.LoginController", controller("demo", "LoginController", "SIGN_IN", false))
                .compile();

        assertThat(incremental.success()).as(incremental.toString()).isTrue();
        assertThat(views("demo")).contains("SIGN_IN(", "MAIN(").doesNotContain("LOGIN_PAGE");
    }

    @Test
    void aViewNameTakenByAControllerNotRecompiledIsStillADuplicate() throws IOException {
        assertThat(compiler()
                .source("demo.App", APP)
                .source("demo.MainController", controller("demo", "MainController", "MAIN", true))
                .compile().success()).isTrue();

        CompilationResult incremental = new TestCompiler(classOutput).resources(resources).generatedSources(generated)
                .source("demo.LoginController", controller("demo", "LoginController", "MAIN", false))
                .compile();

        assertThat(incremental.errorMessages()).singleElement().asString()
                .contains("MAIN", "demo.MainController", "demo.LoginController");
    }

    private TestCompiler compiler() {
        return new TestCompiler(classOutput).resources(resources).generatedSources(generated);
    }

    private String views(String pkg) throws IOException {
        return Files.readString(generated.resolve(pkg.replace('.', '/')).resolve("Views.java"));
    }

    /** A public Controller of /fxml/view.fxml; its annotation is on line 5. */
    private static String controller(String pkg, String simpleName, String viewName, boolean primary) {
        return """
                package %s;

                import com.brendondugan.springfx.SpringFXController;

                @SpringFXController(value = "/fxml/view.fxml", viewName = "%s"%s)
                public class %s {
                }
                """.formatted(pkg, viewName, primary ? ", primary = true" : "", simpleName);
    }
}
