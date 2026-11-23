# View Names are a generated enum

Every `@SpringFXController` must declare a View Name (`viewName = "LOGIN_PAGE"`, `UPPER_SNAKE_CASE`, unique within the module). `springfx-processor` generates one `Views` enum per module from them, so code opens a View with `windows.open(Views.LOGIN_PAGE)` and finds every View by typing `Views.`.

Each constant holds its Controller's `Class` and implements SpringFX's `ViewName` interface, which the Window Manager accepts. `Views` goes in the package of the module's `SpringFXApplication`, or the package given by `-Aspringfx.viewsPackage`, which a module without one must set. The index the processor already keeps in the class output records every View Name, so an IDE build that recompiles only some files still writes the complete enum.

`viewName` is required rather than optional so every View is in the catalogue and every name is the developer's own choice. The enum supplements `open(Class)`; it doesn't replace it.

## Considered Options

- **An IntelliJ plugin (or a Lombok-style compiler hack) so new constants appear as you type**: rejected. A plain annotation processor works in every IDE and in Maven, and the cost is that a new constant shows up after a build, as with MapStruct or the JPA metamodel.
- **Constants holding the Controller's class name as a string, with a lookup at runtime**: rejected. With a `Class`, renaming or moving a Controller is a compile error, and a View can't be named by a typo that only fails at runtime. For the same reason there is no `open(String)`.
- **An optional `viewName`, or names derived from class names**: rejected. Optional leaves gaps in the catalogue, and derived names change silently when a class is renamed.
- **Exposing the FXML path, title or primary flag on each constant**: rejected. They already live on the annotation, and copies would drift.

## Consequences

- Adding `viewName` as a required attribute breaks every v0.1.0 Controller, so it ships in v0.2.0.
- `Views` exists only where `springfx-processor` runs, and View Names are checked only at compile time; nothing checks them at startup.
- A multi-module app has one `Views` per module, and uniqueness is checked per module, because a processor sees one module at a time.
- A package-private Controller that `Views` can't reach is a compile error on its annotation, not a confusing error inside the generated file.
