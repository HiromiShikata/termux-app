package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

public final class BrowserRecentlyClosedTabs {

    public static final int DEFAULT_MAX_SIZE = 10;

    private final int mMaxSize;

    private final Deque<BrowserClosedTab> mClosedTabs = new ArrayDeque<>();

    public BrowserRecentlyClosedTabs() {
        this(DEFAULT_MAX_SIZE);
    }

    public BrowserRecentlyClosedTabs(int maxSize) {
        if (maxSize < 1) throw new IllegalArgumentException("maxSize must be at least 1");
        mMaxSize = maxSize;
    }

    public void push(@NonNull BrowserClosedTab closedTab) {
        mClosedTabs.push(closedTab);
        while (mClosedTabs.size() > mMaxSize) {
            mClosedTabs.pollLast();
        }
    }

    @Nullable
    public BrowserClosedTab pop() {
        return mClosedTabs.poll();
    }

    @Nullable
    public BrowserClosedTab peek() {
        return mClosedTabs.peek();
    }

    public boolean isEmpty() {
        return mClosedTabs.isEmpty();
    }

    public int size() {
        return mClosedTabs.size();
    }

    public int getMaxSize() {
        return mMaxSize;
    }

    public void removeSession(@NonNull String sessionHandle) {
        mClosedTabs.removeIf(closedTab -> closedTab.getSessionHandle().equals(sessionHandle));
    }
}
