# SpringFXApplication does not extend JavaFX Application

The consuming app's main class extends `SpringFXApplication` and calls `SpringFXApplication.launch(App.class, args)`. `SpringFXApplication` is deliberately a plain class, not a subclass of `javafx.application.Application`: when the class named in a fat jar's manifest extends `Application`, the JVM demands JavaFX on the module path and `java -jar` fails with "JavaFX runtime components are missing". Instead, `launch` starts a hidden internal JavaFX `Application` that starts Spring in `init()`, opens the Primary Window in `start()` and closes the context in `stop()`. This differs from `yoep/spring-boot-starter-javafx`, whose entry point extends `Application`; don't "simplify" it back.

## Consequences

- An app needs one class, not a launcher, an FX application and a stage initialiser.
- JavaFX only starts through `launch`, so a plain `@SpringBootTest` context stays headless.
