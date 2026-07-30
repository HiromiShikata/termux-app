package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.util.Log;

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
public class SessionReopenAfterRuntimeReleaseInstrumentedTest {

    private static final String LOG_TAG = "SessionReopenAfterRuntimeRelease";

    private static final long SERVICE_READY_TIMEOUT_MILLIS = 60_000L;

    private static final long SHELL_START_TIMEOUT_MILLIS = 60_000L;

    private static final long SHELL_OUTPUT_TIMEOUT_MILLIS = 60_000L;

    private static final long SHELL_DEATH_TIMEOUT_MILLIS = 60_000L;

    private static final long POLL_INTERVAL_MILLIS = 100L;

    private static final String SYSTEM_SHELL_PATH = "/system/bin/sh";

    private static final String ROOT_WORKING_DIRECTORY = "/";

    private static final String SESSION_UNDER_TEST_NAME = "reopen-after-release-session-under-test";

    private static final String SESSION_THE_VIEW_MOVES_TO_NAME = "reopen-after-release-session-the-view-moves-to";

    private static final String COMMAND_MAKING_THE_FIRST_SHELL_PRINT_ITS_MARKER = "echo FIR''STMARK\r";

    private static final String MARKER_ONLY_THE_FIRST_SHELL_CAN_PRINT = "FIRSTMARK";

    private static final String COMMAND_MAKING_THE_REOPENED_SHELL_PRINT_ITS_MARKER = "echo REOPEN''EDMARK\r";

    private static final String MARKER_ONLY_THE_REOPENED_SHELL_CAN_PRINT = "REOPENEDMARK";

