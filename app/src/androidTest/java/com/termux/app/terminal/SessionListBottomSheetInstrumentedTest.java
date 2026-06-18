package com.termux.app.terminal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.termux.app.TermuxActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

@RunWith(AndroidJUnit4.class)
public class SessionListBottomSheetInstrumentedTest {

    private static final int MAX_IDLE_PUMP_ITERATIONS = 200;

    @Test
    public void showAndToggleOpenTheSheetAndHideAndToggleCloseIt() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        awaitSessionListControllerBound(scenario);

        scenario.onActivity(activity -> {
            SessionListBottomSheetController controller = activity.getSessionListBottomSheetController();
            assertNotNull(controller);
            assertFalse(controller.isOpen());

            controller.show();
            assertTrue(controller.isOpen());
        });
        awaitSheetClosedState(scenario, false);

        scenario.onActivity(activity -> {
            SessionListBottomSheetController controller = activity.getSessionListBottomSheetController();
            controller.hide();
        });
        awaitSheetClosedState(scenario, true);

        scenario.onActivity(activity -> {
            SessionListBottomSheetController controller = activity.getSessionListBottomSheetController();
            assertFalse(controller.isOpen());

            controller.toggle();
            assertTrue(controller.isOpen());
        });
        awaitSheetClosedState(scenario, false);

        scenario.onActivity(activity -> {
            SessionListBottomSheetController controller = activity.getSessionListBottomSheetController();
            controller.toggle();
        });
        awaitSheetClosedState(scenario, true);
    }

    private static void awaitSessionListControllerBound(ActivityScenario<TermuxActivity> scenario) {
        awaitMainThreadCondition(() -> {
            AtomicBoolean controllerBound = new AtomicBoolean(false);
            scenario.onActivity(activity ->
                controllerBound.set(activity.getSessionListBottomSheetController() != null
                    && activity.getTermuxSessionListViewController() != null));
            return controllerBound.get();
        });
    }

    private static void awaitSheetClosedState(ActivityScenario<TermuxActivity> scenario, boolean expectedClosed) {
        boolean reached = awaitMainThreadCondition(() -> {
            AtomicBoolean closed = new AtomicBoolean(false);
            scenario.onActivity(activity ->
                closed.set(!activity.getSessionListBottomSheetController().isOpen()));
            return closed.get() == expectedClosed;
        });
        assertTrue(reached);
    }

    private static boolean awaitMainThreadCondition(BooleanSupplier condition) {
        for (int iteration = 0; iteration < MAX_IDLE_PUMP_ITERATIONS; iteration++) {
            if (condition.getAsBoolean()) {
                return true;
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }
        return condition.getAsBoolean();
    }
}
