package com.brendondugan.springfx.internal.jfx;

import com.brendondugan.springfx.WindowManager;
import com.brendondugan.springfx.WindowOptions;
import com.brendondugan.springfx.WindowSettings;
import com.brendondugan.springfx.internal.config.WindowSettingsResolver;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The Window Manager. It shows the Primary View in the Primary Window and every other View in a
 * Window of its own, and remembers which Window each Controller's View is in until that Window
 * closes. Its state is only touched on the JavaFX application thread.
 */
public class FxWindowManager implements WindowManager {

    /** Content size meaning "the root's preferred size" to {@link Scene}. */
    private static final double PREFERRED = -1;

    private final ViewLoader viewLoader;
    private final WindowSettingsResolver resolver;
    private final FxThread fxThread;
    private final Runnable exit;
    private final Map<Object, Stage> stagesByController = new IdentityHashMap<>();
    private Stage primaryStage;

    /**
     * @param exit what to do when the Primary Window closes: end the application
     */
    public FxWindowManager(ViewLoader viewLoader, WindowSettingsResolver resolver, Runnable exit) {
        this(viewLoader, resolver, new FxThread(), exit);
    }

    FxWindowManager(ViewLoader viewLoader, WindowSettingsResolver resolver, FxThread fxThread, Runnable exit) {
        this.viewLoader = viewLoader;
        this.resolver = resolver;
        this.fxThread = fxThread;
        this.exit = exit;
    }

    /**
     * Loads the Primary View into the stage JavaFX made for the Primary Window, applies the Window
     * Settings and shows it. Closing it ends the application, even if other Windows are open.
     */
    void showPrimary(Stage stage, Class<?> primaryController) {
        fxThread.call(() -> {
            LoadedView view = viewLoader.load(primaryController);
            WindowSettings settings = resolver.settings();
            show(stage, view, new Scene(view.root(), settings.width(), settings.height()), null, () -> {
                stage.setMinWidth(settings.minWidth());
                stage.setMinHeight(settings.minHeight());
                stage.setResizable(settings.resizable());
                stage.setMaximized(settings.maximized());
                stage.setFullScreen(settings.fullScreen());
            });
            primaryStage = stage;
            stage.addEventHandler(WindowEvent.WINDOW_HIDDEN, event -> exit.run());
            return stage;
        });
    }

    @Override
    public Stage primaryStage() {
        return fxThread.call(() -> {
            if (primaryStage == null) {
                throw new IllegalStateException("The Primary Window isn't shown yet");
            }
            return primaryStage;
        });
    }

    @Override
    public Stage open(Class<?> controllerType, WindowOptions options) {
        Objects.requireNonNull(controllerType, "controllerType");
        Objects.requireNonNull(options, "options");
        return fxThread.call(() -> {
            LoadedView view = viewLoader.load(controllerType);
            Stage stage = new Stage();
            if (options.owner() != null) {
                stage.initOwner(options.owner());
            }
            stage.initModality(options.modality());
            Scene scene = new Scene(view.root(),
                    options.width() == null ? PREFERRED : options.width(),
                    options.height() == null ? PREFERRED : options.height());
            show(stage, view, scene, options.title(), () -> { });
            return stage;
        });
    }

    @Override
    public Stage stageOf(Object controller) {
        Objects.requireNonNull(controller, "controller");
        return fxThread.call(() -> openStageOf(controller));
    }

    @Override
    public void close(Object controller) {
        Objects.requireNonNull(controller, "controller");
        fxThread.run(() -> openStageOf(controller).close());
    }

    private Stage openStageOf(Object controller) {
        Stage stage = stagesByController.get(controller);
        if (stage == null) {
            throw new IllegalArgumentException("The View of " + controller.getClass().getName()
                    + " " + controller + " isn't in an open Window");
        }
        return stage;
    }

    /** Applies what every Window gets, records the View's Controllers, and shows the stage. */
    private void show(Stage stage, LoadedView view, Scene scene, String requestedTitle, Runnable beforeShow) {
        scene.getStylesheets().addAll(resolver.stylesheets());
        stage.setScene(scene);
        stage.setTitle(resolver.title(requestedTitle, view.title()));
        resolver.icons().forEach(icon -> stage.getIcons().add(new Image(icon)));
        beforeShow.run();

        stagesByController.put(view.controller(), stage);
        view.nestedControllers().forEach(nested -> stagesByController.put(nested, stage));
        stage.addEventHandler(WindowEvent.WINDOW_HIDDEN,
                event -> stagesByController.values().removeIf(shown -> shown == stage));
        stage.show();
    }
}
