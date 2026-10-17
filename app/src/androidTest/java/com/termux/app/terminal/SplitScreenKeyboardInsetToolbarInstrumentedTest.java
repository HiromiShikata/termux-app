package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.graphics.Rect;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.termux.R;
import com.termux.app.RetryRule;
import com.termux.app.TermuxActivity;
import com.termux.view.TerminalView;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

@RunWith(AndroidJUnit4.class)
public class SplitScreenKeyboardInsetToolbarInstrumentedTest {

    private static final String LOG_TAG = "SplitScreenToolbarTest";

    private static final int TEST_RETRY_ATTEMPTS = 3;

    private static final long ACTIVITY_LAUNCH_TIMEOUT_MILLIS = 20_000L;

    private static final long SERVICE_AND_SESSION_READY_TIMEOUT_MILLIS = 30_000L;

    private static final long MULTI_WINDOW_MODE_ENTERED_TIMEOUT_MILLIS = 15_000L;

    private static final long HEIGHT_RESIZE_APPLIED_TIMEOUT_MILLIS = 15_000L;

    private static final long IME_VISIBILITY_TIMEOUT_MILLIS = 15_000L;

    private static final long POLL_INTERVAL_MILLIS = 150L;

    private static final long LAYOUT_SETTLE_MILLIS = 400L;

    private static final int CLAMPED_HEIGHT_REQUEST_DIVISOR = 8;

    @Rule
    public final RetryRule retryRule = new RetryRule(TEST_RETRY_ATTEMPTS);

    @Test
    public void toolbarSeparatorBoundsStayFixedAcrossRealKeyboardShowAndDismissInClampedSplitScreenHeight()
            throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        String targetPackageName = instrumentation.getTargetContext().getPackageName();

        Assume.assumeTrue(
            "this scenario requires a device that reports split-screen multi-window support",
            "true".equals(runShellCommand(instrumentation,
                "am supports-split-screen-multi-window").trim()));

        Instrumentation.ActivityMonitor monitor =
            instrumentation.addMonitor(TermuxActivity.class.getName(), null, false);
        String primaryLaunchComponent = targetPackageName + "/" + TermuxActivity.class.getName();
        runShellCommand(instrumentation,
            "am start -n " + primaryLaunchComponent + " --windowingMode 3");
        Activity launchedActivity;
        try {
            launchedActivity = monitor.waitForActivityWithTimeout(ACTIVITY_LAUNCH_TIMEOUT_MILLIS);
        } finally {
            instrumentation.removeMonitor(monitor);
        }
        assertNotNull("TermuxActivity must launch when started with split-screen-primary"
            + " windowing mode via the shell", launchedActivity);
        TermuxActivity activity = (TermuxActivity) launchedActivity;

