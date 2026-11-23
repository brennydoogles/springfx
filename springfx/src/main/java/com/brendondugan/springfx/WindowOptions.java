package com.brendondugan.springfx;

import javafx.stage.Modality;
import javafx.stage.Window;

import java.util.Objects;

/**
 * How {@link WindowManager#open(Class, WindowOptions)} opens a Window. Start from
 * {@link #defaults()} and change what you need:
 *
 * <pre>{@code
 * WindowOptions.defaults().withOwner(stage).withModality(Modality.WINDOW_MODAL).withTitle("Settings")
 * }</pre>
 *
 * @param owner the Window that owns the new one, or null for none
 * @param modality the new Window's modality
 * @param title the new Window's title, or null for the View's or the configured title
 * @param width the new Window's content width, or null for the View's preferred width
 * @param height the new Window's content height, or null for the View's preferred height
 */
public record WindowOptions(Window owner, Modality modality, String title, Double width, Double height) {

    private static final WindowOptions DEFAULTS = new WindowOptions(null, Modality.NONE, null, null, null);

    public WindowOptions {
        Objects.requireNonNull(modality, "modality");
    }

    /** No owner, not modal, the default title and the View's preferred size. */
    public static WindowOptions defaults() {
        return DEFAULTS;
    }

    /** These options with the given owner. */
    public WindowOptions withOwner(Window owner) {
        return new WindowOptions(owner, modality, title, width, height);
    }

    /** These options with the given modality. */
    public WindowOptions withModality(Modality modality) {
        return new WindowOptions(owner, modality, title, width, height);
    }

    /** These options with the given title. */
    public WindowOptions withTitle(String title) {
        return new WindowOptions(owner, modality, title, width, height);
    }

    /** These options with the given content size. */
    public WindowOptions withSize(double width, double height) {
        return new WindowOptions(owner, modality, title, width, height);
    }
}
