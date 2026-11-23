package com.brendondugan.springfx.processor;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;

/**
 * A class annotated {@code @SpringFXController}, as the compiler sees it.
 *
 * @param type the Controller class
 * @param annotation the {@code @SpringFXController} on it, where errors are reported
 * @param fxml the FXML path it names, possibly blank
 * @param primary whether it is the primary Controller
 */
record ControllerDeclaration(TypeElement type, AnnotationMirror annotation, String fxml, boolean primary) {

    String name() {
        return type.getQualifiedName().toString();
    }
}
