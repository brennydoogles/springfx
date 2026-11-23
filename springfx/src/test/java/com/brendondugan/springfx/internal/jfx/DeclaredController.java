package com.brendondugan.springfx.internal.jfx;

import com.brendondugan.springfx.SpringFXController;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

@SpringFXController(value = "/fxml/declared.fxml", viewName = "DECLARED")
class DeclaredController {

    final GreetingService greetings;

    @FXML
    VBox box;

    DeclaredController(GreetingService greetings) {
        this.greetings = greetings;
    }
}
