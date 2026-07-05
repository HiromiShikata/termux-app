package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import com.termux.BuildConfig;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.browser.TermuxBrowserController;
import com.termux.app.terminal.SessionNewActivityStore;
import com.termux.app.terminal.TermuxSessionsListViewController;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import java.util.ArrayList;
import java.util.List;

public final class DiagnosticsReportCollector {

    @NonNull
    private final DiagnosticEventLog mEventLog;

    public DiagnosticsReportCollector() {
        this(DiagnosticEventLogHolder.getInstance());
    }

    public DiagnosticsReportCollector(@NonNull DiagnosticEventLog eventLog) {
        mEventLog = eventLog;
    }

    @NonNull
    public DiagnosticsReport collect(@NonNull TermuxActivity activity, long nowMillis) {
        TermuxService service = activity.getTermuxService();

        int countedTowardCap = service != null ? service.getTermuxSessionsSize() : 0;
        int displayedCount = displayedSessionCount(activity);
        int maxSessionsCap = activity.getPreferences().getSessionDefinitionMaxSessions();

        List<DiagnosticsSessionLine> sessionLines = buildSessionLines(service, nowMillis);
        int openTabCount = openTabCount(activity);
        int tabHistoryEntryCount = tabHistoryEntryCount(activity);
        boolean wakeLockHeld = service != null && service.isWakeLockHeld();
        boolean foreground = activity.isVisible();

        List<DiagnosticEvent> recentEvents = mEventLog.tail(50);

        return new DiagnosticsReport(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, nowMillis,
            countedTowardCap, displayedCount, maxSessionsCap, sessionLines,
            openTabCount, tabHistoryEntryCount, wakeLockHeld, foreground, recentEvents);
    }

    private int displayedSessionCount(@NonNull TermuxActivity activity) {
        TermuxSessionsListViewController controller = activity.getTermuxSessionListViewController();
        return controller != null ? controller.getVisibleSessionCount() : 0;
    }

    @NonNull
    private List<DiagnosticsSessionLine> buildSessionLines(TermuxService service, long nowMillis) {
        List<DiagnosticsSessionLine> lines = new ArrayList<>();
        if (service == null) return lines;

        SessionNewActivityStore activityStore = service.getSessionNewActivityStore();
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            String name = terminalSession.mSessionName == null ? "" : terminalSession.mSessionName;
            boolean alive = terminalSession.isRunning();

            Long lastOutputMillis = activityStore != null ? activityStore.getLastOutputActivityTimeMillis(name) : null;
            boolean hasLastActivity = lastOutputMillis != null;
            long secondsSinceLastActivity = hasLastActivity
                ? Math.max(0, (nowMillis - lastOutputMillis) / 1000)
                : 0;

            lines.add(new DiagnosticsSessionLine(name, alive, secondsSinceLastActivity, hasLastActivity));
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
