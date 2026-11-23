package com.brendondugan.springfx;

import javafx.stage.Stage;

/**
 * The Window Manager: the one place a Controller goes to work with Windows outside its FXML.
 * Inject it into a Controller to reach the Primary Window, find the Controller's own Window, open
 * a View in a new Window, or close one.
 *
 * <pre>{@code
 * @SpringFXController(value = "/fxml/main.fxml", viewName = "MAIN", primary = true)
 * public class MainController {
 *     private final WindowManager windows;
 *
 *     public MainController(WindowManager windows) {
 *         this.windows = windows;
 *     }
 *
 *     @FXML
 *     private void onSettings() {
 *         windows.open(Views.SETTINGS, WindowOptions.builder()
 *                 .owner(windows.stageOf(this))
 *                 .modality(Modality.WINDOW_MODAL)
 *                 .build());
 *     }
 * }
 * }</pre>
 *
 * <p>Every method can be called from any thread. A call made off the JavaFX application thread is
 * run on it; methods that return a value wait for it, and {@link #close} does not.
 */
public interface WindowManager {

    /**
     * The stage behind the Primary Window.
     *
     * @throws IllegalStateException if the Primary Window isn't shown yet
     */
    Stage primaryStage();

    /** Shows the Controller's View in a new Window with default options, and returns its stage. */
    default Stage open(Class<?> controllerType) {
        return open(controllerType, WindowOptions.defaults());
    }

    /**
     * Shows the Controller's View in a new Window and returns its stage. Each call makes a fresh
     * Controller. The title is the options' title, else the Controller's
     * {@link SpringFXController#title()}, else the {@code springfx.window.title} setting. The size
     * is the options' size, else the View's preferred size; the Window Settings' size and state
     * apply only to the Primary Window. Window Settings stylesheets and icons apply.
     */
    Stage open(Class<?> controllerType, WindowOptions options);

    /**
     * Shows the named View in a new Window with default options, and returns its stage. The
     * {@code Views} enum that {@code springfx-processor} generates supplies View Names.
     */
    default Stage open(ViewName view) {
        return open(view.controllerType());
    }

    /** Shows the named View in a new Window, as {@link #open(Class, WindowOptions)} does. */
    default Stage open(ViewName view, WindowOptions options) {
        return open(view.controllerType(), options);
    }

    /**
     * The stage of the Window showing the given Controller's View. Works for the Controllers of
     * nested {@code <fx:include>}s too.
     *
     * @throws IllegalArgumentException if the Controller's View isn't in an open Window
     */
    Stage stageOf(Object controller);

    /**
     * Closes the Window showing the given Controller's View. Closing the Primary Window ends the
     * application. Off the JavaFX application thread this returns without waiting, so a
     * Controller with no open Window is reported on that thread instead of thrown.
     *
     * @throws IllegalArgumentException if called on the JavaFX application thread and the
     *         Controller's View isn't in an open Window
     */
    void close(Object controller);
}
