package com.termux.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ShellExitBeforeTheEmulatorExistsIsStillRecordedTest {

    private static final int MESSAGE_PROCESS_EXITED = 4;
    private static final int MESSAGE_NEW_INPUT = 1;
    private static final int SHELL_PROCESS_ID = 4242;
    private static final int SHELL_EXIT_CODE = 3;

    private RecordingTerminalSessionClient client;
    private TerminalSession session;

    @Before
    public void setUp() throws Exception {
        client = new RecordingTerminalSessionClient();
        session = new TerminalSession(null, null, null, null, null, client);
        session.mSessionName = "session-whose-shell-exits-before-its-emulator-exists";
        setShellProcessId(SHELL_PROCESS_ID);
    }

    @Test
    public void aShellThatExitsBeforeTheEmulatorExistsStopsReportingItselfAsRunning() {
        assertNull("the arrangement must start with no emulator, which is the state a session is in "
                + "between being created and being sized, and the state a released session stays in",
            session.getEmulator());
        assertTrue("the arrangement must start with a session that reports a running shell, otherwise "
            + "this assertion cannot show the exit being recorded", session.isRunning());

        deliverProcessExited(SHELL_EXIT_CODE);

        assertFalse("a shell that exits before its emulator exists must still be recorded as ended; "
                + "while it is not, the session reports a running shell for ever, so it keeps a slot in "
                + "the session cap and every selection that skips finished sessions keeps selecting it",
            session.isRunning());
        assertEquals("the exit status must be recorded, because that is what tells the rest of the "
            + "application how the shell ended", SHELL_EXIT_CODE, session.getExitStatus());
    }

    @Test
    public void aShellThatExitsBeforeTheEmulatorExistsClosesItsChannelsAndTellsItsClient() {
        deliverProcessExited(SHELL_EXIT_CODE);

        assertFalse("the queue the writer thread drains into the shell process must be closed when the "
                + "shell ends, otherwise the pseudo-teletype file descriptor and both queues are held "
                + "for the lifetime of the application",
            session.mTerminalToProcessIOQueue.write("keystroke".getBytes(StandardCharsets.UTF_8), 0, 9));
        assertFalse("the queue the reader thread fills must be closed when the shell ends for the same "
                + "reason",
            session.mProcessToTerminalIOQueue.write("output".getBytes(StandardCharsets.UTF_8), 0, 6));
        assertEquals("the client must be told the session finished even when no emulator was ever "
                + "created, because that notification is what removes the session and updates the list; "
                + "without it a dead session sits in the list looking alive",
            1, client.finishedSessions.size());
    }

    @Test
    public void ordinaryShellOutputArrivingBeforeTheEmulatorExistsIsStillNotRenderedAndChangesNothing() {
        session.mProcessToTerminalIOQueue.write("output".getBytes(StandardCharsets.UTF_8), 0, 6);

        deliverMessage(MESSAGE_NEW_INPUT, null);

        assertTrue("a session with no emulator has nothing to render into, so ordinary shell output "
                + "must leave it exactly as it was; only the process exit carries bookkeeping that must "
                + "happen with or without an emulator",
            session.isRunning());
        assertEquals("no emulator means no screen update, so the client must not be told the text "
            + "changed", 0, client.textChangedSessions.size());
        assertEquals("a session that has not finished must not be reported as finished",
            0, client.finishedSessions.size());
    }

    @Test
    public void aShellThatExitsAfterItsEmulatorExistsStillWritesTheCompletionNoticeToTheScreen() {
        session.mEmulator = new TerminalEmulator(session, 20, 4, 4, 8, 100, client);

        deliverProcessExited(SHELL_EXIT_CODE);

        assertFalse("recording the exit without an emulator must not change what happens when there is "
            + "one; the shell must still be recorded as ended", session.isRunning());
        assertEquals("the client must still be told the session finished",
            1, client.finishedSessions.size());
        assertTrue("the completion notice must still reach the screen when there is a screen to write "
                + "it to, otherwise the owner loses the line that tells him the shell ended and how; the "
                + "screen held " + screenText(),
            screenText().contains("[Process completed (code " + SHELL_EXIT_CODE + ") - press Enter]"));
    }

    private String screenText() {
        return session.mEmulator.getScreen().getTranscriptText();
    }

    private void deliverProcessExited(int exitCode) {
        deliverMessage(MESSAGE_PROCESS_EXITED, exitCode);
    }

    private void deliverMessage(int what, Object payload) {
        Message message = new Message();
        message.what = what;
        message.obj = payload;
        session.mMainThreadHandler.handleMessage(message);
    }

    private void setShellProcessId(int shellProcessId) throws Exception {
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(session, shellProcessId);
    }

    private static final class RecordingTerminalSessionClient implements TerminalSessionClient {

        private final List<TerminalSession> finishedSessions = new ArrayList<>();
        private final List<TerminalSession> textChangedSessions = new ArrayList<>();

        @Override
        public void onTextChanged(@NonNull TerminalSession changedSession) {
            textChangedSessions.add(changedSession);
        }

        @Override
        public void onGenuineOutput(@NonNull TerminalSession changedSession) {
        }

        @Override
        public void onTitleChanged(@NonNull TerminalSession changedSession) {
        }

        @Override
        public void onSessionFinished(@NonNull TerminalSession finishedSession) {
            finishedSessions.add(finishedSession);
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
