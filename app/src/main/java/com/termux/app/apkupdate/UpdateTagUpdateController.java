package com.termux.app.apkupdate;

import java.util.HashMap;
import java.util.Map;

public final class UpdateTagUpdateController {

    public interface ReasonTrigger {
        void onUpdateRequested(String reason);
    }

    private final ReasonTrigger reasonTrigger;

    private final Map<String, UpdateTermuxAppTagScanner> mScannerBySessionKey = new HashMap<>();

    public UpdateTagUpdateController(ReasonTrigger reasonTrigger) {
        this.reasonTrigger = reasonTrigger;
    }

    public void onSessionTextChanged(String sessionKey, String screenText) {
        if (sessionKey == null) return;

        UpdateTermuxAppTagScanner scanner = scannerForSession(sessionKey);
        String reason = scanner.newReason(screenText);
        if (reason == null) return;

        scanner.markTriggered(reason);
        reasonTrigger.onUpdateRequested(reason);
    }

    public void forgetSession(String sessionKey) {
        if (sessionKey == null) return;
        mScannerBySessionKey.remove(sessionKey);
    }

    private UpdateTermuxAppTagScanner scannerForSession(String sessionKey) {
        UpdateTermuxAppTagScanner scanner = mScannerBySessionKey.get(sessionKey);
        if (scanner == null) {
            scanner = new UpdateTermuxAppTagScanner();
            mScannerBySessionKey.put(sessionKey, scanner);
        }
        return scanner;
    }
}
