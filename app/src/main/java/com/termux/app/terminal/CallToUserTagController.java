package com.termux.app.terminal;

import com.termux.app.outputtag.OutputTagOccurrence;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CallToUserTagController {

    public interface CallTrigger {
        void onCallToUser(String sessionKey, String reason, String callCycleKey);

        default void onCallToUserInScan(String sessionKey, String reason, String callCycleKey,
                                        boolean newestInScan) {
            onCallToUser(sessionKey, reason, callCycleKey);
        }
    }

    private final CallTrigger callTrigger;

    private final Map<String, CallToUserTagScanner> mScannerBySessionKey = new ConcurrentHashMap<>();

    public CallToUserTagController(CallTrigger callTrigger) {
        this.callTrigger = callTrigger;
    }

    public void onSessionTextChanged(String sessionKey, String screenText) {
        if (sessionKey == null) return;

        CallToUserTagScanner scanner = scannerForSession(sessionKey);
        List<OutputTagOccurrence> newCalls = scanner.newCalls(screenText);
        for (int index = 0; index < newCalls.size(); index++) {
            OutputTagOccurrence call = newCalls.get(index);
            callTrigger.onCallToUserInScan(sessionKey, call.getValue(), call.getDeduplicationKey(),
                index == newCalls.size() - 1);
        }
    }

    public void forgetSession(String sessionKey) {
        if (sessionKey == null) return;
        mScannerBySessionKey.remove(sessionKey);
    }

    private CallToUserTagScanner scannerForSession(String sessionKey) {
        return mScannerBySessionKey.computeIfAbsent(sessionKey, key -> new CallToUserTagScanner());
    }
}
