package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The forced statusline rescan reads every visible session's whole transcript regardless of the
 * content-version skip gate, and the post-reconnect retry ladder repeats it once per backoff rung, so
 * it has to be batched the way the displayed-session refresh already is. These are behavioral tests:
 * they stand up a real activity, service, session activity client and real terminal sessions, drive
 * the production reconnect-then-force-rescan entry point, and then observe which sessions actually had
 * their transcript read and which batches were actually scheduled on the main looper.
 */
@RunWith(RobolectricTestRunner.class)
public class ForcedStatuslineRescanBatchTest {

    private static final int ON_SCREEN_SESSION_COUNT = 12;

    /**
     * The bound the batching has to respect, written out here rather than read from production so
     * widening the production constant fails this test instead of being tracked by it.
     */
    private static final int MAIN_THREAD_TRANSCRIPT_READ_BOUND_PER_PASS = 4;

    private static final long STATUSLINE_RESCAN_BATCH_INTERVAL_MILLIS =
        TermuxTerminalSessionActivityClient.STAGGERED_RECONNECT_INTERVAL_MILLIS;

    private static final int EXPECTED_BATCH_COUNT =
        (ON_SCREEN_SESSION_COUNT + TermuxTerminalSessionActivityClient.STAGGERED_STATUSLINE_RESCAN_BATCH_SIZE - 1)
            / TermuxTerminalSessionActivityClient.STAGGERED_STATUSLINE_RESCAN_BATCH_SIZE;

    /**
     * The session list bottom sheet is what widens the visible set beyond the single foreground
     * session, and {@link TermuxTerminalSessionActivityClient} reads it through the two overridable
     * methods below, so an open sheet showing every session row is reproduced by overriding them.
     */
    private static final class OpenSessionListBottomSheet extends SessionListBottomSheetController {

        private final List<String> onScreenSessionNames;

        OpenSessionListBottomSheet(@NonNull TermuxActivity activity,
                                   @NonNull List<String> onScreenSessionNames) {
            super(activity);
            this.onScreenSessionNames = onScreenSessionNames;
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @NonNull
        @Override
        public List<String> getOnScreenSessionNames() {
            return onScreenSessionNames;
        }
    }

    private TermuxActivity activity;

    private TermuxTerminalSessionActivityClient sessionActivityClient;

    private TermuxShellManager shellManager;

    private final List<TerminalSession> onScreenSessions = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context appContext = RuntimeEnvironment.getApplication();

        TermuxService service = Robolectric.buildService(TermuxService.class).get();
        shellManager = new TermuxShellManager(appContext);
        set(service, TermuxService.class, "mShellManager", shellManager);
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        sessionActivityClient = new TermuxTerminalSessionActivityClient(activity);
        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient",
            sessionActivityClient);
        service.setTermuxTerminalSessionClient(activity.getTermuxTerminalSessionClient());

        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        activity.setContentView(R.layout.activity_termux);

        List<String> onScreenSessionNames = new ArrayList<>();
        for (int index = 1; index <= ON_SCREEN_SESSION_COUNT; index++) {
            TerminalSession session = addSessionHoldingARenderedEmulator(
                String.format("session-on-screen-%02d", index));
            onScreenSessions.add(session);
            onScreenSessionNames.add(session.mSessionName);
        }
        // The displayed-session refresh that runs alongside the forced rescan does its own batching,
        // so every session is kept out of the displayed set and only the forced rescan under test
        // schedules anything.
        preferences.setDisabledSessionNames(String.join("\n", onScreenSessionNames));

        set(activity, TermuxActivity.class, "mIsVisible", true);
        set(activity, TermuxActivity.class, "mSessionListBottomSheetController",
            new OpenSessionListBottomSheet(activity, onScreenSessionNames));

