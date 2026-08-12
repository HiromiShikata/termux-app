package com.termux.app.diagnostics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class DiagnosticsReportBuilder {

    public static final int PASTE_LIMIT_CHARACTERS = 5000;

    private static final String TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static final int LONGEST_STALL_STACK_TRACE_BUDGET_CHARACTERS = 1200;

    private static final int STALL_HOT_PATH_RANKING_BUDGET_CHARACTERS = 900;

    private static final int STALL_HOT_PATH_STACK_TRACE_BUDGET_CHARACTERS = 600;

    private static final int BUSIEST_TARGET_BUDGET_CHARACTERS = 500;

    private static final int PENDING_MESSAGE_LINE_BUDGET_CHARACTERS = 1000;

    private static final int UNDELIVERED_SHELL_INPUT_BUDGET_CHARACTERS = 600;

    private static final int SHELL_EXIT_STATUS_BUDGET_CHARACTERS = 400;

    private static final int OMISSION_NOTE_BUDGET_CHARACTERS = 96;

    private static final int SECTION_CEILING_HEADROOM_CHARACTERS = 300;

    private static final int MAXIMUM_COMMAND_NAMES_REPORTED = 8;

    private static final int MAIN_THREAD_COST_SECTION_CEILING_CHARACTERS =
        PASTE_LIMIT_CHARACTERS - SECTION_CEILING_HEADROOM_CHARACTERS;

    @NonNull
    public String build(@NonNull DiagnosticsReport report) {
        StringBuilder builder = new StringBuilder();

        builder.append("Termux diagnostics report\n");
        builder.append("Generated: ").append(formatTimestamp(report.getReportTimestampMillis())).append('\n');
        builder.append("App version: ").append(report.getVersionName())
            .append(" (").append(report.getVersionCode()).append(")\n");

        appendVersionChangeLine(builder, report.getVersionChange());

        builder.append("Process uptime: ").append(formatUptime(report.getProcessUptimeMillis())).append('\n');

        builder.append('\n');
        appendUndeliveredShellInputSection(builder, report);

        builder.append('\n');
        appendReplacedSessionShellInputSection(builder, report);

        builder.append('\n');
        appendShellExitSection(builder, report);

        builder.append('\n');
        appendPhantomProcessMonitorSection(builder, report);

        builder.append('\n');
        appendAppProcessPopulationSection(builder, report);

        builder.append('\n');
        appendMainThreadCostSection(builder, report);

        builder.append('\n');
        appendBackgroundCycleSection(builder, report);

        builder.append('\n');
        appendMemorySection(builder, report);

        builder.append('\n');
        appendBrowserSection(builder, report);

        builder.append('\n');
        appendWakeLockSection(builder, report);

        builder.append('\n');
        appendSessionsSection(builder, report);

        builder.append('\n');
        appendEventsSection(builder, report);

        return builder.toString();
    }

    private void appendVersionChangeLine(@NonNull StringBuilder builder,
                                         @NonNull DiagnosticsVersionChange versionChange) {
        if (!versionChange.isFirstLaunchOfThisVersion()) return;
        if (versionChange.hasPreviousVersionCode()) {
            builder.append("First launch after replacing version code ")
                .append(versionChange.getPreviousVersionCode()).append('\n');
            return;
        }
        builder.append("First launch after installation\n");
    }

    @NonNull
    private String formatUptime(long uptimeMillis) {
        long totalSeconds = uptimeMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours + "h " + minutes + "m " + seconds + "s";
    }

    private void appendMemorySection(@NonNull StringBuilder builder, @NonNull DiagnosticsReport report) {
        DiagnosticsMemoryUsage memoryUsage = report.getMemoryUsage();
        builder.append("Memory\n");
        builder.append("  Java heap used: ").append(memoryUsage.getJavaHeapUsedMegabytes()).append(" MB\n");
        builder.append("  Java heap total: ").append(memoryUsage.getJavaHeapTotalMegabytes()).append(" MB\n");
        builder.append("  Java heap max: ").append(memoryUsage.getJavaHeapMaxMegabytes()).append(" MB\n");
        builder.append("  Native heap allocated: ")
            .append(memoryUsage.getNativeHeapAllocatedMegabytes()).append(" MB\n");
    }

    private void appendUndeliveredShellInputSection(@NonNull StringBuilder builder,
                                                    @NonNull DiagnosticsReport report) {
        builder.append("Input accepted from the user that never reached the shell\n");
        List<String> undeliveredLines = new ArrayList<>();
        for (DiagnosticsSessionLine sessionLine : report.getSessionLines()) {
            DiagnosticsShellInputDelivery delivery = sessionLine.getShellInputDelivery();
            if (delivery.getBytesAcceptedButNotWrittenYet() == 0 && delivery.isWriterRunning()) {
                continue;
            }
            undeliveredLines.add("  " + sessionLine.getName() + ": "
                + delivery.getBytesAcceptedButNotWrittenYet() + "B of "
                + delivery.getBytesAcceptedForDelivery() + "B accepted, writer "
                + describeWriterState(delivery));
        }
        if (undeliveredLines.isEmpty()) {
            builder.append("  None: every session wrote everything it accepted\n");
            return;
        }
        appendLinesWithinBudget(builder, undeliveredLines, UNDELIVERED_SHELL_INPUT_BUDGET_CHARACTERS);
    }

    private void appendReplacedSessionShellInputSection(@NonNull StringBuilder builder,
                                                        @NonNull DiagnosticsReport report) {
        DiagnosticsReplacedSessionShellInput replaced = report.getReplacedSessionShellInput();
        builder.append("Input stuck on sessions replaced since the app started\n");
        builder.append("  Sessions replaced with input still undelivered: ")
            .append(replaced.getSessionsReplacedWithInputUndelivered()).append('\n');
        builder.append("  Sessions replaced after the input writer had already stopped: ")
            .append(replaced.getSessionsReplacedAfterTheWriterStopped()).append('\n');
        if (replaced.getWorstUndeliveredBytes() > 0) {
            builder.append("  Most left unwritten: ").append(replaced.getWorstUndeliveredBytes())
                .append("B on ").append(replaced.getWorstUndeliveredSessionName()).append('\n');
        }
        if (!replaced.getLastWriterStopSessionName().isEmpty()) {
            builder.append("  Last writer stop: ").append(replaced.getLastWriterStopSessionName())
                .append(" (").append(replaced.getLastWriterStopReason()).append(")\n");
        }
    }

    private void appendShellExitSection(@NonNull StringBuilder builder, @NonNull DiagnosticsReport report) {
        DiagnosticsShellExits shellExits = report.getShellExits();
        builder.append("Shell exits since the app started\n");
        if (shellExits.getCountsByExitStatus().isEmpty()) {
            builder.append("  None: no shell process has exited yet\n");
            return;
        }
        builder.append("  Total: ").append(shellExits.getTotalExitCount()).append('\n');
        List<String> exitStatusLines = new ArrayList<>();
        for (DiagnosticsShellExitCount countByExitStatus : shellExits.getCountsByExitStatus()) {
            exitStatusLines.add("  Exit status " + countByExitStatus.getExitStatus() + ": "
                + countByExitStatus.getCount());
        }
        appendLinesWithinBudget(builder, exitStatusLines, SHELL_EXIT_STATUS_BUDGET_CHARACTERS);
    }

    private void appendPhantomProcessMonitorSection(@NonNull StringBuilder builder,
                                                    @NonNull DiagnosticsReport report) {
        DiagnosticsPhantomProcessMonitor monitor = report.getPhantomProcessMonitor();
        builder.append("Android phantom process monitor\n");
        builder.append("  Monitor flag: ").append(monitor.getMonitorFlagValue()).append('\n');
        Integer enforcedMaximum = monitor.getEnforcedMaximumPhantomProcesses();
        builder.append("  Enforced maximum: ")
            .append(enforcedMaximum == null ? "not readable" : String.valueOf(enforcedMaximum)).append('\n');
        builder.append("  Can be switched off from settings: ")
            .append(monitor.getMonitorCanBeSwitchedOff() ? "yes" : "no").append('\n');
    }

    private void appendAppProcessPopulationSection(@NonNull StringBuilder builder,
                                                   @NonNull DiagnosticsReport report) {
        DiagnosticsAppProcessPopulation population = report.getAppProcessPopulation();
        builder.append("Processes this app is running\n");
        if (!population.getWasMeasured()) {
            String readFailureMessage = population.getReadFailureMessage();
            builder.append("  Total: ")
                .append(readFailureMessage == null ? "not measured yet" : "not readable")
                .append('\n');
            if (readFailureMessage != null) {
                builder.append("  Read failed: ").append(readFailureMessage).append('\n');
            }
            return;
        }
        builder.append("  Total: ").append(population.getTotalProcessCount()).append('\n');
        List<DiagnosticsProcessCommandCount> counts = population.getCountsByCommandName();
        int reportedCount = Math.min(counts.size(), MAXIMUM_COMMAND_NAMES_REPORTED);
        for (int index = 0; index < reportedCount; index++) {
            DiagnosticsProcessCommandCount count = counts.get(index);
            builder.append("    ").append(count.getCommandName()).append(": ")
                .append(count.getProcessCount()).append('\n');
        }
        int omittedCount = counts.size() - reportedCount;
        if (omittedCount > 0) {
            builder.append("    ").append(omittedCount)
                .append(" further command names left out so this report survives being pasted\n");
        }
    }

    @NonNull
    private String describeWriterState(@NonNull DiagnosticsShellInputDelivery delivery) {
        if (delivery.isWriterRunning()) {
            return "running";
        }
        String writerStoppedReason = delivery.getWriterStoppedReason();
        return "stopped" + (writerStoppedReason == null ? "" : " (" + writerStoppedReason + ")");
    }

    private void appendMainThreadCostSection(@NonNull StringBuilder builder, @NonNull DiagnosticsReport report) {
        builder.append("Main-thread cost\n");
        appendWorkCostLines(builder, "Background output tag scan", report.getBackgroundOutputScanCost());
        appendWorkCostLines(builder, "Open-tag scan on the viewed session",
            report.getForegroundOpenTagScanCost());
        appendWorkCostLines(builder, "Buffer reflow on column-changing resize", report.getBufferReflowCost());
        appendSessionReconnectCostLines(builder, report.getSessionReconnectCost());
        appendMainThreadStallLines(builder, report.getMainThreadStalls());
        appendMainLooperQueueLines(builder, report.getMainLooperQueue());
        appendScrollbarViewCensusLines(builder, report.getScrollbarViewCensus());
    }

    private void appendScrollbarViewCensusLines(@NonNull StringBuilder builder,
                                                @NonNull ScrollbarViewCensus census) {
        builder.append("  Views that can hold a scrollbar fade callback\n");
        builder.append("    Total: ").append(census.getScrollbarViewCount()).append('\n');
        if (census.getBusiestClasses().isEmpty()) {
            builder.append("    Busiest classes: none\n");
            return;
        }
        builder.append("    Busiest classes:\n");
        for (ScrollbarViewCensusEntry entry : census.getBusiestClasses()) {
            builder.append("      ").append(entry.getViewCount()).append(" x ")
                .append(entry.getClassName()).append('\n');
        }
    }

    private void appendMainLooperQueueLines(@NonNull StringBuilder builder,
                                            @NonNull DiagnosticsMainLooperQueue looperQueue) {
        builder.append("  Main looper queue\n");
        builder.append("    Pending messages: ").append(looperQueue.getPendingMessageCount()).append('\n');
        if (looperQueue.getBusiestTargets().isEmpty()) {
            builder.append("    Busiest targets: none\n");
            return;
        }
        builder.append("    Busiest targets:\n");
        List<String> targetLines = new ArrayList<>();
        for (DiagnosticsMainLooperQueueTarget target : looperQueue.getBusiestTargets()) {
            targetLines.add("      " + target.getPendingMessageCount() + " x " + target.getDescription());
        }
        appendLinesWithinBudget(builder, targetLines, BUSIEST_TARGET_BUDGET_CHARACTERS);
        appendPendingMessageLines(builder, looperQueue);
    }

    private void appendPendingMessageLines(@NonNull StringBuilder builder,
                                           @NonNull DiagnosticsMainLooperQueue looperQueue) {
        if (looperQueue.getPendingMessageLines().isEmpty()) {
            return;
        }
        builder.append("    Pending messages, oldest first (up to ")
            .append(DiagnosticsMainLooperQueue.MAX_REPORTED_MESSAGE_LINES).append("):\n");
        List<String> pendingMessageLines = new ArrayList<>();
        for (String pendingMessageLine : looperQueue.getPendingMessageLines()) {
            pendingMessageLines.add("      " + pendingMessageLine);
        }
        appendLinesWithinBudget(builder, pendingMessageLines, PENDING_MESSAGE_LINE_BUDGET_CHARACTERS);
    }

    private static void appendLinesWithinBudget(@NonNull StringBuilder builder,
                                                @NonNull List<String> lines, int budgetCharacters) {
        int remainingToSectionCeiling =
            MAIN_THREAD_COST_SECTION_CEILING_CHARACTERS - builder.length() - OMISSION_NOTE_BUDGET_CHARACTERS;
        int allowedCharacters =
            Math.min(budgetCharacters - OMISSION_NOTE_BUDGET_CHARACTERS, remainingToSectionCeiling);
        int spentCharacters = 0;
        int appendedLineCount = 0;
        for (String line : lines) {
            int lineCharacters = line.length() + 1;
            if (spentCharacters + lineCharacters > allowedCharacters) break;
            builder.append(line).append('\n');
            spentCharacters += lineCharacters;
            appendedLineCount++;
        }
        if (appendedLineCount == lines.size()) return;
        builder.append("      ... ").append(lines.size() - appendedLineCount)
            .append(" further lines left out so this report survives being pasted\n");
    }

    private void appendMainThreadStallLines(@NonNull StringBuilder builder,
                                            @NonNull DiagnosticsMainThreadStalls stalls) {
        builder.append("  Stalls over ").append(stalls.getThresholdMillis()).append(" ms\n");
        builder.append("    Count: ").append(stalls.getStallCount()).append('\n');
        if (stalls.getStallCount() == 0) {
            builder.append("    Longest: n/a\n");
            return;
        }
        builder.append("    Longest: ").append(stalls.getMaxStallMillis()).append(" ms\n");
        builder.append("    Longest stall main thread was running:\n");
        List<String> frameLines = new ArrayList<>();
        for (String frame : stalls.getMaxStallStackTrace().split("\n")) {
            frameLines.add("      " + frame);
        }
        appendLinesWithinBudget(builder, frameLines, LONGEST_STALL_STACK_TRACE_BUDGET_CHARACTERS);
        appendMainThreadStallHotPathLines(builder, stalls.getHotPaths());
    }

    private void appendMainThreadStallHotPathLines(@NonNull StringBuilder builder,
                                                   @NonNull List<MainThreadStallHotPath> hotPaths) {
        if (hotPaths.isEmpty()) {
            return;
        }
        builder.append("    Blocking the main thread the longest\n");
        List<String> rankingLines = new ArrayList<>();
        for (MainThreadStallHotPath hotPath : hotPaths) {
            rankingLines.add("      " + hotPath.getTotalBlockedMillis()
                + " ms total over " + hotPath.getStallCount()
                + " stalls, longest " + hotPath.getMaxBlockedMillis() + " ms: "
                + hotPath.getIdentifyingFrame());
        }
        appendLinesWithinBudget(builder, rankingLines, STALL_HOT_PATH_RANKING_BUDGET_CHARACTERS);
        appendMainThreadStallHotPathStackTraceLines(builder, hotPaths);
    }

    private void appendMainThreadStallHotPathStackTraceLines(@NonNull StringBuilder builder,
                                                             @NonNull List<MainThreadStallHotPath> hotPaths) {
        builder.append("    Caller chain of each path blocking the main thread\n");
        List<String> stackTraceLines = new ArrayList<>();
        for (MainThreadStallHotPath hotPath : hotPaths) {
            stackTraceLines.add("      " + hotPath.getIdentifyingFrame());
            for (String frame : hotPath.getStackTrace().split("\n")) {
                stackTraceLines.add("        " + frame);
            }
        }
        appendLinesWithinBudget(builder, stackTraceLines, STALL_HOT_PATH_STACK_TRACE_BUDGET_CHARACTERS);
    }

    private void appendBackgroundCycleSection(@NonNull StringBuilder builder,
                                              @NonNull DiagnosticsReport report) {
        DiagnosticsBackgroundCycle backgroundCycle = report.getBackgroundCycle();
        builder.append("Background cycle\n");
        builder.append("  Cycles run: ").append(backgroundCycle.getCycleCount()).append('\n');
        if (backgroundCycle.getLongestIntervals().isEmpty()) {
            builder.append("  Longest gaps between cycles: none recorded yet\n");
            return;
        }
        builder.append("  Longest gaps between cycles, longest first:\n");
        for (BackgroundCycleInterval interval : backgroundCycle.getLongestIntervals()) {
            builder.append("    ").append(interval.getIntervalMillis())
                .append(" ms against a scheduled ").append(interval.getScheduledIntervalMillis())
                .append(" ms, ending ").append(formatTimestamp(interval.getObservedAtMillis()))
                .append(", activity ").append(interval.isActivityVisible() ? "visible" : "not visible")
                .append('\n');
        }
    }

    private void appendWorkCostLines(@NonNull StringBuilder builder, @NonNull String label,
                                     @NonNull DiagnosticsWorkCostLine cost) {
        builder.append("  ").append(label).append('\n');
        builder.append("    Count: ").append(cost.getSampleCount()).append('\n');
        builder.append("    Total: ").append(cost.getTotalElapsedMillis()).append(" ms\n");
        if (cost.getSampleCount() == 0) {
            builder.append("    Max: n/a\n");
            return;
        }
        builder.append("    Max: ").append(cost.getMaxElapsedMillis()).append(" ms\n");
        builder.append("    Transcript rows at max: ").append(cost.getTranscriptRowsAtMaxElapsed()).append('\n');
    }

    private void appendSessionReconnectCostLines(@NonNull StringBuilder builder,
                                                 @NonNull DiagnosticsSessionReconnectCost cost) {
        builder.append("  Dead session reconnect on the main thread\n");
        builder.append("    Count: ").append(cost.getReconnectCount()).append('\n');
        builder.append("    Total: ").append(cost.getTotalElapsedMillis()).append(" ms\n");
        if (cost.getReconnectCount() == 0) {
            builder.append("    Max: n/a\n");
            return;
        }
        builder.append("    Max: ").append(cost.getMaxElapsedMillis()).append(" ms\n");
        builder.append("    Sessions still queued at max: ")
            .append(cost.getSessionsStillQueuedAtMaxElapsed()).append('\n');
        for (DiagnosticsSessionReconnectCostByReason costByReason : cost.getCostsByReason()) {
            builder.append("    ").append(costByReason.getReason().getReportLabel()).append('\n');
            builder.append("      Count: ").append(costByReason.getReconnectCount()).append('\n');
            builder.append("      Total: ").append(costByReason.getTotalElapsedMillis()).append(" ms\n");
            builder.append("      Max: ").append(costByReason.getMaxElapsedMillis()).append(" ms\n");
        }
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
        builder.append("  Total transcript rows: ").append(report.getTotalTranscriptRows()).append('\n');
        List<DiagnosticsSessionLine> lines = report.getSessionLines();
        if (lines.isEmpty()) {
            builder.append("  (no sessions)\n");
            return;
        }
        for (DiagnosticsSessionLine line : lines) {
            builder.append("  - ").append(line.getName())
                .append(" | ").append(line.isAlive() ? "alive" : "dead")
                .append(" | last activity: ").append(formatSecondsSinceLastActivity(line))
                .append(" | transcript rows: ").append(line.getTranscriptRows())
                .append(" | columns: ").append(line.getColumns())
                .append(" | ").append(line.getListDisplay().getReportLabel())
                .append('\n');
            appendShellInputDelivery(builder, line.getShellInputDelivery());
            appendStatusline(builder, line.getStatusline());
        }
    }

    private void appendStatusline(@NonNull StringBuilder builder,
                                  @NonNull DiagnosticsSessionStatusline statusline) {
        builder.append("      statusline held by the app: ");
        if (statusline.isHeld()) {
            builder.append("call ").append(formatHeldTime(statusline.getCallTimeMillis()))
                .append(", out ").append(formatHeldTime(statusline.getOutTimeMillis()))
                .append(", reply ").append(formatHeldTime(statusline.getReplyTimeMillis()));
        } else {
            builder.append("none");
        }
        builder.append(", dot ").append(statusline.getTier()).append('\n');
    }

    @NonNull
    private String formatHeldTime(@Nullable Long timeMillis) {
        if (timeMillis == null) {
            return "none";
        }
        return formatTimestamp(timeMillis);
    }

    private void appendShellInputDelivery(@NonNull StringBuilder builder,
                                          @NonNull DiagnosticsShellInputDelivery delivery) {
        builder.append("      shell input: accepted ").append(delivery.getBytesAcceptedForDelivery())
            .append("B, written to the shell ").append(delivery.getBytesWrittenToTheShell())
            .append("B, still undelivered ").append(delivery.getBytesAcceptedButNotWrittenYet())
            .append("B, discarded before the queue ")
            .append(delivery.getBytesDiscardedBeforeDelivery()).append("B\n");
        builder.append("      shell input writer: ").append(describeWriterState(delivery)).append('\n');
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
