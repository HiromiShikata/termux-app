package com.termux.app.diagnostics;

import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.termux.app.TermuxActivity;
import com.termux.view.TerminalView;
import com.termux.view.scroll.LatestTerminalScrollStep;
import com.termux.view.scroll.TerminalScrollStepCounter;
import com.termux.view.scroll.TerminalScrollStepCounterHolder;

public final class ScrollWithoutDrawEpisodeCheck {

    private ScrollWithoutDrawEpisodeCheck() {
    }

    public static void run(@NonNull TermuxActivity activity, long nowMillis) {
        TerminalView terminalView = activity.getTerminalView();
        if (terminalView == null) return;
        TerminalScrollStepCounter scrollStepCounter = TerminalScrollStepCounterHolder.getInstance();
        LatestTerminalScrollStep latestScrollStep = LatestTerminalScrollStep.of(scrollStepCounter);
        if (!latestScrollStep.hasStepped()) return;
        DrawTimeRecorder terminalDrawTimeRecorder = TerminalDrawTimeRecorderHolder.getInstance();
        DiagnosticsDrawTime terminalDrawTime = terminalDrawTimeRecorder
            .snapshot(terminalView, SystemClock.elapsedRealtime());
        if (!terminalDrawTime.hasDrawn()) return;
        long lastScrollStepAtMillis = latestScrollStep.getSteppedAtMillis();
        long lastTerminalDrawAtMillis = nowMillis - terminalDrawTime.getMillisSinceLastDraw();
        if (!ScrollWithoutDrawEpisodeRecorderHolder.getInstance()
                .recordEpisode(lastScrollStepAtMillis, lastTerminalDrawAtMillis,
                    terminalDrawTimeRecorder.getLastDrawElapsedRealtimeMillis(terminalView))) {
            return;
        }
        DiagnosticEventLogHolder.record(DiagnosticEventType.SCROLL_WITHOUT_DRAW,
            "terminal last drew " + (lastScrollStepAtMillis - lastTerminalDrawAtMillis)
                + " ms before the most recent scroll step, after "
                + scrollStepCounter.getTotalStepCount() + " scroll steps");
    }
}
