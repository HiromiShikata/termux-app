package com.termux.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
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
public class SessionAttachedBeforeTheViewHasAMeasuredSizeTest {

    private static final int VIEW_WIDTH_PIXELS = 1080;

    private static final int VIEW_HEIGHT_PIXELS = 1920;

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

        List<Integer> paintedFills = drawTheViewAndRecordTheColorsItFillsWith();

        assertEquals("a view that has never been given a session has nothing to render, and the opaque "
                + "background fill is what keeps it from showing whatever was left in the drawing "
                + "buffer, so making an attached session recover from a missing emulator must not "
                + "remove that fill", 1, paintedFills.size());
        assertEquals("the background fill of a view with no session must stay the opaque black the "
                + "terminal has always painted, so the change is confined to the attached-session case",
            OPAQUE_BLACK, paintedFills.get(0).intValue());
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

    private List<Integer> drawTheViewAndRecordTheColorsItFillsWith() {
        Bitmap target = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
            Bitmap.Config.ARGB_8888);
        FillRecordingCanvas canvas = new FillRecordingCanvas(target);
        view.draw(canvas);
        return canvas.recordedFills();
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

    private static final class FillRecordingCanvas extends Canvas {

        private final List<Integer> fills = new ArrayList<>();

        private FillRecordingCanvas(@NonNull Bitmap target) {
            super(target);
        }

        @Override
        public void drawColor(int color) {
            fills.add(color);
            super.drawColor(color);
        }

        private List<Integer> recordedFills() {
            return fills;
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
