package com.termux.app.browser;

public final class BrowserTabPersistenceBatch {

    private int openBatchCount;

    private boolean writeIsDeferred;

    public void begin() {
        openBatchCount++;
    }

    public boolean requestWrite() {
        if (openBatchCount == 0) return true;
        writeIsDeferred = true;
        return false;
    }

    public boolean end() {
        if (openBatchCount == 0) return false;
        openBatchCount--;
        if (openBatchCount > 0) return false;
        boolean writeIsDue = writeIsDeferred;
        writeIsDeferred = false;
        return writeIsDue;
    }
}
