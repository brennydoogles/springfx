# SpringFX

Bind Spring Boot and JavaFX together: one application class, Controllers that own their FXML, a Window Manager for everything outside FXML, and Window Settings with sensible defaults.

> **Status: early development.** Launching an app, showing its Primary View and opening further Windows through the Window Manager work; the compile-time checks are still to come. The design is recorded in [`GLOSSARY.md`](GLOSSARY.md) and [`docs/adr/`](docs/adr/); the work is tracked in [issues](https://github.com/brennydoogles/springfx/issues).

## Usage

```java
@SpringBootApplication
public class MyApp extends SpringFXApplication {
    public static void main(String[] args) {
        launch(MyApp.class, args);
    }
}

@SpringFXController(value = "/fxml/main.fxml", primary = true)
public class MainController {
    private final WindowManager windows;

    public MainController(WindowManager windows) {
        this.windows = windows;
    }

    @FXML
    private void onSettings() {
        windows.open(SettingsController.class);
    }
}
```

`launch` starts Spring and JavaFX together and shows the primary Controller's View in the Primary Window; closing that Window ends the app. `MyApp` doesn't extend JavaFX `Application`, so it works as the main class of a fat jar.

A Controller is a prototype-scoped Spring bean that names its FXML. The FXML may declare `fx:controller` for SceneBuilder, but then it must name the Controller class. Controllers of nested `<fx:include>`s are Spring beans too. Exactly one Controller is `primary`; `title` overrides the Window title for that View.

Window Settings work with no configuration. These are the defaults:

```properties
# Unset: spring.application.name, else "Application"
springfx.window.title=
springfx.window.width=900
springfx.window.height=600
springfx.window.min-width=0
springfx.window.min-height=0
springfx.window.resizable=true
springfx.window.maximized=false
springfx.window.full-screen=false
# Class-path resources or URLs, for every Window
springfx.window.stylesheets=
springfx.window.icons=
```

Size and state apply to the Primary Window; stylesheets and icons apply to every Window.

### The Window Manager

Inject `WindowManager` into a Controller for everything about Windows that happens outside FXML:

```java
@SpringFXController(value = "/fxml/settings.fxml", title = "Settings")
public class SettingsController {
    private final WindowManager windows;

    public SettingsController(WindowManager windows) {
        this.windows = windows;
    }

    @FXML
    private void onSave() {
        // ...
        windows.close(this);
    }
}

// In MainController: open Settings as a dialog owned by the main Window.
windows.open(SettingsController.class, WindowOptions.builder()
        .owner(windows.stageOf(this))
        .modality(Modality.WINDOW_MODAL)
        .width(480.0)
        .height(320.0)
        .build());
```

| Method | Does |
|---|---|
| `primaryStage()` | The Primary Window's stage |
| `open(type)`, `open(type, options)` | Shows that Controller's View in a new Window and returns its stage. Each call makes a fresh Controller. |
| `stageOf(controller)` | The Window a Controller's View is in, including the Controllers of nested `<fx:include>`s |
| `close(controller)` | Closes that Controller's Window. Closing the Primary Window ends the app, even if other Windows are open. |

`WindowOptions` sets an opened Window's owner, modality, title and size. Build one with `WindowOptions.builder()`, or start from `WindowOptions.defaults()` and change options with `withOwner`, `withModality`, `withTitle` and `withSize`. Its title wins over the Controller's `title`, which wins over `springfx.window.title`. Without a size, the Window takes its View's preferred size; the Window Settings' size and state apply only to the Primary Window.

Every method can be called from any thread. Calls off the JavaFX application thread run on it; the methods that return a value wait for it, and `close` doesn't.

## Adding SpringFX to a build

SpringFX is published through [JitPack](https://jitpack.io/#brennydoogles/springfx) from release tags, as `com.github.brennydoogles.springfx:<module>:<tag>`. It needs Java 25 and is built against Spring Boot 4.2.0; JavaFX comes in with the dependency.

Add the JitPack repository. The compiler plugin resolves `annotationProcessorPaths` from these repositories too, so this one entry covers the processor:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Add the runtime library:

```xml
<dependency>
    <groupId>com.github.brennydoogles.springfx</groupId>
    <artifactId>springfx</artifactId>
    <version>v0.1.0</version>
</dependency>
```

From JDK 23, annotation processors on the class path aren't run automatically, so list `springfx-processor` under the compiler's `annotationProcessorPaths` ([ADR-0003](docs/adr/0003-controllers-declare-their-fxml-checked-at-compile-time.md)). If you use other processors, such as Lombok or `spring-boot-configuration-processor`, list them here too, because this list replaces discovery:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>com.github.brennydoogles.springfx</groupId>
                <artifactId>springfx-processor</artifactId>
                <version>v0.1.0</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

Replace `v0.1.0` with the tag you want; the [JitPack page](https://jitpack.io/#brennydoogles/springfx) lists them.

## Building

```bash
./mvnw verify
```

That build is headless: nothing in it starts JavaFX. Tests that open real Windows are tagged `display` and need a screen; add the `display` profile to run them too, in a JVM of their own:

```bash
./mvnw verify -Pdisplay
```

The build has two modules: `springfx`, the runtime library, and `springfx-processor`, its compile-time checks. Releases are git tags, which JitPack builds using `jitpack.yml`.

## Licence

Apache License 2.0. See [LICENSE](LICENSE).
