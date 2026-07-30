package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
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
import com.termux.terminal.TerminalBuffer;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
@Config(
    instrumentedPackages = {
        "com.termux.terminal.TerminalBuffer",
        "com.termux.app.terminal.SessionStatuslineReloadScanner",
        "com.termux.app.terminal.AllSessionsStatuslineParser",
        "com.termux.app.terminal.BackgroundOutputTagScanner"},
    shadows = {
        DisplayedSessionCallScanMainThreadCostTest.CountingTranscriptShadow.class,
        DisplayedSessionCallScanMainThreadCostTest.ThreadRecordingStatuslineReloadScannerShadow.class,
        DisplayedSessionCallScanMainThreadCostTest.ThreadRecordingAllSessionsStatuslineParserShadow.class,
        DisplayedSessionCallScanMainThreadCostTest.ThreadRecordingBackgroundOutputTagScannerShadow.class})
public class DisplayedSessionCallScanMainThreadCostTest {

    private static final int DISPLAYED_SESSION_COUNT = 16;

    private static final int LIVE_SHELL_PROCESS_ID = 4242;

    private static final int TRANSCRIPT_ROWS = 2000;

    private static final int SCREEN_COLUMNS = 80;

    private static final int SCREEN_ROWS = 24;

    private static final int SCROLLBACK_FILLER_ROWS = 120;

    private static final int MAX_TRANSCRIPT_MATERIALIZATIONS_PER_SESSION_PER_SCAN = 1;

    private static final int MAX_SESSIONS_SCANNED_PER_UNINTERRUPTED_MAIN_THREAD_PASS = 4;

    private static final long DRAIN_HORIZON_MILLIS = 30_000L;

    private static final int DRAIN_STEP_LIMIT = 500;

    private static final long SECONDS_AGO_OF_EVERY_OUT_TOKEN = 30L;

    private static final long SECONDS_AGO_OF_THE_NEWER_TOKEN = 60L;

    private static final long SECONDS_AGO_OF_THE_OLDER_TOKEN = 600L;

    static final List<TranscriptMaterialization> transcriptMaterializations =
        Collections.synchronizedList(new ArrayList<>());

    static final List<ThreadUse> statuslineParses = Collections.synchronizedList(new ArrayList<>());

    static final List<ThreadUse> outputTagScans = Collections.synchronizedList(new ArrayList<>());

    static final class ThreadUse {

        final String threadName;

        final boolean onTheMainThread;

        ThreadUse() {
            this.threadName = Thread.currentThread().getName();
            this.onTheMainThread = Looper.myLooper() == Looper.getMainLooper();
        }

        @Override
        public String toString() {
            return threadName + (onTheMainThread ? " (the main thread)" : " (a background thread)");
        }
    }

    static final class TranscriptMaterialization {

        final TerminalBuffer buffer;

        final ThreadUse threadUse;

        final long uptimeMillis;

        TranscriptMaterialization(TerminalBuffer buffer, ThreadUse threadUse, long uptimeMillis) {
            this.buffer = buffer;
            this.threadUse = threadUse;
            this.uptimeMillis = uptimeMillis;
        }
    }

    @Implements(TerminalBuffer.class)
    public static class CountingTranscriptShadow {

        @RealObject
        TerminalBuffer realBuffer;

        @Implementation
        protected String getTranscriptText() {
            record();
            return Shadow.directlyOn(realBuffer, TerminalBuffer.class, "getTranscriptText");
        }

        @Implementation
        protected String getTranscriptTextWithoutJoinedLines() {
            record();
            return Shadow.directlyOn(realBuffer, TerminalBuffer.class,
                "getTranscriptTextWithoutJoinedLines");
        }

        @Implementation
        protected String getTranscriptTextWithFullLinesJoined() {
            record();
            return Shadow.directlyOn(realBuffer, TerminalBuffer.class,
                "getTranscriptTextWithFullLinesJoined");
        }

        private void record() {
            transcriptMaterializations.add(new TranscriptMaterialization(realBuffer, new ThreadUse(),
                SystemClock.uptimeMillis()));
        }
    }