        OffDeviceNativeSubprocessLibrary.tolerateItsAbsence(
            () -> shadowOf(Looper.getMainLooper()).idle());
    }

    @Test
    public void forcedRescanReadsNoMoreThanOneBatchOfTranscriptsInOneUninterruptedMainThreadPass() {
        sessionActivityClient.reconnectDeadDefinitionBackedSessionsThenForceRescanStatusline();

        int readInOneUninterruptedPass = countSessionsWhoseTranscriptWasRead();

        assertTrue("a forced rescan bypasses the content-version skip gate, so it materializes the whole "
                + "2000-row transcript of every session it covers, and the retry ladder repeats that once "
                + "immediately and once per backoff rung; the drawing thread cannot redraw while a pass "
                + "runs, so no pass may read more than "
                + MAIN_THREAD_TRANSCRIPT_READ_BOUND_PER_PASS + " transcripts uninterrupted, the same "
                + "bound the displayed-session refresh already respects, yet over " + ON_SCREEN_SESSION_COUNT
                + " on-screen sessions this pass read " + readInOneUninterruptedPass,
            readInOneUninterruptedPass <= MAIN_THREAD_TRANSCRIPT_READ_BOUND_PER_PASS);
        assertTrue("some transcripts must be deferred out of the first pass, otherwise the visible set "
                + "was read whole and nothing was batched at all",
            readInOneUninterruptedPass < ON_SCREEN_SESSION_COUNT);
    }

    @Test
    public void forcedRescanDefersEveryLaterBatchOneSpacingIntervalFurtherOut() {
        sessionActivityClient.reconnectDeadDefinitionBackedSessionsThenForceRescanStatusline();

        long lastScheduledTaskDelayMillis = lastMainThreadTaskDelayMillis();

        assertTrue("the transcripts kept out of the first pass have to be posted back to the main-thread "
                + "handler for later rather than read anyway, so the forced rescan must leave at least one "
                + "main-thread task scheduled in the future, yet the last task it scheduled is due in "
                + lastScheduledTaskDelayMillis + " milliseconds",
            lastScheduledTaskDelayMillis > 0L);
        assertEquals("each deferred batch must be spaced one further "
                + STATUSLINE_RESCAN_BATCH_INTERVAL_MILLIS + " millisecond interval out so the looper gets "
                + "to draw between them; over " + ON_SCREEN_SESSION_COUNT + " on-screen sessions that is "
                + EXPECTED_BATCH_COUNT + " batches whose last one is due at "
                + ((EXPECTED_BATCH_COUNT - 1) * STATUSLINE_RESCAN_BATCH_INTERVAL_MILLIS)
                + " milliseconds",
            (EXPECTED_BATCH_COUNT - 1) * STATUSLINE_RESCAN_BATCH_INTERVAL_MILLIS,
            lastScheduledTaskDelayMillis);
    }

    @Test
    public void forcedRescanEventuallyReadsEveryVisibleSessionTranscript() {
        sessionActivityClient.reconnectDeadDefinitionBackedSessionsThenForceRescanStatusline();

        OffDeviceNativeSubprocessLibrary.tolerateItsAbsence(() -> shadowOf(Looper.getMainLooper())
            .idleFor(Duration.ofMillis(
                EXPECTED_BATCH_COUNT * STATUSLINE_RESCAN_BATCH_INTERVAL_MILLIS)));

        assertEquals("batching the forced rescan must spread the transcript reads over time rather than "
                + "shed them, and a first pass that stays under the bound is equally satisfied by a rescan "
                + "that reads nothing at all, so every one of the " + ON_SCREEN_SESSION_COUNT
                + " on-screen sessions must have had its statusline read once the deferred batches have "
                + "run, otherwise a row would keep showing a stale call, out or reply time",
            ON_SCREEN_SESSION_COUNT, countSessionsWhoseTranscriptWasRead());
    }

    private long lastMainThreadTaskDelayMillis() {
        Duration lastScheduledTaskTime = shadowOf(Looper.getMainLooper()).getLastScheduledTaskTime();
        return lastScheduledTaskTime.toMillis() - SystemClock.uptimeMillis();
    }

    /**
     * A session whose transcript was read had its screen content version recorded on the scan gate, so
     * asking the gate whether that same unchanged version still needs scanning answers false for
     * exactly the sessions the rescan covered. The query records the version itself, so each test takes
     * this observation once.
     */
    private int countSessionsWhoseTranscriptWasRead() {
        AllSessionsStatuslineScanGate scanGate = statuslineScanGate();
        int read = 0;
        for (TerminalSession session : onScreenSessions) {
            long screenContentVersion = session.getEmulator().getScreenContentVersion();
            if (!scanGate.shouldScan(session.mHandle, screenContentVersion, true)) read++;
        }
        return read;
    }

    private AllSessionsStatuslineScanGate statuslineScanGate() {
        try {
            Field gateField = TermuxTerminalSessionActivityClient.class
                .getDeclaredField("mAllSessionsStatuslineScanGate");
            gateField.setAccessible(true);
            return (AllSessionsStatuslineScanGate) gateField.get(sessionActivityClient);
        } catch (ReflectiveOperationException failedToReadTheScanGate) {
            throw new AssertionError(failedToReadTheScanGate);
        }
    }

    private TerminalSession addSessionHoldingARenderedEmulator(String sessionName) throws Exception {
        TerminalSession terminalSession =
            new TerminalSession("/system/bin/sh", "/", new String[0], new String[0], 2000,
                activity.getTermuxTerminalSessionClient());
        terminalSession.mSessionName = sessionName;
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class,
            boolean.class);
        constructor.setAccessible(true);
        shellManager.mTermuxSessions.add(
            constructor.newInstance(terminalSession, new ExecutionCommand(), null, false));
        OffDeviceNativeSubprocessLibrary.tolerateItsAbsence(
            () -> terminalSession.initializeEmulator(80, 24, 10, 20));
        return terminalSession;
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value)
            throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
