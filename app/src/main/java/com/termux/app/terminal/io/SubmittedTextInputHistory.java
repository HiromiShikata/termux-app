package com.termux.app.terminal.io;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public final class SubmittedTextInputHistory {

    private final int mMaxUnpinnedEntries;

    private final LinkedList<String> mEntries = new LinkedList<>();

    private final Set<String> mPinnedEntries = new LinkedHashSet<>();

    public SubmittedTextInputHistory(int maxUnpinnedEntries) {
        this(maxUnpinnedEntries, new ArrayList<>());
    }

    public SubmittedTextInputHistory(int maxUnpinnedEntries, @NonNull List<String> initialPinnedEntries) {
        mMaxUnpinnedEntries = maxUnpinnedEntries < 1 ? 1 : maxUnpinnedEntries;
        for (String pinnedEntry : initialPinnedEntries) {
            if (pinnedEntry == null || pinnedEntry.isEmpty()) continue;
            if (mEntries.contains(pinnedEntry)) continue;
            mEntries.add(pinnedEntry);
            mPinnedEntries.add(pinnedEntry);
        }
    }

    public void add(@Nullable String submittedTextInput) {
        if (submittedTextInput == null || submittedTextInput.isEmpty()) return;
        mEntries.remove(submittedTextInput);
        mEntries.add(0, submittedTextInput);
        enforceUnpinnedCapacity();
    }

    public boolean isPinned(@NonNull String entry) {
        return mPinnedEntries.contains(entry);
    }

    public void pin(@NonNull String entry) {
        if (!mEntries.contains(entry)) return;
        mPinnedEntries.add(entry);
    }

    public void unpin(@NonNull String entry) {
        mPinnedEntries.remove(entry);
        enforceUnpinnedCapacity();
    }

    public void togglePin(@NonNull String entry) {
        if (isPinned(entry)) {
            unpin(entry);
        } else {
            pin(entry);
        }
    }

    public boolean editPinnedEntry(@NonNull String currentEntry, @Nullable String newEntry) {
        if (!isPinned(currentEntry)) return false;
        if (newEntry == null || newEntry.isEmpty()) return false;
        if (newEntry.equals(currentEntry)) return false;

        int position = mEntries.indexOf(currentEntry);
        if (position < 0) return false;

        int existingNewEntryPosition = mEntries.indexOf(newEntry);
        if (existingNewEntryPosition >= 0) {
            mEntries.remove(existingNewEntryPosition);
            mPinnedEntries.remove(newEntry);
            if (existingNewEntryPosition < position) position--;
        }

        mEntries.set(position, newEntry);
        mPinnedEntries.remove(currentEntry);
        mPinnedEntries.add(newEntry);
        return true;
    }

    public boolean isEmpty() {
        return mEntries.isEmpty();
    }

    @NonNull
    public List<String> getOrderedEntries() {
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
    public List<String> getPinnedEntries() {
        List<String> pinned = new ArrayList<>();
        for (String entry : mEntries) {
            if (mPinnedEntries.contains(entry)) pinned.add(entry);
        }
        return pinned;
    }

    private void enforceUnpinnedCapacity() {
        int unpinnedCount = 0;
        for (String entry : mEntries) {
            if (!mPinnedEntries.contains(entry)) unpinnedCount++;
        }
        Iterator<String> oldestFirst = mEntries.descendingIterator();
        while (unpinnedCount > mMaxUnpinnedEntries && oldestFirst.hasNext()) {
            String entry = oldestFirst.next();
            if (mPinnedEntries.contains(entry)) continue;
            oldestFirst.remove();
            unpinnedCount--;
        }
    }
}
