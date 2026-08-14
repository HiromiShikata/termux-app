package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DiagnosticsMainLooperQueue {

    public static final int MAX_REPORTED_TARGETS = 5;

    public static final int MAX_REPORTED_MESSAGE_LINES = 40;

    private static final String MESSAGE_LINE_PREFIX = "Message ";
    private static final String MESSAGE_LINE_MARKER = "{ when=";
    private static final String WHEN_PREFIX = "when=";
    private static final String TARGET_PREFIX = "target=";
    private static final String CALLBACK_PREFIX = "callback=";
    private static final String BARRIER_PREFIX = "barrier=";
    private static final String SYNCHRONIZATION_BARRIER_DESCRIPTION = "synchronization barrier";

    private final int mPendingMessageCount;

    private final int mSynchronizationBarrierCount;

    private final int mMessageCountBehindFirstSynchronizationBarrier;

    @NonNull
    private final String mFirstSynchronizationBarrierDueDescription;

    @NonNull
    private final List<DiagnosticsMainLooperQueueTarget> mBusiestTargets;

    @NonNull
    private final List<String> mPendingMessageLines;

    private DiagnosticsMainLooperQueue(int pendingMessageCount,
                                       int synchronizationBarrierCount,
                                       int messageCountBehindFirstSynchronizationBarrier,
                                       @NonNull String firstSynchronizationBarrierDueDescription,
                                       @NonNull List<DiagnosticsMainLooperQueueTarget> busiestTargets,
                                       @NonNull List<String> pendingMessageLines) {
        mPendingMessageCount = pendingMessageCount;
        mSynchronizationBarrierCount = synchronizationBarrierCount;
        mMessageCountBehindFirstSynchronizationBarrier = messageCountBehindFirstSynchronizationBarrier;
        mFirstSynchronizationBarrierDueDescription = firstSynchronizationBarrierDueDescription;
        mBusiestTargets = Collections.unmodifiableList(new ArrayList<>(busiestTargets));
        mPendingMessageLines = Collections.unmodifiableList(new ArrayList<>(pendingMessageLines));
    }

    @NonNull
    public static DiagnosticsMainLooperQueue parse(@NonNull List<String> looperDumpLines) {
        Map<String, Integer> countsByTarget = new LinkedHashMap<>();
        List<String> pendingMessageLines = new ArrayList<>();
        int pendingMessageCount = 0;
        int synchronizationBarrierCount = 0;
        int messageCountBehindFirstSynchronizationBarrier = 0;
        String firstSynchronizationBarrierDueDescription = "";
        boolean firstSynchronizationBarrierSeen = false;
        for (String line : looperDumpLines) {
            if (!isMessageLine(line)) continue;
            pendingMessageCount++;
            boolean isSynchronizationBarrier = isSynchronizationBarrierLine(line);
            if (isSynchronizationBarrier) synchronizationBarrierCount++;
            if (isSynchronizationBarrier && !firstSynchronizationBarrierSeen) {
                firstSynchronizationBarrierSeen = true;
                firstSynchronizationBarrierDueDescription = valueAfter(line, WHEN_PREFIX);
            } else if (firstSynchronizationBarrierSeen) {
                messageCountBehindFirstSynchronizationBarrier++;
            }
            if (pendingMessageLines.size() < MAX_REPORTED_MESSAGE_LINES) {
                pendingMessageLines.add(line.trim());
            }
            String target = describeTarget(line);
            Integer previousCount = countsByTarget.get(target);
            countsByTarget.put(target, previousCount == null ? 1 : previousCount + 1);
        }
        return new DiagnosticsMainLooperQueue(pendingMessageCount, synchronizationBarrierCount,
            messageCountBehindFirstSynchronizationBarrier, firstSynchronizationBarrierDueDescription,
            busiestTargets(countsByTarget), pendingMessageLines);
    }

    public int getPendingMessageCount() {
        return mPendingMessageCount;
    }

    public int getSynchronizationBarrierCount() {
        return mSynchronizationBarrierCount;
    }

    public int getMessageCountBehindFirstSynchronizationBarrier() {
        return mMessageCountBehindFirstSynchronizationBarrier;
    }

    @NonNull
    public String getFirstSynchronizationBarrierDueDescription() {
        return mFirstSynchronizationBarrierDueDescription;
    }

    @NonNull
    public List<String> getPendingMessageLines() {
        return mPendingMessageLines;
    }

    @NonNull
    public List<DiagnosticsMainLooperQueueTarget> getBusiestTargets() {
        return mBusiestTargets;
    }

    @NonNull
    private static List<DiagnosticsMainLooperQueueTarget> busiestTargets(
            @NonNull Map<String, Integer> countsByTarget) {
        List<DiagnosticsMainLooperQueueTarget> targets = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : countsByTarget.entrySet()) {
            targets.add(new DiagnosticsMainLooperQueueTarget(entry.getKey(), entry.getValue()));
        }
        Collections.sort(targets, (left, right) -> right.getPendingMessageCount() - left.getPendingMessageCount());
        if (targets.size() > MAX_REPORTED_TARGETS) {
            return new ArrayList<>(targets.subList(0, MAX_REPORTED_TARGETS));
        }
        return targets;
    }

    private static boolean isMessageLine(String line) {
        if (line == null) return false;
        String trimmed = line.trim();
        return trimmed.startsWith(MESSAGE_LINE_PREFIX) && trimmed.contains(MESSAGE_LINE_MARKER);
    }

    private static boolean isSynchronizationBarrierLine(@NonNull String line) {
        return line.contains(BARRIER_PREFIX) && !line.contains(TARGET_PREFIX);
    }

    @NonNull
    private static String describeTarget(@NonNull String messageLine) {
        if (isSynchronizationBarrierLine(messageLine)) return SYNCHRONIZATION_BARRIER_DESCRIPTION;
        String target = valueAfter(messageLine, TARGET_PREFIX);
        String callback = valueAfter(messageLine, CALLBACK_PREFIX);
        if (target.isEmpty() && callback.isEmpty()) return "unknown";
        if (callback.isEmpty()) return target;
        if (target.isEmpty()) return callback;
        return target + " " + callback;
    }

    @NonNull
    private static String valueAfter(@NonNull String line, @NonNull String prefix) {
        int valueStart = line.indexOf(prefix);
        if (valueStart < 0) return "";
        valueStart += prefix.length();
        int valueEnd = valueStart;
        while (valueEnd < line.length() && line.charAt(valueEnd) != ' ' && line.charAt(valueEnd) != '}') {
            valueEnd++;
        }
        return line.substring(valueStart, valueEnd);
    }
}
