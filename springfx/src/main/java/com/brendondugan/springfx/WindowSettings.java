package com.brendondugan.springfx;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Window Settings: the configured starting state of Windows, bound from {@code springfx.window.*}.
 * Size and state apply to the Primary Window only; stylesheets and icons apply to every Window.
 *
 * @param title the Window title; when unset, {@code spring.application.name}, else "Application"
 * @param width the Primary Window's starting width
 * @param height the Primary Window's starting height
 * @param minWidth the Primary Window's minimum width
 * @param minHeight the Primary Window's minimum height
 * @param resizable whether the Primary Window can be resized
 * @param maximized whether the Primary Window starts maximised
 * @param fullScreen whether the Primary Window starts full screen
 * @param stylesheets class-path resources or URLs of stylesheets added to every Window
 * @param icons class-path resources or URLs of images used as every Window's icons
 */
@ConfigurationProperties("springfx.window")
public record WindowSettings(
        String title,
        @DefaultValue("900") double width,
        @DefaultValue("600") double height,
        @DefaultValue("0") double minWidth,
        @DefaultValue("0") double minHeight,
        @DefaultValue("true") boolean resizable,
        @DefaultValue("false") boolean maximized,
        @DefaultValue("false") boolean fullScreen,
        @DefaultValue List<String> stylesheets,
        @DefaultValue List<String> icons) {
}
