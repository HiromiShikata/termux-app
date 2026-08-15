package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ProcessConditionSnapshotFormat {

    public static final String FORMAT_VERSION = "processConditionSnapshot v1";

    private static final String RECORDED_AT_MILLIS = "recordedAtMillis";

    private static final String PROCESS_UPTIME_MILLIS = "processUptimeMillis";

    private static final String MAIN_LOOPER_PENDING_MESSAGE_COUNT = "mainLooperPendingMessageCount";

    private static final String SYNCHRONIZATION_BARRIER_COUNT = "synchronizationBarrierCount";

    private static final String PEAK_PENDING_MESSAGE_COUNT = "peakPendingMessageCount";

    private static final String PEAK_OBSERVED_AT_MILLIS = "peakObservedAtMillis";

    private static final String PEAK_SCROLLBAR_VIEW_COUNT = "peakScrollbarViewCount";

    private static final String KEPT_SCROLL_WITHOUT_DRAW_EPISODE_COUNT = "keptScrollWithoutDrawEpisodeCount";

    private ProcessConditionSnapshotFormat() {
    }

    @NonNull
    public static String format(@NonNull ProcessConditionSnapshot snapshot) {
        if (!snapshot.isRecorded()) {
            throw new IllegalArgumentException("a condition that was never recorded has nothing to"
                + " write, and writing a stand-in would make the next process read numbers no"
                + " process ever measured");
        }
        return FORMAT_VERSION
            + " " + RECORDED_AT_MILLIS + "=" + snapshot.getRecordedAtMillis()
            + " " + PROCESS_UPTIME_MILLIS + "=" + snapshot.getProcessUptimeMillis()
            + " " + MAIN_LOOPER_PENDING_MESSAGE_COUNT + "=" + snapshot.getMainLooperPendingMessageCount()
            + " " + SYNCHRONIZATION_BARRIER_COUNT + "=" + snapshot.getSynchronizationBarrierCount()
            + " " + PEAK_PENDING_MESSAGE_COUNT + "=" + snapshot.getPeakPendingMessageCount()
            + " " + PEAK_OBSERVED_AT_MILLIS + "=" + snapshot.getPeakObservedAtMillis()
            + " " + PEAK_SCROLLBAR_VIEW_COUNT + "=" + snapshot.getPeakScrollbarViewCount()
            + " " + KEPT_SCROLL_WITHOUT_DRAW_EPISODE_COUNT + "=" + snapshot.getKeptScrollWithoutDrawEpisodeCount();
    }

    @NonNull
    public static ProcessConditionSnapshot parse(@NonNull String text) {
        String trimmed = text.trim();
        if (!trimmed.startsWith(FORMAT_VERSION + " ")) {
            throw new IllegalArgumentException("the record does not open with " + FORMAT_VERSION
                + ", so the numbers behind it cannot be read as this format. Record: " + trimmed);
        }
        Map<String, Long> valueByName = new LinkedHashMap<>();
        for (String token : trimmed.substring(FORMAT_VERSION.length() + 1).split(" ")) {
            int separatorIndex = token.indexOf('=');
            if (separatorIndex <= 0) {
                throw new IllegalArgumentException("the record holds a token without a name and a"
                    + " value: " + token + ". Record: " + trimmed);
            }
            valueByName.put(token.substring(0, separatorIndex),
                Long.parseLong(token.substring(separatorIndex + 1)));
        }
        return ProcessConditionSnapshot.recorded(
            requiredValueOf(valueByName, RECORDED_AT_MILLIS, trimmed),
            requiredValueOf(valueByName, PROCESS_UPTIME_MILLIS, trimmed),
            (int) requiredValueOf(valueByName, MAIN_LOOPER_PENDING_MESSAGE_COUNT, trimmed),
            (int) requiredValueOf(valueByName, SYNCHRONIZATION_BARRIER_COUNT, trimmed),
            (int) requiredValueOf(valueByName, PEAK_PENDING_MESSAGE_COUNT, trimmed),
            requiredValueOf(valueByName, PEAK_OBSERVED_AT_MILLIS, trimmed),
            (int) requiredValueOf(valueByName, PEAK_SCROLLBAR_VIEW_COUNT, trimmed),
            (int) requiredValueOf(valueByName, KEPT_SCROLL_WITHOUT_DRAW_EPISODE_COUNT, trimmed));
    }

    private static long requiredValueOf(@NonNull Map<String, Long> valueByName, @NonNull String name,
                                        @NonNull String record) {
        Long value = valueByName.get(name);
        if (value == null) {
            throw new IllegalArgumentException("the record is missing " + name + ", and a reading"
                + " assembled from part of a record would be indistinguishable from a measured one."
                + " Record: " + record);
        }
        return value;
    }
}