    @Implements(SessionStatuslineReloadScanner.class)
    public static class ThreadRecordingStatuslineReloadScannerShadow {

        @RealObject
        SessionStatuslineReloadScanner realScanner;

        @Implementation
        protected void repopulateFromCurrentStatusline(SessionNewActivityStore store,
                                                       String sessionName,
                                                       String visibleScreenText,
                                                       long nowMillis,
                                                       TimeZone timeZone) {
            statuslineParses.add(new ThreadUse());
            Shadow.directlyOn(realScanner, SessionStatuslineReloadScanner.class,
                "repopulateFromCurrentStatusline",
                ClassParameter.from(SessionNewActivityStore.class, store),
                ClassParameter.from(String.class, sessionName),
                ClassParameter.from(String.class, visibleScreenText),
                ClassParameter.from(long.class, nowMillis),
                ClassParameter.from(TimeZone.class, timeZone));
        }
    }

    @Implements(AllSessionsStatuslineParser.class)
    public static class ThreadRecordingAllSessionsStatuslineParserShadow {

        @RealObject
        AllSessionsStatuslineParser realParser;

        @Implementation
        protected List<ParsedStatuslineUpdate> parse(
                List<AllSessionsStatuslineParser.SessionScreenText> sessionScreenTexts,
                long nowMillis,
                TimeZone timeZone) {
            statuslineParses.add(new ThreadUse());
            return Shadow.directlyOn(realParser, AllSessionsStatuslineParser.class, "parse",
                ClassParameter.from(List.class, sessionScreenTexts),
                ClassParameter.from(long.class, nowMillis),
                ClassParameter.from(TimeZone.class, timeZone));
        }
    }

    @Implements(BackgroundOutputTagScanner.class)
    public static class ThreadRecordingBackgroundOutputTagScannerShadow {

        @RealObject
        BackgroundOutputTagScanner realScanner;

        @Implementation
        protected void scan(String sessionKey, String transcript, boolean scanCallToUserTag) {
            outputTagScans.add(new ThreadUse());
            Shadow.directlyOn(realScanner, BackgroundOutputTagScanner.class, "scan",
                ClassParameter.from(String.class, sessionKey),
                ClassParameter.from(String.class, transcript),
                ClassParameter.from(boolean.class, scanCallToUserTag));
        }
    }

    private TermuxActivity activity;

    private TermuxTerminalSessionActivityClient sessionActivityClient;

    private TermuxShellManager shellManager;

    private final List<TerminalSession> displayedSessions = new ArrayList<>();

    private final Map<String, String> callTokenBySessionName = new LinkedHashMap<>();

    private final Map<String, String> outTokenBySessionName = new LinkedHashMap<>();

    private final Map<String, String> replyTokenBySessionName = new LinkedHashMap<>();

    private final Set<String> sessionNamesWithAnUnrepliedCall = new LinkedHashSet<>();

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

        for (int index = 1; index <= DISPLAYED_SESSION_COUNT; index++) {
            displayedSessions.add(addDisplayedSessionHoldingARenderedEmulator(
                String.format(Locale.US, "session-displayed-%02d", index), index));
        }

        set(activity, TermuxActivity.class, "mIsVisible", true);

