package com.brendondugan.springfx;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Controller: the Spring-managed object behind a View. The Controller names its View's
 * FXML file, and a fresh Controller is made each time its View is shown.
 *
 * <p>The FXML may declare {@code fx:controller} so SceneBuilder can help with fx:id and handler
 * names, but if it does it must name the annotated class (ADR-0003).
 *
 * <pre>{@code
 * @SpringFXController(value = "/fxml/main.fxml", primary = true)
 * public class MainController { ... }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public @interface SpringFXController {

    /**
     * The View's FXML file, as a class-path resource. A path starting with {@code /} is absolute;
     * any other path is relative to the Controller's package. Required: set this or {@link #fxml()}.
     */
    @AliasFor("fxml")
    String value() default "";

    /**
     * Alias for {@link #value()}. Declaring the alias also stops Spring from taking the FXML path
     * as the Controller's bean name, which it otherwise does by convention for a {@code value}
     * attribute on an annotation meta-annotated {@code @Component}.
     */
    @AliasFor("value")
    String fxml() default "";

    /** Whether this Controller's View is the Primary View. Exactly one Controller is primary. */
    boolean primary() default false;

    /**
     * The title of a Window showing this View. Empty means the {@code springfx.window.title}
     * setting.
     */
    String title() default "";
}
