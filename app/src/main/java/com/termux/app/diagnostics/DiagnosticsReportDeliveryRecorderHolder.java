package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class DiagnosticsReportDeliveryRecorderHolder {

    private static final DiagnosticsReportDeliveryRecorder INSTANCE =
        new DiagnosticsReportDeliveryRecorder();

    private DiagnosticsReportDeliveryRecorderHolder() {
    }

    @NonNull
    public static DiagnosticsReportDeliveryRecorder getInstance() {
        return INSTANCE;
    }
}