        try {
            runShellCommand(instrumentation,
                "am start -a android.intent.action.MAIN -c android.intent.category.HOME"
                    + " --windowingMode 4");

            boolean enteredMultiWindowMode = awaitCondition(
                () -> mainThreadBoolean(instrumentation, activity::isInMultiWindowMode),
                MULTI_WINDOW_MODE_ENTERED_TIMEOUT_MILLIS);
            Assume.assumeTrue(
                "this scenario requires the launched task to actually enter multi-window mode"
                    + " after requesting split-screen-primary/secondary windowing modes",
                enteredMultiWindowMode);

            awaitServiceAndSessionReady(instrumentation, activity);

            View decorView = mainThreadValue(instrumentation,
                () -> activity.getWindow().getDecorView());

            int[] preResizeLocation = new int[2];
            int[] preResizeSize = new int[2];
            instrumentation.runOnMainSync(() -> {
                decorView.getLocationOnScreen(preResizeLocation);
                preResizeSize[0] = decorView.getWidth();
                preResizeSize[1] = decorView.getHeight();
            });
            int currentLeftPx = preResizeLocation[0];
            int currentTopPx = preResizeLocation[1];
            int currentRightPx = currentLeftPx + preResizeSize[0];
            int preResizeHeightPx = preResizeSize[1];
            int requestedHeightPx = Math.max(1, preResizeHeightPx / CLAMPED_HEIGHT_REQUEST_DIVISOR);
            int requestedBottomPx = currentTopPx + requestedHeightPx;

            int taskId = mainThreadInt(instrumentation, activity::getTaskId);
            runShellCommand(instrumentation, "am task resize " + taskId + " "
                + currentLeftPx + " " + currentTopPx + " " + currentRightPx + " "
                + requestedBottomPx);

            boolean heightChanged = awaitCondition(
                () -> mainThreadInt(instrumentation, decorView::getHeight) != preResizeHeightPx,
                HEIGHT_RESIZE_APPLIED_TIMEOUT_MILLIS);
            Assume.assumeTrue(
                "this scenario requires the device to actually apply the requested small-height"
                    + " task resize so the resulting clamped height can be observed",
                heightChanged);
            Thread.sleep(LAYOUT_SETTLE_MILLIS);

            int clampedHeightPx = mainThreadInt(instrumentation, decorView::getHeight);
            Log.i(LOG_TAG, "preResizeHeightPx=" + preResizeHeightPx
                + " requestedHeightPx=" + requestedHeightPx
                + " clampedHeightPx=" + clampedHeightPx);

            View separatorView = mainThreadValue(instrumentation,
                () -> activity.findViewById(R.id.terminal_toolbar_bottom_separator));
            assertNotNull("the bottom toolbar separator view must exist in this layout",
                separatorView);

            Rect boundsBeforeKeyboard = screenBoundsOf(instrumentation, separatorView);

            TerminalView terminalView = mainThreadValue(instrumentation, activity::getTerminalView);
            assertNotNull("the terminal view must exist so it can be tapped to request a real"
                + " keyboard show", terminalView);
            tapCenterOfView(instrumentation, terminalView);

            boolean imeShown = awaitImeVisibility(instrumentation, decorView, true,
                IME_VISIBILITY_TIMEOUT_MILLIS);
            assertTrue("tapping the terminal must bring up a real on-screen keyboard for this"
                + " clamped small split-screen window height scenario to be exercised", imeShown);

            Rect boundsWhileKeyboardShown = screenBoundsOf(instrumentation, separatorView);
            assertEquals("the bottom toolbar separator must not move while the real keyboard is"
                    + " shown at a clamped small split-screen window height",
                boundsBeforeKeyboard, boundsWhileKeyboardShown);

            UiDevice.getInstance(instrumentation).pressBack();
            boolean imeHidden = awaitImeVisibility(instrumentation, decorView, false,
                IME_VISIBILITY_TIMEOUT_MILLIS);
            assertTrue("the real keyboard must dismiss after pressing back so the toolbar's"
                + " settled position can be asserted", imeHidden);

            Rect boundsAfterKeyboardDismiss = screenBoundsOf(instrumentation, separatorView);
            assertEquals("the bottom toolbar separator must return to its original position"
                    + " after the real keyboard is dismissed at a clamped small split-screen"
                    + " window height",
                boundsBeforeKeyboard, boundsAfterKeyboardDismiss);
        } finally {
            instrumentation.runOnMainSync(activity::finish);
        }
    }

    private static void awaitServiceAndSessionReady(Instrumentation instrumentation,
                                                    TermuxActivity activity)
            throws InterruptedException {
        boolean ready = awaitCondition(() -> mainThreadBoolean(instrumentation,
                () -> activity.getTermuxService() != null && activity.getCurrentSession() != null),
            SERVICE_AND_SESSION_READY_TIMEOUT_MILLIS);
        assertTrue("TermuxService must connect and a default terminal session must be created"
            + " before the terminal can be tapped", ready);
    }

    private static boolean awaitCondition(BooleanSupplier condition, long timeoutMillis)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        return condition.getAsBoolean();
    }

    private static boolean awaitImeVisibility(Instrumentation instrumentation, View anchorView,
                                              boolean expectedVisible, long timeoutMillis)
            throws InterruptedException {
        return awaitCondition(
            () -> imeVisible(instrumentation, anchorView) == expectedVisible, timeoutMillis);
    }

    private static boolean imeVisible(Instrumentation instrumentation, View anchorView) {
        return mainThreadBoolean(instrumentation, () -> {
            WindowInsetsCompat insetsCompat = ViewCompat.getRootWindowInsets(anchorView);
            return insetsCompat != null && insetsCompat.isVisible(WindowInsetsCompat.Type.ime());
        });
    }

    private static Rect screenBoundsOf(Instrumentation instrumentation, View view) {
        int[] location = new int[2];
        int[] size = new int[2];
        instrumentation.runOnMainSync(() -> {
            view.getLocationOnScreen(location);
            size[0] = view.getWidth();
            size[1] = view.getHeight();
        });
        return new Rect(location[0], location[1], location[0] + size[0], location[1] + size[1]);
    }

    private static void tapCenterOfView(Instrumentation instrumentation, View view) {
        int[] location = new int[2];
        int[] size = new int[2];
        instrumentation.runOnMainSync(() -> {
            view.getLocationOnScreen(location);
            size[0] = view.getWidth();
            size[1] = view.getHeight();
        });
        int centerX = location[0] + size[0] / 2;
        int centerY = location[1] + size[1] / 2;
        UiDevice.getInstance(instrumentation).click(centerX, centerY);
    }

    private static <T> T mainThreadValue(Instrumentation instrumentation, Supplier<T> supplier) {
        Object[] result = new Object[1];
        instrumentation.runOnMainSync(() -> result[0] = supplier.get());
        @SuppressWarnings("unchecked")
        T value = (T) result[0];
        return value;
    }

    private static int mainThreadInt(Instrumentation instrumentation, IntSupplier supplier) {
        int[] result = new int[1];
        instrumentation.runOnMainSync(() -> result[0] = supplier.getAsInt());
        return result[0];
    }

    private static boolean mainThreadBoolean(Instrumentation instrumentation,
                                              BooleanSupplier supplier) {
        boolean[] result = new boolean[1];
        instrumentation.runOnMainSync(() -> result[0] = supplier.getAsBoolean());
        return result[0];
    }

    private static String runShellCommand(Instrumentation instrumentation, String command)
            throws IOException {
        UiAutomation uiAutomation = instrumentation.getUiAutomation();
        ParcelFileDescriptor fileDescriptor = uiAutomation.executeShellCommand(command);
        try (InputStream inputStream =
                 new ParcelFileDescriptor.AutoCloseInputStream(fileDescriptor)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int readCount;
            while ((readCount = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, readCount);
            }
            String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
            Log.i(LOG_TAG, "shell[" + command + "] -> " + result.trim());
            return result;
        }
    }
}
