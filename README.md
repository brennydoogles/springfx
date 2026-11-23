# SpringFX

Bind Spring Boot and JavaFX together: one application class, Controllers that own their FXML, a Window Manager for everything outside FXML, and Window Settings with sensible defaults.

> **Status: design stage.** Nothing is released yet. The design is recorded in [`GLOSSARY.md`](GLOSSARY.md) and [`docs/adr/`](docs/adr/); the work is tracked in [issues](https://github.com/brennydoogles/springfx/issues).

## Planned usage

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

```properties
springfx.window.title=My App
springfx.window.width=900
springfx.window.height=600
```

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
