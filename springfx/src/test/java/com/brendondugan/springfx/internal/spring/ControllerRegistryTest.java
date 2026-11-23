package com.brendondugan.springfx.internal.spring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ControllerRegistryTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(ControllerRegistry.class);

    @Test
    void findsThePrimaryController() {
        runner.withBean(PrimaryController.class).withBean(OtherController.class)
                .run(context -> assertThat(context.getBean(ControllerRegistry.class).primaryController())
                        .isEqualTo(PrimaryController.class));
    }

    @Test
    void failsWithoutAPrimaryController() {
        runner.withBean(OtherController.class)
                .run(context -> assertThatIllegalStateException()
                        .isThrownBy(() -> context.getBean(ControllerRegistry.class).primaryController())
                        .withMessageContaining("No primary Controller"));
    }

    @Test
    void failsWithMoreThanOnePrimaryControllerNamingThem() {
        runner.withBean(PrimaryController.class).withBean(SecondPrimaryController.class)
                .run(context -> assertThatIllegalStateException()
                        .isThrownBy(() -> context.getBean(ControllerRegistry.class).primaryController())
                        .withMessageContaining(PrimaryController.class.getName())
                        .withMessageContaining(SecondPrimaryController.class.getName()));
    }

    @Test
    void doesNotCreateControllersToFindThePrimary() {
        runner.withBean(PrimaryController.class).withBean(ExplodingController.class)
                .run(context -> assertThat(context.getBean(ControllerRegistry.class).primaryController())
                        .isEqualTo(PrimaryController.class));
    }

    @Test
    void controllersArePrototypes() {
        runner.withBean(PrimaryController.class)
                .run(context -> assertThat(context.getBean(PrimaryController.class))
                        .isNotSameAs(context.getBean(PrimaryController.class)));
    }

    @Test
    void theFxmlPathIsNotTheBeanName() {
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

        String name = new AnnotationBeanNameGenerator()
                .generateBeanName(new AnnotatedGenericBeanDefinition(PrimaryController.class), registry);

        assertThat(name).isEqualTo("primaryController");
    }
}
