package com.termux.app.terminal;

public final class CallingSessionSplit {

    private final int aboveCount;
    private final int belowCount;
    private final boolean currentCalling;

    CallingSessionSplit(int aboveCount, int belowCount, boolean currentCalling) {
        this.aboveCount = aboveCount;
        this.belowCount = belowCount;
        this.currentCalling = currentCalling;
    }

    public int getAboveCount() {
        return aboveCount;
    }

    public int getBelowCount() {
        return belowCount;
    }

    public boolean isCurrentCalling() {
        return currentCalling;
    }

    public int getTotalCount() {
        return aboveCount + belowCount + (currentCalling ? 1 : 0);
    }
}
