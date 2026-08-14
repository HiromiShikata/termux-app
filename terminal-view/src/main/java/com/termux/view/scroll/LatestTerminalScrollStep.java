package com.termux.view.scroll;

public final class LatestTerminalScrollStep {

    private final boolean mHasStepped;

    private final long mSteppedAtMillis;

    private LatestTerminalScrollStep(boolean hasStepped, long steppedAtMillis) {
        mHasStepped = hasStepped;
        mSteppedAtMillis = steppedAtMillis;
    }

    public static LatestTerminalScrollStep of(TerminalScrollStepCounter scrollStepCounter) {
        boolean hasStepped = false;
        long steppedAtMillis = 0L;
        for (TerminalScrollEvent destination : TerminalScrollEvent.values()) {
            if (scrollStepCounter.getStepCount(destination) == 0) {
                continue;
            }
            long destinationSteppedAtMillis = scrollStepCounter.getLastStepAtMillis(destination);
            if (!hasStepped || destinationSteppedAtMillis > steppedAtMillis) {
                hasStepped = true;
                steppedAtMillis = destinationSteppedAtMillis;
            }
        }
        return new LatestTerminalScrollStep(hasStepped, steppedAtMillis);
    }

    public boolean hasStepped() {
        return mHasStepped;
    }

    public long getSteppedAtMillis() {
        if (!mHasStepped) {
            throw new IllegalStateException("no scroll step has been recorded");
        }
        return mSteppedAtMillis;
    }
}
