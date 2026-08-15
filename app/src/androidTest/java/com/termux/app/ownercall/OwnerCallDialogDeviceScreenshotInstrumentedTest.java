package com.termux.app.ownercall;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.os.SystemClock;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.browser.BrowserTab;
import com.termux.app.browser.TermuxBrowserController;
import com.termux.app.terminal.SessionNewActivityStore;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.app.terminal.TermuxTerminalViewClient;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants;
import com.termux.terminal.TerminalSession;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class OwnerCallDialogDeviceScreenshotInstrumentedTest {

    private static final String SESSION_URL = LocalOwnerCallServer.SESSION_URL;
    private static final String OWNER_CALL_FILE_PATH = LocalOwnerCallServer.OWNER_CALL_FILE_PATH;
    private static final String OLDEST_CALL_BODY =
        "Decide whether the previous addresses may be deleted in bulk.";
    private static final String REPEATED_CALL_BODY =
        "Decide whether the invoice recipient may be changed.";
    private static final String CALL_REASON = "the owner is being called";
    private static final String ANSWERED_CALL_TIME = "2026-08-14T04:22:28Z";
    private static final String REPLY_TIME_BETWEEN_THE_TWO_CALLS = "2026-08-14T04:25:00Z";
    private static final String WAITING_CALL_TIME = "2026-08-14T04:27:00Z";

    private static final long SERVICE_READY_TIMEOUT_MILLIS = 30_000L;
    private static final long SESSION_READY_TIMEOUT_MILLIS = 30_000L;
    private static final long DIALOG_TIMEOUT_MILLIS = 30_000L;
    private static final long TAP_SETTLE_MILLIS = 500L;
    private static final long TAP_DURATION_MILLIS = 50L;
    private static final long POLL_INTERVAL_MILLIS = 100L;
    private static final int QUARTER_OF_THE_SCREEN = 4;
    private static final String SCREENSHOT_DIRECTORY_NAME = "termux-instrumentation-screenshots";
    private static final int TERMINAL_BACKGROUND_COLOR = 0xFF000000;

    private static final String OWNER_CALL_FILE =
        ownerCallDocument("2026-08-14T04:22:28Z", OLDEST_CALL_BODY)
            + ownerCallDocument("2026-08-14T04:25:00Z", REPEATED_CALL_BODY)
            + ownerCallDocument("2026-08-14T04:27:00Z", REPEATED_CALL_BODY);

    private static String ownerCallDocument(String calledAt, String body) {
        return LocalOwnerCallServer.ownerCallDocument(calledAt, body);
    }

    @Rule
    public GrantPermissionRule writeExternalStoragePermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private LocalOwnerCallServer server;
    private String previousSessionDefinitionUrl;
    private ActivityScenario<TermuxActivity> scenario;

    @Before
    public void startTheOwnerCallServer() throws IOException {
        forgetTheStoredDialogPlacement();
        previousSessionDefinitionUrl = preferences().getSessionDefinitionUrl();
        server = new LocalOwnerCallServer(OWNER_CALL_FILE);
        server.start();
    }

    @After
    public void stopTheOwnerCallServer() throws InterruptedException {
        CallingSessionRemove.removeEveryCallingSession(scenario);
        forgetTheStoredDialogPlacement();
        preferences().setSessionDefinitionUrl(previousSessionDefinitionUrl);
        if (server != null) {
            server.stop();
        }
    }

    private static void forgetTheStoredDialogPlacement() {
        int unset = TermuxPreferenceConstants.TERMUX_APP.VALUE_OWNER_CALL_DIALOG_PLACEMENT_UNSET;
        preferences().setOwnerCallDialogPlacement(unset, unset, unset, unset);
    }

    private static TermuxAppSharedPreferences preferences() {
        return TermuxAppSharedPreferences.build(
            InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @Test
    public void theWaitingOwnerCallStaysReadableOverTheTerminalInPortraitAndInLandscape()
        throws Exception {
        ActivityScenario<TermuxActivity> scenario = launchWithACallingSession();

        setOrientation(scenario, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            Configuration.ORIENTATION_PORTRAIT);
        awaitDialogShowing(scenario, "1 / 3");
        assertDialogMatchesTheSpecification(scenario);
        File portraitScreenshot = captureScreenshot(scenario, "owner-call-dialog-device-portrait.png");
        assertNotNull("the portrait device screenshot must reach the directory the workflow pulls",
            portraitScreenshot);
        assertTrue("the portrait device screenshot must be non-empty",
            portraitScreenshot.exists() && portraitScreenshot.length() > 0);

        setOrientation(scenario, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            Configuration.ORIENTATION_LANDSCAPE);
        awaitDialogShowing(scenario, "1 / 3");
        assertDialogMatchesTheSpecification(scenario);
        File landscapeScreenshot = captureScreenshot(scenario, "owner-call-dialog-device-landscape.png");
        assertNotNull("the landscape device screenshot must reach the directory the workflow pulls",
            landscapeScreenshot);
        assertTrue("the landscape device screenshot must be non-empty",
            landscapeScreenshot.exists() && landscapeScreenshot.length() > 0);

        scenario.onActivity(activity -> {
            activity.findViewById(R.id.owner_call_dialog_next_button).performClick();
            assertEquals("2 / 3", textOf(activity, R.id.owner_call_dialog_position));
            assertEquals(REPEATED_CALL_BODY, textOf(activity, R.id.owner_call_dialog_body));
        });
    }

    @Test
    public void closingTheDialogHidesItAndThePendingIndicatorRemainsAndReopensIt()
        throws Exception {
        ActivityScenario<TermuxActivity> scenario = launchWithACallingSession();
        awaitDialogShowing(scenario, "1 / 3");

        scenario.onActivity(activity -> {
            activity.findViewById(R.id.owner_call_dialog_close_button).performClick();
            assertEquals(View.GONE,
                activity.findViewById(R.id.owner_call_dialog).getVisibility());
            assertEquals(View.VISIBLE,
                activity.findViewById(R.id.owner_call_pending_indicator).getVisibility());
        });

        assertNotNull("the closed-state device screenshot must reach the directory the workflow pulls",
            captureScreenshot(scenario, "owner-call-dialog-closed-with-indicator.png"));

        scenario.onActivity(activity -> {
            activity.findViewById(R.id.owner_call_pending_indicator).performClick();
            assertEquals(View.VISIBLE,
                activity.findViewById(R.id.owner_call_dialog).getVisibility());
            assertEquals("1 / 3", textOf(activity, R.id.owner_call_dialog_position));
            assertEquals(OLDEST_CALL_BODY, textOf(activity, R.id.owner_call_dialog_body));
        });

        assertNotNull("the reopened-dialog device screenshot must reach the directory the workflow pulls",
            captureScreenshot(scenario, "owner-call-dialog-reopened-from-indicator.png"));
    }

    @Test
    public void theDragMovesTheDialogAndThePositionSurvivesATerminalScreenUpdate() throws Exception {
        ActivityScenario<TermuxActivity> scenario = launchWithACallingSession();
        awaitDialogShowing(scenario, "1 / 3");
        File screenshotBeforeDrag = captureScreenshot(scenario, "owner-call-dialog-device-before-drag.png");
        assertNotNull("portrait screenshot before drag must reach the directory the workflow pulls",
            screenshotBeforeDrag);
        assertTrue("portrait screenshot before drag must be non-empty",
            screenshotBeforeDrag.exists() && screenshotBeforeDrag.length() > 0);

        AtomicInteger initialBottom = new AtomicInteger();
        AtomicInteger headerScreenX = new AtomicInteger();
        AtomicInteger headerScreenY = new AtomicInteger();
        AtomicInteger headerWidth = new AtomicInteger();
        AtomicInteger headerHeight = new AtomicInteger();
        scenario.onActivity(activity -> {
            View dialog = activity.findViewById(R.id.owner_call_dialog);
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) dialog.getLayoutParams();
            initialBottom.set(p.bottomMargin);
            View header = activity.findViewById(R.id.owner_call_dialog_header);
            int[] location = new int[2];
            header.getLocationInWindow(location);
            headerScreenX.set(location[0]);
            headerScreenY.set(location[1]);
            headerWidth.set(header.getWidth());
            headerHeight.set(header.getHeight());
        });

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        int dragX = headerScreenX.get() + headerWidth.get() / 2;
        int dragY = headerScreenY.get() + headerHeight.get() / 2;
        dragBy(scenario, dragX, dragY, 0, -300);
        instrumentation.waitForIdleSync();

        scenario.onActivity(activity -> {
            View dialog = activity.findViewById(R.id.owner_call_dialog);
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) dialog.getLayoutParams();
            assertTrue("a drag on the header from " + dragX + "," + dragY
                    + " with no long press must move the dialog upward, but the bottom margin went"
                    + " from " + initialBottom.get() + " to " + p.bottomMargin
                    + " with the dialog " + p.width + " by " + p.height
                    + " and the header " + headerWidth.get() + " by " + headerHeight.get()
                    + " at " + headerScreenX.get() + "," + headerScreenY.get(),
                p.bottomMargin > initialBottom.get());
        });
        File screenshotAfterDrag = captureScreenshot(scenario, "owner-call-dialog-device-after-drag.png");
        assertNotNull("portrait screenshot after drag must reach the directory the workflow pulls",
            screenshotAfterDrag);
        assertTrue("portrait screenshot after drag must be non-empty",
            screenshotAfterDrag.exists() && screenshotAfterDrag.length() > 0);

        scenario.onActivity(activity -> activity.showUnansweredOwnerCallsOfDisplayedSession());
        instrumentation.waitForIdleSync();

        scenario.onActivity(activity -> {
            View dialog = activity.findViewById(R.id.owner_call_dialog);
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) dialog.getLayoutParams();
            assertTrue("position must survive a terminal screen update re-bind",
                p.bottomMargin > initialBottom.get());
        });
        File screenshotAfterRebind = captureScreenshot(scenario, "owner-call-dialog-device-after-rebind.png");
        assertNotNull("portrait screenshot after re-bind must reach the directory the workflow pulls",
            screenshotAfterRebind);
        assertTrue("portrait screenshot after re-bind must be non-empty",
            screenshotAfterRebind.exists() && screenshotAfterRebind.length() > 0);
    }

    @Test
    public void theHeaderDragMovesTheDialogSidewaysAndTheRightEdgeDragNarrowsIt() throws Exception {
        ActivityScenario<TermuxActivity> scenario = launchWithACallingSession();
        awaitDialogShowing(scenario, "1 / 3");
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

        AtomicInteger rightEdgeX = new AtomicInteger();
        AtomicInteger rightEdgeY = new AtomicInteger();
        AtomicInteger widthBeforeTheResize = new AtomicInteger();
        scenario.onActivity(activity -> {
            View dialog = activity.findViewById(R.id.owner_call_dialog);
            int[] location = new int[2];
            dialog.getLocationInWindow(location);
            rightEdgeX.set(location[0] + dialog.getWidth() - 1);
            rightEdgeY.set(location[1] + dialog.getHeight() / 2);
            ViewGroup.MarginLayoutParams p =
                (ViewGroup.MarginLayoutParams) dialog.getLayoutParams();
            widthBeforeTheResize.set(p.width);
        });

        dragBy(scenario, rightEdgeX.get(), rightEdgeY.get(), -200, 0);
        instrumentation.waitForIdleSync();

        AtomicInteger leftBeforeTheMove = new AtomicInteger();
        scenario.onActivity(activity -> {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams)
                activity.findViewById(R.id.owner_call_dialog).getLayoutParams();
            assertTrue("dragging the right frame edge left must narrow the dialog",
                p.width < widthBeforeTheResize.get());
            leftBeforeTheMove.set(p.leftMargin);
        });

        AtomicInteger headerX = new AtomicInteger();
        AtomicInteger headerY = new AtomicInteger();
        scenario.onActivity(activity -> {
            View header = activity.findViewById(R.id.owner_call_dialog_header);
            int[] location = new int[2];
            header.getLocationInWindow(location);
            headerX.set(location[0] + header.getWidth() / 2);
            headerY.set(location[1] + header.getHeight() / 2);
        });

        dragBy(scenario, headerX.get(), headerY.get(), 120, 0);
        instrumentation.waitForIdleSync();

        scenario.onActivity(activity -> {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams)
                activity.findViewById(R.id.owner_call_dialog).getLayoutParams();
            assertTrue("a drag on the header must move the narrowed dialog sideways",
                p.leftMargin > leftBeforeTheMove.get());
        });
    }

    @Test
    public void theTopFrameEdgeDragGrowsTheDialogUpwardAndTheBottomEdgeStaysPut() throws Exception {
        ActivityScenario<TermuxActivity> scenario = launchWithACallingSession();
        awaitDialogShowing(scenario, "1 / 3");
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

        AtomicInteger grabX = new AtomicInteger();
        AtomicInteger grabY = new AtomicInteger();
        AtomicInteger heightBeforeTheDrag = new AtomicInteger();
        AtomicInteger bottomBeforeTheDrag = new AtomicInteger();
        AtomicInteger edgeBand = new AtomicInteger();
        scenario.onActivity(activity -> {
            OwnerCallDialogFrame frame = activity.findViewById(R.id.owner_call_dialog);
            int[] location = new int[2];
            frame.getLocationInWindow(location);
            edgeBand.set(frame.getEdgeBandPixels());
            grabX.set(location[0] + 1);
            grabY.set(location[1] + 1);
            ViewGroup.MarginLayoutParams p =
                (ViewGroup.MarginLayoutParams) frame.getLayoutParams();
            heightBeforeTheDrag.set(p.height);
            bottomBeforeTheDrag.set(p.bottomMargin);
            assertTrue("the top left corner must grip the top and the left frame edges",
                frame.gripAt(1, 1).isTopEdgeGripped() && frame.gripAt(1, 1).isLeftEdgeGripped());
        });

        dragBy(scenario, grabX.get(), grabY.get(), 0, -220);
        instrumentation.waitForIdleSync();

        scenario.onActivity(activity -> {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams)
                activity.findViewById(R.id.owner_call_dialog).getLayoutParams();
            assertTrue("a drag on the top frame edge from " + grabX.get() + "," + grabY.get()
                    + " with an edge band of " + edgeBand.get()
                    + " pixels must grow the dialog upward, but the height went from "
                    + heightBeforeTheDrag.get() + " to " + p.height,
                p.height > heightBeforeTheDrag.get());
            assertEquals("the bottom edge must stay where it was while the top edge is dragged",
                bottomBeforeTheDrag.get(), p.bottomMargin);
        });

        assertNotNull("the frame resize screenshot must reach the directory the workflow pulls",
            captureScreenshot(scenario, "owner-call-dialog-after-top-edge-drag.png"));
    }

    private static void dragBy(ActivityScenario<TermuxActivity> scenario, int startX, int startY,
                               int horizontalPixels, int verticalPixels) {
        int slop = ViewConfiguration.get(
            InstrumentationRegistry.getInstrumentation().getTargetContext()).getScaledTouchSlop();
        long downTime = SystemClock.uptimeMillis();
        dispatchToWindow(scenario, MotionEvent.ACTION_DOWN, downTime, downTime, startX, startY);
        dispatchToWindow(scenario, MotionEvent.ACTION_MOVE, downTime, downTime + 20,
            startX + slop + 1, startY + slop + 1);
        dispatchToWindow(scenario, MotionEvent.ACTION_MOVE, downTime, downTime + 40,
            startX + slop + 1 + horizontalPixels, startY + slop + 1 + verticalPixels);
        dispatchToWindow(scenario, MotionEvent.ACTION_UP, downTime, downTime + 60,
            startX + slop + 1 + horizontalPixels, startY + slop + 1 + verticalPixels);
    }

    @Test
    public void aShortTapOnTheNextButtonStillPagesAfterTheDragListenerIsAttached()
        throws Exception {
        ActivityScenario<TermuxActivity> scenario = launchWithACallingSession();
        awaitDialogShowing(scenario, "1 / 3");

        AtomicInteger tapX = new AtomicInteger();
        AtomicInteger tapY = new AtomicInteger();
        AtomicInteger buttonWidth = new AtomicInteger();
        AtomicInteger buttonHeight = new AtomicInteger();
        scenario.onActivity(activity -> {
            View nextButton = activity.findViewById(R.id.owner_call_dialog_next_button);
            assertTrue("the next button must be enabled while the oldest call is displayed",
                nextButton.isEnabled());
            int[] location = new int[2];
            nextButton.getLocationInWindow(location);
            buttonWidth.set(nextButton.getWidth());
            buttonHeight.set(nextButton.getHeight());
            tapX.set(location[0] + nextButton.getWidth() / 2);
            tapY.set(location[1] + nextButton.getHeight() / 2);
        });

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        long downTime = SystemClock.uptimeMillis();
        dispatchToWindow(scenario, MotionEvent.ACTION_DOWN, downTime, downTime,
            tapX.get(), tapY.get());
        dispatchToWindow(scenario, MotionEvent.ACTION_UP, downTime,
            downTime + TAP_DURATION_MILLIS, tapX.get(), tapY.get());
        instrumentation.waitForIdleSync();
        Thread.sleep(TAP_SETTLE_MILLIS);
        instrumentation.waitForIdleSync();

        AtomicReference<String> positionAfterTheTap = new AtomicReference<>("");
        scenario.onActivity(activity ->
            positionAfterTheTap.set(textOf(activity, R.id.owner_call_dialog_position)));

        assertEquals("a short tap at " + tapX.get() + "," + tapY.get()
                + " on the next button, which is " + buttonWidth.get() + " by "
                + buttonHeight.get()
                + " pixels, must page forward to the second call even after the drag listener "
                + "is attached to the header",
            "2 / 3", positionAfterTheTap.get());
    }

    private static void dispatchToWindow(ActivityScenario<TermuxActivity> scenario, int action,
                                         long downTime, long eventTime, int x, int y) {
        scenario.onActivity(activity -> {
            MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
            activity.getWindow().getDecorView().dispatchTouchEvent(event);
            event.recycle();
        });
    }

    @Test
    public void replyingToTheSessionDeletesItsOwnerCallFileAndClosesTheDialog() throws Exception {
        ActivityScenario<TermuxActivity> scenario = launchWithACallingSession();
        awaitDialogShowing(scenario, "1 / 3");

        scenario.onActivity(activity -> {
            TerminalSession session = activity.getCurrentSession();
            assertNotNull(session);
            assertTrue("the reply must be typed into a live session", session.isRunning());
            TermuxTerminalViewClient viewClient = activity.getTermuxTerminalViewClient();
            assertNotNull(viewClient);
            viewClient.onCodePoint('y', false, session);
            viewClient.onCodePoint('\n', false, session);
        });

        awaitDeleteRequest();
        awaitDialogGone(scenario);
        awaitIndicatorGone(scenario);

        assertEquals(Collections.singletonList(OWNER_CALL_FILE_PATH), server.deletedPaths());
        assertFalse("the answered owner call file must no longer be served",
            server.holdsTheOwnerCallFile());
    }

    @Test
    public void aCallTheOwnerAnsweredStaysHiddenWhenTheNextCallArrivesOnTheSameSession()
        throws Exception {
        server.serveOwnerCallFile(ownerCallDocument(ANSWERED_CALL_TIME, OLDEST_CALL_BODY)
            + ownerCallDocument(WAITING_CALL_TIME, REPEATED_CALL_BODY));
        ActivityScenario<TermuxActivity> scenario = launchWithACallingSession();
        awaitDialogShowing(scenario, "1 / 2");

        scenario.onActivity(activity -> {
            SessionNewActivityStore store = activity.getSessionNewActivityStore();
            assertNotNull(store);
            store.recordGenuineAppReply(SESSION_URL,
                Instant.parse(REPLY_TIME_BETWEEN_THE_TWO_CALLS).toEpochMilli());
            store.recordExplicitCall(SESSION_URL, System.currentTimeMillis(), CALL_REASON,
                CALL_REASON + "-" + System.nanoTime());
            activity.showUnansweredOwnerCallsOfDisplayedSession();
        });

        awaitDialogShowing(scenario, "1 / 1");
        scenario.onActivity(activity -> assertEquals(REPEATED_CALL_BODY,
            textOf(activity, R.id.owner_call_dialog_body)));
    }

    @Test
    public void indicatorIsHiddenWhenTheSessionHasNoOwnerCall() throws Exception {
        scenario = ActivityScenario.launch(TermuxActivity.class);
        awaitServiceConnected(scenario);

        scenario.onActivity(activity -> assertEquals(View.GONE,
            activity.findViewById(R.id.owner_call_pending_indicator).getVisibility()));
    }

    @Test
    public void tappingAUrlInTheDialogOpensItInTheInAppBrowser() throws Exception {
        String tappedUrl = server.tappedPageUrl();
        server.serveOwnerCallFile(
            ownerCallDocument("2026-08-14T05:00:00Z", "Approve the release at " + tappedUrl));
        ActivityScenario<TermuxActivity> scenario = launchWithACallingSession();
        awaitDialogShowing(scenario, "1 / 1");

        scenario.onActivity(activity -> {
            TextView body = activity.findViewById(R.id.owner_call_dialog_body);
            assertNotNull(body);
            tappableUrlSpanOf(body).onClick(body);
        });

        awaitBrowserShowingTheTappedUrl(scenario, tappedUrl);
    }

    private static ClickableSpan tappableUrlSpanOf(TextView body) {
        CharSequence text = body.getText();
        assertTrue("the dialog body must carry tappable spans", text instanceof Spanned);
        ClickableSpan[] spans = ((Spanned) text).getSpans(0, text.length(), ClickableSpan.class);
        assertEquals("the URL in the body is the only tappable span", 1, spans.length);
        return spans[0];
    }

    private static void awaitBrowserShowingTheTappedUrl(ActivityScenario<TermuxActivity> scenario,
                                                        String tappedUrl)
        throws InterruptedException {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        long deadline = System.currentTimeMillis() + DIALOG_TIMEOUT_MILLIS;
        AtomicReference<String> openedUrl = new AtomicReference<>(null);
        while (System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync();
            scenario.onActivity(activity -> {
                TermuxBrowserController browserController = activity.getTermuxBrowserController();
                if (browserController == null || !browserController.isBrowserVisible()) return;
                BrowserTab tab = browserController.getActiveTab();
                openedUrl.set(tab == null ? null : tab.getUrl());
            });
            if (tappedUrl.equals(openedUrl.get())) {
                return;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        throw new AssertionError("a tap on the URL in the owner call dialog did not open it in the "
            + "in-app browser; the browser showed " + openedUrl.get());
    }

    private ActivityScenario<TermuxActivity> launchWithACallingSession() throws Exception {
        scenario = ActivityScenario.launch(TermuxActivity.class);
        awaitServiceConnected(scenario);

        scenario.onActivity(activity -> {
            activity.getPreferences().setSessionDefinitionUrl(server.indexUrl());
            activity.loadSessionsFromDefinition();
        });
        awaitLoadedEntries(scenario);

        scenario.onActivity(activity -> {
            TermuxTerminalSessionActivityClient sessionClient =
                activity.getTermuxTerminalSessionClient();
            assertNotNull(sessionClient);
            sessionClient.addNewSession(true, SESSION_URL);
        });
        awaitRunningDisplayedSession(scenario);

        scenario.onActivity(activity -> {
            SessionNewActivityStore store = activity.getSessionNewActivityStore();
            assertNotNull(store);
            store.recordExplicitCall(SESSION_URL, System.currentTimeMillis(), CALL_REASON,
                CALL_REASON + "-" + System.nanoTime());
            activity.showUnansweredOwnerCallsOfDisplayedSession();
        });
        return scenario;
    }

    private static void assertDialogMatchesTheSpecification(
        ActivityScenario<TermuxActivity> scenario) {
        scenario.onActivity(activity -> {
            View dialog = activity.findViewById(R.id.owner_call_dialog);
            View terminalArea = activity.getTerminalView();
            assertEquals(View.VISIBLE, dialog.getVisibility());
            assertEquals(OLDEST_CALL_BODY, textOf(activity, R.id.owner_call_dialog_body));
            assertTrue("the elapsed time since the owner was called must be shown",
                textOf(activity, R.id.owner_call_dialog_relative_time).matches("\\d+[smh]"));
            assertEquals("the dialog must occupy a quarter of the screen height",
                activity.getResources().getDisplayMetrics().heightPixels / QUARTER_OF_THE_SCREEN,
                dialog.getHeight());
            assertEquals("the dialog must span the terminal area width",
                terminalArea.getWidth(), dialog.getWidth());
            assertTrue("terminal rows must stay readable below the dialog",
                dialog.getBottom() < terminalArea.getBottom());
        });
    }

    private static void awaitDialogShowing(ActivityScenario<TermuxActivity> scenario,
                                           String expectedPosition) throws InterruptedException {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        long deadline = System.currentTimeMillis() + DIALOG_TIMEOUT_MILLIS;
        AtomicReference<String> position = new AtomicReference<>("");
        AtomicInteger laidOutHeight = new AtomicInteger();
        while (System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync();
            scenario.onActivity(activity -> {
                View dialog = activity.findViewById(R.id.owner_call_dialog);
                position.set(dialog.getVisibility() == View.VISIBLE
                    ? textOf(activity, R.id.owner_call_dialog_position) : "");
                laidOutHeight.set(dialog.getHeight());
            });
            if (expectedPosition.equals(position.get()) && laidOutHeight.get() > 0) {
                return;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        throw new AssertionError("the owner call dialog never showed " + expectedPosition
            + " over the terminal; it showed \"" + position.get() + "\" laid out at "
            + laidOutHeight.get() + " pixels high, and the owner call server reported "
            + server().describeFailure());
    }

    private static void awaitDialogGone(ActivityScenario<TermuxActivity> scenario)
        throws InterruptedException {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        long deadline = System.currentTimeMillis() + DIALOG_TIMEOUT_MILLIS;
        AtomicInteger visibility = new AtomicInteger(View.VISIBLE);
        while (System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync();
            scenario.onActivity(activity -> visibility.set(
                activity.findViewById(R.id.owner_call_dialog).getVisibility()));
            if (visibility.get() == View.GONE) {
                return;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        throw new AssertionError("the owner call dialog stayed on screen after the owner replied");
    }

    private static void awaitIndicatorGone(ActivityScenario<TermuxActivity> scenario)
        throws InterruptedException {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        long deadline = System.currentTimeMillis() + DIALOG_TIMEOUT_MILLIS;
        AtomicInteger visibility = new AtomicInteger(View.VISIBLE);
        while (System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync();
            scenario.onActivity(activity -> visibility.set(
                activity.findViewById(R.id.owner_call_pending_indicator).getVisibility()));
            if (visibility.get() == View.GONE) {
                return;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        throw new AssertionError(
            "the pending indicator stayed visible after the owner calls were cleared");
    }

    private void awaitDeleteRequest() throws InterruptedException {
        long deadline = System.currentTimeMillis() + DIALOG_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            if (!server.deletedPaths().isEmpty()) {
                return;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        throw new AssertionError("the app never asked the server to delete the answered owner "
            + "calls; the server reported " + server.describeFailure());
    }

    private static void setOrientation(ActivityScenario<TermuxActivity> scenario,
                                       int requestedOrientation, int expectedOrientation)
        throws InterruptedException {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        scenario.onActivity(activity -> activity.setRequestedOrientation(requestedOrientation));
        long deadline = System.currentTimeMillis() + DIALOG_TIMEOUT_MILLIS;
        AtomicInteger orientation = new AtomicInteger();
        while (System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync();
            scenario.onActivity(activity ->
                orientation.set(activity.getResources().getConfiguration().orientation));
            if (orientation.get() == expectedOrientation) {
                return;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        throw new AssertionError("the device never reached the requested orientation "
            + expectedOrientation + "; it stayed at " + orientation.get());
    }

    private static void awaitServiceConnected(ActivityScenario<TermuxActivity> scenario)
        throws InterruptedException {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        long deadline = System.currentTimeMillis() + SERVICE_READY_TIMEOUT_MILLIS;
        AtomicBoolean ready = new AtomicBoolean();
        while (System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync();
            scenario.onActivity(activity -> ready.set(activity.getTermuxService() != null
                && activity.getTermuxTerminalSessionClient() != null
                && activity.getSessionNewActivityStore() != null));
            if (ready.get()) {
                return;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        throw new AssertionError("the termux service did not connect within "
            + SERVICE_READY_TIMEOUT_MILLIS + "ms");
    }

    private void awaitLoadedEntries(ActivityScenario<TermuxActivity> scenario)
        throws InterruptedException {
        long deadline = System.currentTimeMillis() + SERVICE_READY_TIMEOUT_MILLIS;
        AtomicBoolean loaded = new AtomicBoolean();
        while (System.currentTimeMillis() < deadline) {
            scenario.onActivity(activity ->
                loaded.set(!activity.getSessionDefinitionEntries().isEmpty()));
            if (loaded.get()) {
                return;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        throw new AssertionError("the session definition document served over http must reach the "
            + "app, but the owner call server reported " + server.describeFailure());
    }

    private static void awaitRunningDisplayedSession(ActivityScenario<TermuxActivity> scenario)
        throws InterruptedException {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        long deadline = System.currentTimeMillis() + SESSION_READY_TIMEOUT_MILLIS;
        AtomicBoolean running = new AtomicBoolean();
        while (System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync();
            scenario.onActivity(activity -> {
                TerminalSession session = activity.getCurrentSession();
                running.set(session != null && SESSION_URL.equals(session.mSessionName)
                    && session.isRunning() && session.getEmulator() != null);
            });
            if (running.get()) {
                return;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        throw new AssertionError("the session the owner call belongs to never started");
    }

    private static File captureScreenshot(ActivityScenario<TermuxActivity> scenario,
                                          String fileName) {
        AtomicReference<File> written = new AtomicReference<>();
        scenario.onActivity(activity -> written.set(renderScreenshot(activity, fileName)));
        return written.get();
    }

    private static File renderScreenshot(Activity activity, String fileName) {
        View root = activity.findViewById(R.id.activity_termux_root_relative_layout);
        Bitmap bitmap = Bitmap.createBitmap(Math.max(1, root.getWidth()),
            Math.max(1, root.getHeight()), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(TERMINAL_BACKGROUND_COLOR);
        root.draw(canvas);
        File output = new File(sharedScreenshotDirectory(), fileName);
        try (FileOutputStream stream = new FileOutputStream(output)) {
            assertTrue("PNG compression must succeed for " + fileName,
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        return output;
    }

    private static File sharedScreenshotDirectory() {
        File directory = new File(Environment.getExternalStorageDirectory(), SCREENSHOT_DIRECTORY_NAME);
        if (!directory.exists()) {
            assertTrue("shared screenshot directory must be creatable so screenshots reach the "
                + "directory the workflow pulls", directory.mkdirs());
        }
        return directory;
    }

    private static String textOf(Activity activity, int viewId) {
        return ((TextView) activity.findViewById(viewId)).getText().toString();
    }

    private static LocalOwnerCallServer server() {
        return LocalOwnerCallServer.runningServer();
    }
}
