package com.termux.app.diagnostics;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.TermuxActivity;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.crash.CrashReportSupplement;

import java.util.List;

public final class CrashReportDiagnosticsSupplement implements CrashReportSupplement {

    private static final String LOG_TAG = "CrashReportDiagnosticsSupplement";

    static final String SECTION_HEADER = "## Session diagnostics";

    @NonNull
    private final DiagnosticsReportCollector mCollector;
    @NonNull
    private final DiagnosticsReportBuilder mReportBuilder;
    @NonNull
    private final DiagnosticEventLog mEventLog;

    public CrashReportDiagnosticsSupplement() {
        this(new DiagnosticsReportCollector(), new DiagnosticsReportBuilder(),
            DiagnosticEventLogHolder.getInstance());
    }

    CrashReportDiagnosticsSupplement(@NonNull DiagnosticsReportCollector collector,
                                     @NonNull DiagnosticsReportBuilder reportBuilder,
                                     @NonNull DiagnosticEventLog eventLog) {
        mCollector = collector;
        mReportBuilder = reportBuilder;
        mEventLog = eventLog;
    }

    @Override
    @Nullable
    public String buildSupplementSection(@NonNull Context context) {
        try {
            return SECTION_HEADER + "\n\n" + body();
        } catch (Throwable throwable) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to build session diagnostics for crash report", throwable);
            return null;
        }
    }

    @NonNull
    private String body() {
        TermuxActivity activity = TermuxActivityHolder.get();
        if (activity != null) {
            DiagnosticsReport report = mCollector.collect(activity, System.currentTimeMillis());
            return mReportBuilder.build(report);
        }
        return recentEventsOnly();
    }

    @NonNull
    private String recentEventsOnly() {
        StringBuilder builder = new StringBuilder();
        builder.append("No live activity; recent events only\n");
        builder.append("Recent events\n");
        List<DiagnosticEvent> events = mEventLog.tail(50);
        if (events.isEmpty()) {
            builder.append("  (no recent events)\n");
            return builder.toString();
        }
        for (DiagnosticEvent event : events) {
            builder.append("  ").append(event.getType().name());
            if (!event.getDetail().isEmpty()) {
                builder.append(' ').append(event.getDetail());
            }
            builder.append('\n');
        }
        return builder.toString();
    }
}
