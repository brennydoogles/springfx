package com.brendondugan.springfx.internal.jfx;

import com.brendondugan.springfx.WindowSettings;
import com.brendondugan.springfx.internal.config.ApplicationNameProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class PrimaryWindowTest {

    @Test
    void titleIsTheControllersOwnFirst() {
        assertThat(window("Settings title", "app-name").title("Controller title")).isEqualTo("Controller title");
    }

    @Test
    void titleFallsBackToTheWindowSettings() {
        assertThat(window("Settings title", "app-name").title("")).isEqualTo("Settings title");
    }

    @Test
    void titleFallsBackToTheApplicationName() {
        assertThat(window(null, "app-name").title("")).isEqualTo("app-name");
    }

    @Test
    void titleFallsBackToApplication() {
        assertThat(window(null, null).title("")).isEqualTo("Application");
    }

    @Test
    void resolvesClassPathStylesheetsAndKeepsUrls() {
        PrimaryWindow window = new PrimaryWindow(null,
                settings(null, List.of("/css/app.css", "https://example.com/theme.css"), List.of()),
                new ApplicationNameProperties(null));

        assertThat(window.stylesheets()).hasSize(2);
        assertThat(window.stylesheets().getFirst()).startsWith("file:").endsWith("/css/app.css");
        assertThat(window.stylesheets().get(1)).isEqualTo("https://example.com/theme.css");
    }

    @Test
    void reportsAMissingIcon() {
        PrimaryWindow window = new PrimaryWindow(null, settings(null, List.of(), List.of("/icons/missing.png")),
                new ApplicationNameProperties(null));

        assertThatIllegalStateException().isThrownBy(window::icons)
                .withMessageContaining("springfx.window.icons")
                .withMessageContaining("/icons/missing.png");
    }

    private static PrimaryWindow window(String settingsTitle, String applicationName) {
        return new PrimaryWindow(null, settings(settingsTitle, List.of(), List.of()),
                new ApplicationNameProperties(applicationName));
    }

    private static WindowSettings settings(String title, List<String> stylesheets, List<String> icons) {
        return new WindowSettings(title, 900, 600, 0, 0, true, false, false, stylesheets, icons);
    }
}
