package com.termux.app.terminal;

public final class NextVisibleSessionAfterKillSelector {

    public static final int NO_SELECTION = -1;

    private NextVisibleSessionAfterKillSelector() {
    }

    public static int selectNextVisibleSessionPosition(
            int visibleSessionCount,
            int killedVisiblePosition) {
        if (killedVisiblePosition < 0 || killedVisiblePosition >= visibleSessionCount) {
            return visibleSessionCount > 0 ? 0 : NO_SELECTION;
        }

        int lastPosition = visibleSessionCount - 1;
        if (killedVisiblePosition < lastPosition) {
            return killedVisiblePosition + 1;
        }
        if (killedVisiblePosition > 0) {
            return killedVisiblePosition - 1;
        }
        return NO_SELECTION;
    }
}
