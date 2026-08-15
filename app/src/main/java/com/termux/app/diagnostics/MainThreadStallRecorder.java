package com.termux.app.diagnostics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MainThreadStallRecorder {

    public static final String STACK_TRACE_NOT_SAMPLED = "not sampled";

    private static final int MAX_RECORDED_FRAMES = 16;

    private static final int MAX_TRACKED_HOT_PATHS = 64;

    private final Map<String, HotPathAggregate> mHotPathsByStackTrace = new HashMap<>();

    private final long mStallThresholdMillis;

    private final Map<String, Long> mOutstandingMillisByStackTrace = new LinkedHashMap<>();

    private final Map<String, DiagnosticsRenderThread> mRenderThreadByStackTrace = new HashMap<>();

    private boolean mHeartbeatOutstanding;
    private long mHeartbeatPostedAtMillis;
    private long mOutstandingSegmentStartMillis;
    private long mLastStackSampleAtMillis;
    private String mOutstandingStackTrace = STACK_TRACE_NOT_SAMPLED;

    private long mStallCount;
    private long mMaxStallMillis;
    private String mMaxStallStackTrace = "";
    private DiagnosticsRenderThread mMaxStallRenderThread = DiagnosticsRenderThread.NOT_TAKEN;

    private long mStackSampleAttemptCount;
    private long mEmptyStackSampleCount;

    public MainThreadStallRecorder(long stallThresholdMillis) {
        mStallThresholdMillis = stallThresholdMillis;
    }

    public synchronized void heartbeatPosted(long postedAtMillis) {
        mHeartbeatOutstanding = true;
        mHeartbeatPostedAtMillis = postedAtMillis;
        mOutstandingSegmentStartMillis = postedAtMillis;
        mLastStackSampleAtMillis = postedAtMillis;
        mOutstandingStackTrace = STACK_TRACE_NOT_SAMPLED;
        mOutstandingMillisByStackTrace.clear();
        mRenderThreadByStackTrace.clear();
    }

    public synchronized boolean needsStackSample(long sampledAtMillis) {
        if (!mHeartbeatOutstanding
                || sampledAtMillis - mHeartbeatPostedAtMillis < mStallThresholdMillis) {
            return false;
        }
        if (STACK_TRACE_NOT_SAMPLED.equals(mOutstandingStackTrace)) {
            return true;
        }
        return sampledAtMillis - mLastStackSampleAtMillis >= mStallThresholdMillis;
    }

    public synchronized void sampleWhileOutstanding(long sampledAtMillis,
                                                    @Nullable StackTraceElement[] mainThreadStackTrace) {
        sampleWhileOutstanding(sampledAtMillis, mainThreadStackTrace, DiagnosticsRenderThread.NOT_TAKEN);
    }

    public synchronized void sampleWhileOutstanding(long sampledAtMillis,
                                                    @Nullable StackTraceElement[] mainThreadStackTrace,
                                                    @NonNull DiagnosticsRenderThread renderThread) {
        if (!mHeartbeatOutstanding
                || sampledAtMillis - mHeartbeatPostedAtMillis < mStallThresholdMillis) {
            return;
        }
        mStackSampleAttemptCount++;
        String formatted = formatStackTrace(mainThreadStackTrace);
        if (STACK_TRACE_NOT_SAMPLED.equals(formatted)) {
            mEmptyStackSampleCount++;
            return;
        }
        mRenderThreadByStackTrace.put(formatted, renderThread);
        closeOutstandingSegmentAt(sampledAtMillis);
        mLastStackSampleAtMillis = sampledAtMillis;
        mOutstandingStackTrace = formatted;
    }

    private void closeOutstandingSegmentAt(long untilMillis) {
        if (STACK_TRACE_NOT_SAMPLED.equals(mOutstandingStackTrace)) {
            return;
        }
        long heldMillis = untilMillis - mOutstandingSegmentStartMillis;
        Long alreadyHeldMillis = mOutstandingMillisByStackTrace.get(mOutstandingStackTrace);
        mOutstandingMillisByStackTrace.put(mOutstandingStackTrace,
            alreadyHeldMillis == null ? heldMillis : alreadyHeldMillis + heldMillis);
        mOutstandingSegmentStartMillis = untilMillis;
    }

    public synchronized long getStackSampleAttemptCount() {
        return mStackSampleAttemptCount;
    }

    public synchronized long getEmptyStackSampleCount() {
        return mEmptyStackSampleCount;
    }

    public synchronized void heartbeatRan(long ranAtMillis) {
        if (!mHeartbeatOutstanding) {
            return;
        }
        mHeartbeatOutstanding = false;
        long stallMillis = ranAtMillis - mHeartbeatPostedAtMillis;
        if (stallMillis < mStallThresholdMillis) {
            mOutstandingMillisByStackTrace.clear();
            return;
        }
        closeOutstandingSegmentAt(ranAtMillis);
        if (mOutstandingMillisByStackTrace.isEmpty()) {
            mOutstandingMillisByStackTrace.put(STACK_TRACE_NOT_SAMPLED, stallMillis);
        }
        mStallCount++;
        if (stallMillis > mMaxStallMillis) {
            mMaxStallMillis = stallMillis;
            mMaxStallStackTrace = stackTraceHoldingTheMainThreadLongest();
            DiagnosticsRenderThread rt = mRenderThreadByStackTrace.get(mMaxStallStackTrace);
            mMaxStallRenderThread = rt != null ? rt : DiagnosticsRenderThread.NOT_TAKEN;
        }
        for (Map.Entry<String, Long> heldEntry : mOutstandingMillisByStackTrace.entrySet()) {
            recordHotPath(heldEntry.getKey(), heldEntry.getValue());
        }
        mOutstandingMillisByStackTrace.clear();
    }

    @NonNull
    private String stackTraceHoldingTheMainThreadLongest() {
        String longestHoldingStackTrace = STACK_TRACE_NOT_SAMPLED;
        long longestHeldMillis = -1L;
        for (Map.Entry<String, Long> heldEntry : mOutstandingMillisByStackTrace.entrySet()) {
            if (heldEntry.getValue() > longestHeldMillis) {
                longestHeldMillis = heldEntry.getValue();
                longestHoldingStackTrace = heldEntry.getKey();
            }
        }
        return longestHoldingStackTrace;
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

    @NonNull
    public synchronized DiagnosticsRenderThread getMaxStallRenderThread() {
        return mMaxStallRenderThread;
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
