package com.termux.app.diagnostics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MainThreadStallRecorder {

    public static final String STACK_TRACE_NOT_SAMPLED = "not sampled";

    private static final int MAX_RECORDED_FRAMES = 16;

    private static final int MAX_TRACKED_HOT_PATHS = 64;

    private final Map<String, HotPathAggregate> mHotPathsByStackTrace = new HashMap<>();

    private final long mStallThresholdMillis;

    private boolean mHeartbeatOutstanding;
    private long mHeartbeatPostedAtMillis;
    private String mOutstandingStackTrace = STACK_TRACE_NOT_SAMPLED;

    private long mStallCount;
    private long mMaxStallMillis;
    private String mMaxStallStackTrace = "";

    public MainThreadStallRecorder(long stallThresholdMillis) {
        mStallThresholdMillis = stallThresholdMillis;
    }

    public synchronized void heartbeatPosted(long postedAtMillis) {
        mHeartbeatOutstanding = true;
        mHeartbeatPostedAtMillis = postedAtMillis;
        mOutstandingStackTrace = STACK_TRACE_NOT_SAMPLED;
    }

    public synchronized boolean needsStackSample(long sampledAtMillis) {
        return mHeartbeatOutstanding
            && sampledAtMillis - mHeartbeatPostedAtMillis >= mStallThresholdMillis
            && STACK_TRACE_NOT_SAMPLED.equals(mOutstandingStackTrace);
    }

    public synchronized void sampleWhileOutstanding(long sampledAtMillis,
                                                    @Nullable StackTraceElement[] mainThreadStackTrace) {
        if (!mHeartbeatOutstanding
                || sampledAtMillis - mHeartbeatPostedAtMillis < mStallThresholdMillis) {
            return;
        }
        mOutstandingStackTrace = formatStackTrace(mainThreadStackTrace);
    }

    public synchronized void heartbeatRan(long ranAtMillis) {
        if (!mHeartbeatOutstanding) {
            return;
        }
        mHeartbeatOutstanding = false;
        long stallMillis = ranAtMillis - mHeartbeatPostedAtMillis;
        if (stallMillis < mStallThresholdMillis) {
            return;
        }
        mStallCount++;
        if (stallMillis > mMaxStallMillis) {
            mMaxStallMillis = stallMillis;
            mMaxStallStackTrace = mOutstandingStackTrace;
        }
        recordHotPath(mOutstandingStackTrace, stallMillis);
    }

    private void recordHotPath(@NonNull String stackTrace, long blockedMillis) {
        HotPathAggregate aggregate = mHotPathsByStackTrace.get(stackTrace);
        if (aggregate == null) {
            aggregate = new HotPathAggregate();
            mHotPathsByStackTrace.put(stackTrace, aggregate);
        }
        aggregate.addStall(blockedMillis);
        dropLeastBlockingPathsBeyondCapacity();
    }

    private void dropLeastBlockingPathsBeyondCapacity() {
        while (mHotPathsByStackTrace.size() > MAX_TRACKED_HOT_PATHS) {
            String leastBlockingStackTrace = null;
            long leastTotalBlockedMillis = Long.MAX_VALUE;
            for (Map.Entry<String, HotPathAggregate> entry : mHotPathsByStackTrace.entrySet()) {
                long totalBlockedMillis = entry.getValue().mTotalBlockedMillis;
                if (totalBlockedMillis < leastTotalBlockedMillis) {
                    leastTotalBlockedMillis = totalBlockedMillis;
                    leastBlockingStackTrace = entry.getKey();
                }
            }
            mHotPathsByStackTrace.remove(leastBlockingStackTrace);
        }
    }

    @NonNull
    public synchronized List<MainThreadStallHotPath> getHotPathsByTotalBlockedMillis() {
        List<MainThreadStallHotPath> hotPaths = new ArrayList<>();
        for (Map.Entry<String, HotPathAggregate> entry : mHotPathsByStackTrace.entrySet()) {
            HotPathAggregate aggregate = entry.getValue();
            hotPaths.add(new MainThreadStallHotPath(entry.getKey(), aggregate.mStallCount,
                aggregate.mTotalBlockedMillis, aggregate.mMaxBlockedMillis));
        }
        Collections.sort(hotPaths, (left, right) ->
            Long.compare(right.getTotalBlockedMillis(), left.getTotalBlockedMillis()));
        return hotPaths;
    }

    private static final class HotPathAggregate {

        private long mStallCount;
        private long mTotalBlockedMillis;
        private long mMaxBlockedMillis;

        void addStall(long blockedMillis) {
            mStallCount++;
            mTotalBlockedMillis += blockedMillis;
            if (blockedMillis > mMaxBlockedMillis) {
                mMaxBlockedMillis = blockedMillis;
            }
        }
    }

    public synchronized long getStallCount() {
        return mStallCount;
    }

    public synchronized long getMaxStallMillis() {
        return mMaxStallMillis;
    }

    @NonNull
    public synchronized String getMaxStallStackTrace() {
        return mMaxStallStackTrace;
    }

    public long getStallThresholdMillis() {
        return mStallThresholdMillis;
    }

    @NonNull
    private static String formatStackTrace(@Nullable StackTraceElement[] stackTrace) {
        if (stackTrace == null || stackTrace.length == 0) {
            return STACK_TRACE_NOT_SAMPLED;
        }
        StringBuilder formatted = new StringBuilder();
        int frameCount = Math.min(stackTrace.length, MAX_RECORDED_FRAMES);
        for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
            if (frameIndex > 0) {
                formatted.append('\n');
            }
            formatted.append(stackTrace[frameIndex].toString());
        }
        return formatted.toString();
    }
}
