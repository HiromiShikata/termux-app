package com.termux.app.apkupdate;

import java.util.HashMap;
import java.util.Map;

public final class UpdateTagUpdateController {

    public interface ReasonTrigger {
        void onUpdateRequested(String reason);
    }

    private final ReasonTrigger reasonTrigger;

    private final Map<String, UpdateTermuxUpTagScanner> mScannerBySessionKey = new HashMap<>();

    public UpdateTagUpdateController(ReasonTrigger reasonTrigger) {
        this.reasonTrigger = reasonTrigger;
    }

    public void onSessionTextChanged(String sessionKey, String screenText) {
        if (sessionKey == null) return;

        UpdateTermuxUpTagScanner scanner = scannerForSession(sessionKey);
        String reason = scanner.newReason(screenText);
        if (reason == null) return;

        scanner.markTriggered(reason);
        reasonTrigger.onUpdateRequested(reason);
    }

    public void forgetSession(String sessionKey) {
        if (sessionKey == null) return;
        mScannerBySessionKey.remove(sessionKey);
    }

    private UpdateTermuxUpTagScanner scannerForSession(String sessionKey) {
        UpdateTermuxUpTagScanner scanner = mScannerBySessionKey.get(sessionKey);
        if (scanner == null) {
            scanner = new UpdateTermuxUpTagScanner();
            mScannerBySessionKey.put(sessionKey, scanner);
        }
        return scanner;
    }
}
