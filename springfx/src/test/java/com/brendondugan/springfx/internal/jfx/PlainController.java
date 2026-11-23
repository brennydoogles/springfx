package com.brendondugan.springfx.internal.jfx;

import com.brendondugan.springfx.SpringFXController;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

@SpringFXController(value = "/fxml/plain.fxml", primary = true)
class PlainController {

    final GreetingService greetings;

    @FXML
    VBox box;

    PlainController(GreetingService greetings) {
        this.greetings = greetings;
    }
}
