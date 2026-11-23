package com.brendondugan.springfx;

import com.brendondugan.springfx.internal.jfx.FxApplication;

/**
 * Base class for the SpringFX Application: the consuming app's single entry-point class.
 *
 * <pre>{@code
 * @SpringBootApplication
 * public class MyApp extends SpringFXApplication {
 *     public static void main(String[] args) {
 *         launch(MyApp.class, args);
 *     }
 * }
 * }</pre>
 *
 * <p>This class deliberately does not extend {@code javafx.application.Application}: when the main
 * class of a fat jar does, {@code java -jar} fails with "JavaFX runtime components are missing"
 * (ADR-0002). {@link #launch} starts a hidden JavaFX application instead.
 */
public abstract class SpringFXApplication {

    /**
     * Starts Spring and JavaFX together and shows the Primary View in the Primary Window. Returns
     * when the application has exited. If startup fails, the cause is logged, the Spring context
     * is closed and the process exits with status 1.
     *
     * @param applicationClass the {@code @SpringBootApplication} class that extends this one
     * @param args the command-line arguments, passed on to Spring and JavaFX
     */
    public static void launch(Class<? extends SpringFXApplication> applicationClass, String... args) {
        if (!FxApplication.run(applicationClass, args)) {
            System.exit(1);
        }
    }
}
