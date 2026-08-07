package com.termux.app.terminal;

import com.termux.app.outputtag.OutputTagOccurrence;

import java.util.HashMap;
import java.util.Map;

public final class CallToUserTagController {

    public interface CallTrigger {
        void onCallToUser(String sessionKey, String reason, String callCycleKey);
    }

    private final CallTrigger callTrigger;

    private final Map<String, CallToUserTagScanner> mScannerBySessionKey = new HashMap<>();

    public CallToUserTagController(CallTrigger callTrigger) {
        this.callTrigger = callTrigger;
    }

    public void onSessionTextChanged(String sessionKey, String screenText) {
        if (sessionKey == null) return;

        CallToUserTagScanner scanner = scannerForSession(sessionKey);
        for (OutputTagOccurrence call : scanner.newCalls(screenText)) {
            callTrigger.onCallToUser(sessionKey, call.getValue(), call.getDeduplicationKey());
        }
    }

    public void forgetSession(String sessionKey) {
        if (sessionKey == null) return;
        mScannerBySessionKey.remove(sessionKey);
    }

    private CallToUserTagScanner scannerForSession(String sessionKey) {
        CallToUserTagScanner scanner = mScannerBySessionKey.get(sessionKey);
        if (scanner == null) {
            scanner = new CallToUserTagScanner();
            mScannerBySessionKey.put(sessionKey, scanner);
        }
        return scanner;
    }
}
