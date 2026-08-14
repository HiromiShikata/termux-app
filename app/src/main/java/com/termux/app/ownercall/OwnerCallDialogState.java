package com.termux.app.ownercall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class OwnerCallDialogState {

    private static final String SESSION_AND_CALL_SEPARATOR = "\n";
    private static final int OLDEST_CALL = 0;

    private final Set<String> mDismissedCallKeys = new HashSet<>();

    @Nullable
    private String mSessionName;

    @Nullable
    private String mDisplayedCalledAt;

    public void displaySession(@Nullable String sessionName) {
        if (sessionName == null ? mSessionName == null : sessionName.equals(mSessionName)) {
            return;
        }
        mSessionName = sessionName;
        mDisplayedCalledAt = null;
    }

    @Nullable
    public String getSessionName() {
        return mSessionName;
    }

    @NonNull
    public List<OwnerCall> visibleCalls(@NonNull List<OwnerCall> calls) {
        List<OwnerCall> visibleCalls = new ArrayList<>();
        for (OwnerCall call : calls) {
            if (!mDismissedCallKeys.contains(dismissalKey(call))) {
                visibleCalls.add(call);
            }
        }
        return visibleCalls;
    }

    public int indexOfDisplayedCall(@NonNull List<OwnerCall> visibleCalls) {
        if (mDisplayedCalledAt == null) {
            return OLDEST_CALL;
        }
        for (int index = 0; index < visibleCalls.size(); index++) {
            if (mDisplayedCalledAt.equals(visibleCalls.get(index).getCalledAt())) {
                return index;
            }
        }
        return OLDEST_CALL;
    }

    public void displayCallAt(@NonNull List<OwnerCall> visibleCalls, int index) {
        mDisplayedCalledAt = index < 0 || index >= visibleCalls.size()
            ? null : visibleCalls.get(index).getCalledAt();
    }

    public void dismiss(@NonNull OwnerCall call) {
        mDismissedCallKeys.add(dismissalKey(call));
        mDisplayedCalledAt = null;
    }

    @NonNull
    private String dismissalKey(@NonNull OwnerCall call) {
        return mSessionName + SESSION_AND_CALL_SEPARATOR + call.getCalledAt();
    }
}
