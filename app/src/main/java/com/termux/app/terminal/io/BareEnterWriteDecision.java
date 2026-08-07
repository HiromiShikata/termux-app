package com.termux.app.terminal.io;

import androidx.annotation.Nullable;

public final class BareEnterWriteDecision {

    private BareEnterWriteDecision() {
    }

    public static boolean shouldWrite(@Nullable Long lastBareEnterTimeMillis,
                                      @Nullable Long lastOutputActivityTimeMillis) {
        if (lastBareEnterTimeMillis == null) {
            return true;
        }
        return lastOutputActivityTimeMillis != null
            && lastOutputActivityTimeMillis > lastBareEnterTimeMillis;
    }
}
