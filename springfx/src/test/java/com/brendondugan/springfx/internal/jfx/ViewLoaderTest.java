package com.brendondugan.springfx.internal.jfx;

import com.brendondugan.springfx.internal.config.SpringFXAutoConfiguration;
import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ViewLoaderTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringFXAutoConfiguration.class))
            .withBean(GreetingService.class)
            .withBean(PlainController.class)
            .withBean(DeclaredController.class)
            .withBean(MismatchedController.class)
            .withBean(MissingFxmlController.class)
            .withBean(ParentController.class)
            .withBean(ChildController.class);

    @Test
    void bindsFxmlWithoutFxControllerToTheSpringCreatedController() {
        runner.run(context -> {
            LoadedView view = context.getBean(ViewLoader.class).load(PlainController.class);

            PlainController controller = (PlainController) view.controller();
            assertThat(controller.greetings).isSameAs(context.getBean(GreetingService.class));
            assertThat(controller.box).isSameAs(view.root());
        });
    }

    @Test
    void bindsFxmlWhoseFxControllerNamesTheControllerToTheSpringCreatedController() {
        runner.run(context -> {
            LoadedView view = context.getBean(ViewLoader.class).load(DeclaredController.class);

            DeclaredController controller = (DeclaredController) view.controller();
            assertThat(controller.greetings).isSameAs(context.getBean(GreetingService.class));
            assertThat(controller.box).isSameAs(view.root());
        });
    }

    @Test
    void makesAFreshControllerEachTimeAViewIsLoaded() {
        runner.run(context -> {
            ViewLoader loader = context.getBean(ViewLoader.class);

            assertThat(loader.load(PlainController.class).controller())
                    .isNotSameAs(loader.load(PlainController.class).controller());
        });
    }

    @Test
    void makesNestedIncludeControllersSpringBeans() {
        runner.run(context -> {
            ParentController parent = (ParentController) context.getBean(ViewLoader.class)
                    .load(ParentController.class).controller();

            GreetingService greetings = context.getBean(GreetingService.class);
            assertThat(parent.childController.greetings).isSameAs(greetings);
            assertThat(parent.otherController.greetings).isSameAs(greetings);
        });
    }

    @Test
    void reportsTheNestedControllers() {
        runner.run(context -> {
            LoadedView view = context.getBean(ViewLoader.class).load(ParentController.class);
            ParentController parent = (ParentController) view.controller();

            assertThat(view.nestedControllers()).containsExactly(parent.childController, parent.otherController);
        });
    }

    @Test
    void carriesTheControllersTitle() {
        runner.run(context -> assertThat(context.getBean(ViewLoader.class).load(ParentController.class).title())
                .isEqualTo("Parent title"));
    }

    @Test
    void rejectsAnFxControllerNamingAnotherClassNamingBothClasses() {
        runner.run(context -> assertThatIllegalStateException()
                .isThrownBy(() -> context.getBean(ViewLoader.class).load(MismatchedController.class))
                .withMessageContaining(PlainController.class.getName())
                .withMessageContaining(MismatchedController.class.getName()));
    }

    @Test
    void reportsMissingFxml() {
        runner.run(context -> assertThatIllegalStateException()
                .isThrownBy(() -> context.getBean(ViewLoader.class).load(MissingFxmlController.class))
                .withMessageContaining("/fxml/missing.fxml")
                .withMessageContaining(MissingFxmlController.class.getName()));
    }

    @Test
    void loadingAViewDoesNotStartTheJavaFxToolkit() {
        runner.run(context -> {
            context.getBean(ViewLoader.class).load(PlainController.class);

            assertThatIllegalStateException().isThrownBy(() -> Platform.runLater(() -> { }))
                    .withMessageContaining("Toolkit not initialized");
        });
    }

    @Test
    void rejectsAClassThatIsNotAController() {
        runner.run(context -> assertThatIllegalStateException()
                .isThrownBy(() -> context.getBean(ViewLoader.class).load(ChildController.class))
                .withMessageContaining("@SpringFXController"));
    }
}