        drainEveryLooperToTheHorizon();
        forgetEverythingRecordedBeforeTheScanUnderTest();
    }

    @Test
    public void periodicDisplayedSessionScanMaterializesEachSessionTranscriptAtMostOnce() {
        runOnePeriodicDisplayedSessionScan();
        drainEveryLooperToTheHorizon();

        Map<String, Integer> materializationCountBySessionName = materializationCountBySessionName();
        int mostMaterializationsForOneSession = 0;
        String sessionNameWithTheMostMaterializations = "";
        int sessionsNeverMaterialized = 0;
        for (Map.Entry<String, Integer> entry : materializationCountBySessionName.entrySet()) {
            if (entry.getValue() > mostMaterializationsForOneSession) {
                mostMaterializationsForOneSession = entry.getValue();
                sessionNameWithTheMostMaterializations = entry.getKey();
            }
            if (entry.getValue() == 0) sessionsNeverMaterialized++;
        }

        assertTrue("materializing a session's whole scrollback allocates a string as large as the "
                + "transcript rows setting allows, which is " + TRANSCRIPT_ROWS + " rows by default and up "
                + "to 50000 rows once the owner raises it, and the terminal emulator is not thread safe so "
                + "every one of those materializations has to happen on the main thread where the drawing "
                + "thread is waiting; one scan of one session therefore has to build that string at most "
                + MAX_TRANSCRIPT_MATERIALIZATIONS_PER_SESSION_PER_SCAN + " time and share it between the "
                + "statusline read and the output tag scan, yet over " + DISPLAYED_SESSION_COUNT
                + " displayed sessions the session named " + sessionNameWithTheMostMaterializations
                + " had its whole transcript materialized " + mostMaterializationsForOneSession
                + " times in one scan; the per-session counts were " + materializationCountBySessionName,
            mostMaterializationsForOneSession <= MAX_TRANSCRIPT_MATERIALIZATIONS_PER_SESSION_PER_SCAN);

        assertEquals("a scan that reads nothing at all satisfies the bound above without sharing "
                + "anything, so the same run must also show every displayed session really was scanned; "
                + "each of the " + DISPLAYED_SESSION_COUNT + " displayed sessions must have its transcript "
                + "materialized at least once, yet the per-session counts were "
                + materializationCountBySessionName,
            0, sessionsNeverMaterialized);
    }

    @Test
    public void periodicDisplayedSessionScanDoesNotScanEverySessionInOneUninterruptedMainThreadPass() {
        runOnePeriodicDisplayedSessionScan();

        int sessionsScannedInOneUninterruptedPass = sessionsWhoseTranscriptWasMaterialized().size();

        drainEveryLooperToTheHorizon();
        int sessionsScannedOverTheWholeScan = sessionsWhoseTranscriptWasMaterialized().size();

        assertTrue("the once-a-minute displayed-session scan runs on the main thread while the owner is "
                + "looking at the terminal, so a session switch that lands inside the scan cannot draw and "
                + "output arriving from the session the owner just opened cannot be rendered until the "
                + "whole pass finishes; the pass must therefore yield to the looper between sessions the "
                + "way the neighbouring statusline rescan already does, covering no more than "
                + MAX_SESSIONS_SCANNED_PER_UNINTERRUPTED_MAIN_THREAD_PASS
                + " sessions before it returns control, yet over " + DISPLAYED_SESSION_COUNT
                + " displayed sessions it scanned " + sessionsScannedInOneUninterruptedPass
                + " of them in one uninterrupted pass",
            sessionsScannedInOneUninterruptedPass
                <= MAX_SESSIONS_SCANNED_PER_UNINTERRUPTED_MAIN_THREAD_PASS);

        assertEquals("yielding to the looper must change only when each session is scanned, never "
                + "whether it is scanned at all, otherwise the bound above would be satisfied by silently "
                + "dropping sessions and a displayed session that called the owner would keep no red "
                + "marker; every one of the " + DISPLAYED_SESSION_COUNT + " displayed sessions must have "
                + "been scanned once the looper queues drain",
            DISPLAYED_SESSION_COUNT, sessionsScannedOverTheWholeScan);
    }

    @Test
    public void periodicDisplayedSessionScanParsesNothingOnTheMainThread() {
        runOnePeriodicDisplayedSessionScan();
        drainEveryLooperToTheHorizon();

        List<ThreadUse> statuslineParsesOnTheMainThread = onTheMainThread(statuslineParses);
        List<ThreadUse> outputTagScansOnTheMainThread = onTheMainThread(outputTagScans);

        assertEquals("the statusline token parse and the output tag regular expression passes are pure "
                + "CPU work over an already materialized string, and an off-thread parse handler already "
                + "exists in this class and is used by the neighbouring rescan, so neither may run on the "
                + "main thread where they block drawing and rendering; over " + DISPLAYED_SESSION_COUNT
                + " displayed sessions the statusline parse ran on the main thread "
                + statuslineParsesOnTheMainThread.size() + " times and the output tag scan ran on the main "
                + "thread " + outputTagScansOnTheMainThread.size() + " times; the statusline parse threads "
                + "were " + statuslineParses + " and the output tag scan threads were " + outputTagScans,
            0, statuslineParsesOnTheMainThread.size() + outputTagScansOnTheMainThread.size());

        assertEquals("a scan that parses nothing anywhere satisfies the bound above without moving any "
                + "work off the main thread, so the same run must also show the parse really happened and "
                + "produced each displayed session's statusline state; every one of the "
                + DISPLAYED_SESSION_COUNT + " displayed sessions must have stored statusline data",
            DISPLAYED_SESSION_COUNT, sessionNamesWithStoredStatuslineData().size());
    }

    @Test
    public void displayedSessionStatuslineRefreshMakesNoDisplayedSessionWaitBehindABatchDelay() {
        long refreshStartUptimeMillis = SystemClock.uptimeMillis();
        sessionActivityClient.reconnectDeadDefinitionBackedSessionsThenForceRescanStatusline();
        drainEveryLooperToTheHorizon();

        Map<String, Long> refreshDelayBySessionName =
            firstMaterializationDelayBySessionName(refreshStartUptimeMillis);
        long earliestRefreshDelayMillis = Long.MAX_VALUE;
        long latestRefreshDelayMillis = Long.MIN_VALUE;
        for (Long refreshDelayMillis : refreshDelayBySessionName.values()) {
            earliestRefreshDelayMillis = Math.min(earliestRefreshDelayMillis, refreshDelayMillis);
            latestRefreshDelayMillis = Math.max(latestRefreshDelayMillis, refreshDelayMillis);
        }

        assertEquals("the displayed-session statusline refresh is split into batches spaced a second "
                + "apart only because reading one session's statusline is expensive, and that spacing "
                + "makes a displayed session the owner can see wait for its own refresh; over "
                + DISPLAYED_SESSION_COUNT + " displayed sessions the last one currently waits seconds "
                + "longer than the first, which the owner has ruled out, so once the per-session cost is "
                + "removed every displayed session's refresh must be scheduled without a per-batch delay "
                + "and no displayed session may be refreshed later than another; the earliest refresh "
                + "happened " + earliestRefreshDelayMillis + " milliseconds after the refresh was asked "
                + "for and the latest happened " + latestRefreshDelayMillis + " milliseconds after it, "
                + "with the per-session delays being " + refreshDelayBySessionName,
            earliestRefreshDelayMillis, latestRefreshDelayMillis);

        assertEquals("removing the batch delay must not remove the refresh itself, so the same run must "
                + "also show every displayed session was refreshed; each of the " + DISPLAYED_SESSION_COUNT
                + " displayed sessions must have had its statusline read",
            DISPLAYED_SESSION_COUNT, refreshDelayBySessionName.size());
    }

    @Test
    public void periodicDisplayedSessionScanRecordsTheStatuslineTimesEveryDisplayedSessionShows() {
        runOnePeriodicDisplayedSessionScan();
        drainEveryLooperToTheHorizon();

        SessionNewActivityStore store = activity.getSessionNewActivityStore();
        Map<String, String> recordedCallTokenBySessionName = new LinkedHashMap<>();
        Map<String, String> recordedOutTokenBySessionName = new LinkedHashMap<>();
        Map<String, String> recordedReplyTokenBySessionName = new LinkedHashMap<>();
        for (TerminalSession session : displayedSessions) {
            recordedCallTokenBySessionName.put(session.mSessionName,
                clockToken(store.getStatuslineCallTimeMillis(session.mSessionName)));
            recordedOutTokenBySessionName.put(session.mSessionName,
                clockToken(store.getStatuslineOutTimeMillis(session.mSessionName)));
            recordedReplyTokenBySessionName.put(session.mSessionName,
                clockToken(store.getStatuslineReplyTimeMillis(session.mSessionName)));
        }

        assertEquals("making the scan cheaper must not change which call time each displayed session "
                + "shows, otherwise a row would display a time its session never emitted",
            callTokenBySessionName, recordedCallTokenBySessionName);
        assertEquals("making the scan cheaper must not change which out time each displayed session "
                + "shows, otherwise a row would display a time its session never emitted",
            outTokenBySessionName, recordedOutTokenBySessionName);
        assertEquals("making the scan cheaper must not change which reply time each displayed session "
                + "shows, otherwise a row would display a time its session never emitted",
            replyTokenBySessionName, recordedReplyTokenBySessionName);
    }

    @Test
    public void periodicDisplayedSessionScanKeepsTheCallToUserStateEachStatuslineImplies() {
        runOnePeriodicDisplayedSessionScan();
        drainEveryLooperToTheHorizon();

        SessionNewActivityStore store = activity.getSessionNewActivityStore();
        Set<String> sessionNamesWithAPendingCallRecorded = new LinkedHashSet<>();
        Set<String> sessionNamesShownRed = new LinkedHashSet<>();
        for (TerminalSession session : displayedSessions) {
            if (store.hasPendingExplicitCall(session.mSessionName)) {
                sessionNamesWithAPendingCallRecorded.add(session.mSessionName);
            }
            if (store.tierFor(session.mSessionName) == SessionNewActivityTier.RED) {
                sessionNamesShownRed.add(session.mSessionName);
            }
        }

        assertEquals("the whole point of the once-a-minute displayed-session scan is that a displayed "
                + "session whose call time is newer than its reply time surfaces its red marker without "
                + "the owner opening it, so making the scan cheaper must leave exactly the same sessions "
                + "marked as calling the owner",
            sessionNamesWithAnUnrepliedCall, sessionNamesWithAPendingCallRecorded);
        assertEquals("the red row tier is what the owner actually looks at, so making the scan cheaper "
                + "must leave exactly the same displayed sessions shown red",
            sessionNamesWithAnUnrepliedCall, sessionNamesShownRed);
    }

    @Test
    public void periodicDisplayedSessionScanReadsEveryEmulatorOnlyFromTheMainThread() {
        runOnePeriodicDisplayedSessionScan();
        drainEveryLooperToTheHorizon();

        List<ThreadUse> materializationsOffTheMainThread = new ArrayList<>();
        for (TranscriptMaterialization materialization : new ArrayList<>(transcriptMaterializations)) {
            if (!materialization.threadUse.onTheMainThread) {
                materializationsOffTheMainThread.add(materialization.threadUse);
            }
        }

        assertEquals("the terminal emulator is not thread safe, so moving the parse off the main thread "
                + "must not take the emulator read with it; every transcript materialization must still "
                + "happen on the main thread and only the pure token and regular expression work over the "
                + "already materialized string may move, yet these materializations happened elsewhere: "
                + materializationsOffTheMainThread,
            0, materializationsOffTheMainThread.size());

        assertTrue("a scan that materializes nothing satisfies the bound above without reading any "
                + "emulator at all, so the same run must also show the scan really read the transcripts",
            !transcriptMaterializations.isEmpty());
    }

    @Test
    public void periodicDisplayedSessionScanFeedsEveryDisplayedSessionTranscriptToTheOutputTagScan() {
        runOnePeriodicDisplayedSessionScan();
        drainEveryLooperToTheHorizon();

        assertEquals("the output tag scan is what records an explicit call and an app update tag for a "
                + "displayed but not foreground session, so making the scan cheaper must not reduce how "
                + "many displayed sessions it covers; each of the " + DISPLAYED_SESSION_COUNT
                + " displayed sessions must be handed to it exactly once per scan, yet the scans "
                + "recorded were " + outputTagScans,
            DISPLAYED_SESSION_COUNT, outputTagScans.size());
    }

    private void runOnePeriodicDisplayedSessionScan() {
        sessionActivityClient.startDisplayedSessionCallScanTick();
        sessionActivityClient.stopDisplayedSessionCallScanTick();
    }

    private void forgetEverythingRecordedBeforeTheScanUnderTest() {
        transcriptMaterializations.clear();
        statuslineParses.clear();
        outputTagScans.clear();
    }

    private List<ThreadUse> onTheMainThread(List<ThreadUse> threadUses) {
        List<ThreadUse> occurrences = new ArrayList<>();
        for (ThreadUse threadUse : new ArrayList<>(threadUses)) {
            if (threadUse.onTheMainThread) occurrences.add(threadUse);
        }
        return occurrences;
    }

    private Set<String> sessionNamesWithStoredStatuslineData() {
        SessionNewActivityStore store = activity.getSessionNewActivityStore();
        Set<String> sessionNames = new LinkedHashSet<>();
        for (TerminalSession session : displayedSessions) {
            if (store != null && store.hasStoredStatuslineData(session.mSessionName)) {
                sessionNames.add(session.mSessionName);
            }
        }
        return sessionNames;
    }

    private Map<String, Integer> materializationCountBySessionName() {
        Map<String, Integer> countBySessionName = new LinkedHashMap<>();
        for (TerminalSession session : displayedSessions) {
            Set<TerminalBuffer> buffers = buffersOf(session);
            int count = 0;
            for (TranscriptMaterialization materialization : new ArrayList<>(transcriptMaterializations)) {
                if (buffers.contains(materialization.buffer)) count++;
            }
            countBySessionName.put(session.mSessionName, count);
        }
        return countBySessionName;
    }

    private Set<String> sessionsWhoseTranscriptWasMaterialized() {
        Set<String> sessionNames = new LinkedHashSet<>();
        for (Map.Entry<String, Integer> entry : materializationCountBySessionName().entrySet()) {
            if (entry.getValue() > 0) sessionNames.add(entry.getKey());
        }
        return sessionNames;
    }

    private Map<String, Long> firstMaterializationDelayBySessionName(long startUptimeMillis) {
        Map<String, Long> delayBySessionName = new LinkedHashMap<>();
        for (TerminalSession session : displayedSessions) {
            Set<TerminalBuffer> buffers = buffersOf(session);
            for (TranscriptMaterialization materialization : new ArrayList<>(transcriptMaterializations)) {
                if (!buffers.contains(materialization.buffer)) continue;
                delayBySessionName.put(session.mSessionName,
                    materialization.uptimeMillis - startUptimeMillis);
                break;
            }
        }
        return delayBySessionName;
    }

    private Set<TerminalBuffer> buffersOf(TerminalSession session) {
        Set<TerminalBuffer> buffers = new LinkedHashSet<>();
        TerminalEmulator emulator = session.getEmulator();
        if (emulator == null) return buffers;
        buffers.add(emulator.getScreen());
        for (String bufferFieldName : new String[]{"mMainBuffer", "mAltBuffer"}) {
            try {
                Field bufferField = TerminalEmulator.class.getDeclaredField(bufferFieldName);
                bufferField.setAccessible(true);
                Object buffer = bufferField.get(emulator);
                if (buffer instanceof TerminalBuffer) buffers.add((TerminalBuffer) buffer);
            } catch (ReflectiveOperationException theEmulatorNoLongerHoldsThatBuffer) {
                continue;
            }
        }
        return buffers;
    }

    private void drainEveryLooperToTheHorizon() {
        long horizonUptimeMillis = SystemClock.uptimeMillis() + DRAIN_HORIZON_MILLIS;
        for (int step = 0; step < DRAIN_STEP_LIMIT; step++) {
            for (Looper looper : loopersTheClientOwnsBesidesTheMainOne()) {
                OffDeviceNativeSubprocessLibrary.tolerateItsAbsence(() -> shadowOf(looper).idle());
            }
            OffDeviceNativeSubprocessLibrary.tolerateItsAbsence(
                () -> shadowOf(Looper.getMainLooper()).idle());
            if (!everyLooperTheClientOwnsIsIdle()) continue;
            long nextTaskDueUptimeMillis =
                shadowOf(Looper.getMainLooper()).getNextScheduledTaskTime().toMillis();
            if (nextTaskDueUptimeMillis <= 0L) return;
            if (nextTaskDueUptimeMillis > horizonUptimeMillis) return;
            long remainingMillis = Math.max(0L, nextTaskDueUptimeMillis - SystemClock.uptimeMillis());
            long millisToAdvance = remainingMillis;
            OffDeviceNativeSubprocessLibrary.tolerateItsAbsence(() -> shadowOf(Looper.getMainLooper())
                .idleFor(Duration.ofMillis(millisToAdvance)));
        }
    }

    private boolean everyLooperTheClientOwnsIsIdle() {
        for (Looper looper : loopersTheClientOwnsBesidesTheMainOne()) {
            if (!shadowOf(looper).isIdle()) return false;
        }
        return shadowOf(Looper.getMainLooper()).isIdle();
    }

    @NonNull
    private List<Looper> loopersTheClientOwnsBesidesTheMainOne() {
        List<Looper> loopers = new ArrayList<>();
        for (Field field : TermuxTerminalSessionActivityClient.class.getDeclaredFields()) {
            field.setAccessible(true);
            Object value;
            try {
                value = field.get(sessionActivityClient);
            } catch (ReflectiveOperationException theFieldIsNotReadable) {
                continue;
            }
            Looper looper = null;
            if (value instanceof HandlerThread) looper = ((HandlerThread) value).getLooper();
            if (value instanceof Handler) looper = ((Handler) value).getLooper();
            if (looper == null) continue;
            if (looper == Looper.getMainLooper()) continue;
            if (!loopers.contains(looper)) loopers.add(looper);
        }
        return loopers;
    }

    private TerminalSession addDisplayedSessionHoldingARenderedEmulator(String sessionName, int index)
            throws Exception {
        TerminalSession terminalSession =
            new TerminalSession("/system/bin/sh", "/", new String[0], new String[0], TRANSCRIPT_ROWS,
                activity.getTermuxTerminalSessionClient());
        terminalSession.mSessionName = sessionName;
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class,
            boolean.class);
        constructor.setAccessible(true);
        shellManager.mTermuxSessions.add(
            constructor.newInstance(terminalSession, new ExecutionCommand(), null, false));
        OffDeviceNativeSubprocessLibrary.tolerateItsAbsence(
            () -> terminalSession.initializeEmulator(SCREEN_COLUMNS, SCREEN_ROWS, 10, 20));
        Field shellProcessId = TerminalSession.class.getDeclaredField("mShellPid");
        shellProcessId.setAccessible(true);
        shellProcessId.setInt(terminalSession, LIVE_SHELL_PROCESS_ID);
        renderAScrollbackAndAStatusline(terminalSession, index);
        return terminalSession;
    }

    private void renderAScrollbackAndAStatusline(TerminalSession session, int index) {
        boolean unrepliedCall = index % 2 == 1;
        String outToken = clockTokenSecondsAgo(SECONDS_AGO_OF_EVERY_OUT_TOKEN);
        String callToken = clockTokenSecondsAgo(unrepliedCall
            ? SECONDS_AGO_OF_THE_NEWER_TOKEN
            : SECONDS_AGO_OF_THE_OLDER_TOKEN);
        String replyToken = clockTokenSecondsAgo(unrepliedCall
            ? SECONDS_AGO_OF_THE_OLDER_TOKEN
            : SECONDS_AGO_OF_THE_NEWER_TOKEN);
        callTokenBySessionName.put(session.mSessionName, callToken);
        outTokenBySessionName.put(session.mSessionName, outToken);
        replyTokenBySessionName.put(session.mSessionName, replyToken);
        if (unrepliedCall) sessionNamesWithAnUnrepliedCall.add(session.mSessionName);

        StringBuilder rendered = new StringBuilder();
        for (int row = 0; row < SCROLLBACK_FILLER_ROWS; row++) {
            rendered.append("scrollback row ").append(row).append(" of ").append(session.mSessionName)
                .append("\r\n");
        }
        rendered.append("claude  call:").append(callToken).append("  out:").append(outToken)
            .append("  reply:").append(replyToken).append("\r\n");
        byte[] renderedBytes = rendered.toString().getBytes(StandardCharsets.UTF_8);
        session.getEmulator().append(renderedBytes, renderedBytes.length);
    }

    private String clockTokenSecondsAgo(long secondsAgo) {
        return clockToken(System.currentTimeMillis() - secondsAgo * 1_000L);
    }

    private String clockToken(Long timeMillis) {
        if (timeMillis == null) return "absent";
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.setTimeInMillis(timeMillis);
        return String.format(Locale.US, "%02d:%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value)
            throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
