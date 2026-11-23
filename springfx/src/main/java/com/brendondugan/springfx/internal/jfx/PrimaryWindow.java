package com.brendondugan.springfx.internal.jfx;

import com.brendondugan.springfx.WindowSettings;
import com.brendondugan.springfx.internal.config.ApplicationNameProperties;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.springframework.util.ClassUtils;

import java.net.URL;
import java.util.List;

/** Shows the Primary View in the Primary Window, applying the Window Settings. */
public class PrimaryWindow {

    private static final String DEFAULT_TITLE = "Application";

    private final ViewLoader viewLoader;
    private final WindowSettings settings;
    private final ApplicationNameProperties application;

    public PrimaryWindow(ViewLoader viewLoader, WindowSettings settings, ApplicationNameProperties application) {
        this.viewLoader = viewLoader;
        this.settings = settings;
        this.application = application;
    }

    /**
     * Loads the Primary View into the stage behind the Primary Window and shows it. Closing the
     * Primary Window ends the application, even if other Windows are open. Call on the JavaFX
     * application thread.
     */
    void show(Stage stage, Class<?> primaryController) {
        LoadedView view = viewLoader.load(primaryController);
        Scene scene = new Scene(view.root(), settings.width(), settings.height());
        scene.getStylesheets().addAll(stylesheets());
        stage.setScene(scene);
        stage.setTitle(title(view.title()));
        stage.setMinWidth(settings.minWidth());
        stage.setMinHeight(settings.minHeight());
        stage.setResizable(settings.resizable());
        icons().forEach(icon -> stage.getIcons().add(new Image(icon)));
        stage.setMaximized(settings.maximized());
        stage.setFullScreen(settings.fullScreen());
        stage.setOnHidden(event -> Platform.exit());
        stage.show();
    }

    /** The Window title: the Controller's own, else {@code springfx.window.title}, else the app name. */
    String title(String controllerTitle) {
        if (!controllerTitle.isBlank()) {
            return controllerTitle;
        }
        if (settings.title() != null && !settings.title().isBlank()) {
            return settings.title();
        }
        if (application.name() != null && !application.name().isBlank()) {
            return application.name();
        }
        return DEFAULT_TITLE;
    }

    /** The configured stylesheets as URLs JavaFX can load. */
    List<String> stylesheets() {
        return settings.stylesheets().stream().map(path -> resolve(path, "stylesheets")).toList();
    }

    /** The configured icons as URLs JavaFX can load. */
    List<String> icons() {
        return settings.icons().stream().map(path -> resolve(path, "icons")).toList();
    }

    private static String resolve(String location, String setting) {
        if (location.contains(":")) {
            return location;
        }
        String resource = location.startsWith("/") ? location.substring(1) : location;
        URL url = ClassUtils.getDefaultClassLoader().getResource(resource);
        if (url == null) {
            throw new IllegalStateException("springfx.window." + setting + " entry " + location
                    + " was not found on the class path");
        }
        return url.toExternalForm();
    }
}
