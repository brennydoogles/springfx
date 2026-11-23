package com.brendondugan.springfx.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ControllerRegistryTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringFXAutoConfiguration.class))
            .withBean(GreetingService.class);

    @Test
    void findsThePrimaryController() {
        runner.withBean(PlainController.class).withBean(DeclaredController.class)
                .run(context -> assertThat(context.getBean(ControllerRegistry.class).primaryController())
                        .isEqualTo(PlainController.class));
    }

    @Test
    void failsWithoutAPrimaryController() {
        runner.withBean(DeclaredController.class)
                .run(context -> assertThatIllegalStateException()
                        .isThrownBy(() -> context.getBean(ControllerRegistry.class).primaryController())
                        .withMessageContaining("No primary Controller"));
    }

    @Test
    void failsWithMoreThanOnePrimaryControllerNamingThem() {
        runner.withBean(PlainController.class).withBean(SecondPrimaryController.class)
                .run(context -> assertThatIllegalStateException()
                        .isThrownBy(() -> context.getBean(ControllerRegistry.class).primaryController())
                        .withMessageContaining(PlainController.class.getName())
                        .withMessageContaining(SecondPrimaryController.class.getName()));
    }

    @Test
    void doesNotCreateControllersToFindThePrimary() {
        runner.withBean(PlainController.class).withBean(MissingFxmlController.class)
                .run(context -> {
                    context.getBean(ControllerRegistry.class).primaryController();

                    assertThat(context.getBeanFactory().getSingletonNames())
                            .doesNotContain(context.getBeanNamesForType(PlainController.class));
                });
    }

    @Test
    void controllersArePrototypes() {
        runner.withBean(PlainController.class)
                .run(context -> assertThat(context.getBean(PlainController.class))
                        .isNotSameAs(context.getBean(PlainController.class)));
    }

    @Test
    void theFxmlPathIsNotTheBeanName() {
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

        String name = new AnnotationBeanNameGenerator()
                .generateBeanName(new AnnotatedGenericBeanDefinition(PlainController.class), registry);

        assertThat(name).isEqualTo("plainController");
    }
}
