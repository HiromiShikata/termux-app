package com.termux.app.ownercall;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public final class OwnerCallDialogBinder {

    public interface OwnerCallDialogActions {

        void onPreviousCallRequested();

        void onNextCallRequested();

        void onCallDismissed(@NonNull OwnerCall call);
    }

    private OwnerCallDialogBinder() {
    }

    @NonNull
    public static OwnerCallDialogPaging bind(@NonNull View root,
                                             @NonNull List<OwnerCall> calls,
                                             int requestedIndex,
                                             long nowMillis,
                                             @NonNull OwnerCallDialogGeometry geometry,
                                             @Nullable OwnerCallDialogActions actions) {
        throw new UnsupportedOperationException();
    }
}
