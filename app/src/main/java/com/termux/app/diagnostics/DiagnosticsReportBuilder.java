package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class DiagnosticsReportBuilder {

    private static final String TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    @NonNull
    public String build(@NonNull DiagnosticsReport report) {
        StringBuilder builder = new StringBuilder();

        builder.append("Termux diagnostics report\n");
        builder.append("Generated: ").append(formatTimestamp(report.getReportTimestampMillis())).append('\n');
        builder.append("App version: ").append(report.getVersionName())
            .append(" (").append(report.getVersionCode()).append(")\n");

        builder.append('\n');
        appendSessionsSection(builder, report);

        builder.append('\n');
        appendBrowserSection(builder, report);

        builder.append('\n');
        appendWakeLockSection(builder, report);

        builder.append('\n');
        appendEventsSection(builder, report);

        return builder.toString();
    }

    private void appendSessionsSection(@NonNull StringBuilder builder, @NonNull DiagnosticsReport report) {
        builder.append("Sessions\n");
        builder.append("  Counted toward cap: ").append(report.getSessionsCountedTowardCap()).append('\n');
        builder.append("  Displayed in list: ").append(report.getSessionsDisplayedCount()).append('\n');
        int orphaned = report.getOrphanedSessionCount();
        if (orphaned > 0) {
            builder.append("  Orphaned (counted but not displayed): ").append(orphaned).append('\n');
        }
        builder.append("  Max sessions cap: ").append(report.getMaxSessionsCap()).append('\n');
        List<DiagnosticsSessionLine> lines = report.getSessionLines();
        if (lines.isEmpty()) {
            builder.append("  (no sessions)\n");
            return;
        }
        for (DiagnosticsSessionLine line : lines) {
            builder.append("  - ").append(line.getName())
                .append(" | ").append(line.isAlive() ? "alive" : "dead")
                .append(" | last activity: ").append(formatSecondsSinceLastActivity(line))
                .append('\n');
        }
    }

    @NonNull
    private String formatSecondsSinceLastActivity(@NonNull DiagnosticsSessionLine line) {
        if (!line.hasLastActivity()) {
            return "n/a";
        }
        return line.getSecondsSinceLastActivity() + "s ago";
    }

    private void appendBrowserSection(@NonNull StringBuilder builder, @NonNull DiagnosticsReport report) {
        builder.append("Browser\n");
        builder.append("  Open tabs: ").append(report.getOpenTabCount()).append('\n');
        builder.append("  Tab-history entries: ").append(report.getTabHistoryEntryCount()).append('\n');
    }

    private void appendWakeLockSection(@NonNull StringBuilder builder, @NonNull DiagnosticsReport report) {
        builder.append("Wake lock\n");
        builder.append("  Held: ").append(report.isWakeLockHeld() ? "yes" : "no").append('\n');
        builder.append("  App state: ").append(report.isForeground() ? "foreground" : "background").append('\n');
    }

    private void appendEventsSection(@NonNull StringBuilder builder, @NonNull DiagnosticsReport report) {
        builder.append("Recent events\n");
        List<DiagnosticEvent> events = report.getRecentEvents();
        if (events.isEmpty()) {
            builder.append("  (no recent events)\n");
            return;
        }
        for (DiagnosticEvent event : events) {
            builder.append("  ").append(formatTimestamp(event.getTimestampMillis()))
                .append(' ').append(event.getType().name());
            if (!event.getDetail().isEmpty()) {
                builder.append(' ').append(event.getDetail());
            }
            builder.append('\n');
        }
    }

    @NonNull
    private String formatTimestamp(long timestampMillis) {
        SimpleDateFormat format = new SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(timestampMillis));
    }
}
