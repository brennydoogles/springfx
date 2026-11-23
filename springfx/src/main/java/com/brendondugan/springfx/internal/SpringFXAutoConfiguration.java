package com.brendondugan.springfx.internal;

import com.brendondugan.springfx.WindowSettings;
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
    PrimaryWindow springFXPrimaryWindow(ViewLoader viewLoader, WindowSettings settings,
            ApplicationNameProperties application) {
        return new PrimaryWindow(viewLoader, settings, application);
    }
}
