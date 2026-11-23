package com.brendondugan.springfx.internal;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * The hidden JavaFX application behind {@code SpringFXApplication.launch} (ADR-0002). It starts
 * Spring in {@link #init()}, shows the Primary View in {@link #start(Stage)} and closes the
 * context in {@link #stop()}. Not API: JavaFX needs it public to create it.
 */
public final class FxApplication extends Application {

    private static final Log log = LogFactory.getLog(FxApplication.class);

    private static volatile Class<?> applicationClass;
    private static volatile boolean failed;

    private ConfigurableApplicationContext context;
    private Class<?> primaryController;

    /**
     * Runs the application and returns when it has exited.
     *
     * @return false if startup failed
     */
    public static boolean run(Class<?> application, String[] args) {
        applicationClass = application;
        failed = false;
        Application.launch(FxApplication.class, args);
        return !failed;
    }

    @Override
    public void init() {
        try {
            context = new SpringApplicationBuilder(applicationClass)
                    .headless(false)
                    .run(getParameters().getRaw().toArray(String[]::new));
            primaryController = context.getBean(ControllerRegistry.class).primaryController();
        } catch (RuntimeException e) {
            // SpringApplication logs its own run failures; log only what happened after it.
            fail(context == null ? null : e);
        }
    }

    @Override
    public void start(Stage stage) {
        if (failed) {
            Platform.exit();
            return;
        }
        try {
            context.getBean(PrimaryWindow.class).show(stage, primaryController);
        } catch (RuntimeException e) {
            fail(e);
            Platform.exit();
        }
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
    }

    private void fail(RuntimeException cause) {
        failed = true;
        if (cause != null) {
            log.error("SpringFX could not show the Primary View", cause);
        }
        if (context != null) {
            context.close();
        }
    }
}
