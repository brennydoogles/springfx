package com.brendondugan.springfx.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code spring.application.name}, the fallback Window title. */
@ConfigurationProperties("spring.application")
public record ApplicationNameProperties(String name) {
}
