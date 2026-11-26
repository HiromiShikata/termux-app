package com.termux.app.terminal;

import androidx.annotation.NonNull;

public final class CallToUserAlertGate {

    private CallToUserAlertGate() {
    }

    public static boolean shouldAlert(@NonNull CallToUserAlertLedger ledger, boolean mayAlert,
                                      boolean newCallRecorded, @NonNull String sessionName,
                                      @NonNull String callCycleKey, @NonNull String tagValue) {
        if (!mayAlert) return false;
        if (callCycleKey.equals(tagValue)) {
            ledger.recordAlerted(sessionName, callCycleKey, tagValue);
            return true;
        }
        return newCallRecorded && ledger.markAlertedIfNew(sessionName, callCycleKey, tagValue);
    }
}
