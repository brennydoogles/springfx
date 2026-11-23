package com.brendondugan.springfx.internal.spring;

import com.brendondugan.springfx.SpringFXController;

/** A Controller that fails if anything creates it. */
@SpringFXController(value = "/fxml/exploding.fxml", viewName = "EXPLODING")
class ExplodingController {

    ExplodingController() {
        throw new IllegalStateException("ExplodingController must not be created");
    }
}
