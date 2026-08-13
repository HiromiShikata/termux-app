package com.termux.app.diagnostics;

import android.os.Debug;
import android.os.SystemClock;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.BuildConfig;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.browser.TermuxBrowserController;
import com.termux.app.process.AppProcessPopulationReader;
import com.termux.app.sessiondefinition.SessionDefinitionCapCountPlanner;
import com.termux.app.terminal.SessionNewActivityStore;
import com.termux.app.terminal.SessionNewActivityTier;
import com.termux.app.terminal.TermuxSessionsListViewController;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalBuffer;
import com.termux.shared.termux.terminal.ShellOutputParseCostCounterHolder;
import com.termux.terminal.TerminalBufferReflowCostCounterHolder;
import com.termux.terminal.ShellInputDeliveryRecord;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class DiagnosticsReportCollector {

    @NonNull
    private final DiagnosticEventLog mEventLog;

    @NonNull
    private final SessionDefinitionCapCountPlanner mCapCountPlanner = new SessionDefinitionCapCountPlanner();

    @NonNull
    private final AppProcessPopulationReader mProcessPopulationReader = new AppProcessPopulationReader();

    public DiagnosticsReportCollector() {
        this(DiagnosticEventLogHolder.getInstance());
    }

    public DiagnosticsReportCollector(@NonNull DiagnosticEventLog eventLog) {
        mEventLog = eventLog;
    }

    @NonNull
    public DiagnosticsReport collect(@NonNull TermuxActivity activity, long nowMillis) {
        TermuxService service = activity.getTermuxService();

        int countedTowardCap = cappedSessionCount(service, activity);
        int displayedCount = displayedSessionCount(activity);
        int maxSessionsCap = activity.getPreferences().getSessionDefinitionMaxSessions();

        List<DiagnosticsSessionLine> sessionLines =
            buildSessionLines(service, nowMillis, sessionIndexesDisplayedInList(activity),
                collapsedProjectSessionNames(activity), hiddenSessionNamesBeingHidden(activity));
        int openTabCount = openTabCount(activity);
        int tabHistoryEntryCount = tabHistoryEntryCount(activity);
        boolean wakeLockHeld = service != null && service.isWakeLockHeld();
        boolean foreground = activity.isVisible();

        List<DiagnosticEvent> recentEvents = mEventLog.tail(50);

        return new DiagnosticsReport(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, nowMillis,
            countedTowardCap, displayedCount, maxSessionsCap, sessionLines,
            openTabCount, tabHistoryEntryCount, wakeLockHeld, foreground, recentEvents,
            collectMemoryUsage(),
            DiagnosticsWorkCostLine.of(BackgroundOutputScanCostCounterHolder.getInstance()),
            DiagnosticsWorkCostLine.of(ForegroundOpenTagScanCostCounterHolder.getInstance()),
            DiagnosticsWorkCostLine.of(TerminalBufferReflowCostCounterHolder.getInstance()),
            DiagnosticsSessionReconnectCost.of(SessionReconnectCostCounterHolder.getInstance()),
            ReplacedSessionShellInputRecorderHolder.getInstance().snapshot(),
            DiagnosticsMainThreadStalls.of(MainThreadStallWatchdog.getRecorder()),
            MainLooperQueueSnapshot.take(),
            scrollbarViewCensus(activity),
            ProcessUptimeTracker.uptimeMillis(SystemClock.elapsedRealtime()),
            DiagnosticsBackgroundCycle.of(BackgroundCycleIntervalRecorderHolder.getInstance()),
            AppVersionChangeHolder.getInstance(),
            ShellExitStatusRecorderHolder.getInstance().snapshot(),
            PhantomProcessMonitorStateHolder.getInstance().snapshot(),
            mProcessPopulationReader.read(),
            DiagnosticsWorkCostLine.of(TerminalDrawCostCounterHolder.getInstance()),
            DiagnosticsWorkCostLine.of(ShellOutputParseCostCounterHolder.getInstance()),
            DiagnosticsSessionCreationPaths.of(SessionCreationPathCounterHolder.getInstance()));
    }

    @NonNull
    private ScrollbarViewCensus scrollbarViewCensus(@NonNull TermuxActivity activity) {
        View decorView = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
        if (decorView == null) return ScrollbarViewCensus.empty();
        return ScrollbarViewCensus.take(new AndroidScrollbarViewNode(decorView));
    }

    @NonNull
    private DiagnosticsMemoryUsage collectMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalBytes = runtime.totalMemory();
        long usedBytes = totalBytes - runtime.freeMemory();
        return new DiagnosticsMemoryUsage(toMegabytes(usedBytes), toMegabytes(totalBytes),
            toMegabytes(runtime.maxMemory()), toMegabytes(Debug.getNativeHeapAllocatedSize()));
    }

    private long toMegabytes(long bytes) {
        return bytes / (1024L * 1024L);
    }

    /**
     * Counts the sessions that actually count toward the max-session cap, using the exact same
     * live-only rule the cap enforces (see {@link
     * com.termux.app.terminal.TermuxTerminalSessionActivityClient#cappedSessionCount}). Dead sessions
     * — orphans and dead-but-reconnectable rows — are excluded, so the diagnostics "counted toward cap"
     * figure matches the number the cap uses rather than {@code getTermuxSessionsSize()} (all sessions).
     */
    private int cappedSessionCount(TermuxService service, @NonNull TermuxActivity activity) {
        if (service == null) return 0;
        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = new ArrayList<>();
        for (TermuxSession termuxSession : new ArrayList<>(service.getTermuxSessions())) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            countedSessions.add(new SessionDefinitionCapCountPlanner.CountedSession(
                terminalSession == null ? null : terminalSession.mSessionName,
                terminalSession != null && terminalSession.isRunning()));
        }
        return mCapCountPlanner.countSessionsTowardCap(countedSessions, hiddenSessionNames(activity));
    }

    @NonNull
    private Set<String> hiddenSessionNames(@NonNull TermuxActivity activity) {
        return activity.getPreferences() == null
            ? Collections.emptySet() : activity.getPreferences().getDisabledSessionNames();
    }

    private int displayedSessionCount(@NonNull TermuxActivity activity) {
        TermuxSessionsListViewController controller = activity.getTermuxSessionListViewController();
        return controller != null ? controller.getVisibleSessionCount() : 0;
    }

    @Nullable
    private List<Integer> sessionIndexesDisplayedInList(@NonNull TermuxActivity activity) {
        TermuxSessionsListViewController controller = activity.getTermuxSessionListViewController();
        return controller == null ? null : controller.getSessionIndexesDisplayedInList();
    }

    @NonNull
    private Set<String> collapsedProjectSessionNames(@NonNull TermuxActivity activity) {
        TermuxSessionsListViewController controller = activity.getTermuxSessionListViewController();
        return controller == null ? Collections.emptySet() : controller.getCollapsedProjectSessionNames();
    }

    @NonNull
    private Set<String> hiddenSessionNamesBeingHidden(@NonNull TermuxActivity activity) {
        TermuxSessionsListViewController controller = activity.getTermuxSessionListViewController();
        if (controller == null || !controller.isHidingHiddenSessions()) return Collections.emptySet();
        return hiddenSessionNames(activity);
    }

    @NonNull
    private List<DiagnosticsSessionLine> buildSessionLines(TermuxService service, long nowMillis,
                                                           @Nullable List<Integer> sessionIndexesDisplayedInList,
                                                           @NonNull Set<String> collapsedProjectSessionNames,
                                                           @NonNull Set<String> hiddenSessionNames) {
        List<DiagnosticsSessionLine> lines = new ArrayList<>();
        if (service == null) return lines;

        SessionNewActivityStore activityStore = service.getSessionNewActivityStore();
        List<TermuxSession> termuxSessions = service.getTermuxSessions();
        for (int sessionIndex = 0; sessionIndex < termuxSessions.size(); sessionIndex++) {
            TermuxSession termuxSession = termuxSessions.get(sessionIndex);
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            String name = terminalSession.mSessionName == null ? "" : terminalSession.mSessionName;
            boolean alive = terminalSession.isRunning();

            Long lastOutputMillis = activityStore != null ? activityStore.getLastOutputActivityTimeMillis(name) : null;
            boolean hasLastActivity = lastOutputMillis != null;
            long secondsSinceLastActivity = hasLastActivity
                ? Math.max(0, (nowMillis - lastOutputMillis) / 1000)
                : 0;

            TerminalEmulator emulator = terminalSession.getEmulator();
            TerminalBuffer screen = emulator == null ? null : emulator.getScreen();
            int transcriptRows = screen == null ? 0 : screen.getActiveTranscriptRows();
            int columns = emulator == null ? 0 : emulator.mColumns;

            ShellInputDeliveryRecord deliveryRecord = terminalSession.getShellInputDeliveryRecord();
            DiagnosticsShellInputDelivery shellInputDelivery = new DiagnosticsShellInputDelivery(
                deliveryRecord.getBytesAcceptedForDelivery(),
                deliveryRecord.getBytesWrittenToTheShell(),
                deliveryRecord.getBytesDiscardedBeforeDelivery(),
                deliveryRecord.isWriterRunning(),
                deliveryRecord.getWriterStoppedReason());

            DiagnosticsSessionStatusline statusline = activityStore == null
                ? new DiagnosticsSessionStatusline(null, null, null, SessionNewActivityTier.NONE)
                : new DiagnosticsSessionStatusline(
                    activityStore.getStatuslineCallTimeMillis(name),
                    activityStore.getStatuslineOutTimeMillis(name),
                    activityStore.getStatuslineReplyTimeMillis(name),
                    activityStore.tierFor(name, nowMillis));

            DiagnosticsScrollGestureRouting scrollGestureRouting = emulator == null
                ? DiagnosticsScrollGestureRouting.withoutAnEmulator()
                : DiagnosticsScrollGestureRouting.ofEmulatorState(
                    emulator.isMouseTrackingActive(), emulator.isAlternateBufferActive());

            DiagnosticsSessionListDisplay listDisplay =
                DiagnosticsSessionListDisplay.ofSessionIndex(sessionIndex, sessionIndexesDisplayedInList);

            lines.add(new DiagnosticsSessionLine(name, alive, secondsSinceLastActivity, hasLastActivity,
                transcriptRows, columns, listDisplay,
                shellInputDelivery, statusline, scrollGestureRouting,
                DiagnosticsSessionListAbsence.ofListState(listDisplay, name,
                    collapsedProjectSessionNames, hiddenSessionNames)));
        }
        return lines;
    }

    private int openTabCount(@NonNull TermuxActivity activity) {
        TermuxBrowserController browserController = activity.getTermuxBrowserController();
        return browserController != null ? browserController.getTotalOpenTabCount() : 0;
    }

    private int tabHistoryEntryCount(@NonNull TermuxActivity activity) {
        TermuxBrowserController browserController = activity.getTermuxBrowserController();
        return browserController != null ? browserController.getTabHistoryEntryCount() : 0;
    }
}
