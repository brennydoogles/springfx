# SpringFX is a separate library, published from git tags via JitPack

SpringFX grew out of `cronometer-export-helper`, but wiring Spring Boot to JavaFX isn't specific to that app and we intend to publish it as a starter. So it is its own public project from day one, rather than a package or module inside the app: the boundary can't erode, and the app consumes it exactly as any other user would. It is distributed through JitPack, which builds artifacts from git tags (`com.github.brennydoogles.springfx:<module>:<tag>`), so a release is just a tag and no repository credentials or Maven Central setup are needed yet. The build is multi-module: `springfx` (the runtime library, package `com.brendondugan.springfx`, registered through `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`) and `springfx-processor` (compile-time checks, ADR-0003), because an annotation processor can't run in the module that compiles it.

## Considered Options

- **A package or module inside the app**: simpler to start, but every change to the boundary is one commit away and extraction becomes a project of its own.
- **Maven Central now**: the "real" home for a starter, but needs a verified namespace, signing and a release process before anything works.
- **Adopting `yoep/spring-boot-starter-javafx`**: outdated, and its entry point extends JavaFX `Application`, which breaks fat jars (ADR-0002).

## Consequences

- JitPack must be able to build with the project's JDK; `jitpack.yml` pins it.
- Consumers add the JitPack repository to their build, including for the annotation processor path.
