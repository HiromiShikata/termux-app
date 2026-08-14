package com.termux.app.ownercall;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public final class OwnerCallDialogController implements OwnerCallDialogBinder.OwnerCallDialogActions {

    public interface OwnerCallSource {

        @NonNull
        List<OwnerCall> callsForSession(@Nullable String sessionName);
    }

    public interface OwnerCallDialogGeometrySource {

        @NonNull
        OwnerCallDialogGeometry currentGeometry();
    }

    public OwnerCallDialogController(@NonNull View root,
                                     @NonNull OwnerCallSource callSource,
                                     @NonNull OwnerCallDialogGeometrySource geometrySource) {
        throw new UnsupportedOperationException();
    }

    public void showCallsForSession(@Nullable String sessionName, long nowMillis) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onPreviousCallRequested() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onNextCallRequested() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onCallDismissed(@NonNull OwnerCall call) {
        throw new UnsupportedOperationException();
    }
}
