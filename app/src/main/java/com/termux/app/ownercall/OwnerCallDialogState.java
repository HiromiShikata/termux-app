package com.termux.app.ownercall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public final class OwnerCallDialogState {

    private static final int OLDEST_CALL = 0;

    private boolean mIsDialogClosed;

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
        mIsDialogClosed = false;
    }

    @Nullable
    public String getSessionName() {
        return mSessionName;
    }

    public int indexOfDisplayedCall(@NonNull List<OwnerCall> calls) {
        if (mDisplayedCalledAt == null) {
            return OLDEST_CALL;
        }
        for (int index = 0; index < calls.size(); index++) {
            if (mDisplayedCalledAt.equals(calls.get(index).getCalledAt())) {
                return index;
            }
        }
        return OLDEST_CALL;
    }

    public void displayCallAt(@NonNull List<OwnerCall> calls, int index) {
        mDisplayedCalledAt = index < 0 || index >= calls.size()
            ? null : calls.get(index).getCalledAt();
    }

    public void closeDialog() {
    }

    public boolean isDialogClosed() {
        return false;
    }

    public void reopenDialog() {
    }
}
