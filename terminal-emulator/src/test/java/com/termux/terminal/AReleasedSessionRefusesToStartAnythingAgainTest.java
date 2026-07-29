package com.termux.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

public class AReleasedSessionRefusesToStartAnythingAgainTest {

    private static final int NO_SHELL_PROCESS = -1;
    private static final int TERMINAL_COLUMNS = 80;
    private static final int TERMINAL_ROWS = 24;
    private static final int TERMINAL_CELL_WIDTH_PIXELS = 12;
    private static final int TERMINAL_CELL_HEIGHT_PIXELS = 24;

    private TerminalSession session;

    @Before
    public void setUp() throws Exception {
        session = new TerminalSession(null, null, null, null, null,
            new SilentTerminalSessionClient());
        session.mSessionName = "session-the-owner-hid";
        setShellProcessId(NO_SHELL_PROCESS);
        session.releaseRuntimeResources();
    }

    @Test
    public void aReleasedSessionRefusesASizeUpdateInsteadOfForkingAnotherShell() {
        session.updateSize(TERMINAL_COLUMNS, TERMINAL_ROWS, TERMINAL_CELL_WIDTH_PIXELS,
            TERMINAL_CELL_HEIGHT_PIXELS);

        assertNull("a released session holds no terminal emulator, and a size update that finds none "
                + "must refuse rather than build one, because building one forks a brand new shell "
                + "process for a session the owner hid, which is exactly what hiding exists to prevent",
            session.getEmulator());
        assertFalse("refusing the size update must leave the session with no shell process; a session "
                + "that reports a running shell again also takes back a slot under the session cap",
            session.isRunning());
        assertEquals("the pseudo-teletype file descriptor must stay closed, otherwise the refusal only "
                + "hid the fork rather than preventing it", TerminalSession.NO_TERMINAL_FILE_DESCRIPTOR,
            terminalFileDescriptor());
    }

    @Test
    public void aReleasedSessionRefusesAResetInsteadOfFailingOnItsAbsentEmulator() {
        session.reset();

        assertNull("a reset of a released session must refuse in the same way a size update does, so "
                + "the two paths out of a released session behave alike rather than one refusing and "
                + "the other failing on the emulator that release removed", session.getEmulator());
    }

    private int terminalFileDescriptor() {
        try {
            Field terminalFileDescriptor =
                TerminalSession.class.getDeclaredField("mTerminalFileDescriptor");
            terminalFileDescriptor.setAccessible(true);
            return (Integer) terminalFileDescriptor.get(session);
        } catch (ReflectiveOperationException failedToReadTheFileDescriptor) {
            throw new AssertionError(failedToReadTheFileDescriptor);
        }
    }

    private void setShellProcessId(int shellProcessId) throws Exception {
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(session, shellProcessId);
    }

    private static final class SilentTerminalSessionClient implements TerminalSessionClient {

        @Override
        public void onTextChanged(@NonNull TerminalSession changedSession) {
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
        public void onSpeakNotification(@NonNull TerminalSession session, String text) {
        }

        @Override
        public void onColorsChanged(@NonNull TerminalSession session) {
        }

        @Override
        public void onGenuineOutput(@NonNull TerminalSession session) {
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
        public void logError(String logTag, String message) {
        }

        @Override
        public void logWarn(String logTag, String message) {
        }

        @Override
        public void logInfo(String logTag, String message) {
        }

        @Override
        public void logDebug(String logTag, String message) {
        }

        @Override
        public void logVerbose(String logTag, String message) {
        }

        @Override
        public void logStackTraceWithMessage(String logTag, String message, Exception e) {
        }

        @Override
        public void logStackTrace(String logTag, Exception e) {
        }
    }
}
