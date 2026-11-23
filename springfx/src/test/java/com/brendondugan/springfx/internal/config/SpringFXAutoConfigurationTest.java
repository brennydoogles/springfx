package com.brendondugan.springfx.internal.config;

import com.brendondugan.springfx.WindowSettings;
import com.brendondugan.springfx.WindowManager;
import com.brendondugan.springfx.internal.jfx.FxWindowManager;
import com.brendondugan.springfx.internal.jfx.ViewLoader;
import com.brendondugan.springfx.internal.spring.ControllerRegistry;
import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class SpringFXAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringFXAutoConfiguration.class));

    @Test
    void windowSettingsWorkWithNoConfiguration() {
        runner.run(context -> {
            WindowSettings settings = context.getBean(WindowSettings.class);

            assertThat(settings.title()).isNull();
            assertThat(settings.width()).isEqualTo(900);
            assertThat(settings.height()).isEqualTo(600);
            assertThat(settings.minWidth()).isZero();
            assertThat(settings.minHeight()).isZero();
            assertThat(settings.resizable()).isTrue();
            assertThat(settings.maximized()).isFalse();
            assertThat(settings.fullScreen()).isFalse();
            assertThat(settings.stylesheets()).isEmpty();
            assertThat(settings.icons()).isEmpty();
        });
    }

    @Test
    void windowSettingsBindFromSpringfxWindowProperties() {
        runner.withPropertyValues(
                        "springfx.window.title=My App",
                        "springfx.window.width=1024",
                        "springfx.window.height=768",
                        "springfx.window.min-width=640",
                        "springfx.window.min-height=480",
                        "springfx.window.resizable=false",
                        "springfx.window.maximized=true",
                        "springfx.window.full-screen=true",
                        "springfx.window.stylesheets=/css/app.css,/css/dark.css",
                        "springfx.window.icons[0]=/icons/app.png")
                .run(context -> assertThat(context.getBean(WindowSettings.class)).isEqualTo(new WindowSettings(
                        "My App", 1024, 768, 640, 480, false, true, true,
                        java.util.List.of("/css/app.css", "/css/dark.css"), java.util.List.of("/icons/app.png"))));
    }

    @Test
    void registersSpringFXsBeans() {
        runner.run(context -> assertThat(context)
                .hasSingleBean(ControllerRegistry.class)
                .hasSingleBean(ViewLoader.class)
                .hasSingleBean(WindowSettingsResolver.class)
                .hasSingleBean(FxWindowManager.class)
                .hasSingleBean(WindowManager.class));
    }

    @Test
    void creatingTheContextDoesNotStartTheJavaFxToolkit() {
        runner.run(context -> assertThatIllegalStateException().isThrownBy(() -> Platform.runLater(() -> { }))
                .withMessageContaining("Toolkit not initialized"));
    }
}
