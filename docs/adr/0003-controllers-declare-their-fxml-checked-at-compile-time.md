# Controllers declare their FXML, and the pairing is checked at compile time

A Controller names its View's FXML file with `@SpringFXController("/fxml/main.fxml")`, the way a `@RestController` owns its routes, rather than FXML being loaded by path from elsewhere. FXML files may still declare `fx:controller` so SceneBuilder keeps its controller-aware help (fx:id and handler suggestions), but when present it must name the annotated class; SpringFX supplies the Spring-created Controller through a controller factory, since `FXMLLoader` rejects an explicitly set controller when `fx:controller` is present. The `springfx-processor` annotation processor fails compilation if a Controller's FXML is missing, its `fx:controller` names another class, more than one Controller is marked primary, or a module with a `SpringFXApplication` has none. It reads FXML from the source resources folder (processor option `-Aspringfx.resources`, falling back to the class output), so IDE builds that haven't copied resources yet still get the right answer. At runtime the loader also confirms it used the Spring-created Controller, as a safety net for FXML the processor can't see.

## Considered Options

- **Removing `fx:controller` entirely**: one source of truth, but SceneBuilder loses its controller-aware features.
- **Checking at startup or in a test**: no processor module needed, but errors surface after compilation.

## Consequences

- From JDK 23 annotation processors aren't discovered automatically, so consumers list `springfx-processor` under the compiler's `annotationProcessorPaths`.
