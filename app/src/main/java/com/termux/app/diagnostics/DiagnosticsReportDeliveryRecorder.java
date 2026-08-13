package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class DiagnosticsReportDeliveryRecorder {

    @NonNull
    private volatile DiagnosticsReportDelivery mLastDelivery = DiagnosticsReportDelivery.NONE;

    public void recordDelivery(@NonNull DiagnosticsReportDelivery delivery) {
        mLastDelivery = delivery;
    }

    @NonNull
    public DiagnosticsReportDelivery snapshot() {
        return mLastDelivery;
    }
}
