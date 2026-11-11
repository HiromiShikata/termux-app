package com.termux.app.voicepatrol;

import com.termux.app.ownercall.OwnerCall;
import com.termux.app.ownercall.OwnerCallInbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Keeps the owner calls of every calling session for the voice patrol, whichever session the
 * owner is looking at, so the patrol can go through them in bell order with the screen off.
 * Reads run on the background executor and their results are applied on the main thread, which is
 * the only thread that touches the cache.
 */
public final class VoicePatrolOwnerCallCache {

    public interface CallSource {
        List<OwnerCall> readCalls(String fileUrl, String sessionName) throws Exception;
    }

    private final CallSource mSource;
    private final Executor mBackground;
    private final Executor mMainThread;
    private final VoicePatrolLog mLog;
    private final Map<String, Entry> mEntriesBySessionName = new HashMap<>();
    private final Set<String> mReadsInFlight = new HashSet<>();
    private boolean mClosed;

    public VoicePatrolOwnerCallCache(CallSource source, Executor background, Executor mainThread,
                                     VoicePatrolLog log) {
        mSource = source;
        mBackground = background;
        mMainThread = mainThread;
        mLog = log;
    }

    /**
     * Returns the calls read for the session so far and starts a read when none has been made, the
     * file address changed, or the last read is older than the refresh interval. Main thread only.
     */
    public void close() {
        mClosed = true;
        mEntriesBySessionName.clear();
        mReadsInFlight.clear();
    }

    public List<OwnerCall> callsOf(String sessionName, String fileUrl, long nowMillis) {
        if (mClosed) {
            return Collections.<OwnerCall>emptyList();
        }
        if (fileUrl == null) {
            mEntriesBySessionName.remove(sessionName);
            return Collections.<OwnerCall>emptyList();
        }
        Entry entry = mEntriesBySessionName.get(sessionName);
        if (!mReadsInFlight.contains(sessionName) && isStale(entry, fileUrl, nowMillis)) {
            startRead(sessionName, fileUrl, nowMillis);
        }
        Entry current = mEntriesBySessionName.get(sessionName);
        return current == null ? Collections.<OwnerCall>emptyList() : current.mCalls;
    }

    /** Forgets every session that is not in the given set, so a session that stopped calling is read again if it calls again. */
    public void retainOnly(Set<String> sessionNames) {
        mEntriesBySessionName.keySet().retainAll(sessionNames);
    }

    private static boolean isStale(Entry entry, String fileUrl, long nowMillis) {
        if (entry == null || !fileUrl.equals(entry.mFileUrl)) {
            return true;
        }
        long sinceReadMillis = nowMillis - entry.mReadStartedAtMillis;
        return sinceReadMillis < 0 || sinceReadMillis >= OwnerCallInbox.MINIMUM_READ_INTERVAL_MILLIS;
    }

    private void startRead(String sessionName, String fileUrl, long startedAtMillis) {
        mReadsInFlight.add(sessionName);
        mBackground.execute(() -> {
            List<OwnerCall> calls = null;
            Exception failure = null;
            try {
                calls = mSource.readCalls(fileUrl, sessionName);
            } catch (Exception readFailure) {
                failure = readFailure;
            }
            List<OwnerCall> readCalls = calls;
            Exception readFailure = failure;
            mMainThread.execute(() -> finishRead(sessionName, fileUrl, startedAtMillis, readCalls, readFailure));
        });
    }

    private void finishRead(String sessionName, String fileUrl, long startedAtMillis,
                            List<OwnerCall> calls, Exception failure) {
        if (mClosed) {
            return;
        }
        mReadsInFlight.remove(sessionName);
        if (failure != null) {
            mLog.warn("Reading the owner calls of " + sessionName + " failed: " + failure.getMessage());
            Entry previous = mEntriesBySessionName.get(sessionName);
            boolean sameFile = previous != null && fileUrl.equals(previous.mFileUrl);
            List<OwnerCall> kept = sameFile ? previous.mCalls : new ArrayList<OwnerCall>();
            mEntriesBySessionName.put(sessionName, new Entry(fileUrl, kept, startedAtMillis));
            return;
        }
        mEntriesBySessionName.put(sessionName, new Entry(fileUrl, calls, startedAtMillis));
    }

    private static final class Entry {
        final String mFileUrl;
        final List<OwnerCall> mCalls;
        final long mReadStartedAtMillis;

        Entry(String fileUrl, List<OwnerCall> calls, long readStartedAtMillis) {
            mFileUrl = fileUrl;
            mCalls = Collections.unmodifiableList(new ArrayList<>(calls));
            mReadStartedAtMillis = readStartedAtMillis;
        }
    }
}
