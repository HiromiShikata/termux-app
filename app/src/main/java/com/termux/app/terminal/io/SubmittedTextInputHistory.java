package com.termux.app.terminal.io;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SubmittedTextInputHistory {

    private final int mMaxUnpinnedSize;

    private final List<String> mEntries = new ArrayList<>();

    private final Set<String> mPinnedEntries = new LinkedHashSet<>();

    public SubmittedTextInputHistory(int maxUnpinnedSize) {
        this(maxUnpinnedSize, null);
    }

    public SubmittedTextInputHistory(int maxUnpinnedSize, @Nullable List<String> pinnedEntries) {
        mMaxUnpinnedSize = Math.max(maxUnpinnedSize, 0);
        if (pinnedEntries == null) return;
        for (String pinnedEntry : pinnedEntries) {
            if (pinnedEntry == null || pinnedEntry.isEmpty()) continue;
            if (mEntries.contains(pinnedEntry)) continue;
            mEntries.add(pinnedEntry);
            mPinnedEntries.add(pinnedEntry);
        }
    }

    public void add(@Nullable String entry) {
        if (entry == null || entry.isEmpty()) return;
        mEntries.remove(entry);
        mEntries.add(0, entry);
        enforceUnpinnedLimit();
    }

    public void pin(@Nullable String entry) {
        if (entry == null || entry.isEmpty()) return;
        if (!mEntries.contains(entry)) mEntries.add(0, entry);
        mPinnedEntries.add(entry);
    }

    public void unpin(@Nullable String entry) {
        if (entry == null) return;
        mPinnedEntries.remove(entry);
        enforceUnpinnedLimit();
    }

    public void setPinned(@Nullable String entry, boolean pinned) {
        if (pinned) {
            pin(entry);
        } else {
            unpin(entry);
        }
    }

    public boolean isPinned(@Nullable String entry) {
        return entry != null && mPinnedEntries.contains(entry);
    }

    public boolean isEmpty() {
        return mEntries.isEmpty();
    }

    @NonNull
    public List<String> orderedEntries() {
        List<String> ordered = new ArrayList<>();
        for (String entry : mEntries) {
            if (mPinnedEntries.contains(entry)) ordered.add(entry);
        }
        for (String entry : mEntries) {
            if (!mPinnedEntries.contains(entry)) ordered.add(entry);
        }
        return ordered;
    }

    @NonNull
    public List<String> pinnedEntries() {
        List<String> pinned = new ArrayList<>();
        for (String entry : mEntries) {
            if (mPinnedEntries.contains(entry)) pinned.add(entry);
        }
        return pinned;
    }

    private void enforceUnpinnedLimit() {
        int unpinnedCount = 0;
        for (String entry : mEntries) {
            if (!mPinnedEntries.contains(entry)) unpinnedCount++;
        }
        for (int index = mEntries.size() - 1; index >= 0 && unpinnedCount > mMaxUnpinnedSize; index--) {
            String entry = mEntries.get(index);
            if (mPinnedEntries.contains(entry)) continue;
            mEntries.remove(index);
            unpinnedCount--;
        }
    }
}
