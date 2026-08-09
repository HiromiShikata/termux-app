package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import com.termux.app.sessiondefinition.SessionReconnectReason;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Accumulates the main-thread time spent reconnecting dead sessions, together with how many sessions
 * were still waiting behind the slowest one, so a report can separate a single slow reconnect from a
 * burst that occupies the main thread once per session.
 * <p>
 * The same figures are kept a second time against the rule that planned each reconnect. Three rules
 * plan one, and they do not mean the same thing: a session whose shell process is gone has genuinely
 * lost its connection, while a session that is merely quiet may be healthy and idle, and replacing it
 * discards input. Only a per-rule split says which of them is producing the reconnects being seen.
 * <p>
 * A record call takes no lock and writes no log, and allocates only the first time each rule is seen,
 * so an instance is cheap enough to stay enabled permanently on the main thread. State is
 * process-lifetime only and is never persisted.
 */
public final class SessionReconnectCostCounter {

    private static final long NANOS_PER_MILLISECOND = 1000000L;

    private static final class ElapsedNanosForOneReason {

        private long reconnectCount;
        private long totalElapsedNanos;
        private long maxElapsedNanos;

        private void record(long elapsedNanos) {
            reconnectCount++;
            totalElapsedNanos += elapsedNanos;
            if (elapsedNanos > maxElapsedNanos) {
                maxElapsedNanos = elapsedNanos;
            }
        }
    }

    private final Map<SessionReconnectReason, ElapsedNanosForOneReason> elapsedNanosByReason =
        new EnumMap<>(SessionReconnectReason.class);

    private long mReconnectCount;
    private long mTotalElapsedNanos;
    private long mMaxElapsedNanos;
    private int mSessionsStillQueuedAtMaxElapsed;

    public void record(@NonNull SessionReconnectReason reason, long elapsedNanos,
                       int sessionsStillQueued) {
        mReconnectCount++;
        mTotalElapsedNanos += elapsedNanos;
        if (elapsedNanos > mMaxElapsedNanos) {
            mMaxElapsedNanos = elapsedNanos;
            mSessionsStillQueuedAtMaxElapsed = sessionsStillQueued;
        }
        elapsedNanosForOneReason(reason).record(elapsedNanos);
    }

    private ElapsedNanosForOneReason elapsedNanosForOneReason(@NonNull SessionReconnectReason reason) {
        ElapsedNanosForOneReason elapsedNanos = elapsedNanosByReason.get(reason);
        if (elapsedNanos == null) {
            elapsedNanos = new ElapsedNanosForOneReason();
            elapsedNanosByReason.put(reason, elapsedNanos);
        }
        return elapsedNanos;
    }

    public long getReconnectCount() {
        return mReconnectCount;
    }

    public long getTotalElapsedMillis() {
        return mTotalElapsedNanos / NANOS_PER_MILLISECOND;
    }

    public long getMaxElapsedMillis() {
        return mMaxElapsedNanos / NANOS_PER_MILLISECOND;
    }

    public int getSessionsStillQueuedAtMaxElapsed() {
        return mSessionsStillQueuedAtMaxElapsed;
    }

    @NonNull
    public List<DiagnosticsSessionReconnectCostByReason> getCostsByReason() {
        List<DiagnosticsSessionReconnectCostByReason> costsByReason = new ArrayList<>();
        for (SessionReconnectReason reason : SessionReconnectReason.values()) {
            ElapsedNanosForOneReason elapsedNanos = elapsedNanosByReason.get(reason);
            if (elapsedNanos == null) {
                continue;
            }
            costsByReason.add(new DiagnosticsSessionReconnectCostByReason(reason,
                elapsedNanos.reconnectCount, elapsedNanos.totalElapsedNanos / NANOS_PER_MILLISECOND,
                elapsedNanos.maxElapsedNanos / NANOS_PER_MILLISECOND));
        }
        return costsByReason;
    }
}
