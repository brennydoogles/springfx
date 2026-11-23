package com.brendondugan.springfx.internal;

/** The Controller of a nested {@code <fx:include>} with no bean definition of its own. */
class UnregisteredChildController {

    final GreetingService greetings;

    UnregisteredChildController(GreetingService greetings) {
        this.greetings = greetings;
    }
}
