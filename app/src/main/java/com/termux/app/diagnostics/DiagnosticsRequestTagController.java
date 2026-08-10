package com.termux.app.diagnostics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class DiagnosticsRequestTagController {

    public interface DiagnosticsReportSender {
        void sendDiagnosticsReportToSession(@NonNull String sessionKey);
    }

    private final Map<String, DiagnosticsRequestTagScanner> mScannerBySessionKey = new HashMap<>();

    private DiagnosticsReportSender mReportSender;

    public DiagnosticsRequestTagController(@Nullable DiagnosticsReportSender reportSender) {
        mReportSender = reportSender;
    }

    public void setReportSender(@Nullable DiagnosticsReportSender reportSender) {
        mReportSender = reportSender;
    }

    public void onSessionTextChanged(@Nullable String sessionKey, @Nullable String screenText) {
        if (sessionKey == null) return;

        DiagnosticsReportSender reportSender = mReportSender;
        if (reportSender == null) return;

        DiagnosticsRequestTagScanner scanner = scannerForSession(sessionKey);
        for (String ignoredRequestLabel : scanner.newReportRequests(screenText)) {
            reportSender.sendDiagnosticsReportToSession(sessionKey);
        }
    }

    public void forgetSession(@Nullable String sessionKey) {
        if (sessionKey == null) return;
        mScannerBySessionKey.remove(sessionKey);
    }

    private DiagnosticsRequestTagScanner scannerForSession(@NonNull String sessionKey) {
        DiagnosticsRequestTagScanner scanner = mScannerBySessionKey.get(sessionKey);
        if (scanner == null) {
            scanner = new DiagnosticsRequestTagScanner();
            mScannerBySessionKey.put(sessionKey, scanner);
        }
        return scanner;
    }
}
