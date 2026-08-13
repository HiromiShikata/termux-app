package com.termux.app.ownercall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.sessiondefinition.UnansweredOwnerCall;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class OwnerCallDialogState {

    private static final String SESSION_AND_CALL_SEPARATOR = "\n";

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
    public List<UnansweredOwnerCall> visibleCalls(@NonNull List<UnansweredOwnerCall> calls) {
        List<UnansweredOwnerCall> visible = new ArrayList<>();
        for (UnansweredOwnerCall call : calls) {
            if (!mDismissedCallKeys.contains(dismissalKey(call))) {
                visible.add(call);
            }
        }
        return visible;
    }

    public int indexOfDisplayedCall(@NonNull List<UnansweredOwnerCall> visibleCalls) {
        if (mDisplayedCalledAt == null) {
            return 0;
        }
        for (int index = 0; index < visibleCalls.size(); index++) {
            if (mDisplayedCalledAt.equals(visibleCalls.get(index).getCalledAt())) {
                return index;
            }
        }
        return 0;
    }

    public void displayCallAt(@NonNull List<UnansweredOwnerCall> visibleCalls, int index) {
        mDisplayedCalledAt = index < 0 || index >= visibleCalls.size()
            ? null : visibleCalls.get(index).getCalledAt();
    }

    public void dismiss(@NonNull UnansweredOwnerCall call) {
        mDismissedCallKeys.add(dismissalKey(call));
        mDisplayedCalledAt = null;
    }

    @NonNull
    private String dismissalKey(@NonNull UnansweredOwnerCall call) {
        return mSessionName + SESSION_AND_CALL_SEPARATOR + call.getCalledAt();
    }
}