    private static final String COMMAND_MAKING_THE_SHELL_EXIT_ON_ITS_OWN = "exit\r";

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
    }

    @Test
    public void aSessionReleasedAfterItsShellExitedOnItsOwnReopensWithAShellWhoseInputAndOutputActuallyFlow()
        throws Exception {
        final TerminalSession sessionUnderTest = createTheSessionsAndOpenTheOneUnderTest();

        int shellProcessIdBeforeTheExit = readOnMainThread(activity -> sessionUnderTest.getPid());
        TerminalEmulator emulatorBeforeTheExit = readOnMainThread(activity -> sessionUnderTest.getEmulator());
        awaitMarkerOf(sessionUnderTest, COMMAND_MAKING_THE_FIRST_SHELL_PRINT_ITS_MARKER,
            MARKER_ONLY_THE_FIRST_SHELL_CAN_PRINT, "the first shell to print its marker");

        moveTheViewToTheOtherSession();
        writeToSession(sessionUnderTest, COMMAND_MAKING_THE_SHELL_EXIT_ON_ITS_OWN);
        awaitDisappearanceOfProcess(shellProcessIdBeforeTheExit);
        awaitConditionOnMainThread(SHELL_DEATH_TIMEOUT_MILLIS, "the session to notice its shell exited",
            activity -> !sessionUnderTest.isRunning());

        assertSame("a session whose shell exited while out of sight must stay in the list so its row "
                + "reopens rather than vanishing from under the owner",
            sessionUnderTest, liveSessionNamed(SESSION_UNDER_TEST_NAME));

        runOnMainThread(activity -> sessionUnderTest.releaseRuntimeResourcesKeepingTheRowReopenable());
        assertNull("releasing the runtime of a row whose shell had already exited must drop its emulator",
            readOnMainThread(activity -> sessionUnderTest.getEmulator()));

        int shellProcessIdAfterTheReopen = reopenAndAwaitAReplacementShell(sessionUnderTest,
            shellProcessIdBeforeTheExit);
        awaitMarkerOf(sessionUnderTest, COMMAND_MAKING_THE_REOPENED_SHELL_PRINT_ITS_MARKER,
            MARKER_ONLY_THE_REOPENED_SHELL_CAN_PRINT,
            "the shell that replaced the one which exited on its own to print its marker through the "
                + "byte queues that exit closed");
        assertReopenedOntoAFreshEmulator(sessionUnderTest, emulatorBeforeTheExit);

        Log.w(LOG_TAG, "shell process " + shellProcessIdBeforeTheExit + " exited on its own and shell process "
            + shellProcessIdAfterTheReopen + " printed " + MARKER_ONLY_THE_REOPENED_SHELL_CAN_PRINT
            + " through the byte queues that replaced the ones its exit closed");
    }

    @Test
    public void aSessionReleasedWhileItsShellWasStillRunningReopensWithAShellWhoseInputAndOutputActuallyFlow()
        throws Exception {
        final TerminalSession sessionUnderTest = createTheSessionsAndOpenTheOneUnderTest();

        int shellProcessIdBeforeTheRelease = readOnMainThread(activity -> sessionUnderTest.getPid());
        TerminalEmulator emulatorBeforeTheRelease = readOnMainThread(activity -> sessionUnderTest.getEmulator());
        awaitMarkerOf(sessionUnderTest, COMMAND_MAKING_THE_FIRST_SHELL_PRINT_ITS_MARKER,
            MARKER_ONLY_THE_FIRST_SHELL_CAN_PRINT, "the first shell to print its marker");

        moveTheViewToTheOtherSession();
        runOnMainThread(activity -> sessionUnderTest.releaseRuntimeResourcesKeepingTheRowReopenable());

        assertNull("releasing the runtime of a row that died out of sight must drop its emulator",
            readOnMainThread(activity -> sessionUnderTest.getEmulator()));
        assertEquals("releasing the runtime must disown the shell process identifier",
            TerminalSession.NO_SHELL_PROCESS_PID, (int) readOnMainThread(activity -> sessionUnderTest.getPid()));
        assertFalse("the released session must not report itself as running",
            readOnMainThread(activity -> sessionUnderTest.isRunning()));
        assertSame("the released session must stay in the list, which is the whole point of releasing it "
                + "in place rather than removing it: the owner's row must still be there to reopen",
            sessionUnderTest, liveSessionNamed(SESSION_UNDER_TEST_NAME));
        awaitDisappearanceOfProcess(shellProcessIdBeforeTheRelease);

        int shellProcessIdAfterTheReopen = reopenAndAwaitAReplacementShell(sessionUnderTest,
            shellProcessIdBeforeTheRelease);
        awaitMarkerOf(sessionUnderTest, COMMAND_MAKING_THE_REOPENED_SHELL_PRINT_ITS_MARKER,
            MARKER_ONLY_THE_REOPENED_SHELL_CAN_PRINT,
            "the reopened shell to print its marker through the byte queues that replaced the released ones");
        assertReopenedOntoAFreshEmulator(sessionUnderTest, emulatorBeforeTheRelease);

        Log.w(LOG_TAG, "released shell process " + shellProcessIdBeforeTheRelease + " is gone and shell process "
            + shellProcessIdAfterTheReopen + " printed " + MARKER_ONLY_THE_REOPENED_SHELL_CAN_PRINT
            + " through the byte queues that replaced the closed ones");
    }

    @NonNull
    private TerminalSession createTheSessionsAndOpenTheOneUnderTest() throws Exception {
        mSessionUnderTest = createSessionNamed(SESSION_UNDER_TEST_NAME);
        mSessionTheViewMovesTo = createSessionNamed(SESSION_THE_VIEW_MOVES_TO_NAME);
        final TerminalSession sessionUnderTest = mSessionUnderTest;

        openSession(sessionUnderTest);
        awaitShellProcessOf(sessionUnderTest, "the session under test to fork its first shell");
        int shellProcessId = readOnMainThread(activity -> sessionUnderTest.getPid());
        assertTrue("the first shell process must exist, " + describeProcess(shellProcessId),
            processDirectoryOf(shellProcessId).exists());
        assertNotNull("the session under test must hold an emulator once it is open",
            readOnMainThread(activity -> sessionUnderTest.getEmulator()));
        return sessionUnderTest;
    }

    private void moveTheViewToTheOtherSession() throws Exception {
        final TerminalSession sessionTheViewMovesTo = mSessionTheViewMovesTo;
        assertNotNull("the test must have a second session to move the view to", sessionTheViewMovesTo);
        openSession(sessionTheViewMovesTo);
        awaitShellProcessOf(sessionTheViewMovesTo, "the session the view moves to to fork its shell");
    }

    private int reopenAndAwaitAReplacementShell(@NonNull TerminalSession session, int shellProcessIdBefore)
        throws Exception {
        openSession(session);
        awaitShellProcessOf(session, "the reopened session to fork a replacement shell");
        int shellProcessIdAfter = readOnMainThread(activity -> session.getPid());
        assertNotEquals("the reopened session must own a different shell process than the dead one",
            shellProcessIdBefore, shellProcessIdAfter);
        assertTrue("the replacement shell process must exist after the reopen, "
            + describeProcess(shellProcessIdAfter), processDirectoryOf(shellProcessIdAfter).exists());
        return shellProcessIdAfter;
    }

    private void assertReopenedOntoAFreshEmulator(@NonNull TerminalSession session,
                                                  @Nullable TerminalEmulator emulatorBefore) {
        assertNotSame("the reopened session must render into a new emulator rather than the dead one",
            emulatorBefore, readOnMainThread(activity -> session.getEmulator()));
        assertFalse("the scrollback of the dead emulator must not survive into the reopened session",
            transcriptOf(session).contains(MARKER_ONLY_THE_FIRST_SHELL_CAN_PRINT));
        assertTrue("the reopened session must report itself as running",
            readOnMainThread(activity -> session.isRunning()));
    }

    @Nullable
    private TerminalSession liveSessionNamed(@NonNull String sessionName) {
        return readOnMainThread(activity -> {
            TermuxService service = activity.getTermuxService();
            if (service == null) return null;
            TermuxSession termuxSession = service.getTermuxSessionForSessionName(sessionName);
            return termuxSession == null ? null : termuxSession.getTerminalSession();
        });
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

    private void awaitMarkerOf(@NonNull TerminalSession session, @NonNull String command,
                               @NonNull String expectedMarker, @NonNull String description) throws Exception {
        writeToSession(session, command);
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
        long deadlineMillis = System.currentTimeMillis() + SHELL_DEATH_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadlineMillis) {
            if (!processDirectoryOf(shellProcessId).exists()) return;
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        fail("Timed out after " + SHELL_DEATH_TIMEOUT_MILLIS
            + "ms waiting for the dead shell process to be gone, " + describeProcess(shellProcessId));
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
