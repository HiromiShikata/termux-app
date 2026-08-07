package com.termux.app.apkupdate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class UpdateTagUpdateController {

    public interface ReasonTrigger {
        void onUpdateRequested(String reason);
    }

    private final ReasonTrigger reasonTrigger;

    private final Map<String, UpdateTermuxAppTagScanner> mScannerBySessionKey = new ConcurrentHashMap<>();

    public UpdateTagUpdateController(ReasonTrigger reasonTrigger) {
        this.reasonTrigger = reasonTrigger;
    }

    public void onSessionTextChanged(String sessionKey, String screenText) {
        if (sessionKey == null) return;

        UpdateTermuxAppTagScanner scanner = scannerForSession(sessionKey);
        for (String reason : scanner.newReasons(screenText)) {
            reasonTrigger.onUpdateRequested(reason);
        }
    }

    public void forgetSession(String sessionKey) {
        if (sessionKey == null) return;
        mScannerBySessionKey.remove(sessionKey);
    }

    private UpdateTermuxAppTagScanner scannerForSession(String sessionKey) {
        return mScannerBySessionKey.computeIfAbsent(sessionKey, key -> new UpdateTermuxAppTagScanner());
    }
}
