package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.termux.app.RetryRule;
import com.termux.app.TermuxActivity;
import com.termux.app.browser.BrowserTab;
import com.termux.app.browser.TermuxBrowserController;
import com.termux.shared.termux.terminal.TermuxTerminalSessionClientBase;
import com.termux.terminal.TerminalSession;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

@RunWith(AndroidJUnit4.class)
public class SessionListBottomSheetBrowserUntouchedInstrumentedTest {

    @Rule
    public final RetryRule retryRule = new RetryRule();

    private static final int MAX_IDLE_PUMP_ITERATIONS = 200;

    private static final int SHEET_OPEN_CLOSE_CYCLES = 3;

    private static final String LOOPBACK_TAB_URL = "http://127.0.0.1/";

    private static final String DETACHED_SESSION_SHELL = "/system/bin/sh";

    @Test
    public void repeatedlyOpeningAndClosingTheSheetLeavesTheBrowserVisibleOnTheSameTab() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        awaitSessionListControllerBound(scenario);

        scenario.onActivity(activity -> {
            TermuxBrowserController browserController = activity.getTermuxBrowserController();
            SessionListBottomSheetController sheetController = activity.getSessionListBottomSheetController();
            assertNotNull(browserController);
            assertNotNull(sheetController);

            browserController.onSessionChanged(newDetachedSession());
            browserController.openUrlInNewTab(LOOPBACK_TAB_URL);

            BrowserTab tabBeforeSheet = browserController.getActiveTab();
            assertNotNull(tabBeforeSheet);
            assertTrue(browserController.isBrowserVisible());

            for (int cycle = 0; cycle < SHEET_OPEN_CLOSE_CYCLES; cycle++) {
                sheetController.show();
                assertTrue(sheetController.isOpen());
                assertBrowserUntouched(browserController, tabBeforeSheet);

                sheetController.hide();
                assertBrowserUntouched(browserController, tabBeforeSheet);
            }
        });
    }

    private static void assertBrowserUntouched(TermuxBrowserController browserController, BrowserTab expectedTab) {
        assertTrue(browserController.isBrowserVisible());
        assertEquals(expectedTab, browserController.getActiveTab());
        assertNotNull(browserController.getActiveTab());
        assertEquals(LOOPBACK_TAB_URL, browserController.getActiveTab().getUrl());
    }

    private static void awaitSessionListControllerBound(ActivityScenario<TermuxActivity> scenario) {
        boolean bound = awaitMainThreadCondition(() -> {
            AtomicBoolean controllerBound = new AtomicBoolean(false);
            scenario.onActivity(activity ->
                controllerBound.set(activity.getSessionListBottomSheetController() != null
                    && activity.getTermuxSessionListViewController() != null
                    && activity.getTermuxBrowserController() != null));
            return controllerBound.get();
        });
        assertTrue(bound);
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

    private static TerminalSession newDetachedSession() {
        return new TerminalSession(DETACHED_SESSION_SHELL, "/", new String[]{DETACHED_SESSION_SHELL},
            new String[0], null, new TermuxTerminalSessionClientBase());
    }
}
