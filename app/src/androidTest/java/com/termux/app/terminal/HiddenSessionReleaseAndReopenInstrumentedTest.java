package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class HiddenSessionReleaseAndReopenInstrumentedTest {

    private static final long SERVICE_READY_TIMEOUT_MILLIS = 60_000L;

    private static final long SHELL_START_TIMEOUT_MILLIS = 60_000L;

    private static final long SHELL_OUTPUT_TIMEOUT_MILLIS = 60_000L;

    private static final long SHELL_PROCESS_DISAPPEARANCE_TIMEOUT_MILLIS = 60_000L;

    private static final long POLL_INTERVAL_MILLIS = 100L;

    private static final String SYSTEM_SHELL_PATH = "/system/bin/sh";

    private static final String ROOT_WORKING_DIRECTORY = "/";

    private static final String SESSION_UNDER_TEST_NAME = "release-reopen-session-under-test";

    private static final String SESSION_THE_VIEW_MOVES_TO_NAME = "release-reopen-session-the-view-moves-to";

    private static final String COMMAND_MAKING_THE_SHELL_PRINT_THE_MARKER_BEFORE_RELEASE = "echo REL''MARKA\r";

    private static final String MARKER_ONLY_THE_SHELL_ITSELF_CAN_PRINT_BEFORE_RELEASE = "RELMARKA";

    private static final String COMMAND_MAKING_THE_SHELL_PRINT_THE_MARKER_AFTER_REOPEN = "echo REO''MARKB\r";

    private static final String MARKER_ONLY_THE_SHELL_ITSELF_CAN_PRINT_AFTER_REOPEN = "REOMARKB";

    private ActivityScenario<TermuxActivity> mScenario;

    private String mAutosshCommandBeforeTest;

    @Nullable
    private TerminalSession mSessionUnderTest;

    @Nullable
    private TerminalSession mSessionTheViewMovesTo;

    @Before
    public void launchActivityAndSilenceTheAutomaticReconnect() throws Exception {
        mScenario = ActivityScenario.launch(TermuxActivity.class);
        awaitConditionOnMainThread(SERVICE_READY_TIMEOUT_MILLIS, "the service to connect",
            activity -> activity.getTermuxService() != null && activity.getTerminalView() != null
                && activity.getTermuxTerminalSessionClient() != null && activity.getPreferences() != null);
        mAutosshCommandBeforeTest = readOnMainThread(activity -> activity.getPreferences().getAutosshCommand());
        runOnMainThread(activity -> {
            TermuxAppSharedPreferences preferences = activity.getPreferences();
            preferences.setAutosshCommand("");
            preferences.setSessionDisabled(SESSION_UNDER_TEST_NAME, false);
            preferences.setSessionDisabled(SESSION_THE_VIEW_MOVES_TO_NAME, false);
        });
    }

    @After
    public void removeTheSessionsThisTestCreated() {
        if (mScenario == null) return;
        runOnMainThread(activity -> {
            TermuxAppSharedPreferences preferences = activity.getPreferences();
            if (preferences != null) {
                preferences.setAutosshCommand(mAutosshCommandBeforeTest == null ? "" : mAutosshCommandBeforeTest);
                preferences.setSessionDisabled(SESSION_UNDER_TEST_NAME, false);
                preferences.setSessionDisabled(SESSION_THE_VIEW_MOVES_TO_NAME, false);
            }
            TermuxService service = activity.getTermuxService();
            if (service == null) return;
            removeSession(service, mSessionUnderTest);
            removeSession(service, mSessionTheViewMovesTo);
        });
        mScenario.close();
    }

    @Test
    public void aHiddenSessionTheSweepReleasedReopensWithAShellWhoseInputAndOutputActuallyFlow() throws Exception {
        mSessionUnderTest = createSessionNamed(SESSION_UNDER_TEST_NAME);
        mSessionTheViewMovesTo = createSessionNamed(SESSION_THE_VIEW_MOVES_TO_NAME);
        final TerminalSession sessionUnderTest = mSessionUnderTest;
        final TerminalSession sessionTheViewMovesTo = mSessionTheViewMovesTo;

        openSession(sessionUnderTest);
        awaitShellProcessOf(sessionUnderTest, "the session under test to fork its first shell");
        int shellProcessIdBeforeRelease = readOnMainThread(activity -> sessionUnderTest.getPid());
        assertTrue("the first shell process must exist before the release, "
                + describeProcess(shellProcessIdBeforeRelease),
            processDirectoryOf(shellProcessIdBeforeRelease).exists());

        writeToSession(sessionUnderTest, COMMAND_MAKING_THE_SHELL_PRINT_THE_MARKER_BEFORE_RELEASE);
        awaitTranscriptOf(sessionUnderTest, MARKER_ONLY_THE_SHELL_ITSELF_CAN_PRINT_BEFORE_RELEASE,
            "the first shell to print its marker");
        TerminalEmulator emulatorBeforeRelease = readOnMainThread(activity -> sessionUnderTest.getEmulator());
        assertNotNull("the session under test must hold an emulator before the release", emulatorBeforeRelease);

        openSession(sessionTheViewMovesTo);
        runOnMainThread(activity -> activity.getPreferences().setSessionDisabled(SESSION_UNDER_TEST_NAME, true));
        runOnMainThread(activity ->
            activity.getTermuxTerminalSessionClient().reconnectDeadDefinitionBackedSessionsInBackground());

        assertNull("the sweep must release the emulator of the hidden session",
            readOnMainThread(activity -> sessionUnderTest.getEmulator()));
        assertEquals("the sweep must disown the shell process identifier of the hidden session",
            TerminalSession.NO_SHELL_PROCESS_PID, (int) readOnMainThread(activity -> sessionUnderTest.getPid()));
        assertFalse("the released session must not report itself as running",
            readOnMainThread(activity -> sessionUnderTest.isRunning()));
        awaitDisappearanceOfProcess(shellProcessIdBeforeRelease);

        openSession(sessionUnderTest);
        awaitShellProcessOf(sessionUnderTest, "the reopened session to fork a replacement shell");
        int shellProcessIdAfterReopen = readOnMainThread(activity -> sessionUnderTest.getPid());
        assertNotEquals("the reopened session must own a different shell process than the released one",
            shellProcessIdBeforeRelease, shellProcessIdAfterReopen);
        assertTrue("the replacement shell process must exist after the reopen, "
                + describeProcess(shellProcessIdAfterReopen),
            processDirectoryOf(shellProcessIdAfterReopen).exists());
        assertFalse("reopening the row must clear its hidden mark",
            readOnMainThread(activity ->
                activity.getPreferences().getDisabledSessionNames().contains(SESSION_UNDER_TEST_NAME)));

        writeToSession(sessionUnderTest, COMMAND_MAKING_THE_SHELL_PRINT_THE_MARKER_AFTER_REOPEN);
        awaitTranscriptOf(sessionUnderTest, MARKER_ONLY_THE_SHELL_ITSELF_CAN_PRINT_AFTER_REOPEN,
            "the reopened shell to print its marker through the replaced byte queues");

        TerminalEmulator emulatorAfterReopen = readOnMainThread(activity -> sessionUnderTest.getEmulator());
        assertNotSame("the reopened session must render into a new emulator rather than the released one",
            emulatorBeforeRelease, emulatorAfterReopen);
        assertFalse("the scrollback of the released emulator must not survive into the reopened session",
            transcriptOf(sessionUnderTest).contains(MARKER_ONLY_THE_SHELL_ITSELF_CAN_PRINT_BEFORE_RELEASE));
        assertTrue("the reopened session must report itself as running",
            readOnMainThread(activity -> sessionUnderTest.isRunning()));
    }

    @NonNull
    private TerminalSession createSessionNamed(@NonNull String sessionName) {
        TerminalSession createdSession = readOnMainThread(activity -> {
            TermuxService service = activity.getTermuxService();
            TermuxSession termuxSession = service.createTermuxSession(SYSTEM_SHELL_PATH, new String[0], null,
                ROOT_WORKING_DIRECTORY, true, sessionName);
            return termuxSession == null ? null : termuxSession.getTerminalSession();
        });
        assertNotNull("the service must create a session named " + sessionName, createdSession);
        return createdSession;
    }

    private void openSession(@NonNull TerminalSession session) {
        runOnMainThread(activity ->
            activity.getTermuxTerminalSessionClient().switchToSessionReconnectingIfDead(session));
    }

    private void writeToSession(@NonNull TerminalSession session, @NonNull String input) {
        runOnMainThread(activity -> session.write(input));
    }

    private void awaitShellProcessOf(@NonNull TerminalSession session, @NonNull String description)
        throws Exception {
        awaitConditionOnMainThread(SHELL_START_TIMEOUT_MILLIS, description,
            activity -> session.getPid() > 0 && session.isRunning() && session.getEmulator() != null);
    }

    private void awaitTranscriptOf(@NonNull TerminalSession session, @NonNull String expectedMarker,
                                   @NonNull String description) throws Exception {
        long deadlineMillis = System.currentTimeMillis() + SHELL_OUTPUT_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadlineMillis) {
            if (transcriptOf(session).contains(expectedMarker)) return;
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        fail("Timed out after " + SHELL_OUTPUT_TIMEOUT_MILLIS + "ms waiting for " + description
            + ". Expected the marker \"" + expectedMarker + "\" in the transcript, which was:\n"
            + transcriptOf(session));
    }

    @NonNull
    private String transcriptOf(@NonNull TerminalSession session) {
        String transcript = readOnMainThread(activity -> {
            TerminalEmulator emulator = session.getEmulator();
            return emulator == null ? null : emulator.getScreen().getTranscriptText();
        });
        return transcript == null ? "" : transcript;
    }

    private void awaitDisappearanceOfProcess(int shellProcessId) throws Exception {
        long deadlineMillis = System.currentTimeMillis() + SHELL_PROCESS_DISAPPEARANCE_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadlineMillis) {
            if (!processDirectoryOf(shellProcessId).exists()) return;
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        fail("Timed out after " + SHELL_PROCESS_DISAPPEARANCE_TIMEOUT_MILLIS
            + "ms waiting for the released shell process to be gone, " + describeProcess(shellProcessId));
    }

    @NonNull
    private File processDirectoryOf(int shellProcessId) {
        return new File("/proc/" + shellProcessId);
    }

    @NonNull
    private String describeProcess(int shellProcessId) {
        File statusFile = new File(processDirectoryOf(shellProcessId), "stat");
        if (!statusFile.exists()) {
            return "process " + shellProcessId + " has no /proc entry";
        }
        try (InputStream statusStream = new FileInputStream(statusFile)) {
            ByteArrayOutputStream statusBytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[512];
            int bytesRead;
            while ((bytesRead = statusStream.read(buffer)) > 0) {
                statusBytes.write(buffer, 0, bytesRead);
            }
            return "process " + shellProcessId + " /proc entry reads: " + statusBytes.toString("UTF-8").trim();
        } catch (Exception readFailure) {
            return "process " + shellProcessId + " /proc entry could not be read: " + readFailure;
        }
    }

    private void removeSession(@NonNull TermuxService service, @Nullable TerminalSession session) {
        if (session == null) return;
        session.finishIfRunning();
        service.removeTermuxSession(session);
    }

    private void runOnMainThread(@NonNull MainThreadAction action) {
        mScenario.onActivity(action::run);
    }

    private <T> T readOnMainThread(@NonNull MainThreadReader<T> reader) {
        AtomicReference<T> readValue = new AtomicReference<>();
        mScenario.onActivity(activity -> readValue.set(reader.read(activity)));
        return readValue.get();
    }

    private void awaitConditionOnMainThread(long timeoutMillis, @NonNull String description,
                                            @NonNull MainThreadReader<Boolean> condition) throws Exception {
        long deadlineMillis = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadlineMillis) {
            if (Boolean.TRUE.equals(readOnMainThread(condition))) return;
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        fail("Timed out after " + timeoutMillis + "ms waiting for " + description);
    }

    private interface MainThreadAction {
        void run(@NonNull TermuxActivity activity);
    }

    private interface MainThreadReader<T> {
        T read(@NonNull TermuxActivity activity);
    }
}
