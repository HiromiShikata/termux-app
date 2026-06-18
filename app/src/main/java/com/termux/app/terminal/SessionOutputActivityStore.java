package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

public class SessionOutputActivityStore {

    private final Set<String> mSessionHandlesWithOutputActivity = new HashSet<>();

    public void markOutputActivity(@NonNull String sessionHandle) {
        mSessionHandlesWithOutputActivity.add(sessionHandle);
    }

    public void clearOutputActivity(@NonNull String sessionHandle) {
        mSessionHandlesWithOutputActivity.remove(sessionHandle);
    }

    public boolean hasOutputActivity(@NonNull String sessionHandle) {
        return mSessionHandlesWithOutputActivity.contains(sessionHandle);
    }
}
