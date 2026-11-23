package com.brendondugan.springfx.internal.config;

import com.brendondugan.springfx.WindowSettings;
import com.brendondugan.springfx.internal.jfx.FxApplication;
import com.brendondugan.springfx.internal.jfx.FxWindowManager;
import com.brendondugan.springfx.internal.jfx.ViewLoader;
import com.brendondugan.springfx.internal.spring.ControllerRegistry;
import javafx.application.Platform;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Registers SpringFX's beans. Nothing here starts the JavaFX toolkit, so a plain
 * {@code @SpringBootTest} context stays headless; only {@link FxApplication} does.
 */
@AutoConfiguration
@EnableConfigurationProperties({WindowSettings.class, ApplicationNameProperties.class})
public class SpringFXAutoConfiguration {

    @Bean
    ControllerRegistry springFXControllerRegistry(ApplicationContext context) {
        return new ControllerRegistry(context);
    }

    @Bean
    ViewLoader springFXViewLoader(ApplicationContext context) {
        return new ViewLoader(context);
    }

    @Bean
    WindowSettingsResolver springFXWindowSettingsResolver(WindowSettings settings,
            ApplicationNameProperties application) {
        return new WindowSettingsResolver(settings, application);
    }

    @Bean
    FxWindowManager springFXWindowManager(ViewLoader viewLoader, WindowSettingsResolver resolver) {
        return new FxWindowManager(viewLoader, resolver, Platform::exit);
    }
}
