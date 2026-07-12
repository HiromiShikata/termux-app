package com.termux.shared.termux.crash;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.logger.Logger;

public final class CrashReportSupplementHolder {

    private static final String LOG_TAG = "CrashReportSupplementHolder";

    @Nullable
    private static volatile CrashReportSupplement sSupplement;

    private CrashReportSupplementHolder() {
    }

    public static void set(@Nullable CrashReportSupplement supplement) {
        sSupplement = supplement;
    }

    @Nullable
    public static String buildSupplementSection(@NonNull Context context) {
        CrashReportSupplement supplement = sSupplement;
        if (supplement == null) return null;
        try {
            return supplement.buildSupplementSection(context);
        } catch (Throwable throwable) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to build crash report supplement section", throwable);
            return null;
        }
    }
}
