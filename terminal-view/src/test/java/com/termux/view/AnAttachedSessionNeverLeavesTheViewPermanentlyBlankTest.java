package com.termux.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AnAttachedSessionNeverLeavesTheViewPermanentlyBlankTest {

    private static final int VIEW_WIDTH_PIXELS = 1080;

    private static final int VIEW_HEIGHT_PIXELS = 1920;

    private static final int RELAID_OUT_VIEW_WIDTH_PIXELS = 1920;

    private static final int RELAID_OUT_VIEW_HEIGHT_PIXELS = 1080;

    private static final int TERMINAL_TEXT_SIZE = 12;

    private static final int SESSION_TRANSCRIPT_ROWS = 2000;

    private static final int SESSION_EMULATOR_COLUMNS = 80;

    private static final int SESSION_EMULATOR_ROWS = 24;

    private static final float FONT_WIDTH_PIXELS = 10.0f;

    private static final int FONT_LINE_SPACING_PIXELS = 20;

    private static final int FONT_LINE_SPACING_AND_ASCENT_PIXELS = 15;

    private static final int OPAQUE_BLACK = 0xFF000000;

    private TerminalView view;

    private TerminalSession session;

    @Before
    public void setUp() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        view = new TerminalView(context, null);
        view.setTerminalViewClient(new SilentTerminalViewClient());
        view.setTextSize(TERMINAL_TEXT_SIZE);
        giveTheViewDeviceLikeFontMetrics(view);
        session = aLiveSessionAlreadyHoldingATerminalEmulator();
    }

    @Test
    public void aScreenUpdateForAnAttachedSessionWithoutAnEmulatorBuildsOneInsteadOfBeingDropped() {
        assertTheViewHasNoMeasuredSize();

        view.attachSession(session);
        view.onScreenUpdated();

        assertNotNull("the session is attached and its shell is producing output, so the screen update "
                + "that carries that output is the owner's only remaining path back from the opaque "
                + "black fill that is painted whenever the view holds no emulator; dropping the update "
                + "leaves the view black with the emulator still absent and nothing at all bounding how "
                + "long that lasts, because no timer, retry or watchdog exists and every further update "
                + "from the same session is dropped by the same guard, so the update must leave the view "
                + "holding the emulator of the session it is displaying",
            view.mEmulator);
        assertEquals("the emulator the update leaves behind must be the one the attached session is "
                + "writing its output into, because an emulator that belongs to anything else renders "
                + "a screen the owner's shell never produced", session.getEmulator(), view.mEmulator);
    }

    @Test
    public void aSessionAttachedBeforeTheViewHasAMeasuredSizeIsRenderedOnceTheViewObtainsOne() {
        assertTheViewHasNoMeasuredSize();

        view.attachSession(session);
        giveTheViewASize();

        assertNotNull("a session attached while the view still reports zero width and zero height "
                + "cannot have its emulator built at attachment time, so the attachment stays owed; "
                + "once the view obtains a usable size that owed attachment must be completed without "
                + "the owner doing anything and without the session being attached a second time, "
                + "otherwise the view keeps painting the opaque black fill over a session that is "
                + "running", view.mEmulator);
        assertEquals("completing the owed attachment must adopt the emulator of the session the view "
                + "was asked to display, not some other emulator, otherwise the owner is shown the "
                + "output of a session they did not switch to", session.getEmulator(), view.mEmulator);
    }

    @Test
    public void aViewWithNoSessionAttachedStillPaintsItsBackgroundFill() {
        giveTheViewASize();

        List<Integer> paintedFills = drawTheView().recordedFills();

        assertEquals("a view that has never been given a session has nothing to render, and the opaque "
                + "background fill is what keeps it from showing whatever was left in the drawing "
                + "buffer, so making an attached session recover from a missing emulator must not "
                + "remove that fill", 1, paintedFills.size());
        assertEquals("the background fill of a view with no session must stay the opaque black the "
                + "terminal has always painted, so the change is confined to the attached-session case",
            OPAQUE_BLACK, paintedFills.get(0).intValue());
    }

    @Test
    public void switchingToASessionWhoseRuntimeResourcesWereReleasedLeavesTheViewAbleToDisplaySomething()
            throws Exception {
        displayALiveSessionAtAUsableSize();
        TerminalSession releasedSession = aSessionWhoseRuntimeResourcesWereReleased();

        view.attachSession(releasedSession);

        assertTheViewCanStillDisplaySomething("the owner switched to a session whose runtime resources "
            + "had already been released");
    }

    @Test
    public void aLayoutPassAfterSwitchingToSuchASessionLeavesTheViewAbleToDisplaySomething()
            throws Exception {
        displayALiveSessionAtAUsableSize();
        TerminalSession releasedSession = aSessionWhoseRuntimeResourcesWereReleased();
        view.attachSession(releasedSession);

        relayOutTheViewAtADifferentSize();

        assertTheViewCanStillDisplaySomething("a later layout pass reached the view after the owner had "
            + "switched to a session whose runtime resources had already been released");
    }

    private void assertTheViewCanStillDisplaySomething(@NonNull String situation) {
        boolean holdsAnEmulatorToRender = view.mEmulator != null;
        boolean paintedMoreThanTheBareBlackFill =
            drawTheView().paintedAnythingOtherThanASingleOpaqueBlackFill();

        assertTrue("a released session has no terminal emulator and no shell process behind it, and "
                + "TerminalSession.updateSize returns without building one, so the view's own "
                + "updateSize assigns null to the emulator it is about to render and onDraw then fills "
                + "the whole view with opaque black; nothing at all takes the view out of that state, "
                + "because every later layout pass repeats the same assignment and no screen update can "
                + "arrive from a session that has neither an emulator nor a process, which is the "
                + "terminal going black on a session switch and staying black that the owner reported; "
                + situation + ", so the view must not be left with nothing it can display, whether it "
                + "refuses to bind a session it cannot render, recovers by obtaining a session it can, "
                + "or surfaces the state instead of silently painting black, yet the view holds no "
                + "emulator and its draw produced nothing but the single opaque black fill",
            holdsAnEmulatorToRender || paintedMoreThanTheBareBlackFill);
        assertTheViewRendersTheSessionItReportsAsDisplayed();
    }

    private void assertTheViewRendersTheSessionItReportsAsDisplayed() {
        if (view.mEmulator == null) return;
        TerminalSession displayedSession = view.getCurrentSession();
        assertEquals("keeping the view out of the permanently blank state must not be done by rendering "
                + "one session while reporting another as the displayed one, because the owner would "
                + "then be typing into a session other than the screen in front of them, and the "
                + "session name overlay, the session list and the toolbar all read the displayed "
                + "session from this view; so whatever emulator the view renders must belong to the "
                + "session the view reports as displayed",
            displayedSession == null ? null : displayedSession.getEmulator(), view.mEmulator);
    }

    private void displayALiveSessionAtAUsableSize() {
        giveTheViewASize();
        view.attachSession(session);
        assertNotNull("these scenarios start from a terminal the owner is already working in, so the "
                + "view must hold the live session's emulator before the switch is made",
            view.mEmulator);
    }

    private void assertTheViewHasNoMeasuredSize() {
        assertTrue("this scenario only exists while the view reports no measured size, which is the "
                + "state a freshly constructed view is in until a layout pass reaches it, so the width "
                + "and height must both be zero before the session is attached; the view reported "
                + view.getWidth() + " by " + view.getHeight(),
            view.getWidth() == 0 && view.getHeight() == 0);
    }

    private void giveTheViewASize() {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(VIEW_WIDTH_PIXELS, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(VIEW_HEIGHT_PIXELS, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, VIEW_WIDTH_PIXELS, VIEW_HEIGHT_PIXELS);
        assertTrue("the step that gives the view a size must actually give it one, otherwise the "
                + "assertion that follows is met or missed for the wrong reason; the view reported "
                + view.getWidth() + " by " + view.getHeight(),
            view.getWidth() == VIEW_WIDTH_PIXELS && view.getHeight() == VIEW_HEIGHT_PIXELS);
    }

    private void relayOutTheViewAtADifferentSize() {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(RELAID_OUT_VIEW_WIDTH_PIXELS, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(RELAID_OUT_VIEW_HEIGHT_PIXELS, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, RELAID_OUT_VIEW_WIDTH_PIXELS, RELAID_OUT_VIEW_HEIGHT_PIXELS);
        assertTrue("the later layout pass must actually change the view's dimensions, because a layout "
                + "to the size the view already has never reaches onSizeChanged and would leave this "
                + "scenario indistinguishable from the one without a layout pass; the view reported "
                + view.getWidth() + " by " + view.getHeight(),
            view.getWidth() == RELAID_OUT_VIEW_WIDTH_PIXELS
                && view.getHeight() == RELAID_OUT_VIEW_HEIGHT_PIXELS);
    }

    private PaintOperationRecordingCanvas drawTheView() {
        Bitmap target = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
            Bitmap.Config.ARGB_8888);
        PaintOperationRecordingCanvas canvas = new PaintOperationRecordingCanvas(target);
        view.draw(canvas);
        return canvas;
    }

    private TerminalSession aLiveSessionAlreadyHoldingATerminalEmulator() throws Exception {
        TerminalSession created = new TerminalSession("/system/bin/sh", "/", new String[0],
            new String[0], SESSION_TRANSCRIPT_ROWS, new SilentTerminalSessionClient());
        try {
            created.initializeEmulator(SESSION_EMULATOR_COLUMNS, SESSION_EMULATOR_ROWS,
                (int) FONT_WIDTH_PIXELS, FONT_LINE_SPACING_PIXELS);
        } catch (LinkageError deviceOnlyNativeSubprocessLibraryAbsent) {
            assertItIsTheOnlyAbsenceThisRunTolerates(deviceOnlyNativeSubprocessLibraryAbsent);
        }
        assertNotNull("every scenario here starts from a session that already holds a terminal "
                + "emulator, which is the state a session is left in by the startup session load "
                + "before the displayed view is ever laid out", created.getEmulator());
        makeThePseudoTeletypeResizeInertBecauseOnlyADeviceCanPerformIt(created);
        return created;
    }

    private TerminalSession aSessionWhoseRuntimeResourcesWereReleased() {
        TerminalSession created = new TerminalSession("/system/bin/sh", "/", new String[0],
            new String[0], SESSION_TRANSCRIPT_ROWS, new SilentTerminalSessionClient());
        try {
            created.initializeEmulator(SESSION_EMULATOR_COLUMNS, SESSION_EMULATOR_ROWS,
                (int) FONT_WIDTH_PIXELS, FONT_LINE_SPACING_PIXELS);
        } catch (LinkageError deviceOnlyNativeSubprocessLibraryAbsent) {
            assertItIsTheOnlyAbsenceThisRunTolerates(deviceOnlyNativeSubprocessLibraryAbsent);
        }
        created.releaseRuntimeResources();
        assertNull("this scenario is about a session the owner can be switched to after its runtime "
                + "resources have been released, and releasing them is what removes the terminal "
                + "emulator, so the session must hold none", created.getEmulator());
        assertFalse("a released session holds no shell process either, so no output can arrive from it "
                + "to repair a view that fails to render it", created.isRunning());
        return created;
    }

    private void makeThePseudoTeletypeResizeInertBecauseOnlyADeviceCanPerformIt(
            @NonNull TerminalSession target) throws Exception {
        Field runtimeResourcesReleased =
            TerminalSession.class.getDeclaredField("mRuntimeResourcesReleased");
        runtimeResourcesReleased.setAccessible(true);
        runtimeResourcesReleased.set(target, true);
    }

    private void assertItIsTheOnlyAbsenceThisRunTolerates(@NonNull LinkageError error) {
        Throwable rootCause = error;
        while (rootCause.getCause() != null) rootCause = rootCause.getCause();
        String absorbedFailure = rootCause.getClass().getName() + ": " + rootCause.getMessage();
        assertTrue("a Java virtual machine run can only absorb the absence of the device-only native "
                + "subprocess library that TerminalSession.initializeEmulator loads after it has "
                + "already constructed the terminal emulator; every other failure is a real one and "
                + "must surface instead of being discarded, yet this run absorbed " + absorbedFailure,
            absorbedFailure.contains("UnsatisfiedLinkError")
                || absorbedFailure.contains("com.termux.terminal.JNI"));
    }

    private void giveTheViewDeviceLikeFontMetrics(@NonNull TerminalView target) throws Exception {
        Field rendererField = TerminalView.class.getDeclaredField("mRenderer");
        rendererField.setAccessible(true);
        Object renderer = rendererField.get(target);
        setField(renderer, "mFontWidth", FONT_WIDTH_PIXELS);
        setField(renderer, "mFontLineSpacing", FONT_LINE_SPACING_PIXELS);
        setField(renderer, "mFontLineSpacingAndAscent", FONT_LINE_SPACING_AND_ASCENT_PIXELS);
    }

    private void setField(@NonNull Object target, @NonNull String fieldName, @NonNull Object value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class PaintOperationRecordingCanvas extends Canvas {

        private final List<Integer> fills = new ArrayList<>();

        private int operationsThatAreNotAFill;

        private PaintOperationRecordingCanvas(@NonNull Bitmap target) {
            super(target);
        }

        @Override
        public void drawColor(int color) {
            fills.add(color);
            super.drawColor(color);
        }

        @Override
        public void drawColor(int color, @NonNull PorterDuff.Mode mode) {
            fills.add(color);
            super.drawColor(color, mode);
        }

        @Override
        public void drawRect(float left, float top, float right, float bottom,
                            @NonNull Paint paint) {
            operationsThatAreNotAFill++;
            super.drawRect(left, top, right, bottom, paint);
        }

        @Override
        public void drawRect(@NonNull RectF rect, @NonNull Paint paint) {
            operationsThatAreNotAFill++;
            super.drawRect(rect, paint);
        }

        @Override
        public void drawRoundRect(@NonNull RectF rect, float rx, float ry, @NonNull Paint paint) {
            operationsThatAreNotAFill++;
            super.drawRoundRect(rect, rx, ry, paint);
        }

        @Override
        public void drawText(@NonNull String text, float x, float y, @NonNull Paint paint) {
            operationsThatAreNotAFill++;
            super.drawText(text, x, y, paint);
        }

        @Override
        public void drawText(@NonNull CharSequence text, int start, int end, float x, float y,
                            @NonNull Paint paint) {
            operationsThatAreNotAFill++;
            super.drawText(text, start, end, x, y, paint);
        }

        @Override
        public void drawTextRun(@NonNull char[] text, int index, int count, int contextIndex,
                               int contextCount, float x, float y, boolean isRtl,
                               @NonNull Paint paint) {
            operationsThatAreNotAFill++;
            super.drawTextRun(text, index, count, contextIndex, contextCount, x, y, isRtl, paint);
        }

        @Override
        public void drawTextRun(@NonNull CharSequence text, int start, int end, int contextStart,
                               int contextEnd, float x, float y, boolean isRtl,
                               @NonNull Paint paint) {
            operationsThatAreNotAFill++;
            super.drawTextRun(text, start, end, contextStart, contextEnd, x, y, isRtl, paint);
        }

        @Override
        public void drawBitmap(@NonNull Bitmap bitmap, float left, float top,
                              @Nullable Paint paint) {
            operationsThatAreNotAFill++;
            super.drawBitmap(bitmap, left, top, paint);
        }

        private List<Integer> recordedFills() {
            return fills;
        }

        private boolean paintedAnythingOtherThanASingleOpaqueBlackFill() {
            if (operationsThatAreNotAFill > 0) return true;
            if (fills.size() != 1) return true;
            return fills.get(0) != OPAQUE_BLACK;
        }
    }

    private static final class SilentTerminalViewClient implements TerminalViewClient {

        @Override
        public float onScale(float scale) {
            return scale;
        }

        @Override
        public void onSingleTapUp(MotionEvent e) {
        }

        @Override
        public boolean shouldBackButtonBeMappedToEscape() {
            return false;
        }

        @Override
        public boolean shouldEnforceCharBasedInput() {
            return false;
        }

        @Override
        public boolean shouldUseCtrlSpaceWorkaround() {
            return false;
        }

        @Override
        public boolean isTerminalViewSelected() {
            return true;
        }

        @Override
        public void copyModeChanged(boolean copyMode) {
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent e, TerminalSession session) {
            return false;
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent e) {
            return false;
        }

        @Override
        public boolean onLongPress(MotionEvent event) {
            return false;
        }

        @Override
        public boolean readControlKey() {
            return false;
        }

        @Override
        public boolean readAltKey() {
            return false;
        }

        @Override
        public boolean readShiftKey() {
            return false;
        }

        @Override
        public boolean readFnKey() {
            return false;
        }

        @Override
        public boolean onCodePoint(int codePoint, boolean ctrlDown, TerminalSession session) {
            return false;
        }

        @Override
        public void onEmulatorSet() {
        }

        @Override
        public void logError(String tag, String message) {
        }

        @Override
        public void logWarn(String tag, String message) {
        }

        @Override
        public void logInfo(String tag, String message) {
        }

        @Override
        public void logDebug(String tag, String message) {
        }

        @Override
        public void logVerbose(String tag, String message) {
        }

        @Override
        public void logStackTraceWithMessage(String tag, String message, Exception e) {
        }

        @Override
        public void logStackTrace(String tag, Exception e) {
        }
    }

    private static final class SilentTerminalSessionClient implements TerminalSessionClient {

        @Override
        public void onTextChanged(@NonNull TerminalSession changedSession) {
        }

        @Override
        public void onGenuineOutput(@NonNull TerminalSession changedSession) {
        }

        @Override
        public void onTitleChanged(@NonNull TerminalSession changedSession) {
        }

        @Override
        public void onSessionFinished(@NonNull TerminalSession finishedSession) {
        }

        @Override
        public void onCopyTextToClipboard(@NonNull TerminalSession session, String text) {
        }

        @Override
        public void onPasteTextFromClipboard(@Nullable TerminalSession session) {
        }

        @Override
        public void onBell(@NonNull TerminalSession session) {
        }

        @Override
        public void onSpeakNotification(@NonNull TerminalSession session, @NonNull String text) {
        }

        @Override
        public void onColorsChanged(@NonNull TerminalSession session) {
        }

        @Override
        public void onTerminalCursorStateChange(boolean state) {
        }

        @Override
        public void setTerminalShellPid(@NonNull TerminalSession session, int pid) {
        }

        @Override
        public Integer getTerminalCursorStyle() {
            return null;
        }

        @Override
        public void logError(String tag, String message) {
        }

        @Override
        public void logWarn(String tag, String message) {
        }

        @Override
        public void logInfo(String tag, String message) {
        }

        @Override
        public void logDebug(String tag, String message) {
        }

        @Override
        public void logVerbose(String tag, String message) {
        }

        @Override
        public void logStackTraceWithMessage(String tag, String message, Exception e) {
        }

        @Override
        public void logStackTrace(String tag, Exception e) {
        }
    }
}
