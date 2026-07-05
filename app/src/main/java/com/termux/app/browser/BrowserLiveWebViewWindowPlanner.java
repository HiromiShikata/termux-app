package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BrowserLiveWebViewWindowPlanner<T> {

    public static final int DEFAULT_WINDOW_SIZE = 4;

    private final List<T> mToKeepLive;

    private final List<T> mToRelease;

    private BrowserLiveWebViewWindowPlanner(@NonNull List<T> toKeepLive, @NonNull List<T> toRelease) {
        mToKeepLive = Collections.unmodifiableList(new ArrayList<>(toKeepLive));
        mToRelease = Collections.unmodifiableList(new ArrayList<>(toRelease));
    }

    @NonNull
    public static <T> BrowserLiveWebViewWindowPlanner<T> resolve(
            @Nullable T displayedWebViewOwner,
            @NonNull List<T> liveOwnersMostRecentFirst,
            int windowSize) {
        int boundedWindowSize = Math.max(1, windowSize);
        LinkedHashSet<T> keepLive = new LinkedHashSet<>();
        if (displayedWebViewOwner != null) keepLive.add(displayedWebViewOwner);
        for (T owner : liveOwnersMostRecentFirst) {
            if (owner == null) continue;
            if (keepLive.size() >= boundedWindowSize) break;
            keepLive.add(owner);
        }
        List<T> toRelease = new ArrayList<>();
        Set<T> alreadyListedForRelease = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (T owner : liveOwnersMostRecentFirst) {
            if (owner == null) continue;
            if (keepLive.contains(owner)) continue;
            if (!alreadyListedForRelease.add(owner)) continue;
            toRelease.add(owner);
        }
        return new BrowserLiveWebViewWindowPlanner<>(new ArrayList<>(keepLive), toRelease);
    }

    @NonNull
    public List<T> getOwnersToKeepLive() {
        return mToKeepLive;
    }

    @NonNull
    public List<T> getOwnersToRelease() {
        return mToRelease;
    }
}
