package com.brendondugan.springfx.internal.jfx;

/** The Controller of a nested {@code <fx:include>}, registered as a Spring bean. */
class ChildController {

    final GreetingService greetings;

    ChildController(GreetingService greetings) {
        this.greetings = greetings;
    }
}
