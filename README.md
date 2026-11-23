# SpringFX

Bind Spring Boot and JavaFX together: one application class, Controllers that own their FXML, a Window Manager for everything outside FXML, and Window Settings with sensible defaults.

> **Status: early development.** Launching an app and showing its Primary View works; the Window Manager and the compile-time checks are still to come. The design is recorded in [`GLOSSARY.md`](GLOSSARY.md) and [`docs/adr/`](docs/adr/); the work is tracked in [issues](https://github.com/brennydoogles/springfx/issues).

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

(`WindowManager` arrives with [#4](https://github.com/brennydoogles/springfx/issues/4).)

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

The build has two modules: `springfx`, the runtime library, and `springfx-processor`, its compile-time checks. Releases are git tags, which JitPack builds using `jitpack.yml`.

## Licence

Apache License 2.0. See [LICENSE](LICENSE).
