package com.termux.app.terminal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CallToUserTagController {

    public interface CallTrigger {
        void onCallToUser(String sessionKey, String reason);
    }

    private final CallTrigger callTrigger;

    private final Map<String, CallToUserTagScanner> mScannerBySessionKey = new ConcurrentHashMap<>();

    public CallToUserTagController(CallTrigger callTrigger) {
        this.callTrigger = callTrigger;
    }

    public void onSessionTextChanged(String sessionKey, String screenText) {
        if (sessionKey == null) return;

        CallToUserTagScanner scanner = scannerForSession(sessionKey);
        for (String reason : scanner.newReasons(screenText)) {
            callTrigger.onCallToUser(sessionKey, reason);
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
