package com.termux.app.ownercall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public final class OwnerCallInbox {

    public interface OnOwnerCallsChangedListener {
        void onOwnerCallsChanged();
    }

    public OwnerCallInbox() {
        throw new UnsupportedOperationException();
    }

    public OwnerCallInbox(@NonNull OwnerCallFileTransport transport) {
        throw new UnsupportedOperationException();
    }

    public void refreshFor(@Nullable String sessionName,
                           boolean sessionHasUnansweredCall,
                           @Nullable String fileUrl,
                           @NonNull OnOwnerCallsChangedListener listener) {
        throw new UnsupportedOperationException();
    }

    @NonNull
    public List<OwnerCall> callsFor(@Nullable String sessionName) {
        throw new UnsupportedOperationException();
    }

    public void deleteAnsweredCalls(@Nullable String sessionName,
                                    @Nullable String fileUrl,
                                    @NonNull OnOwnerCallsChangedListener listener) {
        throw new UnsupportedOperationException();
    }
}
