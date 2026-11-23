package com.brendondugan.springfx.internal.config;

import com.brendondugan.springfx.WindowSettings;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class WindowSettingsResolverTest {

    @Test
    void titleIsTheRequestedTitleFirst() {
        assertThat(resolver("Settings title", "app-name").title("Requested", "Controller title"))
                .isEqualTo("Requested");
    }

    @Test
    void titleFallsBackToTheControllersOwn() {
        assertThat(resolver("Settings title", "app-name").title(null, "Controller title"))
                .isEqualTo("Controller title");
    }

    @Test
    void titleFallsBackToTheWindowSettings() {
        assertThat(resolver("Settings title", "app-name").title(null, "")).isEqualTo("Settings title");
    }

    @Test
    void titleFallsBackToTheApplicationName() {
        assertThat(resolver(null, "app-name").title(null, "")).isEqualTo("app-name");
    }

    @Test
    void titleFallsBackToApplication() {
        assertThat(resolver(null, null).title(null, "")).isEqualTo("Application");
    }

    @Test
    void blankTitlesDoNotCount() {
        assertThat(resolver(" ", "app-name").title(" ", " ")).isEqualTo("app-name");
    }

    @Test
    void resolvesClassPathStylesheetsAndKeepsUrls() {
        WindowSettingsResolver resolver = styledResolver(List.of("/css/app.css", "https://example.com/theme.css"), List.of());

        assertThat(resolver.stylesheets()).hasSize(2);
        assertThat(resolver.stylesheets().getFirst()).startsWith("file:").endsWith("/css/app.css");
        assertThat(resolver.stylesheets().get(1)).isEqualTo("https://example.com/theme.css");
    }

    @Test
    void resolvesClassPathIconsWithOrWithoutALeadingSlash() {
        WindowSettingsResolver resolver = styledResolver(List.of(), List.of("/icons/app.png", "icons/app.png"));

        assertThat(resolver.icons()).hasSize(2).allSatisfy(icon -> assertThat(icon).endsWith("/icons/app.png"));
    }

    @Test
    void reportsAMissingStylesheet() {
        assertThatIllegalStateException().isThrownBy(styledResolver(List.of("/css/missing.css"), List.of())::stylesheets)
                .withMessageContaining("springfx.window.stylesheets")
                .withMessageContaining("/css/missing.css");
    }

    @Test
    void reportsAMissingIcon() {
        assertThatIllegalStateException().isThrownBy(styledResolver(List.of(), List.of("/icons/missing.png"))::icons)
                .withMessageContaining("springfx.window.icons")
                .withMessageContaining("/icons/missing.png");
    }

    private static WindowSettingsResolver resolver(String settingsTitle, String applicationName) {
        return new WindowSettingsResolver(settings(settingsTitle, List.of(), List.of()),
                new ApplicationNameProperties(applicationName));
    }

    private static WindowSettingsResolver styledResolver(List<String> stylesheets, List<String> icons) {
        return new WindowSettingsResolver(settings(null, stylesheets, icons), new ApplicationNameProperties(null));
    }

    private static WindowSettings settings(String title, List<String> stylesheets, List<String> icons) {
        return new WindowSettings(title, 900, 600, 0, 0, true, false, false, stylesheets, icons);
    }
}
