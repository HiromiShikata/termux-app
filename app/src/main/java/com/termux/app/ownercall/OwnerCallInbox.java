package com.termux.app.ownercall;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.terminal.HostTmuxSessionName;
import com.termux.shared.logger.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OwnerCallInbox {

    public interface OnOwnerCallsChangedListener {
        void onOwnerCallsChanged();
    }

    public static final long MINIMUM_READ_INTERVAL_MILLIS = 60000L;

    private static final String LOG_TAG = "OwnerCallInbox";

    @NonNull
    private final OwnerCallFileTransport mTransport;

    @NonNull
    private final OwnerCallDocumentParser mParser = new OwnerCallDocumentParser();

    @NonNull
    private List<OwnerCall> mCalls = Collections.emptyList();

    @Nullable
    private String mHeldSessionName;

    @Nullable
    private String mReadFileUrl;

    @Nullable
    private String mFileUrlBeingRead;

    @Nullable
    private String mLastReadFileUrl;

    private long mLastReadAtMillis;

    public OwnerCallInbox() {
        this(new HttpOwnerCallFileTransport());
    }

    public OwnerCallInbox(@NonNull OwnerCallFileTransport transport) {
        mTransport = transport;
    }

    public void refreshFor(@Nullable String sessionName,
                           boolean sessionHasUnansweredCall,
                           @Nullable String fileUrl,
                           long nowMillis,
                           @NonNull OnOwnerCallsChangedListener listener) {
        if (sessionName == null || sessionName.isEmpty() || !sessionHasUnansweredCall
            || fileUrl == null) {
            forget();
            return;
        }
        if (fileUrl.equals(mReadFileUrl) || fileUrl.equals(mFileUrlBeingRead)
            || wasReadWithinTheMinimumInterval(fileUrl, nowMillis)) {
            return;
        }
        forget();
        mFileUrlBeingRead = fileUrl;
        mLastReadFileUrl = fileUrl;
        mLastReadAtMillis = nowMillis;
        readFile(sessionName, fileUrl, listener);
    }

    private boolean wasReadWithinTheMinimumInterval(@NonNull String fileUrl, long nowMillis) {
        if (!fileUrl.equals(mLastReadFileUrl)) {
            return false;
        }
        long sinceLastRead = nowMillis - mLastReadAtMillis;
        return sinceLastRead >= 0 && sinceLastRead < MINIMUM_READ_INTERVAL_MILLIS;
    }

    @NonNull
    public List<OwnerCall> callsFor(@Nullable String sessionName) {
        if (sessionName == null || !sessionName.equals(mHeldSessionName)) {
            return Collections.emptyList();
        }
        return mCalls;
    }

    public void deleteAnsweredCalls(@Nullable String sessionName,
                                    @Nullable String fileUrl,
                                    @NonNull OnOwnerCallsChangedListener listener) {
        if (fileUrl == null) {
            return;
        }
        new Thread(() -> {
            try {
                mTransport.delete(fileUrl);
            } catch (Exception undeletableFile) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to delete the answered owner "
                    + "calls at " + OwnerCallFileUrl.describe(fileUrl), undeletableFile);
            }
        }).start();
        if (sessionName != null && sessionName.equals(mHeldSessionName)) {
            forget();
            listener.onOwnerCallsChanged();
        }
    }

    private void readFile(@NonNull String sessionName, @NonNull String fileUrl,
                          @NonNull OnOwnerCallsChangedListener listener) {
        String hostTmuxSessionName = HostTmuxSessionName.normalize(sessionName);
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            List<OwnerCall> calls;
            try {
                calls = callsOfSession(mParser.parse(mTransport.fetch(fileUrl)),
                    hostTmuxSessionName);
            } catch (Exception unreadableFile) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to read the owner calls at "
                    + OwnerCallFileUrl.describe(fileUrl), unreadableFile);
                mainThreadHandler.post(() -> {
                    if (fileUrl.equals(mFileUrlBeingRead)) {
                        mFileUrlBeingRead = null;
                    }
                });
                return;
            }
            mainThreadHandler.post(() -> {
                if (!fileUrl.equals(mFileUrlBeingRead)) {
                    return;
                }
                mFileUrlBeingRead = null;
                if (calls.isEmpty()) {
                    return;
                }
                mReadFileUrl = fileUrl;
                mHeldSessionName = sessionName;
                mCalls = calls;
                listener.onOwnerCallsChanged();
            });
        }).start();
    }

    @NonNull
    private static List<OwnerCall> callsOfSession(@NonNull List<OwnerCall> calls,
                                                  @NonNull String hostTmuxSessionName) {
        List<OwnerCall> callsOfSession = new ArrayList<>();
        for (OwnerCall call : calls) {
            if (hostTmuxSessionName.equals(call.getSessionName())) {
                callsOfSession.add(call);
            } else {
                Logger.logWarn(LOG_TAG, "an owner call file names the session " + call.getSessionName()
                    + " while the app opened " + hostTmuxSessionName + ", so it is not shown");
            }
        }
        return Collections.unmodifiableList(callsOfSession);
    }

    private void forget() {
        mCalls = Collections.emptyList();
        mHeldSessionName = null;
        mReadFileUrl = null;
        mFileUrlBeingRead = null;
        mLastReadFileUrl = null;
        mLastReadAtMillis = 0;
    }
}
