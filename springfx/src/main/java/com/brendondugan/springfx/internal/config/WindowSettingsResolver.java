package com.brendondugan.springfx.internal.config;

import com.brendondugan.springfx.WindowSettings;
import org.springframework.util.ClassUtils;

import java.net.URL;
import java.util.List;

/** Resolves the Window Settings into the title, stylesheets and icons a Window gets. */
public class WindowSettingsResolver {

    private static final String DEFAULT_TITLE = "Application";

    private final WindowSettings settings;
    private final ApplicationNameProperties application;

    public WindowSettingsResolver(WindowSettings settings, ApplicationNameProperties application) {
        this.settings = settings;
        this.application = application;
    }

    public WindowSettings settings() {
        return settings;
    }

    /**
     * A Window's title: the first that is set of the title asked for when opening it (null for
     * none), the Controller's own, {@code springfx.window.title}, {@code spring.application.name},
     * and finally "Application".
     */
    public String title(String requestedTitle, String controllerTitle) {
        for (String title : new String[] {requestedTitle, controllerTitle, settings.title(), application.name()}) {
            if (title != null && !title.isBlank()) {
                return title;
            }
        }
        return DEFAULT_TITLE;
    }

    /** The configured stylesheets, for every Window, as URLs JavaFX can load. */
    public List<String> stylesheets() {
        return settings.stylesheets().stream().map(path -> resolve(path, "stylesheets")).toList();
    }

    /** The configured icons, for every Window, as URLs JavaFX can load. */
    public List<String> icons() {
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
