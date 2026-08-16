package com.termux.app.diagnostics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.sessiondefinition.SessionReconnectBlockerCensus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class DiagnosticsReportBuilder {

    public static final int PASTE_LIMIT_CHARACTERS = 5000;

    private static final String TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static final String LONGEST_STALL_STACK_TRACE = "longest stall stack trace";

    private static final String STALL_HOT_PATH_RANKING = "stall hot path ranking";

    private static final String STALL_HOT_PATH_STACK_TRACES = "stall hot path stack traces";

    private static final String BUSIEST_TARGETS = "main looper queue busiest targets";

    private static final String PEAK_BUSIEST_TARGETS = "peak main looper queue busiest targets";

    private static final String PEAK_SCROLLBAR_VIEW_CLASSES = "peak scrollbar view classes";

    private static final String PENDING_MESSAGE_LINES = "pending main looper message lines";

    private static final String UNDELIVERED_SHELL_INPUT = "undelivered shell input";

    private static final String SHELL_EXIT_STATUSES = "shell exit statuses";

    private static final String SESSION_RECONNECT_COST_BY_REASON = "session reconnect cost by reason";

    private static final String SESSION_CREATION_PATHS = "session creation paths";

    private static final String RECONNECT_BLOCKERS = "reconnect blockers";

    private static final String APP_PROCESS_COMMAND_NAMES = "app process command names";

    private static final String LIVE_SCROLLBAR_VIEW_CLASSES = "live scrollbar view classes";

    private static final String PREVIOUS_PROCESS_EXITS = "previous process exits";

    private static final String OMISSION_NOTE_PREFIX = "      ... ";

    private static final String OMISSION_NOTE_SUFFIX =
        " further lines left out so this report survives being pasted\n";

    private static final int LONGEST_OMITTED_LINE_COUNT_DIGITS = 6;

    private static final int OMISSION_NOTE_CHARACTERS = OMISSION_NOTE_PREFIX.length()
        + LONGEST_OMITTED_LINE_COUNT_DIGITS + OMISSION_NOTE_SUFFIX.length();

    private static final int REPORT_CEILING_HEADROOM_CHARACTERS = 300;

    private static final int MAXIMUM_COMMAND_NAMES_REPORTED = 8;

    private static final int REPORT_CEILING_CHARACTERS =
        PASTE_LIMIT_CHARACTERS - REPORT_CEILING_HEADROOM_CHARACTERS;

    private static final List<DiagnosticsReportSubsection> SUBSECTIONS_IN_PRIORITY_ORDER =
        Collections.unmodifiableList(Arrays.asList(
            new DiagnosticsReportSubsection(PREVIOUS_PROCESS_EXITS, 800),
            new DiagnosticsReportSubsection(PENDING_MESSAGE_LINES, 1000),
            new DiagnosticsReportSubsection(BUSIEST_TARGETS, 500),
            new DiagnosticsReportSubsection(LIVE_SCROLLBAR_VIEW_CLASSES, 500),
            new DiagnosticsReportSubsection(UNDELIVERED_SHELL_INPUT, 600),
            new DiagnosticsReportSubsection(SHELL_EXIT_STATUSES, 400),
            new DiagnosticsReportSubsection(PEAK_BUSIEST_TARGETS, 500),
            new DiagnosticsReportSubsection(PEAK_SCROLLBAR_VIEW_CLASSES, 500),
            new DiagnosticsReportSubsection(RECONNECT_BLOCKERS, 560),
            new DiagnosticsReportSubsection(LONGEST_STALL_STACK_TRACE, 1140),
            new DiagnosticsReportSubsection(STALL_HOT_PATH_RANKING, 900),
            new DiagnosticsReportSubsection(SESSION_RECONNECT_COST_BY_REASON, 600),
            new DiagnosticsReportSubsection(SESSION_CREATION_PATHS, 500),
            new DiagnosticsReportSubsection(APP_PROCESS_COMMAND_NAMES, 500),
            new DiagnosticsReportSubsection(STALL_HOT_PATH_STACK_TRACES, 600)));

    private static final List<String> SUBSECTION_NAMES_IN_PRINT_ORDER =
        Collections.unmodifiableList(Arrays.asList(
            PREVIOUS_PROCESS_EXITS,
            UNDELIVERED_SHELL_INPUT,
            SHELL_EXIT_STATUSES,
            SESSION_CREATION_PATHS,
            RECONNECT_BLOCKERS,
            APP_PROCESS_COMMAND_NAMES,
            SESSION_RECONNECT_COST_BY_REASON,
            BUSIEST_TARGETS,
            LIVE_SCROLLBAR_VIEW_CLASSES,
            PEAK_BUSIEST_TARGETS,
            PEAK_SCROLLBAR_VIEW_CLASSES,
            PENDING_MESSAGE_LINES,
            LONGEST_STALL_STACK_TRACE,
            STALL_HOT_PATH_RANKING,
            STALL_HOT_PATH_STACK_TRACES));

    @NonNull
    public String build(@NonNull DiagnosticsReport report) {
        DiagnosticsReportText measuredText = render(report,
            DiagnosticsReportCharacterAllowances.unlimited());
        DiagnosticsReportCharacterAllowances allowances =
            DiagnosticsReportCharacterAllowances.allocatedByPriority(REPORT_CEILING_CHARACTERS,
                OMISSION_NOTE_CHARACTERS, SUBSECTIONS_IN_PRIORITY_ORDER,
                SUBSECTION_NAMES_IN_PRINT_ORDER, measuredText.getMeasuredOffsetByName(),
                measuredText.getMeasuredCharactersByName());
        if (allowances.grantsEveryWantedCharacter()) {
            return measuredText.toString();
        }
        return render(report, allowances).toString();
    }

    @NonNull
    public String buildForDeliveryIntoASession(@NonNull DiagnosticsReport report) {
        return render(report, DiagnosticsReportCharacterAllowances.unlimited()).toString();
    }

    @NonNull
    private DiagnosticsReportText render(@NonNull DiagnosticsReport report,
                                         @NonNull DiagnosticsReportCharacterAllowances allowances) {
        DiagnosticsReportText builder = new DiagnosticsReportText(allowances);

        builder.append("Termux diagnostics report\n");
        builder.append("Generated: ").append(formatTimestamp(report.getReportTimestampMillis())).append('\n');
        builder.append("App version: ").append(report.getVersionName())
            .append(" (").append(report.getVersionCode()).append(")\n");

        appendVersionChangeLine(builder, report.getVersionChange());

        builder.append("Process uptime: ").append(formatUptime(report.getProcessUptimeMillis())).append('\n');

        builder.append('\n');
        appendPreviousProcessExitSection(builder, report);

        builder.append('\n');
        appendPreviousProcessConditionSection(builder, report);

        builder.append('\n');
        appendUndeliveredShellInputSection(builder, report);

        builder.append('\n');
        appendReplacedSessionShellInputSection(builder, report);

        builder.append('\n');
        appendShellExitSection(builder, report);

        builder.append('\n');
        appendSessionCreationPathSection(builder, report);

        builder.append('\n');
        appendReconnectBlockerSection(builder, report);

        builder.append('\n');
        appendActivityWindowSection(builder, report);

        builder.append('\n');
        appendWindowConditionSection(builder, report);

        appendScrollWithoutDrawSection(builder, report);

        builder.append('\n');
        appendReportDeliverySection(builder, report);

        builder.append('\n');
        appendPhantomProcessMonitorSection(builder, report);

        builder.append('\n');
        appendAppProcessPopulationSection(builder, report);

        builder.append('\n');
        appendMainThreadCostSection(builder, report);

        builder.append('\n');
        appendRenderThreadSection(builder, report);

        builder.append('\n');
        appendBackgroundCycleSection(builder, report);

        builder.append('\n');
        appendMemorySection(builder, report);

        builder.append('\n');
        appendBrowserSection(builder, report);

        builder.append('\n');
        appendWakeLockSection(builder, report);

        builder.append('\n');
        appendScrollStepSection(builder, report);

        builder.append('\n');
        appendTouchEventSection(builder, report);

        builder.append('\n');
        appendSessionsSection(builder, report);

        builder.append('\n');
        appendEventsSection(builder, report);

        return builder;
    }

    private void appendVersionChangeLine(@NonNull DiagnosticsReportText builder,
                                         @NonNull DiagnosticsVersionChange versionChange) {
        if (!versionChange.isFirstLaunchOfThisVersion()) return;
        if (versionChange.hasPreviousVersionCode()) {
            builder.append("First launch after replacing version code ")
                .append(versionChange.getPreviousVersionCode()).append('\n');
            return;
        }
        builder.append("First launch after installation\n");
    }

    private void appendPreviousProcessConditionSection(@NonNull DiagnosticsReportText builder,
                                                       @NonNull DiagnosticsReport report) {
        ProcessConditionSnapshot condition = report.getPreviousProcessCondition();
        builder.append("Condition of the previous process before it ended\n");
        if (!condition.isRecorded()) {
            String unreadableReason = condition.getUnreadableReason();
            if (unreadableReason == null) {
                builder.append("  None: no earlier process recorded its condition\n");
                return;
            }
            builder.append("  Unreadable: ").append(unreadableReason).append('\n');
            return;
        }
        builder.append("  Last recorded: ")
            .append(formatTimestamp(condition.getRecordedAtMillis())).append('\n');
        builder.append("  Process uptime then: ")
            .append(formatUptime(condition.getProcessUptimeMillis())).append('\n');
        builder.append("  Main looper pending messages then: ")
            .append(condition.getMainLooperPendingMessageCount()).append('\n');
        builder.append("  Synchronization barriers in the queue then: ")
            .append(condition.getSynchronizationBarrierCount()).append('\n');
        builder.append("  Deepest main looper queue it saw: ")
            .append(condition.getPeakPendingMessageCount())
            .append(" at ").append(formatTimestamp(condition.getPeakObservedAtMillis())).append('\n');
        builder.append("  Views that could hold a scrollbar fade callback then: ")
            .append(condition.getPeakScrollbarViewCount()).append('\n');
        builder.append("  Scroll gestures the terminal never drew for (up to ")
            .append(ScrollWithoutDrawEpisodeRecorder.MAX_RETAINED_EPISODES).append(" kept): ")
            .append(condition.getKeptScrollWithoutDrawEpisodeCount()).append('\n');
    }

    @NonNull
    private String formatUptime(long uptimeMillis) {
        long totalSeconds = uptimeMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours + "h " + minutes + "m " + seconds + "s";
    }

    private void appendMemorySection(@NonNull DiagnosticsReportText builder, @NonNull DiagnosticsReport report) {
        DiagnosticsMemoryUsage memoryUsage = report.getMemoryUsage();
        builder.append("Memory\n");
        builder.append("  Java heap used: ").append(memoryUsage.getJavaHeapUsedMegabytes()).append(" MB\n");
        builder.append("  Java heap total: ").append(memoryUsage.getJavaHeapTotalMegabytes()).append(" MB\n");
        builder.append("  Java heap max: ").append(memoryUsage.getJavaHeapMaxMegabytes()).append(" MB\n");
        builder.append("  Native heap allocated: ")
            .append(memoryUsage.getNativeHeapAllocatedMegabytes()).append(" MB\n");
    }

    private void appendUndeliveredShellInputSection(@NonNull DiagnosticsReportText builder,
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
        appendLinesWithinBudget(builder, undeliveredLines, UNDELIVERED_SHELL_INPUT);
    }

    private void appendReplacedSessionShellInputSection(@NonNull DiagnosticsReportText builder,
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

    private void appendPreviousProcessExitSection(@NonNull DiagnosticsReportText builder,
                                                  @NonNull DiagnosticsReport report) {
        DiagnosticsPreviousProcessExits previousProcessExits = report.getPreviousProcessExits();
        builder.append("Why the recent processes of this app ended\n");
        switch (previousProcessExits.getReading()) {
            case NOT_TAKEN:
                builder.append("  Not measured\n");
                return;
            case NOT_KEPT_BY_THIS_ANDROID:
                builder.append("  This version of Android does not keep a record of why a process ended\n");
                return;
            default:
                break;
        }
        if (previousProcessExits.getExits().isEmpty()) {
            builder.append("  None: the system holds no record of a process of this app ending\n");
            return;
        }
        List<String> endingLines = new ArrayList<>();
        for (DiagnosticsPreviousProcessExit previousProcessExit : previousProcessExits.getExits()) {
            endingLines.add("  " + formatTimestamp(previousProcessExit.getEndedAtMillis())
                + ": " + previousProcessExit.getReasonLabel()
                + ", while " + previousProcessExit.getImportanceLabel());
            String description = previousProcessExit.getDescription();
            if (description == null || description.isEmpty()) continue;
            endingLines.add("    Described by the system as: " + description);
        }
        appendLinesWithinBudget(builder, endingLines, PREVIOUS_PROCESS_EXITS);
    }

    private void appendShellExitSection(@NonNull DiagnosticsReportText builder, @NonNull DiagnosticsReport report) {
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
        appendLinesWithinBudget(builder, exitStatusLines, SHELL_EXIT_STATUSES);
    }

    private void appendSessionCreationPathSection(@NonNull DiagnosticsReportText builder,
                                                  @NonNull DiagnosticsReport report) {
        DiagnosticsSessionCreationPaths creationPaths = report.getSessionCreationPaths();
        builder.append("Sessions created since the app started\n");
        if (creationPaths.getCountsByPath().isEmpty()) {
            builder.append("  None: no session has been created since the app started\n");
            return;
        }
        builder.append("  Total: ").append(creationPaths.getTotalCreationCount()).append('\n');
        List<String> countByPathLines = new ArrayList<>();
        for (DiagnosticsSessionCreationPathCount countByPath : creationPaths.getCountsByPath()) {
            countByPathLines.add("  " + countByPath.getPath().getReportLabel() + ": "
                + countByPath.getCreationCount());
        }
        appendLinesWithinBudget(builder, countByPathLines, SESSION_CREATION_PATHS);
    }

    private void appendReconnectBlockerSection(@NonNull DiagnosticsReportText builder,
                                               @NonNull DiagnosticsReport report) {
        SessionReconnectBlockerCensus census = report.getReconnectBlockerCensus();
        builder.append("Why the sessions on screen were not reconnected at the last scan\n");
        if (!census.isTaken()) {
            builder.append("  None: no background reconnect scan has run yet\n");
            return;
        }
        builder.append("  Scanned ").append(formatTimestamp(census.getTakenAtMillis()))
            .append(", considered ").append(census.getConsideredCount())
            .append(", planned ").append(census.getPlannedCount()).append('\n');
        List<String> blockerLines = new ArrayList<>();
        blockerLines.add("  Shell gone: " + census.getShellGoneCount());
        blockerLines.add("    still marked reconnecting: "
            + census.getShellGoneMarkedReconnectingCount()
            + ", longest " + formatUptime(census.getLongestReconnectingMillis()));
        blockerLines.add("    inside the exit backoff: "
            + census.getShellGoneInsideTheExitBackoffCount()
            + ", longest wait left " + formatUptime(census.getLongestExitBackoffRemainingMillis()));
        blockerLines.add("    reported failed to reconnect: "
            + census.getShellGoneReportedFailedCount());
        blockerLines.add("    ready but left out of the plan: "
            + census.getShellGoneReadyButNotPlannedCount());
        blockerLines.add("  Alive but silent and out of reach: " + census.getSilentCount());
        blockerLines.add("    the one on screen, which this scan skips: "
            + census.getSilentDisplayedRightNowCount());
        blockerLines.add("    still marked reconnecting: "
            + census.getSilentMarkedReconnectingCount());
        blockerLines.add("    inside the silence backoff: "
            + census.getSilentInsideTheSilenceBackoffCount()
            + ", longest wait left " + formatUptime(census.getLongestSilenceBackoffRemainingMillis()));
        appendLinesWithinBudget(builder, blockerLines, RECONNECT_BLOCKERS);
    }

    private void appendScrollStepSection(@NonNull DiagnosticsReportText builder,
                                         @NonNull DiagnosticsReport report) {
        DiagnosticsScrollSteps scrollSteps = report.getScrollSteps();
        builder.append("Scroll steps since the app started\n");
        if (scrollSteps.getCountsByDestination().isEmpty()) {
            builder.append("  None: no scroll gesture has reached the terminal view yet\n");
            return;
        }
        builder.append("  Total: ").append(scrollSteps.getTotalStepCount()).append('\n');
        for (DiagnosticsScrollStepCount countByDestination : scrollSteps.getCountsByDestination()) {
            builder.append("  To ").append(countByDestination.getDestinationLabel())
                .append(": ").append(countByDestination.getStepCount())
                .append(", most recent ")
                .append(formatTimestamp(countByDestination.getLastStepAtMillis())).append('\n');
        }
    }

    private void appendTouchEventSection(@NonNull DiagnosticsReportText builder,
                                         @NonNull DiagnosticsReport report) {
        DiagnosticsTouchEvents touchEvents = report.getTouchEvents();
        builder.append("Touch events the terminal view received since the app started\n");
        if (touchEvents.getCountsByKind().isEmpty()) {
            builder.append("  None: the terminal view has received no touch at all\n");
            return;
        }
        builder.append("  Total: ").append(touchEvents.getTotalTouchCount()).append('\n');
        for (DiagnosticsTouchCount countByKind : touchEvents.getCountsByKind()) {
            builder.append("  Of ").append(countByKind.getKindLabel())
                .append(": ").append(countByKind.getTouchCount())
                .append(", most recent ")
                .append(formatTimestamp(countByKind.getLastTouchAtMillis())).append('\n');
        }
    }

    private void appendActivityWindowSection(@NonNull DiagnosticsReportText builder,
                                             @NonNull DiagnosticsReport report) {
        DiagnosticsActivityWindows activityWindows = report.getActivityWindows();
        builder.append("Activity window builds since the app started\n");
        if (activityWindows.getCreatedCount() == 0) {
            builder.append("  None: the activity window has not been built yet\n");
            return;
        }
        builder.append("  Built: ").append(activityWindows.getCreatedCount()).append('\n');
        builder.append("  Torn down: ").append(activityWindows.getDestroyedCount()).append('\n');
        builder.append("  Teardown not run: ").append(activityWindows.getTeardownNotRunCount()).append('\n');
    }

    private void appendWindowConditionSection(@NonNull DiagnosticsReportText builder,
                                              @NonNull DiagnosticsReport report) {
        DiagnosticsWindowCondition condition = report.getActivityWindows().getCondition();
        builder.append("Activity window condition when this reading was taken\n");
        if (!condition.wasMeasured()) {
            builder.append("  Not measured\n");
            return;
        }
        appendDrawTimeLine(builder, "  Window last drew: ", condition.getWindowDrawTime());
        appendDrawTimeLine(builder, "  Terminal last drew: ", condition.getTerminalDrawTime());
        builder.append("  Window visibility: ").append(condition.getWindowVisibility()).append('\n');
        builder.append("  Attached to a window: ")
            .append(condition.isAttachedToWindow() ? "yes" : "no").append('\n');
        builder.append("  Has window focus: ")
            .append(condition.hasWindowFocus() ? "yes" : "no").append('\n');
    }

    private void appendDrawTimeLine(@NonNull DiagnosticsReportText builder, @NonNull String label,
                                    @NonNull DiagnosticsDrawTime drawTime) {
        if (drawTime.hasDrawn()) {
            builder.append(label).append(drawTime.getMillisSinceLastDraw()).append(" ms ago\n");
            return;
        }
        builder.append(label).append("not since the process started\n");
    }

    private void appendReportDeliverySection(@NonNull DiagnosticsReportText builder,
                                             @NonNull DiagnosticsReport report) {
        DiagnosticsReportDelivery delivery = report.getLastReportDelivery();
        builder.append("Last diagnostics report delivery\n");
        if (!delivery.wasAttempted()) {
            builder.append("  None: no report has been delivered to a session yet\n");
            return;
        }
        builder.append("  Session: ").append(delivery.getSessionName()).append('\n');
        builder.append("  Pasted: ").append(delivery.getPastedCharacters()).append(" characters in ")
            .append(delivery.getPasteMillis()).append(" ms\n");
        builder.append("  Enter accepted for delivery: ")
            .append(delivery.wasEnterAcceptedForDelivery() ? "yes" : "no").append('\n');
        builder.append("  Input reached the program after the paste: ")
            .append(delivery.didInputReachTheProgramAfterThePaste() ? "yes" : "no").append('\n');
    }

    private void appendPhantomProcessMonitorSection(@NonNull DiagnosticsReportText builder,
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

    private void appendRenderThreadSection(@NonNull DiagnosticsReportText builder,
                                           @NonNull DiagnosticsReport report) {
        DiagnosticsRenderThread renderThread = report.getMainThreadStalls().getRenderThread();
        builder.append("Render thread\n");
        switch (renderThread.getReading()) {
            case NOT_TAKEN:
                builder.append("  Not measured yet\n");
                return;
            case READ_FAILED:
                builder.append("  Not measured: ").append(renderThread.getReadFailureMessage())
                    .append('\n');
                return;
            case THREAD_ABSENT:
                builder.append("  Not measured: this process is running no thread named ")
                    .append(DiagnosticsRenderThread.THREAD_NAME).append('\n');
                return;
            default:
                builder.append("  Processor time: ").append(renderThread.getUserTimeMillis())
                    .append(" ms user, ").append(renderThread.getSystemTimeMillis())
                    .append(" ms system\n");
                builder.append("  State: ").append(renderThread.getSchedulerState()).append('\n');
        }
    }

    private void appendAppProcessPopulationSection(@NonNull DiagnosticsReportText builder,
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
        List<String> commandCountLines = new ArrayList<>();
        for (int index = 0; index < reportedCount; index++) {
            DiagnosticsProcessCommandCount count = counts.get(index);
            commandCountLines.add("    " + count.getCommandName() + ": " + count.getProcessCount());
        }
        appendLinesWithinBudget(builder, commandCountLines, APP_PROCESS_COMMAND_NAMES);
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

    private void appendMainThreadCostSection(@NonNull DiagnosticsReportText builder, @NonNull DiagnosticsReport report) {
        builder.append("Main-thread cost\n");
        appendWorkCostLines(builder, "Background output tag scan", report.getBackgroundOutputScanCost());
        appendWorkCostLines(builder, "Open-tag scan on the viewed session",
            report.getForegroundOpenTagScanCost());
        appendWorkCostLines(builder, "Buffer reflow on column-changing resize", report.getBufferReflowCost());
        appendWorkCostLines(builder, "Terminal draw", report.getTerminalDrawCost());
        appendWorkCostLines(builder, "Shell output parse", report.getShellOutputParseCost());
        appendSessionReconnectCostLines(builder, report.getSessionReconnectCost());
        appendMainLooperQueueLines(builder, report.getMainLooperQueue());
        appendScrollbarViewCensusLines(builder, report.getScrollbarViewCensus());
        appendMainLooperQueuePeakLines(builder, report.getMainLooperQueuePeak());
        appendPendingMessageLines(builder, report.getMainLooperQueue());
        appendMainThreadStallLines(builder, report.getMainThreadStalls());
    }

    private void appendScrollbarViewCensusLines(@NonNull DiagnosticsReportText builder,
                                                @NonNull ScrollbarViewCensus census) {
        builder.append("  Views that can hold a scrollbar fade callback\n");
        builder.append("    Total: ").append(census.getScrollbarViewCount()).append('\n');
        appendScrollbarViewCensusWindowLines(builder, census);
        if (census.getBusiestClasses().isEmpty()) {
            builder.append("    Busiest classes: none\n");
            return;
        }
        builder.append("    Busiest classes:\n");
        List<String> classLines = new ArrayList<>();
        for (ScrollbarViewCensusEntry entry : census.getBusiestClasses()) {
            classLines.add("      " + entry.getViewCount() + " x " + entry.getClassName());
        }
        appendLinesWithinBudget(builder, classLines, LIVE_SCROLLBAR_VIEW_CLASSES);
    }

    private void appendScrollbarViewCensusWindowLines(@NonNull DiagnosticsReportText builder,
                                                      @NonNull ScrollbarViewCensus census) {
        if (census.getWindowCount() == 0 && census.getWindowsNoLongerReachableCount() == 0) return;
        builder.append("    Windows walked: ").append(census.getWindowCount()).append('\n');
        builder.append("    Windows no longer reachable: ")
            .append(census.getWindowsNoLongerReachableCount()).append('\n');
    }

    private void appendMainLooperQueuePeakLines(@NonNull DiagnosticsReportText builder,
                                                @NonNull DiagnosticsMainLooperQueuePeak peak) {
        builder.append("  Highest main looper queue observed since the process started\n");
        if (!peak.wasObserved()) {
            builder.append("    Not sampled yet\n");
            return;
        }
        builder.append("    Pending messages: ").append(peak.getPendingMessageCount())
            .append(" at ").append(formatTimestamp(peak.getObservedAtMillis())).append('\n');
        appendPeakBusiestTargetLines(builder, peak.getBusiestTargets());
        appendPeakScrollbarViewCensusLines(builder, peak.getScrollbarViewCensus());
    }

    private void appendPeakBusiestTargetLines(@NonNull DiagnosticsReportText builder,
                                              @NonNull List<DiagnosticsMainLooperQueueTarget> busiestTargets) {
        if (busiestTargets.isEmpty()) {
            builder.append("    Busiest targets then: none\n");
            return;
        }
        builder.append("    Busiest targets then:\n");
        List<String> targetLines = new ArrayList<>();
        for (DiagnosticsMainLooperQueueTarget target : busiestTargets) {
            targetLines.add("      " + target.getPendingMessageCount() + " x " + target.getDescription());
        }
        appendLinesWithinBudget(builder, targetLines, PEAK_BUSIEST_TARGETS);
    }

    private void appendPeakScrollbarViewCensusLines(@NonNull DiagnosticsReportText builder,
                                                    @NonNull ScrollbarViewCensus census) {
        builder.append("    Views that could hold a scrollbar fade callback then: ")
            .append(census.getScrollbarViewCount()).append('\n');
        if (census.getBusiestClasses().isEmpty()) return;
        builder.append("    Busiest classes then:\n");
        List<String> classLines = new ArrayList<>();
        for (ScrollbarViewCensusEntry entry : census.getBusiestClasses()) {
            classLines.add("      " + entry.getViewCount() + " x " + entry.getClassName());
        }
        appendLinesWithinBudget(builder, classLines, PEAK_SCROLLBAR_VIEW_CLASSES);
    }

    private void appendMainLooperQueueLines(@NonNull DiagnosticsReportText builder,
                                            @NonNull DiagnosticsMainLooperQueue looperQueue) {
        builder.append("  Main looper queue\n");
        builder.append("    Pending messages: ").append(looperQueue.getPendingMessageCount()).append('\n');
        appendSynchronizationBarrierLines(builder, looperQueue);
        if (looperQueue.getBusiestTargets().isEmpty()) {
            builder.append("    Busiest targets: none\n");
            return;
        }
        builder.append("    Busiest targets:\n");
        List<String> targetLines = new ArrayList<>();
        for (DiagnosticsMainLooperQueueTarget target : looperQueue.getBusiestTargets()) {
            targetLines.add("      " + target.getPendingMessageCount() + " x " + target.getDescription());
        }
        appendLinesWithinBudget(builder, targetLines, BUSIEST_TARGETS);
    }

    private void appendSynchronizationBarrierLines(@NonNull DiagnosticsReportText builder,
                                                   @NonNull DiagnosticsMainLooperQueue looperQueue) {
        builder.append("    Synchronization barriers in the queue: ")
            .append(looperQueue.getSynchronizationBarrierCount()).append('\n');
        if (looperQueue.getSynchronizationBarrierCount() == 0) return;
        builder.append("    Oldest barrier due ")
            .append(looperQueue.getFirstSynchronizationBarrierDueDescription())
            .append(", with ").append(looperQueue.getMessageCountBehindFirstSynchronizationBarrier())
            .append(" messages queued behind it\n");
    }

    private void appendPendingMessageLines(@NonNull DiagnosticsReportText builder,
                                           @NonNull DiagnosticsMainLooperQueue looperQueue) {
        if (looperQueue.getPendingMessageLines().isEmpty()) {
            return;
        }
        builder.append("  Pending main looper messages, oldest first (up to ")
            .append(DiagnosticsMainLooperQueue.MAX_REPORTED_MESSAGE_LINES).append("):\n");
        List<String> pendingMessageLines = new ArrayList<>();
        for (String pendingMessageLine : looperQueue.getPendingMessageLines()) {
            pendingMessageLines.add("    " + pendingMessageLine);
        }
        appendLinesWithinBudget(builder, pendingMessageLines, PENDING_MESSAGE_LINES);
    }

    private static void appendLinesWithinBudget(@NonNull DiagnosticsReportText builder,
                                                @NonNull List<String> lines,
                                                @NonNull String subsectionName) {
        int measuredCharacters = 0;
        for (String line : lines) {
            measuredCharacters += line.length() + 1;
        }
        builder.recordSubsection(subsectionName, measuredCharacters);
        int allowedCharacters = builder.getAllowedCharactersOf(subsectionName);
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
        builder.append(OMISSION_NOTE_PREFIX).append(lines.size() - appendedLineCount)
            .append(OMISSION_NOTE_SUFFIX);
    }

    private void appendMaxStallRenderThreadLine(@NonNull DiagnosticsReportText builder,
                                               @NonNull DiagnosticsRenderThread renderThread) {
        switch (renderThread.getReading()) {
            case NOT_TAKEN:
                return;
            case READ_FAILED:
                builder.append("    Render thread when sampled: not readable (")
                    .append(renderThread.getReadFailureMessage()).append(")\n");
                return;
            case THREAD_ABSENT:
                builder.append("    Render thread when sampled: thread absent\n");
                return;
            default:
                builder.append("    Render thread when sampled: ")
                    .append(renderThread.getUserTimeMillis()).append(" ms user, ")
                    .append(renderThread.getSystemTimeMillis()).append(" ms system, state ")
                    .append(renderThread.getSchedulerState()).append('\n');
        }
    }

    private void appendMainThreadStallLines(@NonNull DiagnosticsReportText builder,
                                            @NonNull DiagnosticsMainThreadStalls stalls) {
        builder.append("  Stalls over ").append(stalls.getThresholdMillis()).append(" ms\n");
        builder.append("    Count: ").append(stalls.getStallCount()).append('\n');
        builder.append("    Stack sample attempts: ").append(stalls.getStackSampleAttemptCount())
            .append(", of which the runtime returned no frames: ")
            .append(stalls.getEmptyStackSampleCount()).append('\n');
        if (stalls.getStallCount() == 0) {
            builder.append("    Longest: n/a\n");
            return;
        }
        builder.append("    Longest: ").append(stalls.getMaxStallMillis()).append(" ms\n");
        appendMaxStallRenderThreadLine(builder, stalls.getMaxStallRenderThread());
        builder.append("    Longest stall main thread was running:\n");
        List<String> frameLines = new ArrayList<>();
        for (String frame : stalls.getMaxStallStackTrace().split("\n")) {
            frameLines.add("      " + frame);
        }
        appendLinesWithinBudget(builder, frameLines, LONGEST_STALL_STACK_TRACE);
        appendMainThreadStallHotPathLines(builder, stalls.getHotPaths());
    }

    private void appendMainThreadStallHotPathLines(@NonNull DiagnosticsReportText builder,
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
        appendLinesWithinBudget(builder, rankingLines, STALL_HOT_PATH_RANKING);
        appendMainThreadStallHotPathStackTraceLines(builder, hotPaths);
    }

    private void appendMainThreadStallHotPathStackTraceLines(@NonNull DiagnosticsReportText builder,
                                                             @NonNull List<MainThreadStallHotPath> hotPaths) {
        builder.append("    Caller chain of each path blocking the main thread\n");
        List<String> stackTraceLines = new ArrayList<>();
        for (MainThreadStallHotPath hotPath : hotPaths) {
            stackTraceLines.add("      " + hotPath.getIdentifyingFrame());
            for (String frame : hotPath.getStackTrace().split("\n")) {
                stackTraceLines.add("        " + frame);
            }
        }
        appendLinesWithinBudget(builder, stackTraceLines, STALL_HOT_PATH_STACK_TRACES);
    }

    private void appendBackgroundCycleSection(@NonNull DiagnosticsReportText builder,
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

    private void appendWorkCostLines(@NonNull DiagnosticsReportText builder, @NonNull String label,
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

    private void appendSessionReconnectCostLines(@NonNull DiagnosticsReportText builder,
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
        List<String> costByReasonLines = new ArrayList<>();
        for (DiagnosticsSessionReconnectCostByReason costByReason : cost.getCostsByReason()) {
            costByReasonLines.add("    " + costByReason.getReason().getReportLabel());
            costByReasonLines.add("      Count: " + costByReason.getReconnectCount());
            costByReasonLines.add("      Total: " + costByReason.getTotalElapsedMillis() + " ms");
            costByReasonLines.add("      Max: " + costByReason.getMaxElapsedMillis() + " ms");
        }
        appendLinesWithinBudget(builder, costByReasonLines, SESSION_RECONNECT_COST_BY_REASON);
    }

    private void appendSessionsSection(@NonNull DiagnosticsReportText builder, @NonNull DiagnosticsReport report) {
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
            appendListAbsence(builder, line.getListAbsence());
            appendScrollGestureRouting(builder, line.getScrollGestureRouting());
            appendShellInputDelivery(builder, line.getShellInputDelivery());
            appendStatusline(builder, line.getStatusline());
        }
    }

    private void appendListAbsence(@NonNull DiagnosticsReportText builder,
                                   @NonNull DiagnosticsSessionListAbsence listAbsence) {
        if (!listAbsence.hasReason()) return;
        builder.append("      ").append(listAbsence.getReportLabel()).append('\n');
    }

    private void appendScrollGestureRouting(@NonNull DiagnosticsReportText builder,
                                            @NonNull DiagnosticsScrollGestureRouting routing) {
        builder.append("      a scroll gesture goes to ").append(routing.getReportLabel()).append('\n');
    }

    private void appendStatusline(@NonNull DiagnosticsReportText builder,
                                  @NonNull DiagnosticsSessionStatusline statusline) {
        builder.append("      statusline held by the app: ");
        if (statusline.isHeld()) {
            builder.append("call ").append(formatHeldTime(statusline.getCallTimeMillis()))
                .append(", out ").append(formatHeldTime(statusline.getOutTimeMillis()))
                .append(", reply ").append(formatHeldTime(statusline.getReplyTimeMillis()));
        } else {
            builder.append("none");
        }
        builder.append(", dot ").append(statusline.getTier().name()).append('\n');
    }

    @NonNull
    private String formatHeldTime(@Nullable Long timeMillis) {
        if (timeMillis == null) {
            return "none";
        }
        return formatTimestamp(timeMillis);
    }

    private void appendShellInputDelivery(@NonNull DiagnosticsReportText builder,
                                          @NonNull DiagnosticsShellInputDelivery delivery) {
        builder.append("      shell input: accepted ").append(delivery.getBytesAcceptedForDelivery())
            .append('B');
        appendTimeOfLastBytes(builder, delivery.getLastBytesAcceptedAtMillis());
        builder.append(", written to the shell ").append(delivery.getBytesWrittenToTheShell())
            .append("B, still undelivered ").append(delivery.getBytesAcceptedButNotWrittenYet())
            .append("B, discarded before the queue ")
            .append(delivery.getBytesDiscardedBeforeDelivery()).append('B');
        appendTimeOfLastBytes(builder, delivery.getLastBytesDiscardedAtMillis());
        builder.append('\n');
        builder.append("      shell input writer: ").append(describeWriterState(delivery)).append('\n');
    }

    private void appendTimeOfLastBytes(@NonNull DiagnosticsReportText builder,
                                       @Nullable Long timeMillis) {
        if (timeMillis == null) return;
        builder.append(" at ").append(formatTimestamp(timeMillis));
    }

    @NonNull
    private String formatSecondsSinceLastActivity(@NonNull DiagnosticsSessionLine line) {
        if (!line.hasLastActivity()) {
            return "n/a";
        }
        return line.getSecondsSinceLastActivity() + "s ago";
    }

    private void appendBrowserSection(@NonNull DiagnosticsReportText builder, @NonNull DiagnosticsReport report) {
        builder.append("Browser\n");
        builder.append("  Open tabs: ").append(report.getOpenTabCount()).append('\n');
        builder.append("  Tab-history entries: ").append(report.getTabHistoryEntryCount()).append('\n');
    }

    private void appendWakeLockSection(@NonNull DiagnosticsReportText builder, @NonNull DiagnosticsReport report) {
        builder.append("Wake lock\n");
        builder.append("  Held: ").append(report.isWakeLockHeld() ? "yes" : "no").append('\n');
        builder.append("  App state: ").append(report.isForeground() ? "foreground" : "background").append('\n');
    }

    private void appendScrollWithoutDrawSection(@NonNull DiagnosticsReportText builder,
                                               @NonNull DiagnosticsReport report) {
        List<ScrollWithoutDrawEpisode> episodes =
            report.getActivityWindows().getScrollWithoutDrawEpisodes();
        builder.append("Scrolled without the terminal drawing\n");
        if (episodes.isEmpty()) {
            builder.append("  None: the terminal has drawn since it was last scrolled\n");
            return;
        }
        for (ScrollWithoutDrawEpisode episode : episodes) {
            builder.append("  ").append(formatTimestamp(episode.getScrolledAtMillis()))
                .append(" scrolled ").append(episode.getUndrawnForMillis())
                .append(" ms after the terminal last drew\n");
        }
    }

    private void appendEventsSection(@NonNull DiagnosticsReportText builder, @NonNull DiagnosticsReport report) {
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
