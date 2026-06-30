package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BrowserSessionSplitRatios {

    private final Map<String, Float> mRatiosBySessionName = new LinkedHashMap<>();

    @Nullable
    public Float getRatio(@Nullable String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) return null;
        return mRatiosBySessionName.get(sessionName);
    }

    public float resolveRatioToApply(@Nullable String sessionName) {
        return BrowserSplitRatio.resolveRatioToApply(getRatio(sessionName));
    }

    public void setRatio(@Nullable String sessionName, float ratio) {
        if (sessionName == null || sessionName.isEmpty()) return;
        float clampedRatio = BrowserSplitRatio.clamp(ratio);
        if (BrowserSplitRatio.isCollapsed(clampedRatio)) {
            mRatiosBySessionName.remove(sessionName);
            return;
        }
        mRatiosBySessionName.put(sessionName, clampedRatio);
    }

    public void removeSession(@Nullable String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) return;
        mRatiosBySessionName.remove(sessionName);
    }

    public void replaceAll(@NonNull Map<String, Float> ratiosBySessionName) {
        mRatiosBySessionName.clear();
        for (Map.Entry<String, Float> entry : ratiosBySessionName.entrySet()) {
            setRatio(entry.getKey(), entry.getValue());
        }
    }

    @NonNull
    public Map<String, Float> asMap() {
        return new LinkedHashMap<>(mRatiosBySessionName);
    }
}
