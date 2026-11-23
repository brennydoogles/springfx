package com.brendondugan.springfx.internal;

import javafx.scene.Parent;

/**
 * A View loaded from its FXML.
 *
 * @param root the root node of the View
 * @param controller the Spring-created Controller the FXML was bound to
 * @param title the Controller's own Window title, or empty
 */
record LoadedView(Parent root, Object controller, String title) {
}
