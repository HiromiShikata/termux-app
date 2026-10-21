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
import android.view.ViewGroup;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import androidx.test.uiautomator.UiDevice;

import com.termux.R;
import com.termux.app.RetryRule;
import com.termux.app.TermuxActivity;
import com.termux.app.apkupdate.ApkUpdateForegroundCheckThrottle;
import com.termux.shared.view.ViewUtils;
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

    private static final long MARGIN_SETTLE_TIMEOUT_MILLIS = 15_000L;

    private static final String TARGETED_INSET_BRANCH_MARGIN_LOG_MESSAGE_PREFIX =
        "Setting bottom margin to real IME inset height ";

    private static final Pattern TARGETED_INSET_BRANCH_MARGIN_LOG_PATTERN =
        Pattern.compile(Pattern.quote(TARGETED_INSET_BRANCH_MARGIN_LOG_MESSAGE_PREFIX) + "(\\d+)");

    private static final int CLAMPED_HEIGHT_REQUEST_DIVISOR = 8;

    private static final float[] GROWN_HEIGHT_FRACTIONS_OF_DISPLAY_HEIGHT =
        {0.55f, 0.56f, 0.565f, 0.57f, 0.58f};

    private static final long APK_UPDATE_FOREGROUND_CHECK_SUPPRESSION_MILLIS = 600_000L;

    private static final int CLAMPED_TASK_TOUCHABLE_BOUNDARY_SAFETY_MARGIN_PX = 20;

    private static final long DOUBLE_TAP_TIMEOUT_SAFETY_MARGIN_MILLIS = 150L;

    private static final Pattern TASK_RECORD_STACK_ID_PATTERN =
        Pattern.compile("#(\\d+)\\b.*?StackId=(\\d+)");

    private static final Pattern RESUMED_ACTIVITY_COMPONENT_PATTERN =
        Pattern.compile("ResumedActivity:\\s*ActivityRecord\\{\\S+\\s+\\S+\\s+(\\S+)");

    private static final Pattern INPUT_METHOD_CLIENT_STATE_LINE_PATTERN = Pattern.compile(
        "(?m)^\\s*(mCurClient=.*|mCurFocusedWindow=.*|mShowRequested=.*|mServedView=.*)$");

    private static final Pattern INPUT_METHOD_INPUT_SHOWN_PATTERN =
        Pattern.compile("mInputShown=(true|false)");

    private static final Pattern INPUT_METHOD_SERVED_VIEW_CLASS_NAME_PATTERN =
        Pattern.compile("mServedView=(\\S+)\\{");

    private static final Pattern INPUT_METHOD_TOUCHABLE_REGION_TOP_PATTERN =
        Pattern.compile("touchable region=SkRegion\\(\\(\\d+,(\\d+),");

    private static final String SETTINGS_SECONDARY_PANE_PACKAGE_NAME = "com.android.settings";

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

        SplitScreenPrimaryPaneHandle pane = launchAndFocusSplitScreenPrimaryPaneWithSettingsSecondary(
            instrumentation, targetPackageName);
        TermuxActivity activity = pane.activity;
        View decorView = pane.decorView;

        try {
            int requestedHeightPx = Math.max(1, pane.preResizeHeightPx / CLAMPED_HEIGHT_REQUEST_DIVISOR);
            int requestedBottomPx = pane.currentTopPx + requestedHeightPx;
            runShellCommand(instrumentation, "am stack resize " + pane.stackId + " "
                + pane.currentLeftPx + " " + pane.currentTopPx + " " + pane.currentRightPx + " "
                + requestedBottomPx);

            boolean heightChanged = awaitCondition(
                () -> mainThreadInt(instrumentation, decorView::getHeight) != pane.preResizeHeightPx,
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
            Log.i(LOG_TAG, "preResizeHeightPx=" + pane.preResizeHeightPx
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

            int maxSafeTapYInclusivePx =
                requestedBottomPx - CLAMPED_TASK_TOUCHABLE_BOUNDARY_SAFETY_MARGIN_PX;

            boolean imeShown = tapViewRepeatedlyUntilImeVisible(instrumentation, decorView,
                terminalView, maxSafeTapYInclusivePx, IME_VISIBILITY_TIMEOUT_MILLIS);
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
            forceStopSettingsSecondaryPaneQuietly(instrumentation);
        }
    }

    @Test
    public void fullScreenKeyboardToggleKeepsImeVisibleAndToolbarFlushAboveIt() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        String targetPackageName = instrumentation.getTargetContext().getPackageName();
        suppressApkUpdateForegroundCheckForTestDuration(instrumentation);
        finishExistingTermuxActivity(instrumentation);
        runShellCommand(instrumentation,
            "am start -n " + targetPackageName + "/" + TermuxActivity.class.getName());
        TermuxActivity activity = awaitResumedTermuxActivity(instrumentation);
        try {
            awaitServiceAndSessionReady(instrumentation, activity);
            View decorView = mainThreadValue(instrumentation,
                () -> activity.getWindow().getDecorView());
            View separatorView = mainThreadValue(instrumentation,
                () -> activity.findViewById(R.id.terminal_toolbar_bottom_separator));
            View keyboardToggleButton = mainThreadValue(instrumentation,
                () -> activity.findViewById(R.id.terminal_toolbar_keyboard_toggle_button));
            assertNotNull("toolbar separator must exist", separatorView);
            assertNotNull("keyboard toggle button must exist", keyboardToggleButton);
            if (imeShownAccordingToSystemOrFalse(instrumentation)) {
                UiDevice.getInstance(instrumentation).pressBack();
            }
            assertTrue("full-screen baseline must start with the keyboard hidden",
                awaitImeVisibility(instrumentation, decorView, false, IME_VISIBILITY_TIMEOUT_MILLIS));
            Rect separatorBeforeKeyboard = screenBoundsOf(instrumentation, separatorView);
            Rect windowBeforeKeyboard = screenBoundsOf(instrumentation, decorView);
            View rootView = mainThreadValue(instrumentation, activity::getTermuxActivityRootView);
            assertNotNull("root view must exist", rootView);
            Rect rootBeforeKeyboard = screenBoundsOf(instrumentation, rootView);
            int separatorInsetFromRootBottomPx = rootBeforeKeyboard.bottom - separatorBeforeKeyboard.top;

            tapCenterOfView(instrumentation, keyboardToggleButton,
                windowBeforeKeyboard.bottom - 1);
            assertTrue("keyboard toggle must show the real on-screen keyboard",
                awaitImeVisibility(instrumentation, decorView, true, IME_VISIBILITY_TIMEOUT_MILLIS));
            for (int sample = 0; sample < 6; sample++) {
                Thread.sleep(700L);
                assertTrue("keyboard must remain shown after toolbar toggle, sample " + sample,
                    imeVisible(instrumentation, decorView));
            }
            Rect separatorWithKeyboard = screenBoundsOf(instrumentation, separatorView);
            WindowInsetsCompat shownInsets = mainThreadValue(instrumentation,
                () -> ViewCompat.getRootWindowInsets(decorView));
            assertNotNull("visible keyboard must deliver window insets", shownInsets);
            int realImeInsetBottomPx = shownInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            assertTrue("visible keyboard must report a positive IME inset", realImeInsetBottomPx > 0);
            int keyboardTopOnScreenPx = keyboardTouchableTopOnScreenPx(instrumentation);
            assertEquals("full-screen toolbar must stay flush above the real keyboard without"
                    + " an extra navigation-bar-sized displacement",
                keyboardTopOnScreenPx - separatorInsetFromRootBottomPx,
                separatorWithKeyboard.top);
        } finally {
            instrumentation.runOnMainSync(activity::finish);
        }
    }

    @Test
    public void toolbarMovesOnlyWithRealImeInsetWhenKeyboardOverlapsSplitScreenPane()
            throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        String targetPackageName = instrumentation.getTargetContext().getPackageName();
        suppressApkUpdateForegroundCheckForTestDuration(instrumentation);
        finishExistingTermuxActivity(instrumentation);
        Assume.assumeTrue("split-screen support is required", "true".equals(
            runShellCommand(instrumentation, "am supports-split-screen-multi-window").trim()));

        SplitScreenPrimaryPaneHandle pane = launchAndFocusSplitScreenPrimaryPaneWithSettingsSecondary(
            instrumentation, targetPackageName);
        TermuxActivity activity = pane.activity;
        View decorView = pane.decorView;
        try {
            int displayHeightPx = mainThreadValue(instrumentation,
                () -> ViewUtils.getDisplaySize(activity, false).y);
            int requestedBottomPx = pane.currentTopPx + Math.round(displayHeightPx * 0.6375f);
            runShellCommand(instrumentation, "am stack resize " + pane.stackId + " "
                + pane.currentLeftPx + " " + pane.currentTopPx + " " + pane.currentRightPx
                + " " + requestedBottomPx);
            assertTrue("the split-screen pane must reach the requested overlap height",
                awaitCondition(() -> mainThreadInt(instrumentation, decorView::getHeight)
                    == requestedBottomPx - pane.currentTopPx, HEIGHT_RESIZE_APPLIED_TIMEOUT_MILLIS));
            Thread.sleep(LAYOUT_SETTLE_MILLIS);

            View separatorView = mainThreadValue(instrumentation,
                () -> activity.findViewById(R.id.terminal_toolbar_bottom_separator));
            TerminalView terminalView = mainThreadValue(instrumentation, activity::getTerminalView);
            View bottomSpaceView = mainThreadValue(instrumentation,
                activity::getTermuxActivityBottomSpaceView);
            View rootView = mainThreadValue(instrumentation, activity::getTermuxActivityRootView);
            assertNotNull("toolbar separator must exist", separatorView);
            assertNotNull("terminal tap target must exist", terminalView);
            assertNotNull("bottom space view must exist", bottomSpaceView);
            assertNotNull("root view must exist", rootView);
            if (imeShownAccordingToSystemOrFalse(instrumentation)) {
                UiDevice.getInstance(instrumentation).pressBack();
            }
            assertTrue("split-screen baseline must start with the keyboard hidden",
                awaitImeVisibility(instrumentation, decorView, false, IME_VISIBILITY_TIMEOUT_MILLIS));
            Rect separatorBeforeKeyboard = screenBoundsOf(instrumentation, separatorView);
            Rect rootBeforeKeyboard = screenBoundsOf(instrumentation, rootView);
            Rect windowBeforeKeyboard = screenBoundsOf(instrumentation, decorView);
            int windowHeightBeforeKeyboardPx = mainThreadInt(instrumentation, decorView::getHeight);

            assertTrue("terminal tap must display a real keyboard",
                tapViewRepeatedlyUntilImeVisible(instrumentation, decorView, terminalView,
                    requestedBottomPx - CLAMPED_TASK_TOUCHABLE_BOUNDARY_SAFETY_MARGIN_PX,
                    IME_VISIBILITY_TIMEOUT_MILLIS));
            Thread.sleep(LAYOUT_SETTLE_MILLIS);
            Rect separatorWithKeyboard = screenBoundsOf(instrumentation, separatorView);
            KeyboardOverlapSnapshot shown = captureKeyboardOverlapSnapshot(
                instrumentation, decorView, bottomSpaceView, rootView);
            int windowResizePx = windowHeightBeforeKeyboardPx - shown.decorHeightPx;
            assertTrue("the keyboard must report a positive IME inset",
                shown.realImeInsetBottomPx > 0);
            int keyboardTopOnScreenPx = keyboardTouchableTopOnScreenPx(instrumentation);
            int separatorInsetFromRootBottomPx = rootBeforeKeyboard.bottom - separatorBeforeKeyboard.top;
            assertEquals("split-screen toolbar must sit flush above the real keyboard whether"
                    + " the pane resizes or the unconsumed inset becomes a margin; windowResizePx="
                    + windowResizePx,
                keyboardTopOnScreenPx - separatorInsetFromRootBottomPx,
                separatorWithKeyboard.top);

            UiDevice.getInstance(instrumentation).pressBack();
            assertTrue("keyboard must dismiss", awaitImeVisibility(instrumentation,
                decorView, false, IME_VISIBILITY_TIMEOUT_MILLIS));
            assertTrue("pane must restore its prior height", awaitCondition(
                () -> mainThreadInt(instrumentation, decorView::getHeight)
                    == windowHeightBeforeKeyboardPx, HEIGHT_RESIZE_APPLIED_TIMEOUT_MILLIS));
            assertEquals("toolbar must return to its original screen position",
                separatorBeforeKeyboard, screenBoundsOf(instrumentation, separatorView));
        } finally {
            instrumentation.runOnMainSync(activity::finish);
            forceStopSettingsSecondaryPaneQuietly(instrumentation);
        }
    }

    @Test
    public void toolbarBottomMarginMatchesRealImeInsetWhenKeyboardOverlapsWithoutWindowResizeInSplitScreen()
            throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        String targetPackageName = instrumentation.getTargetContext().getPackageName();
        suppressApkUpdateForegroundCheckForTestDuration(instrumentation);

        Assume.assumeTrue(
            "this scenario requires a device that reports split-screen multi-window support",
            "true".equals(runShellCommand(instrumentation,
                "am supports-split-screen-multi-window").trim()));

        SplitScreenPrimaryPaneHandle pane = launchAndFocusSplitScreenPrimaryPaneWithSettingsSecondary(
            instrumentation, targetPackageName);
        TermuxActivity activity = pane.activity;
        View decorView = pane.decorView;

        try {
            View bottomSpaceView = mainThreadValue(instrumentation,
                activity::getTermuxActivityBottomSpaceView);
            assertNotNull("the bottom space view that"
                + " recomputeBottomMarginUsingWindowInsets measures against must exist in this"
                + " layout", bottomSpaceView);

            View rootView = mainThreadValue(instrumentation, activity::getTermuxActivityRootView);
            assertNotNull("the activity's root view, whose bottom margin"
                + " recomputeBottomMarginUsingWindowInsets adjusts, must exist", rootView);
            com.termux.shared.logger.Logger.setLogLevel(activity,
                com.termux.shared.logger.Logger.LOG_LEVEL_VERBOSE);
            instrumentation.runOnMainSync(() ->
                ((TermuxActivityRootView) rootView).setIsRootViewLoggingEnabled(true));

            TerminalView terminalView = mainThreadValue(instrumentation, activity::getTerminalView);
            assertNotNull("the terminal view must exist so it can be tapped to request a real"
                + " keyboard show", terminalView);

            int displayHeightPx = mainThreadValue(instrumentation,
                () -> ViewUtils.getDisplaySize(activity, false).y);

            KeyboardOverlapSnapshot branchFiredSnapshot = null;
            int decorHeightBeforeKeyboardAtBranchFirePx = -1;
            int previousObservedHeightPx = pane.preResizeHeightPx;
            for (float growthFraction : GROWN_HEIGHT_FRACTIONS_OF_DISPLAY_HEIGHT) {
                int requestedHeightPx = Math.round(displayHeightPx * growthFraction);
                int requestedBottomPx = pane.currentTopPx + requestedHeightPx;
                runShellCommand(instrumentation, "am stack resize " + pane.stackId + " "
                    + pane.currentLeftPx + " " + pane.currentTopPx + " " + pane.currentRightPx
                    + " " + requestedBottomPx);

                int baselineHeightForThisAttemptPx = previousObservedHeightPx;
                boolean heightChanged = awaitCondition(
                    () -> mainThreadInt(instrumentation, decorView::getHeight)
                        != baselineHeightForThisAttemptPx,
                    HEIGHT_RESIZE_APPLIED_TIMEOUT_MILLIS);
                if (!heightChanged) {
                    Log.i(LOG_TAG, "growthAttempt growthFraction=" + growthFraction
                        + " requestedHeightPx=" + requestedHeightPx
                        + " skipped=heightDidNotChange");
                    continue;
                }
                Thread.sleep(LAYOUT_SETTLE_MILLIS);
                int decorHeightBeforeKeyboardPx = mainThreadInt(instrumentation, decorView::getHeight);
                previousObservedHeightPx = decorHeightBeforeKeyboardPx;

                clearLogcat(instrumentation);

                int maxSafeTapYInclusivePx = requestedBottomPx
                    - CLAMPED_TASK_TOUCHABLE_BOUNDARY_SAFETY_MARGIN_PX;
                boolean imeShown = tapViewRepeatedlyUntilImeVisible(instrumentation, decorView,
                    terminalView, maxSafeTapYInclusivePx, IME_VISIBILITY_TIMEOUT_MILLIS);
                if (!imeShown) {
                    Log.i(LOG_TAG, "growthAttempt growthFraction=" + growthFraction
                        + " requestedHeightPx=" + requestedHeightPx
                        + " decorHeightBeforeKeyboardPx=" + decorHeightBeforeKeyboardPx
                        + " skipped=realKeyboardDidNotShow");
                    continue;
                }

                Thread.sleep(LAYOUT_SETTLE_MILLIS);
                java.util.List<Integer> targetedBranchMarginValues =
                    targetedInsetBranchMarginValuesInLogcat(instrumentation);
                KeyboardOverlapSnapshot settled = captureKeyboardOverlapSnapshot(
                    instrumentation, decorView, bottomSpaceView, rootView);

                UiDevice.getInstance(instrumentation).pressBack();
                awaitImeVisibility(instrumentation, decorView, false, IME_VISIBILITY_TIMEOUT_MILLIS);

                boolean targetedBranchFired = !targetedBranchMarginValues.isEmpty()
                    && settled.keyboardVisible
                    && settled.decorHeightPx == decorHeightBeforeKeyboardPx;

                Log.i(LOG_TAG, "growthAttempt growthFraction=" + growthFraction
                    + " requestedHeightPx=" + requestedHeightPx
                    + " decorHeightBeforeKeyboardPx=" + decorHeightBeforeKeyboardPx
                    + " targetedBranchMarginValuesInLogcat=" + targetedBranchMarginValues
                    + " settledKeyboardVisible=" + settled.keyboardVisible
                    + " settledRealImeInsetBottomPx=" + settled.realImeInsetBottomPx
                    + " settledIsVisible=" + settled.isVisible
                    + " settledIsVisibleBecauseMargin=" + settled.isVisibleBecauseMargin
                    + " settledRootViewBottomMarginPx=" + settled.rootViewBottomMarginPx
                    + " settledDecorHeightPx=" + settled.decorHeightPx
                    + " targetedBranchFired=" + targetedBranchFired);

                if (targetedBranchFired) {
                    branchFiredSnapshot = settled;
                    decorHeightBeforeKeyboardAtBranchFirePx = decorHeightBeforeKeyboardPx;
                    break;
                }
            }

            Assume.assumeTrue("this scenario requires a split-screen primary pane height where"
                + " the real on-screen keyboard overlaps the window's own visible display frame"
                + " without the task's own rendered window size actually changing, which depends"
                + " on this device's display height and on-screen keyboard height; see logcat"
                + " tag " + LOG_TAG + " growthAttempt lines for what was observed at each"
                + " candidate height", branchFiredSnapshot != null);

            assertEquals("the window's own rendered frame height must not change when the real"
                    + " keyboard is shown in this scenario, since the margin-correction branch"
                    + " under test exists precisely to compensate for a keyboard overlap that"
                    + " occurs WITHOUT an actual window resize",
                decorHeightBeforeKeyboardAtBranchFirePx, branchFiredSnapshot.decorHeightPx);

            assertTrue("the real on-screen keyboard's IME inset must be genuinely positive for"
                    + " this scenario to exercise the margin-correction branch",
                branchFiredSnapshot.realImeInsetBottomPx > 0);

            assertEquals("the root view's bottom margin must exactly track the real IME inset"
                    + " height reported by the system when the keyboard genuinely overlaps the"
                    + " window without an actual window resize, since TermuxActivityRootView"
                    + " sets the margin directly from this value in that case",
                branchFiredSnapshot.realImeInsetBottomPx, branchFiredSnapshot.rootViewBottomMarginPx);
        } finally {
            instrumentation.runOnMainSync(activity::finish);
            forceStopSettingsSecondaryPaneQuietly(instrumentation);
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

    /** Force-stops the Settings app that
     * {@link #launchAndFocusSplitScreenPrimaryPaneWithSettingsSecondary} starts as the
     * split-screen secondary pane, so its task does not linger as a stray entry in 'dumpsys
     * activity activities' for a later test, or a later {@link RetryRule} retry attempt of this
     * same test, to be confused by. Called from a scenario's finally block, so this must run
     * regardless of whether the scenario passed or failed; any shell-command failure is logged
     * rather than propagated so it never masks that scenario's own pass/fail result. */
    private static void forceStopSettingsSecondaryPaneQuietly(Instrumentation instrumentation) {
        try {
            runShellCommand(instrumentation, "am force-stop " + SETTINGS_SECONDARY_PANE_PACKAGE_NAME);
        } catch (IOException e) {
            Log.i(LOG_TAG, "failed to force-stop " + SETTINGS_SECONDARY_PANE_PACKAGE_NAME
                + " while cleaning up the split-screen secondary pane: " + e);
        }
    }

    /** Holds the split-screen-primary {@link TermuxActivity} pane state shared by every scenario
     * once the primary pane has been launched, placed into genuine split-screen alongside a
     * Settings secondary pane, and refocused so it is the resumed, window-focused activity ready
     * to be resized. */
    private static final class SplitScreenPrimaryPaneHandle {
        final TermuxActivity activity;
        final View decorView;
        final int stackId;
        final int currentLeftPx;
        final int currentTopPx;
        final int currentRightPx;
        final int preResizeHeightPx;

        SplitScreenPrimaryPaneHandle(TermuxActivity activity, View decorView, int stackId,
                int currentLeftPx, int currentTopPx, int currentRightPx, int preResizeHeightPx) {
            this.activity = activity;
            this.decorView = decorView;
            this.stackId = stackId;
            this.currentLeftPx = currentLeftPx;
            this.currentTopPx = currentTopPx;
            this.currentRightPx = currentRightPx;
            this.preResizeHeightPx = preResizeHeightPx;
        }
    }

    private static TermuxActivity awaitResumedTermuxActivity(Instrumentation instrumentation)
            throws InterruptedException {
        TermuxActivity[] observedActivity = new TermuxActivity[1];
        boolean resumed = awaitCondition(
            () -> {
                observedActivity[0] = resumedTermuxActivityOrNull(instrumentation);
                return observedActivity[0] != null;
            },
            ACTIVITY_LAUNCH_TIMEOUT_MILLIS);
        assertTrue("TermuxActivity must become resumed after the launch request", resumed);
        return observedActivity[0];
    }

    private static TermuxActivity resumedTermuxActivityOrNull(Instrumentation instrumentation) {
        return mainThreadValue(instrumentation, () -> {
            for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)) {
                if (activity instanceof TermuxActivity) {
                    return (TermuxActivity) activity;
                }
            }
            return null;
        });
    }

    private static void finishExistingTermuxActivity(Instrumentation instrumentation)
            throws InterruptedException {
        TermuxActivity existingActivity = mainThreadValue(instrumentation, () -> {
            Stage[] liveStages = {
                Stage.RESUMED, Stage.STARTED, Stage.PAUSED, Stage.STOPPED, Stage.CREATED
            };
            for (Stage stage : liveStages) {
                for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(stage)) {
                    if (activity instanceof TermuxActivity) {
                        return (TermuxActivity) activity;
                    }
                }
            }
            return null;
        });
        if (existingActivity == null) return;
        instrumentation.runOnMainSync(existingActivity::finish);
        assertTrue("prior TermuxActivity must finish before a new windowing mode is requested",
            awaitCondition(() -> mainThreadBoolean(instrumentation, existingActivity::isDestroyed),
                ACTIVITY_LAUNCH_TIMEOUT_MILLIS));
    }

    private static SplitScreenPrimaryPaneHandle launchAndFocusSplitScreenPrimaryPaneWithSettingsSecondary(
            Instrumentation instrumentation, String targetPackageName)
            throws IOException, InterruptedException {
        String primaryLaunchComponent = targetPackageName + "/" + TermuxActivity.class.getName();
        runShellCommand(instrumentation,
            "am start -n " + primaryLaunchComponent + " --windowingMode 3");
        TermuxActivity activity = awaitResumedTermuxActivity(instrumentation);

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

        int taskId = mainThreadInt(instrumentation, activity::getTaskId);
        int stackId = stackIdContainingTaskOrNegativeOneWhenNotFound(instrumentation, taskId);
        assertTrue("the split-screen-primary task's containing StackId must be discoverable"
            + " from 'dumpsys activity activities' so the stack (the unit split-screen"
            + " resize actually applies to, unlike 'am task resize' which is rejected for a"
            + " task inside a split-screen stack) can be resized", stackId >= 0);

        return new SplitScreenPrimaryPaneHandle(activity, decorView, stackId, currentLeftPx,
            currentTopPx, currentRightPx, preResizeHeightPx);
    }

    /** A single point-in-time reading of every quantity
     * {@link TermuxActivityRootView#recomputeBottomMarginUsingWindowInsets} bases its decision
     * on, computed independently from the test using the same production
     * {@link ViewUtils} helpers, plus the root view's own currently-applied bottom margin and the
     * decor view's current height. */
    private static final class KeyboardOverlapSnapshot {
        final boolean keyboardVisible;
        final int realImeInsetBottomPx;
        final boolean isVisible;
        final boolean isVisibleBecauseMargin;
        final int rootViewBottomMarginPx;
        final int decorHeightPx;

        KeyboardOverlapSnapshot(boolean keyboardVisible, int realImeInsetBottomPx,
                boolean isVisible, boolean isVisibleBecauseMargin, int rootViewBottomMarginPx,
                int decorHeightPx) {
            this.keyboardVisible = keyboardVisible;
            this.realImeInsetBottomPx = realImeInsetBottomPx;
            this.isVisible = isVisible;
            this.isVisibleBecauseMargin = isVisibleBecauseMargin;
            this.rootViewBottomMarginPx = rootViewBottomMarginPx;
            this.decorHeightPx = decorHeightPx;
        }
    }

    private static KeyboardOverlapSnapshot captureKeyboardOverlapSnapshot(
            Instrumentation instrumentation, View decorView, View bottomSpaceView, View rootView) {
        Object[] result = new Object[1];
        instrumentation.runOnMainSync(() -> {
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decorView);
            boolean keyboardVisible = ViewUtils.isKeyboardVisible(insets);
            int realImeInsetBottomPx = insets != null
                ? insets.getInsets(WindowInsetsCompat.Type.ime()).bottom : 0;
            int statusBarHeightPx = insets != null
                ? insets.getInsets(WindowInsetsCompat.Type.statusBars()).top : 0;
            Rect[] windowAndViewRects =
                ViewUtils.getWindowAndViewRects(bottomSpaceView, statusBarHeightPx);
            int rootViewBottomMarginPx = 0;
            if (rootView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                rootViewBottomMarginPx =
                    ((ViewGroup.MarginLayoutParams) rootView.getLayoutParams()).bottomMargin;
            }
            boolean isVisible = windowAndViewRects != null
                && ViewUtils.isRectAbove(windowAndViewRects[0], windowAndViewRects[1]);
            boolean isVisibleBecauseMargin = windowAndViewRects != null
                && windowAndViewRects[0].bottom == windowAndViewRects[1].bottom
                && rootViewBottomMarginPx > 0;
            result[0] = new KeyboardOverlapSnapshot(keyboardVisible, realImeInsetBottomPx,
                isVisible, isVisibleBecauseMargin, rootViewBottomMarginPx, decorView.getHeight());
        });
        return (KeyboardOverlapSnapshot) result[0];
    }

    /** Clears the device's logcat buffer so a later read of
     * {@link #targetedInsetBranchMarginValuesInLogcat} only reflects lines logged after this
     * call. */
    private static void clearLogcat(Instrumentation instrumentation) throws IOException {
        runShellCommand(instrumentation, "logcat -c");
    }

    /** Returns, in the order logged, every value {@link TermuxActivityRootView}'s own verbose
     * logcat trace recorded it having baked directly into the root view's bottom margin from its
     * {@code realKeyboardImeInsetHeightPx} field, by searching the device's current logcat
     * buffer for that branch's exact, unique log message
     * ({@link #TARGETED_INSET_BRANCH_MARGIN_LOG_MESSAGE_PREFIX}). Returns an empty list if that
     * branch did not fire since the buffer was last cleared. {@code UiAutomation#executeShellCommand}
     * does not run its command through a shell - confirmed live: a {@code logcat -d | grep -F '...'}
     * command built the same way every other shell command in this file is built returned an
     * empty result in an actual instrumented run despite the target line being present in that
     * same run's own captured logcat, while the identical command run directly via {@code adb
     * shell} on the host (which does invoke a real shell) returned matches - so filtering is
     * done here in this process on the plain, unfiltered dump instead of relying on a pipe. */
    private static java.util.List<Integer> targetedInsetBranchMarginValuesInLogcat(
            Instrumentation instrumentation) throws IOException {
        String fullDump = runShellCommand(instrumentation, "logcat -d");
        java.util.List<Integer> values = new java.util.ArrayList<>();
        Matcher matcher = TARGETED_INSET_BRANCH_MARGIN_LOG_PATTERN.matcher(fullDump);
        while (matcher.find()) {
            values.add(Integer.parseInt(matcher.group(1)));
        }
        return values;
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

    private static int keyboardTouchableTopOnScreenPx(Instrumentation instrumentation)
            throws IOException {
        String windowsDump = runShellCommand(instrumentation, "dumpsys window windows");
        int inputMethodWindow = windowsDump.indexOf(" InputMethod}");
        assertTrue("visible keyboard must have an InputMethod window", inputMethodWindow >= 0);
        int nextWindow = windowsDump.indexOf("Window #", inputMethodWindow);
        String inputMethodSection = windowsDump.substring(inputMethodWindow,
            nextWindow >= 0 ? nextWindow : windowsDump.length());
        Matcher touchableRegion = INPUT_METHOD_TOUCHABLE_REGION_TOP_PATTERN.matcher(inputMethodSection);
        assertTrue("visible keyboard must expose its on-screen touchable region",
            touchableRegion.find());
        return Integer.parseInt(touchableRegion.group(1));
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
                                                             int maxSafeTapYInclusivePx,
                                                             long timeoutMillis)
            throws InterruptedException, IOException {
        long minSpacingPastDoubleTapTimeoutMillis =
            ViewConfiguration.getDoubleTapTimeout() + DOUBLE_TAP_TIMEOUT_SAFETY_MARGIN_MILLIS;
        long deadline = System.currentTimeMillis() + timeoutMillis;
        do {
            tapCenterOfView(instrumentation, tapTargetView, maxSafeTapYInclusivePx);
            if (awaitImeVisibility(instrumentation, anchorView, true,
                    minSpacingPastDoubleTapTimeoutMillis)) {
                return true;
            }
        } while (System.currentTimeMillis() < deadline);
        return imeVisible(instrumentation, anchorView);
    }

    private static boolean imeVisible(Instrumentation instrumentation, View anchorView) {
        return mainThreadBoolean(instrumentation, anchorView::hasWindowFocus)
            && imeShownAccordingToSystemOrFalse(instrumentation);
    }

    private static boolean imeShownAccordingToSystemOrFalse(Instrumentation instrumentation) {
        try {
            String inputMethodDump = runShellCommand(instrumentation, "dumpsys input_method");
            String targetPackageNamePrefix =
                instrumentation.getTargetContext().getPackageName() + ".";
            Matcher servedViewMatcher =
                INPUT_METHOD_SERVED_VIEW_CLASS_NAME_PATTERN.matcher(inputMethodDump);
            if (!servedViewMatcher.find()
                    || !servedViewMatcher.group(1).startsWith(targetPackageNamePrefix)) {
                return false;
            }
            Matcher inputShownMatcher = INPUT_METHOD_INPUT_SHOWN_PATTERN.matcher(inputMethodDump);
            return inputShownMatcher.find() && Boolean.parseBoolean(inputShownMatcher.group(1));
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
                                         int maxSafeTapYInclusivePx) {
        int[] location = new int[2];
        int[] size = new int[2];
        instrumentation.runOnMainSync(() -> {
            view.getLocationOnScreen(location);
            size[0] = view.getWidth();
            size[1] = view.getHeight();
        });
        int centerX = location[0] + size[0] / 2;
        int preferredCenterY = location[1] + size[1] / 2;
        int highestSafeYInclusivePx = Math.min(location[1] + size[1] - 1, maxSafeTapYInclusivePx);
        int centerY = Math.max(location[1], Math.min(preferredCenterY, highestSafeYInclusivePx));
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
