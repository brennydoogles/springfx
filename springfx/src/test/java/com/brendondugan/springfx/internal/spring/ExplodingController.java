package com.brendondugan.springfx.internal.spring;

import com.brendondugan.springfx.SpringFXController;

/** A Controller that fails if anything creates it. */
@SpringFXController("/fxml/exploding.fxml")
class ExplodingController {

    ExplodingController() {
        throw new IllegalStateException("ExplodingController must not be created");
    }
}
