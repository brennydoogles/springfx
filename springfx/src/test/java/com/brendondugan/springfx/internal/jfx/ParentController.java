package com.brendondugan.springfx.internal.jfx;

import com.brendondugan.springfx.SpringFXController;
import javafx.fxml.FXML;

@SpringFXController(value = "/fxml/parent.fxml", title = "Parent title")
class ParentController {

    @FXML
    ChildController childController;

    @FXML
    UnregisteredChildController otherController;
}
