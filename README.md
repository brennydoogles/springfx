# SpringFX

Bind Spring Boot and JavaFX together: one application class, Controllers that own their FXML, a Window Manager for everything outside FXML, and Window Settings with sensible defaults.

A Spring Boot app that uses SpringFX needs one application class and no JavaFX plumbing of its own. Spring creates the Controllers, JavaFX owns the screen, and SpringFX connects the two. The terms used here (View, Controller, Primary View, Window, Window Settings, Window Manager) are defined in [`GLOSSARY.md`](GLOSSARY.md), and the design decisions are in [`docs/adr/`](docs/adr/).

## Requirements

- Java 25
- Spring Boot 4.2.0 (SpringFX is built against it)
- Maven. JavaFX 27.0.1 comes in with the `springfx` dependency, with the right native jars for your platform.

## 1. Add SpringFX to the build

SpringFX is published through [JitPack](https://jitpack.io/#brennydoogles/springfx) from git tags, as `com.github.brennydoogles.springfx:<module>:<tag>`. Add the JitPack repository. The compiler plugin resolves `annotationProcessorPaths` from these repositories too, so this one entry covers the processor:

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

Add the compile-time checks. From JDK 23, annotation processors on the class path aren't run automatically, so list `springfx-processor` under the compiler's `annotationProcessorPaths` ([ADR-0003](docs/adr/0003-controllers-declare-their-fxml-checked-at-compile-time.md)). This list replaces discovery, so list any other processors you use here too, such as Lombok or `spring-boot-configuration-processor`:

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
        <compilerArgs>
            <arg>-Aspringfx.resources=${project.basedir}/src/main/resources</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

Replace `v0.1.0` with the tag you want; the [JitPack page](https://jitpack.io/#brennydoogles/springfx) lists them. For a runnable fat jar, add `spring-boot-maven-plugin` as usual.

## 2. Write the application class

```java
@SpringBootApplication
public class MyApp extends SpringFXApplication {
    public static void main(String[] args) {
        launch(MyApp.class, args);
    }
}
```

**`SpringFXApplication`** is the base class of your SpringFX Application. `launch(MyApp.class, args)` does three things:

1. Starts Spring.
2. Shows the primary Controller's View in the Primary Window.
3. Returns when the app has exited.

Closing the Primary Window ends the app, even if other Windows are open, and closes the Spring context.

If startup fails (no primary Controller, or a View that can't load), the cause is logged, the context is closed and the process exits with status 1.

`SpringFXApplication` deliberately doesn't extend JavaFX `Application`, so `MyApp` works as the main class of a fat jar with `java -jar` ([ADR-0002](docs/adr/0002-springfx-application-does-not-extend-javafx-application.md)).

## 3. Write Controllers and their FXML

```java
@SpringFXController(value = "/fxml/main.fxml", viewName = "MAIN", primary = true)
public class MainController {
    private final GreetingService greetings;

    @FXML
    private Label greeting;

    public MainController(GreetingService greetings) {
        this.greetings = greetings;
    }

    @FXML
    private void initialize() {
        greeting.setText(greetings.hello());
    }
}
```

```xml
<!-- src/main/resources/fxml/main.fxml -->
<?import javafx.scene.control.Label?>
<?import javafx.scene.layout.VBox?>
<VBox xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.example.MainController">
    <Label fx:id="greeting"/>
</VBox>
```

**`@SpringFXController`** marks a Controller: the Spring bean behind a View, which names its View's FXML.

| Attribute | Default | Meaning |
|---|---|---|
| `value` (or `fxml`) | required | The View's FXML, as a class-path resource. A path starting with `/` is absolute; any other path is relative to the Controller's package. |
| `viewName` | required | The View's View Name, such as `LOGIN_PAGE`: `UPPER_SNAKE_CASE` and unique within the module. It becomes a constant of the generated `Views` enum. |
| `primary` | `false` | Whether this is the Primary View. Exactly one Controller in an app is primary. |
| `title` | `""` | The title of a Window showing this View. Empty means `springfx.window.title`. |

Things to know about Controllers:

- **A fresh Controller every time.** Controllers are prototype-scoped beans (the annotation is meta-annotated `@Component`), so each time a View is shown it gets a new one, created by Spring with constructor injection.
- **`fx:controller` is optional.** Keep it so SceneBuilder can suggest `fx:id`s and handlers, but if the FXML declares it, it must name the annotated class. For a nested class that's the binary name, `Outer$Inner`. Either way, SpringFX binds the Spring-created Controller.
- **Nested Views are Spring-managed too.** The Controllers of nested `<fx:include>`s come from Spring: the bean of that type if there is one, else a new instance with its dependencies injected.

## 4. Configure the Windows

Window Settings come from `springfx.window.*` and work with no configuration at all. Inject **`WindowSettings`** (a `@ConfigurationProperties` record) if a Controller needs to read them.

| Property | Default | Applies to | Meaning |
|---|---|---|---|
| `springfx.window.title` | `spring.application.name`, else `Application` | every Window, as the fallback title | The Window title |
| `springfx.window.width` | `900` | Primary Window | Starting width of its content |
| `springfx.window.height` | `600` | Primary Window | Starting height of its content |
| `springfx.window.min-width` | `0` | Primary Window | Minimum width |
| `springfx.window.min-height` | `0` | Primary Window | Minimum height |
| `springfx.window.resizable` | `true` | Primary Window | Whether it can be resized |
| `springfx.window.maximized` | `false` | Primary Window | Whether it starts maximised |
| `springfx.window.full-screen` | `false` | Primary Window | Whether it starts full screen |
| `springfx.window.stylesheets` | none | every Window | Stylesheets, as class-path resources (`/css/app.css`) or URLs |
| `springfx.window.icons` | none | every Window | Window icons, as class-path resources (`/icons/app.png`) or URLs |

```properties
spring.application.name=My App
springfx.window.width=1024
springfx.window.height=768
springfx.window.stylesheets=/css/app.css
springfx.window.icons=/icons/app-32.png,/icons/app-256.png
```

A Window's title is the first that is set of:

1. the `WindowOptions` title it was opened with
2. its Controller's `title`
3. `springfx.window.title`
4. `spring.application.name`
5. `Application`

## 5. Open more Windows with the Window Manager

**`WindowManager`** is a singleton bean: inject it into a Controller for everything about Windows that happens outside FXML.

```java
@SpringFXController(value = "/fxml/settings.fxml", viewName = "SETTINGS", title = "Settings")
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
windows.open(Views.SETTINGS, WindowOptions.builder()
        .owner(windows.stageOf(this))
        .modality(Modality.WINDOW_MODAL)
        .width(480.0)
        .height(320.0)
        .build());
```

| Method | Does |
|---|---|
| `primaryStage()` | The Primary Window's stage |
| `open(view)`, `open(view, options)` | Shows the View with that View Name, such as `Views.SETTINGS`, in a new Window and returns its stage. Each call makes a fresh Controller. |
| `open(type)`, `open(type, options)` | The same, naming the View by its Controller class |
| `stageOf(controller)` | The Window a Controller's View is in, including the Controllers of nested `<fx:include>`s |
| `close(controller)` | Closes that Controller's Window. Closing the Primary Window ends the app. |

**`WindowOptions`** says how `open` opens a Window. Build one with `WindowOptions.builder()`, setting only what you need. `toBuilder()` starts from existing options, and `WindowOptions.defaults()` is what `open(type)` uses.

| Option | Default | Meaning |
|---|---|---|
| `owner` | none | The Window that owns the new one |
| `modality` | `Modality.NONE` | The new Window's modality |
| `title` | none | The new Window's title, which wins over the Controller's |
| `width`, `height` | the View's preferred size | The new Window's content size |

### Views

`springfx-processor` generates a `Views` enum with one constant per View Name, sorted alphabetically, so you can type `Views.` and pick a View ([ADR-0004](docs/adr/0004-view-names-are-a-generated-enum.md)):

```java
windows.open(Views.LOGIN_PAGE);
```

Each constant implements SpringFX's `ViewName` interface; `controllerType()` returns its Controller class.

- **Where it goes.** `Views` is generated in the package of the module's `SpringFXApplication` subclass. Set `-Aspringfx.viewsPackage=<package>` to put it elsewhere. A module with Controllers but no application class, or with application classes in different packages, must set the option.
- **Generated at compile time.** It's written to `target/generated-sources/annotations`, which IntelliJ and other IDEs treat as a source folder. A new or renamed View Name shows up in the IDE after the next build.
- **Needs the processor.** Without `springfx-processor` on `annotationProcessorPaths`, there is no `Views`.

Every `WindowManager` method can be called from any thread. Calls off the JavaFX application thread run on it; the methods that return a value wait for it, and `close` doesn't.

## 6. Let the compiler check your Controllers

With `springfx-processor` configured as in step 1, these are compile errors, reported on the Controller's `@SpringFXController`:

- the FXML file it names doesn't exist, or it names none
- its View Name isn't `UPPER_SNAKE_CASE`, or another Controller in the module has the same one
- `Views` couldn't refer to it, because it's package-private (or nested in a non-public class) in another package
- the FXML's `fx:controller` names a different class (leaving `fx:controller` out is fine)
- more than one Controller is `primary = true`
- the module has a `SpringFXApplication` subclass but no primary Controller

**Telling it where FXML lives.** `-Aspringfx.resources` gives the folder or folders that hold your FXML; separate several with the platform path separator. Without it, the processor reads FXML from the class output. That works in a Maven build, where resources are copied before compiling, but can miss files an IDE hasn't copied yet.

**Where `Views` goes.** `-Aspringfx.viewsPackage` sets the package of the generated `Views` (see [Views](#views)).

**What it reads.** Only each FXML file's root element.

**IDE incremental builds.** These recompile only the changed files. To keep the primary-Controller checks right there, the processor records the module's Controllers and application classes in `META-INF/springfx/module-index` in the class output. Each build then checks the primary Controller and writes the complete `Views` even when it recompiles only some files.

## Testing your app

JavaFX starts only inside `SpringFXApplication.launch`. A plain `@SpringBootTest` context, SpringFX's beans included, never starts the JavaFX toolkit, so your service and Controller tests run without a display.

## Building SpringFX

```bash
./mvnw verify
```

That build is headless: nothing in it starts JavaFX. Tests that open real Windows are tagged `display` and need a screen; add the `display` profile to run them too, in a JVM of their own:

```bash
./mvnw verify -Pdisplay
```

The build has two modules: `springfx`, the runtime library, and `springfx-processor`, its compile-time checks. Releases are git tags, which JitPack builds using `jitpack.yml`; see the [releases](https://github.com/brennydoogles/springfx/releases).

## Licence

Apache License 2.0. See [LICENSE](LICENSE).
