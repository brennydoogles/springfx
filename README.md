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

Artifacts will be published through [JitPack](https://jitpack.io/#brennydoogles/springfx) from release tags.

## Licence

Apache License 2.0. See [LICENSE](LICENSE).
