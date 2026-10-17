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
import android.view.ViewConfiguration;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.termux.R;
import com.termux.app.RetryRule;
import com.termux.app.TermuxActivity;
import com.termux.app.apkupdate.ApkUpdateForegroundCheckThrottle;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RunWith(AndroidJUnit4.class)
public class SplitScreenKeyboardInsetToolbarInstrumentedTest {

    private static final String LOG_TAG = "SplitScreenToolbarTest";

    private static final int TEST_RETRY_ATTEMPTS = 3;

    private static final long ACTIVITY_LAUNCH_TIMEOUT_MILLIS = 20_000L;

    private static final long SERVICE_AND_SESSION_READY_TIMEOUT_MILLIS = 30_000L;

    private static final long MULTI_WINDOW_MODE_ENTERED_TIMEOUT_MILLIS = 15_000L;

    private static final long HEIGHT_RESIZE_APPLIED_TIMEOUT_MILLIS = 15_000L;

    private static final long IME_VISIBILITY_TIMEOUT_MILLIS = 15_000L;

    private static final long PRIMARY_PANE_WINDOW_FOCUS_TIMEOUT_MILLIS = 15_000L;

    private static final long POLL_INTERVAL_MILLIS = 150L;

    private static final long LAYOUT_SETTLE_MILLIS = 400L;

    private static final int CLAMPED_HEIGHT_REQUEST_DIVISOR = 8;

    private static final long APK_UPDATE_FOREGROUND_CHECK_SUPPRESSION_MILLIS = 600_000L;

    private static final long DOCKED_STACK_DIVIDER_FRAME_SETTLE_TIMEOUT_MILLIS = 8_000L;

    private static final long DOCKED_STACK_DIVIDER_FRAME_STABILITY_POLL_INTERVAL_MILLIS = 300L;

    private static final int DOCKED_STACK_DIVIDER_FRAME_STABILITY_REQUIRED_CONSECUTIVE_READS = 2;

    private static final int DOCKED_STACK_DIVIDER_SAFETY_MARGIN_PX = 20;

    private static final long DOUBLE_TAP_TIMEOUT_SAFETY_MARGIN_MILLIS = 150L;

    private static final Pattern TASK_RECORD_STACK_ID_PATTERN =
        Pattern.compile("#(\\d+)\\b.*?StackId=(\\d+)");

    private static final Pattern RESUMED_ACTIVITY_COMPONENT_PATTERN =
        Pattern.compile("ResumedActivity:\\s*ActivityRecord\\{\\S+\\s+\\S+\\s+(\\S+)");

    private static final Pattern INPUT_METHOD_CLIENT_STATE_LINE_PATTERN = Pattern.compile(
        "(?m)^\\s*(mCurClient=.*|mCurFocusedWindow=.*|mShowRequested=.*|mServedView=.*)$");

    private static final Pattern INPUT_METHOD_INPUT_SHOWN_PATTERN =
        Pattern.compile("mInputShown=(true|false)");

    private static final String DOCKED_STACK_DIVIDER_WINDOW_TITLE = "DockedStackDivider";

    private static final Pattern WINDOW_FRAME_BOTTOM_PATTERN =
        Pattern.compile("mFrame=\\[-?\\d+,-?\\d+\\]\\[-?\\d+,(-?\\d+)\\]");

    @Rule
    public final RetryRule retryRule = new RetryRule(TEST_RETRY_ATTEMPTS);

