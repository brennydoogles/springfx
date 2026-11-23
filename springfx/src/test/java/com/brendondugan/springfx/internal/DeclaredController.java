package com.brendondugan.springfx.internal;

import com.brendondugan.springfx.SpringFXController;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

@SpringFXController("/fxml/declared.fxml")
class DeclaredController {

    final GreetingService greetings;

    @FXML
    VBox box;

    DeclaredController(GreetingService greetings) {
        this.greetings = greetings;
    }
}
