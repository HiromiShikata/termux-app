package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

public final class DiagnosticEventLog {

    public static final int DEFAULT_MAX_ENTRIES = 200;

    private final int mMaxEntries;
    private final Deque<DiagnosticEvent> mEvents = new ArrayDeque<>();

    public DiagnosticEventLog() {
        this(DEFAULT_MAX_ENTRIES);
    }

    public DiagnosticEventLog(int maxEntries) {
        mMaxEntries = maxEntries < 1 ? 1 : maxEntries;
    }

    public synchronized void record(long timestampMillis, @NonNull DiagnosticEventType type, @NonNull String detail) {
        mEvents.add(new DiagnosticEvent(timestampMillis, type, detail));
        while (mEvents.size() > mMaxEntries) {
            mEvents.remove();
        }
    }

    public synchronized int size() {
        return mEvents.size();
    }

    @NonNull
    public synchronized List<DiagnosticEvent> tail(int count) {
        if (count <= 0) return Collections.emptyList();
        int skip = mEvents.size() - count;
        if (skip < 0) skip = 0;
        List<DiagnosticEvent> result = new ArrayList<>();
        Iterator<DiagnosticEvent> iterator = mEvents.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            DiagnosticEvent event = iterator.next();
            if (index >= skip) {
                result.add(event);
            }
            index++;
        }
        return result;
    }
}
