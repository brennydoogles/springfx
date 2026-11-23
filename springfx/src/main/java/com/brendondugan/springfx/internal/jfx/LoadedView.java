package com.brendondugan.springfx.internal.jfx;

import javafx.scene.Parent;

import java.util.List;

/**
 * A View loaded from its FXML.
 *
 * @param root the root node of the View
 * @param controller the Spring-created Controller the FXML was bound to
 * @param nestedControllers the Controllers of its nested {@code <fx:include>}s
 * @param title the Controller's own Window title, or empty
 */
record LoadedView(Parent root, Object controller, List<Object> nestedControllers, String title) {
}