    @Test
    public void toolbarSeparatorBoundsStayFixedAcrossRealKeyboardShowAndDismissInClampedSplitScreenHeight()
            throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        String targetPackageName = instrumentation.getTargetContext().getPackageName();
        suppressApkUpdateForegroundCheckForTestDuration(instrumentation);

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
                "am start -a android.settings.SETTINGS --windowingMode 4");

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

            runShellCommand(instrumentation, "am start -n " + primaryLaunchComponent);
            boolean primaryPaneRegainedFocusAfterRefocus = awaitPrimaryPaneWindowFocusAndResumed(
                instrumentation, decorView, targetPackageName, TermuxActivity.class.getName(),
                PRIMARY_PANE_WINDOW_FOCUS_TIMEOUT_MILLIS);
            assertTrue("the primary split-screen pane must regain input focus and become the"
                + " resumed activity after being re-started in place, since"
                + " InputMethodManager#showSoftInput silently no-ops for a view whose window is"
                + " not the currently focused one", primaryPaneRegainedFocusAfterRefocus);

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
            int stackId = stackIdContainingTaskOrNegativeOneWhenNotFound(instrumentation, taskId);
            assertTrue("the split-screen-primary task's containing StackId must be discoverable"
                + " from 'dumpsys activity activities' so the stack (the unit split-screen"
                + " resize actually applies to, unlike 'am task resize' which is rejected for a"
                + " task inside a split-screen stack) can be resized", stackId >= 0);
            runShellCommand(instrumentation, "am stack resize " + stackId + " "
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

            Log.i(LOG_TAG, "afterResize hasWindowFocus="
                + mainThreadBoolean(instrumentation, decorView::hasWindowFocus)
                + " resumedActivityComponent="
                + resumedActivityComponentOrUnavailable(instrumentation));

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

            boolean imeShown = tapViewRepeatedlyUntilImeVisible(instrumentation, decorView,
                terminalView, IME_VISIBILITY_TIMEOUT_MILLIS);
            boolean altFocusableImFlagSet = mainThreadBoolean(instrumentation, () ->
                (activity.getWindow().getAttributes().flags
                    & android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM) != 0);
            Log.i(LOG_TAG, "afterTapLoop imeShown=" + imeShown
                + " altFocusableImFlagSet=" + altFocusableImFlagSet
                + " isSoftKeyboardEnabled=" + activity.getPreferences().isSoftKeyboardEnabled()
                + " isSoftKeyboardEnabledOnlyIfNoHardware="
                + activity.getPreferences().isSoftKeyboardEnabledOnlyIfNoHardware()
                + " inputMethodClientState="
                + inputMethodClientStateSummaryOrUnavailable(instrumentation));
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

    private static void suppressApkUpdateForegroundCheckForTestDuration(
            Instrumentation instrumentation) {
        PreferenceManager.getDefaultSharedPreferences(instrumentation.getTargetContext())
            .edit()
            .putLong(ApkUpdateForegroundCheckThrottle.PREFERENCE_KEY_LAST_FOREGROUND_CHECK_AT,
                System.currentTimeMillis() + APK_UPDATE_FOREGROUND_CHECK_SUPPRESSION_MILLIS)
            .commit();
    }

    private static int stackIdContainingTaskOrNegativeOneWhenNotFound(
            Instrumentation instrumentation, int taskId)
            throws IOException {
        String activitiesDump = runShellCommand(instrumentation, "dumpsys activity activities");
        Matcher matcher = TASK_RECORD_STACK_ID_PATTERN.matcher(activitiesDump);
        while (matcher.find()) {
            if (Integer.parseInt(matcher.group(1)) == taskId) {
                return Integer.parseInt(matcher.group(2));
            }
        }
        return -1;
    }

    private static String resumedActivityComponentOrUnavailable(Instrumentation instrumentation)
            throws IOException {
        String activitiesDump = runShellCommand(instrumentation, "dumpsys activity activities");
        Matcher matcher = RESUMED_ACTIVITY_COMPONENT_PATTERN.matcher(activitiesDump);
        return matcher.find() ? matcher.group(1) : "unavailable";
    }

    private static String inputMethodClientStateSummaryOrUnavailable(
            Instrumentation instrumentation) throws IOException {
        String inputMethodDump = runShellCommand(instrumentation, "dumpsys input_method");
        Matcher matcher = INPUT_METHOD_CLIENT_STATE_LINE_PATTERN.matcher(inputMethodDump);
        StringBuilder summary = new StringBuilder();
        while (matcher.find()) {
            if (summary.length() > 0) {
                summary.append(" | ");
            }
            summary.append(matcher.group(1).trim());
        }
        return summary.length() > 0 ? summary.toString() : "unavailable";
    }

    private static boolean resumedActivityComponentMatches(
            String resumedActivityComponentOrUnavailable, String expectedPackageName,
            String expectedClassName) {
        String[] packageAndClass = resumedActivityComponentOrUnavailable.split("/", 2);
        if (packageAndClass.length != 2 || !expectedPackageName.equals(packageAndClass[0])) {
            return false;
        }
        String classNameOrPackageRelativeSuffix = packageAndClass[1];
        String resolvedClassName = classNameOrPackageRelativeSuffix.startsWith(".")
            ? expectedPackageName + classNameOrPackageRelativeSuffix
            : classNameOrPackageRelativeSuffix;
        return expectedClassName.equals(resolvedClassName);
    }

    private static boolean awaitPrimaryPaneWindowFocusAndResumed(Instrumentation instrumentation,
            View decorView, String expectedPackageName, String expectedClassName,
            long timeoutMillis) throws InterruptedException, IOException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (true) {
            boolean hasWindowFocus = mainThreadBoolean(instrumentation, decorView::hasWindowFocus);
            String resumedActivityComponent = resumedActivityComponentOrUnavailable(instrumentation);
            boolean isPrimaryPaneResumed = resumedActivityComponentMatches(
                resumedActivityComponent, expectedPackageName, expectedClassName);
            if (hasWindowFocus && isPrimaryPaneResumed) {
                Log.i(LOG_TAG, "afterRefocus hasWindowFocus=" + hasWindowFocus
                    + " resumedActivityComponent=" + resumedActivityComponent);
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                Log.i(LOG_TAG, "afterRefocus hasWindowFocus=" + hasWindowFocus
                    + " resumedActivityComponent=" + resumedActivityComponent);
                return false;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
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

    private static boolean tapViewRepeatedlyUntilImeVisible(Instrumentation instrumentation,
                                                             View anchorView, View tapTargetView,
                                                             long timeoutMillis)
            throws InterruptedException, IOException {
        int minSafeTapYInclusivePx = awaitStableDockedStackDividerFrameBottomPx(instrumentation);
        long minSpacingPastDoubleTapTimeoutMillis =
            ViewConfiguration.getDoubleTapTimeout() + DOUBLE_TAP_TIMEOUT_SAFETY_MARGIN_MILLIS;
        long deadline = System.currentTimeMillis() + timeoutMillis;
        do {
            tapCenterOfView(instrumentation, tapTargetView, minSafeTapYInclusivePx);
            if (awaitImeVisibility(instrumentation, anchorView, true,
                    minSpacingPastDoubleTapTimeoutMillis)) {
                return true;
            }
        } while (System.currentTimeMillis() < deadline);
        return imeVisible(instrumentation, anchorView);
    }

    private static int dockedStackDividerFrameBottomOrMinValue(Instrumentation instrumentation)
            throws IOException {
        String windowDump = runShellCommand(instrumentation, "dumpsys window windows");
        int maximumBottomPx = Integer.MIN_VALUE;
        for (String windowBlock : windowDump.split("\n  Window #")) {
            if (!windowBlock.contains(DOCKED_STACK_DIVIDER_WINDOW_TITLE)) {
                continue;
            }
            Matcher matcher = WINDOW_FRAME_BOTTOM_PATTERN.matcher(windowBlock);
            if (matcher.find()) {
                maximumBottomPx = Math.max(maximumBottomPx, Integer.parseInt(matcher.group(1)));
            }
        }
        return maximumBottomPx;
    }

    private static int awaitStableDockedStackDividerFrameBottomPx(Instrumentation instrumentation)
            throws InterruptedException, IOException {
        long deadline =
            System.currentTimeMillis() + DOCKED_STACK_DIVIDER_FRAME_SETTLE_TIMEOUT_MILLIS;
        int previousBottomPx = Integer.MIN_VALUE;
        int consecutiveMatches = 0;
        while (true) {
            int currentBottomPx = dockedStackDividerFrameBottomOrMinValue(instrumentation);
            if (currentBottomPx != Integer.MIN_VALUE && currentBottomPx == previousBottomPx) {
                consecutiveMatches++;
            } else {
                consecutiveMatches = currentBottomPx == Integer.MIN_VALUE ? 0 : 1;
                previousBottomPx = currentBottomPx;
            }
            if (consecutiveMatches >= DOCKED_STACK_DIVIDER_FRAME_STABILITY_REQUIRED_CONSECUTIVE_READS
                    || System.currentTimeMillis() >= deadline) {
                break;
            }
            Thread.sleep(DOCKED_STACK_DIVIDER_FRAME_STABILITY_POLL_INTERVAL_MILLIS);
        }
        if (previousBottomPx == Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return previousBottomPx + DOCKED_STACK_DIVIDER_SAFETY_MARGIN_PX;
    }

    private static boolean imeVisible(Instrumentation instrumentation, View anchorView) {
        boolean insetsReportVisible = mainThreadBoolean(instrumentation, () -> {
            WindowInsetsCompat insetsCompat = ViewCompat.getRootWindowInsets(anchorView);
            return insetsCompat != null && insetsCompat.isVisible(WindowInsetsCompat.Type.ime());
        });
        // On API 29 WindowInsetsCompat#isVisible(Type.ime()) is a compat approximation, not the
        // real platform signal (that only landed in API 30), and it was observed via
        // 'dumpsys input_method' to disagree with the platform's own mInputShown state in this
        // clamped split-screen window height scenario: the platform genuinely shows the keyboard
        // (mShowRequested=true mInputShown=true) while this compat check still reports false.
        // Fall back to the platform's own ground truth so a real keyboard show/hide is not missed.
        return insetsReportVisible || imeShownAccordingToSystemOrFalse(instrumentation);
    }

    private static boolean imeShownAccordingToSystemOrFalse(Instrumentation instrumentation) {
        try {
            String inputMethodDump = runShellCommand(instrumentation, "dumpsys input_method");
            Matcher matcher = INPUT_METHOD_INPUT_SHOWN_PATTERN.matcher(inputMethodDump);
            return matcher.find() && Boolean.parseBoolean(matcher.group(1));
        } catch (IOException e) {
            return false;
        }
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

    private static void tapCenterOfView(Instrumentation instrumentation, View view,
                                         int minSafeTapYInclusivePx) {
        int[] location = new int[2];
        int[] size = new int[2];
        instrumentation.runOnMainSync(() -> {
            view.getLocationOnScreen(location);
            size[0] = view.getWidth();
            size[1] = view.getHeight();
        });
        int centerX = location[0] + size[0] / 2;
        int preferredCenterY = location[1] + size[1] / 2;
        int lowestSafeYInclusivePx = Math.max(location[1], minSafeTapYInclusivePx);
        int centerY = Math.min(location[1] + size[1] - 1,
            Math.max(preferredCenterY, lowestSafeYInclusivePx));
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
