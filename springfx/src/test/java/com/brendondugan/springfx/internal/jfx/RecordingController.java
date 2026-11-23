package com.brendondugan.springfx.internal.jfx;

import com.brendondugan.springfx.SpringFXController;

@SpringFXController(value = "/fxml/recording.fxml", viewName = "RECORDING")
class RecordingController {

    RecordingController(ControllerLog log) {
        log.created.add(this);
    }
}
