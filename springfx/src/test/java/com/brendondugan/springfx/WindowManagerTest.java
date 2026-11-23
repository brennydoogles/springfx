package com.brendondugan.springfx;

import javafx.stage.Modality;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WindowManagerTest {

    enum TestViews implements ViewName {
        SETTINGS;

        @Override
        public Class<?> controllerType() {
            return String.class;
        }
    }

    private final List<Object> opened = new ArrayList<>();

    /** Records what the open(Class, WindowOptions) it delegates to was asked for. Creates no Stage. */
    private final WindowManager windows = new WindowManager() {
        @Override
        public Stage primaryStage() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Stage open(Class<?> controllerType, WindowOptions options) {
            opened.add(controllerType);
            opened.add(options);
            return null;
        }

        @Override
        public Stage stageOf(Object controller) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close(Object controller) {
            throw new UnsupportedOperationException();
        }
    };

    @Test
    void openingAViewNameOpensItsControllersView() {
        windows.open(TestViews.SETTINGS);

        assertThat(opened).containsExactly(String.class, WindowOptions.defaults());
    }

    @Test
    void openingAViewNameWithOptionsPassesThemOn() {
        WindowOptions options = WindowOptions.builder().modality(Modality.WINDOW_MODAL).build();

        windows.open(TestViews.SETTINGS, options);

        assertThat(opened).containsExactly(String.class, options);
    }
}
