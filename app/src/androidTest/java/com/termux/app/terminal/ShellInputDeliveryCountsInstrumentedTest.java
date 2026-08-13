package com.termux.app.terminal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.diagnostics.SessionCreationPath;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.ShellInputDeliveryRecord;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class ShellInputDeliveryCountsInstrumentedTest {

    private static final long SERVICE_READY_TIMEOUT_MILLIS = 60_000L;

    private static final long SHELL_START_TIMEOUT_MILLIS = 60_000L;

    private static final long SHELL_OUTPUT_TIMEOUT_MILLIS = 60_000L;

    private static final long POLL_INTERVAL_MILLIS = 100L;

    private static final String SYSTEM_SHELL_PATH = "/system/bin/sh";

    private static final String ROOT_WORKING_DIRECTORY = "/";

    private static final String SESSION_UNDER_TEST_NAME = "shell-input-delivery-counts-session";

    private static final String COMMAND_MAKING_THE_SHELL_PRINT_ITS_MARKER = "echo DELIVER''EDMARK\r";

    private static final String MARKER_ONLY_THE_SHELL_CAN_PRINT = "DELIVEREDMARK";

    private ActivityScenario<TermuxActivity> mScenario;

    private String mAutosshCommandBeforeTest;

    @Nullable
    private TerminalSession mSessionUnderTest;

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
        });
    }

    @After
    public void removeTheSessionThisTestCreated() {
        if (mScenario == null) return;
        runOnMainThread(activity -> {
            TermuxAppSharedPreferences preferences = activity.getPreferences();
            if (preferences != null) {
                preferences.setAutosshCommand(mAutosshCommandBeforeTest == null ? "" : mAutosshCommandBeforeTest);
                preferences.setSessionDisabled(SESSION_UNDER_TEST_NAME, false);
            }
            TermuxService service = activity.getTermuxService();
            if (service == null || mSessionUnderTest == null) return;
            mSessionUnderTest.finishIfRunning();
            service.removeTermuxSession(mSessionUnderTest);
        });
    }

    @Test
    public void inputTheShellActuallyReceivedIsCountedAsAcceptedAndAsWrittenOnARealDevice() throws Exception {
        final TerminalSession sessionUnderTest = createAndOpenTheSessionUnderTest();
        ShellInputDeliveryRecord record = sessionUnderTest.getShellInputDeliveryRecord();

        long acceptedBeforeTheCommand = record.getBytesAcceptedForDelivery();
        long writtenBeforeTheCommand = record.getBytesWrittenToTheShell();
        int commandLength =
            COMMAND_MAKING_THE_SHELL_PRINT_ITS_MARKER.getBytes(StandardCharsets.UTF_8).length;

        writeToSession(sessionUnderTest, COMMAND_MAKING_THE_SHELL_PRINT_ITS_MARKER);
        awaitMarkerOf(sessionUnderTest);

        assertTrue("the shell printed the marker, so the command reached it, and the count of bytes"
                + " accepted for delivery must have grown by at least that command. It was "
                + acceptedBeforeTheCommand + " before and " + record.getBytesAcceptedForDelivery()
                + " after, for a command of " + commandLength + " bytes",
            record.getBytesAcceptedForDelivery() - acceptedBeforeTheCommand >= commandLength);

        awaitWrittenBytesToReach(record, writtenBeforeTheCommand + commandLength);

        assertTrue("a session whose shell is reading its input has a running writer, and the report"
                + " would name a live session as broken if this said otherwise", record.isWriterRunning());
        assertNull("no failure occurred, so no stopping reason may be reported",
            record.getWriterStoppedReason());
    }

    @Test
    public void inputWrittenAfterTheRuntimeWasReleasedIsCountedAsDiscardedOnARealDevice() throws Exception {
        final TerminalSession sessionUnderTest = createAndOpenTheSessionUnderTest();
        ShellInputDeliveryRecord record = sessionUnderTest.getShellInputDeliveryRecord();

        runOnMainThread(activity -> sessionUnderTest.releaseRuntimeResourcesKeepingTheRowReopenable());
        long discardedBeforeTheWrite = record.getBytesDiscardedBeforeDelivery();
        long acceptedBeforeTheWrite = record.getBytesAcceptedForDelivery();
        int commandLength =
            COMMAND_MAKING_THE_SHELL_PRINT_ITS_MARKER.getBytes(StandardCharsets.UTF_8).length;

        writeToSession(sessionUnderTest, COMMAND_MAKING_THE_SHELL_PRINT_ITS_MARKER);

        assertTrue("a submission to a session with no shell process reaches nothing, and the toolbar"
                + " clears the field as though it had been delivered, so it has to be counted as"
                + " discarded rather than leaving no trace. It was " + discardedBeforeTheWrite
                + " before and " + record.getBytesDiscardedBeforeDelivery() + " after, for a"
                + " submission of " + commandLength + " bytes",
            record.getBytesDiscardedBeforeDelivery() - discardedBeforeTheWrite >= commandLength);
        assertTrue("nothing was handed to the queue, so nothing may be counted as accepted",
            record.getBytesAcceptedForDelivery() == acceptedBeforeTheWrite);
    }

    @NonNull
    private TerminalSession createAndOpenTheSessionUnderTest() throws Exception {
        TerminalSession createdSession = readOnMainThread(activity -> {
            TermuxService service = activity.getTermuxService();
            TermuxSession termuxSession = service.createTermuxSession(SYSTEM_SHELL_PATH, new String[0], null,
                ROOT_WORKING_DIRECTORY, true, SESSION_UNDER_TEST_NAME,
                SessionCreationPath.NEW_SESSION_THE_OWNER_ASKED_FOR);
            return termuxSession == null ? null : termuxSession.getTerminalSession();
        });
        assertNotNull("the service must create the session under test", createdSession);
        mSessionUnderTest = createdSession;
        runOnMainThread(activity ->
            activity.getTermuxTerminalSessionClient().switchToSessionReconnectingIfDead(createdSession));
        awaitConditionOnMainThread(SHELL_START_TIMEOUT_MILLIS, "the session under test to fork its shell",
            activity -> createdSession.getPid() > 0 && createdSession.isRunning()
                && createdSession.getEmulator() != null);
        return createdSession;
    }

    private void awaitWrittenBytesToReach(@NonNull ShellInputDeliveryRecord record, long expectedMinimum)
        throws Exception {
        long deadlineMillis = System.currentTimeMillis() + SHELL_OUTPUT_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadlineMillis) {
            if (record.getBytesWrittenToTheShell() >= expectedMinimum) return;
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        fail("The shell printed the marker, so the bytes reached the pseudo terminal, yet the count of"
            + " bytes written to the shell stopped at " + record.getBytesWrittenToTheShell()
            + " instead of reaching " + expectedMinimum + ". A report built on this count would"
            + " understate what was delivered and name a delivery that happened as a loss");
    }

    private void writeToSession(@NonNull TerminalSession session, @NonNull String input) {
        runOnMainThread(activity -> session.write(input));
    }

    private void awaitMarkerOf(@NonNull TerminalSession session) throws Exception {
        long deadlineMillis = System.currentTimeMillis() + SHELL_OUTPUT_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadlineMillis) {
            if (transcriptOf(session).contains(MARKER_ONLY_THE_SHELL_CAN_PRINT)) return;
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        fail("Timed out after " + SHELL_OUTPUT_TIMEOUT_MILLIS + "ms waiting for the shell to print "
            + MARKER_ONLY_THE_SHELL_CAN_PRINT + ", so this run cannot tell a counted delivery from an"
            + " uncounted one. The transcript was:\n" + transcriptOf(session));
    }

    @NonNull
    private String transcriptOf(@NonNull TerminalSession session) {
        String transcript = readOnMainThread(activity -> {
            TerminalEmulator emulator = session.getEmulator();
            return emulator == null ? null : emulator.getScreen().getTranscriptText();
        });
        return transcript == null ? "" : transcript;
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
