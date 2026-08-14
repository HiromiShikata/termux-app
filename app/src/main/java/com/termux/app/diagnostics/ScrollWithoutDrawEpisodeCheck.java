package com.termux.app.diagnostics;

import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.termux.app.TermuxActivity;
import com.termux.view.TerminalView;
import com.termux.view.scroll.TerminalScrollEvent;
import com.termux.view.scroll.TerminalScrollStepCounter;
import com.termux.view.scroll.TerminalScrollStepCounterHolder;

public final class ScrollWithoutDrawEpisodeCheck {

    private ScrollWithoutDrawEpisodeCheck() {
    }

    public static void run(@NonNull TermuxActivity activity, long nowMillis) {
        TerminalView terminalView = activity.getTerminalView();
        if (terminalView == null) return;
        TerminalScrollStepCounter scrollStepCounter = TerminalScrollStepCounterHolder.getInstance();
        if (scrollStepCounter.getTotalStepCount() == 0) return;
        DiagnosticsDrawTime terminalDrawTime = TerminalDrawTimeRecorderHolder.getInstance()
            .snapshot(terminalView, SystemClock.elapsedRealtime());
        if (!terminalDrawTime.hasDrawn()) return;
        long lastScrollStepAtMillis = lastScrollStepAtMillis(scrollStepCounter);
        long lastTerminalDrawAtMillis = nowMillis - terminalDrawTime.getMillisSinceLastDraw();
        if (!ScrollWithoutDrawEpisodeRecorderHolder.getInstance()
                .recordEpisode(lastScrollStepAtMillis, lastTerminalDrawAtMillis)) {
            return;
        }
        DiagnosticEventLogHolder.record(DiagnosticEventType.SCROLL_WITHOUT_DRAW,
            "terminal last drew " + (lastScrollStepAtMillis - lastTerminalDrawAtMillis)
                + " ms before the most recent scroll step, after "
                + scrollStepCounter.getTotalStepCount() + " scroll steps");
    }

    private static long lastScrollStepAtMillis(@NonNull TerminalScrollStepCounter scrollStepCounter) {
        long lastStepAtMillis = Long.MIN_VALUE;
        for (TerminalScrollEvent destination : TerminalScrollEvent.values()) {
            if (scrollStepCounter.getStepCount(destination) == 0) continue;
            long destinationLastStepAtMillis = scrollStepCounter.getLastStepAtMillis(destination);
            if (destinationLastStepAtMillis > lastStepAtMillis) {
                lastStepAtMillis = destinationLastStepAtMillis;
            }
        }
        return lastStepAtMillis;
    }
}
