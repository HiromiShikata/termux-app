package com.termux.terminal;

public final class RemoteRepaintWindowSizeNudge {

    private final boolean shouldNudge;
    private final int nudgedRows;
    private final int restoredRows;

    private RemoteRepaintWindowSizeNudge(boolean shouldNudge, int nudgedRows, int restoredRows) {
        this.shouldNudge = shouldNudge;
        this.nudgedRows = nudgedRows;
        this.restoredRows = restoredRows;
    }

    public static RemoteRepaintWindowSizeNudge forCurrentSize(int columns, int rows) {
        if (columns <= 0 || rows <= 0) {
            return new RemoteRepaintWindowSizeNudge(false, rows, rows);
        }
        int nudgedRows = Math.max(1, rows - 1);
        if (nudgedRows == rows) {
            return new RemoteRepaintWindowSizeNudge(false, rows, rows);
        }
        return new RemoteRepaintWindowSizeNudge(true, nudgedRows, rows);
    }

    public boolean shouldNudge() {
        return shouldNudge;
    }

    public int getNudgedRows() {
        return nudgedRows;
    }

    public int getRestoredRows() {
        return restoredRows;
    }
}
