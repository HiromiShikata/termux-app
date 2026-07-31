package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DiagnosticsMainLooperQueue {

    public static final int MAX_REPORTED_TARGETS = 5;

    private static final String MESSAGE_LINE_PREFIX = "Message ";
    private static final String MESSAGE_LINE_MARKER = "{ when=";
    private static final String TARGET_PREFIX = "target=";
    private static final String CALLBACK_PREFIX = "callback=";

    private final int mPendingMessageCount;

    @NonNull
    private final List<DiagnosticsMainLooperQueueTarget> mBusiestTargets;

    private DiagnosticsMainLooperQueue(int pendingMessageCount,
                                       @NonNull List<DiagnosticsMainLooperQueueTarget> busiestTargets) {
        mPendingMessageCount = pendingMessageCount;
        mBusiestTargets = Collections.unmodifiableList(new ArrayList<>(busiestTargets));
    }

    @NonNull
    public static DiagnosticsMainLooperQueue parse(@NonNull List<String> looperDumpLines) {
        Map<String, Integer> countsByTarget = new LinkedHashMap<>();
        int pendingMessageCount = 0;
        for (String line : looperDumpLines) {
            if (!isMessageLine(line)) continue;
            pendingMessageCount++;
            String target = describeTarget(line);
            Integer previousCount = countsByTarget.get(target);
            countsByTarget.put(target, previousCount == null ? 1 : previousCount + 1);
        }
        return new DiagnosticsMainLooperQueue(pendingMessageCount, busiestTargets(countsByTarget));
    }

    public int getPendingMessageCount() {
        return mPendingMessageCount;
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

    @NonNull
    private static String describeTarget(@NonNull String messageLine) {
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
