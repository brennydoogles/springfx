package com.brendondugan.springfx.internal.spring;

import com.brendondugan.springfx.SpringFXController;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.List;

/** Finds the app's Controllers from their bean definitions, without creating any of them. */
public class ControllerRegistry {

    private final ListableBeanFactory beans;

    public ControllerRegistry(ListableBeanFactory beans) {
        this.beans = beans;
    }

    /**
     * The Controller of the Primary View.
     *
     * @throws IllegalStateException unless exactly one Controller is marked primary
     */
    public Class<?> primaryController() {
        List<Class<?>> primaries = Arrays.stream(beans.getBeanNamesForAnnotation(SpringFXController.class))
                .filter(name -> beans.findAnnotationOnBean(name, SpringFXController.class).primary())
                .<Class<?>>map(name -> ClassUtils.getUserClass(beans.getType(name)))
                .toList();
        if (primaries.isEmpty()) {
            throw new IllegalStateException("No primary Controller: mark exactly one @SpringFXController"
                    + " with primary = true to choose the Primary View");
        }
        if (primaries.size() > 1) {
            throw new IllegalStateException("More than one primary Controller: "
                    + primaries.stream().map(Class::getName).toList()
                    + "; mark exactly one @SpringFXController with primary = true");
        }
        return primaries.getFirst();
    }
}
