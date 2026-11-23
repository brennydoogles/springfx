package com.brendondugan.springfx;

import javafx.stage.Modality;
import javafx.stage.Window;
import lombok.Builder;

/**
 * How {@link WindowManager#open(Class, WindowOptions)} opens a Window. Build one, setting only
 * what you need:
 *
 * <pre>{@code
 * WindowOptions.builder().owner(stage).modality(Modality.WINDOW_MODAL).title("Settings").build()
 * }</pre>
 *
 * <p>or start from {@link #defaults()} and change what you need:
 *
 * <pre>{@code
 * WindowOptions.defaults().withOwner(stage).withModality(Modality.WINDOW_MODAL).withTitle("Settings")
 * }</pre>
 *
 * @param owner the Window that owns the new one, or null for none
 * @param modality the new Window's modality, or null for {@link Modality#NONE}
 * @param title the new Window's title, or null for the View's or the configured title
 * @param width the new Window's content width, or null for the View's preferred width
 * @param height the new Window's content height, or null for the View's preferred height
 */
@Builder(toBuilder = true)
public record WindowOptions(Window owner, Modality modality, String title, Double width, Double height) {

    private static final WindowOptions DEFAULTS = builder().build();

    /** Fills in {@link Modality#NONE} for a null modality, which is what an unset builder gives. */
    public WindowOptions {
        if (modality == null) {
            modality = Modality.NONE;
        }
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
