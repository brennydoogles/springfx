package com.brendondugan.springfx.internal.jfx;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Records each RecordingController as Spring creates it, so tests can reach the Controller of a Window. */
class ControllerLog {

    final List<RecordingController> created = new CopyOnWriteArrayList<>();

    RecordingController last() {
        return created.getLast();
    }
}
