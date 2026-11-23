package com.brendondugan.springfx;

import javafx.stage.Modality;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WindowOptionsTest {

    @Test
    void anEmptyBuilderGivesTheDefaults() {
        assertThat(WindowOptions.builder().build()).isEqualTo(WindowOptions.defaults());
    }

    @Test
    void theDefaultsAreUnownedNotModalUntitledAndUnsized() {
        assertThat(WindowOptions.defaults()).isEqualTo(new WindowOptions(null, Modality.NONE, null, null, null));
    }

    @Test
    void theBuilderSetsEachOption() {
        WindowOptions options = WindowOptions.builder()
                .modality(Modality.APPLICATION_MODAL)
                .title("Settings")
                .width(480.0)
                .height(320.0)
                .build();

        assertThat(options).isEqualTo(new WindowOptions(null, Modality.APPLICATION_MODAL, "Settings", 480.0, 320.0));
    }

    @Test
    void toBuilderStartsFromExistingOptions() {
        WindowOptions titled = WindowOptions.builder().title("Settings").build();

        assertThat(titled.toBuilder().modality(Modality.WINDOW_MODAL).build())
                .isEqualTo(new WindowOptions(null, Modality.WINDOW_MODAL, "Settings", null, null));
    }

    @Test
    void aNullModalityMeansNotModal() {
        assertThat(WindowOptions.builder().modality(null).build().modality()).isEqualTo(Modality.NONE);
        assertThat(WindowOptions.builder().modality(Modality.WINDOW_MODAL).build().toBuilder().modality(null).build()
                .modality()).isEqualTo(Modality.NONE);
    }
}
