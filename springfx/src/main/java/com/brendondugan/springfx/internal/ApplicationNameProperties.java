package com.brendondugan.springfx.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code spring.application.name}, the fallback Window title. */
@ConfigurationProperties("spring.application")
record ApplicationNameProperties(String name) {
}
