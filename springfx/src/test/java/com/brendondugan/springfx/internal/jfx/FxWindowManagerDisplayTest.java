package com.brendondugan.springfx.internal.jfx;

import com.brendondugan.springfx.WindowOptions;
import com.brendondugan.springfx.internal.config.SpringFXAutoConfiguration;
import com.brendondugan.springfx.internal.config.WindowSettingsResolver;
import javafx.application.Platform;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Opens real Windows, so it needs a display and starts the JavaFX toolkit itself. Tagged
 * {@code display} and left out of the default build; run it with {@code ./mvnw verify -Pdisplay}.
 */
@Tag("display")
class FxWindowManagerDisplayTest {

    private static final FxThread FX = new FxThread();

    private final AtomicInteger exits = new AtomicInteger();

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringFXAutoConfiguration.class))
            .withPropertyValues(
                    "springfx.window.title=Configured title",
                    "springfx.window.width=700",
                    "springfx.window.height=500",
                    "springfx.window.min-width=300",
                    "springfx.window.stylesheets=/css/app.css",
                    "springfx.window.icons=/icons/app.png")
            .withBean(GreetingService.class)
            .withBean(ControllerLog.class)
            .withBean(RecordingController.class)
            .withBean(ParentController.class)
            .withBean(ChildController.class);

    @BeforeAll
    static void startToolkit() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyStarted) {
            started.countDown();
        }
        Platform.setImplicitExit(false);
        assertThat(started.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @AfterEach
    void closeEveryWindow() {
        FX.call(() -> {
            List.copyOf(Window.getWindows()).forEach(Window::hide);
            return null;
        });
    }

    @Test
    void opensAViewInANewWindowWithAFreshControllerEachTime() {
        run((context, windows) -> {
            Stage first = windows.open(RecordingController.class);
            RecordingController firstController = log(context).last();
            Stage second = windows.open(RecordingController.class);
            RecordingController secondController = log(context).last();

            assertThat(first).isNotSameAs(second);
            assertThat(firstController).isNotSameAs(secondController);
            assertThat(fx(first::isShowing)).isTrue();
            assertThat(fx(second::isShowing)).isTrue();
            assertThat(windows.stageOf(firstController)).isSameAs(first);
            assertThat(windows.stageOf(secondController)).isSameAs(second);
        });
    }

    @Test
    void opensUnownedAndNotModalByDefault() {
        run((context, windows) -> {
            Stage stage = windows.open(RecordingController.class);

            assertThat(fx(stage::getOwner)).isNull();
            assertThat(fx(stage::getModality)).isEqualTo(Modality.NONE);
        });
    }

    @Test
    void opensAnOwnedModalWindow() {
        run((context, windows) -> {
            Stage primary = windows.primaryStage();

            Stage dialog = windows.open(RecordingController.class,
                    WindowOptions.builder().owner(primary).modality(Modality.WINDOW_MODAL).build());

            assertThat(fx(dialog::getOwner)).isSameAs(primary);
            assertThat(fx(dialog::getModality)).isEqualTo(Modality.WINDOW_MODAL);
        });
    }

    @Test
    void titlesComeFromTheOptionsThenTheControllerThenTheWindowSettings() {
        run((context, windows) -> {
            assertThat(fx(windows.open(ParentController.class, WindowOptions.defaults().withTitle("Asked for"))::getTitle))
                    .isEqualTo("Asked for");
            assertThat(fx(windows.open(ParentController.class)::getTitle)).isEqualTo("Parent title");
            assertThat(fx(windows.open(RecordingController.class)::getTitle)).isEqualTo("Configured title");
            assertThat(fx(windows.primaryStage()::getTitle)).isEqualTo("Configured title");
        });
    }

    @Test
    void stylesheetsAndIconsApplyToEveryWindow() {
        run((context, windows) -> {
            Stage opened = windows.open(RecordingController.class);

            for (Stage stage : List.of(windows.primaryStage(), opened)) {
                assertThat(fx(() -> stage.getScene().getStylesheets())).singleElement()
                        .satisfies(css -> assertThat(css).endsWith("/css/app.css"));
                assertThat(fx(() -> stage.getIcons().size())).isEqualTo(1);
            }
        });
    }

    @Test
    void sizeAndStateSettingsApplyOnlyToThePrimaryWindow() {
        run((context, windows) -> {
            Stage primary = windows.primaryStage();
            Stage preferred = windows.open(RecordingController.class);
            Stage sized = windows.open(RecordingController.class, WindowOptions.defaults().withSize(400, 300));

            assertThat(fx(() -> primary.getScene().getWidth())).isEqualTo(700);
            assertThat(fx(() -> primary.getScene().getHeight())).isEqualTo(500);
            assertThat(fx(primary::getMinWidth)).isEqualTo(300);
            assertThat(fx(() -> preferred.getScene().getWidth())).isEqualTo(320);
            assertThat(fx(() -> preferred.getScene().getHeight())).isEqualTo(200);
            assertThat(fx(preferred::getMinWidth)).isNotEqualTo(300);
            assertThat(fx(() -> sized.getScene().getWidth())).isEqualTo(400);
            assertThat(fx(() -> sized.getScene().getHeight())).isEqualTo(300);
        });
    }

    @Test
    void stageOfFindsThePrimaryWindow() {
        run((context, windows) -> {
            RecordingController primaryController = log(context).created.getFirst();

            assertThat(windows.stageOf(primaryController)).isSameAs(windows.primaryStage());
        });
    }

    @Test
    void stageOfFindsTheWindowOfANestedController() {
        run((context, windows) -> {
            Stage stage = windows.open(ParentController.class);
            // ChildController is a singleton here, so this is the instance the include used.
            ChildController child = context.getBean(ChildController.class);

            assertThat(windows.stageOf(child)).isSameAs(stage);
        });
    }

    @Test
    void closeClosesThatControllersWindowOnly() {
        run((context, windows) -> {
            Stage first = windows.open(RecordingController.class);
            RecordingController firstController = log(context).last();
            Stage second = windows.open(RecordingController.class);

            windows.close(firstController);
            flushFxThread();

            assertThat(fx(first::isShowing)).isFalse();
            assertThat(fx(second::isShowing)).isTrue();
            assertThat(fx(windows.primaryStage()::isShowing)).isTrue();
            assertThatIllegalArgumentException().isThrownBy(() -> windows.stageOf(firstController))
                    .withMessageContaining("isn't in an open Window");
            assertThat(exits).hasValue(0);
        });
    }

    @Test
    void closingThePrimaryWindowEndsTheAppEvenWithOtherWindowsOpen() {
        run((context, windows) -> {
            RecordingController primaryController = log(context).created.getFirst();
            windows.open(RecordingController.class);

            windows.close(primaryController);
            flushFxThread();

            assertThat(exits).hasValue(1);
        });
    }

    @Test
    void worksFromABackgroundThread() {
        run((context, windows) -> {
            Stage stage = background(() -> windows.open(RecordingController.class));
            RecordingController controller = log(context).last();

            assertThat(fx(stage::isShowing)).isTrue();
            assertThat(background(() -> windows.stageOf(controller))).isSameAs(stage);
            assertThat(background(windows::primaryStage)).isSameAs(fx(windows::primaryStage));

            background(() -> {
                windows.close(controller);
                return null;
            });
            assertThat(fx(stage::isShowing)).isFalse();
        });
    }

    @Test
    void primaryStageFailsBeforeThePrimaryWindowIsShown() {
        runner.run(context -> assertThatIllegalStateException()
                .isThrownBy(() -> manager(context).primaryStage())
                .withMessageContaining("Primary Window"));
    }

    private interface WindowTest {
        void run(AssertableApplicationContext context, FxWindowManager windows) throws Exception;
    }

    /** Shows a RecordingController as the Primary View, then runs the test off the FX thread. */
    private void run(WindowTest test) {
        runner.run((ContextConsumer<AssertableApplicationContext>) context -> {
            FxWindowManager windows = manager(context);
            Stage primary = fx(Stage::new);
            windows.showPrimary(primary, RecordingController.class);
            test.run(context, windows);
        });
    }

    private FxWindowManager manager(AssertableApplicationContext context) {
        return new FxWindowManager(context.getBean(ViewLoader.class), context.getBean(WindowSettingsResolver.class),
                exits::incrementAndGet);
    }

    private static ControllerLog log(AssertableApplicationContext context) {
        return context.getBean(ControllerLog.class);
    }

    private static <T> T fx(Supplier<T> work) {
        return FX.call(work);
    }

    /** Runs the work on a thread that is neither the test's nor the FX thread, and waits for it. */
    private static <T> T background(Supplier<T> work) throws Exception {
        CompletableFuture<Boolean> onFxThread = new CompletableFuture<>();
        T result = CompletableFuture.supplyAsync(() -> {
            onFxThread.complete(Platform.isFxApplicationThread());
            return work.get();
        }).get(10, TimeUnit.SECONDS);
        assertThat(onFxThread.get()).isFalse();
        flushFxThread();
        return result;
    }

    /** Waits for work already dispatched to the FX thread without waiting, such as close(). */
    private static void flushFxThread() {
        fx(() -> null);
    }
}
