package com.brendondugan.springfx;

/**
 * A View Name: the developer-chosen name code uses to refer to a View. {@code springfx-processor}
 * generates a {@code Views} enum implementing this from each Controller's
 * {@link SpringFXController#viewName()}, so a View can be opened with
 * {@code windows.open(Views.LOGIN_PAGE)} (ADR-0004).
 */
public interface ViewName {

    /** The Controller of the named View. */
    Class<?> controllerType();
}
