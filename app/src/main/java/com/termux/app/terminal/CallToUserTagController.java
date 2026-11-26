package com.termux.app.terminal;

import com.termux.app.outputtag.OutputTagOccurrence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CallToUserTagController {

    public interface CallTrigger {
        void onCallToUser(String sessionKey, String reason, String callCycleKey);

        default void onCallToUserInScan(String sessionKey, String reason, String callCycleKey,
                                        boolean mayAlert) {
            onCallToUser(sessionKey, reason, callCycleKey);
        }

        default boolean hasAlertedCall(String sessionKey, String callCycleKey, String tagValue) {
            return false;
        }
    }

    private final CallTrigger callTrigger;

    private final Map<String, CallToUserTagScanner> mScannerBySessionKey = new ConcurrentHashMap<>();

    private final Map<String, List<OutputTagOccurrence>> mVisibleCallsBySessionKey = new HashMap<>();

    public CallToUserTagController(CallTrigger callTrigger) {
        this.callTrigger = callTrigger;
    }

    public synchronized void onSessionTextChanged(String sessionKey, String screenText) {
        if (sessionKey == null) return;

        CallToUserTagScanner scanner = scannerForSession(sessionKey);
        List<OutputTagOccurrence> visibleCalls = scanner.visibleCalls(screenText);
        List<OutputTagOccurrence> newCalls = scanner.newCalls(screenText);
        int previouslySeenCount = previouslySeenCount(sessionKey, visibleCalls);
        int firstNewIndex = visibleCalls.size() - newCalls.size();
        for (int index = 0; index < newCalls.size(); index++) {
            OutputTagOccurrence call = newCalls.get(index);
            boolean newlyAppended = firstNewIndex + index >= previouslySeenCount;
            boolean newest = index == newCalls.size() - 1;
            callTrigger.onCallToUserInScan(sessionKey, call.getValue(), call.getDeduplicationKey(),
                newest && newlyAppended);
        }
        mVisibleCallsBySessionKey.put(sessionKey, visibleCalls);
    }

    public synchronized void forgetSession(String sessionKey) {
        if (sessionKey == null) return;
        mScannerBySessionKey.remove(sessionKey);
        mVisibleCallsBySessionKey.remove(sessionKey);
    }

    private int previouslySeenCount(String sessionKey, List<OutputTagOccurrence> visibleCalls) {
        List<OutputTagOccurrence> previousCalls = mVisibleCallsBySessionKey.get(sessionKey);
        if (previousCalls != null) {
            return CallToUserOccurrenceAlignment.matchedPrefixLength(previousCalls, visibleCalls);
        }
        int count = 0;
        while (count < visibleCalls.size()) {
            OutputTagOccurrence call = visibleCalls.get(count);
            if (!callTrigger.hasAlertedCall(sessionKey, call.getDeduplicationKey(), call.getValue())) break;
            count++;
        }
        return count;
    }

    private CallToUserTagScanner scannerForSession(String sessionKey) {
        return mScannerBySessionKey.computeIfAbsent(sessionKey, key -> new CallToUserTagScanner());
    }
}
