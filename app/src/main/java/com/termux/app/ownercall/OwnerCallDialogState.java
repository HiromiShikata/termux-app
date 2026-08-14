package com.termux.app.ownercall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public final class OwnerCallDialogState {

    public void displaySession(@Nullable String sessionName) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    public String getSessionName() {
        throw new UnsupportedOperationException();
    }

    @NonNull
    public List<OwnerCall> visibleCalls(@NonNull List<OwnerCall> calls) {
        throw new UnsupportedOperationException();
    }

    public int indexOfDisplayedCall(@NonNull List<OwnerCall> visibleCalls) {
        throw new UnsupportedOperationException();
    }

    public void displayCallAt(@NonNull List<OwnerCall> visibleCalls, int index) {
        throw new UnsupportedOperationException();
    }

    public void dismiss(@NonNull OwnerCall call) {
        throw new UnsupportedOperationException();
    }
}
