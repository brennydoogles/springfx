package com.brendondugan.springfx.internal.jfx;

import com.brendondugan.springfx.SpringFXController;

@SpringFXController("/fxml/recording.fxml")
class RecordingController {

    RecordingController(ControllerLog log) {
        log.created.add(this);
    }
}
