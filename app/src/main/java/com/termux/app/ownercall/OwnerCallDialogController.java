package com.termux.app.ownercall;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.sessiondefinition.UnansweredOwnerCall;

import java.util.List;

public final class OwnerCallDialogController implements OwnerCallDialogBinder.OwnerCallDialogActions {

    private static final int KEEP_DISPLAYED_CALL = 0;
    private static final int PREVIOUS_CALL = -1;
    private static final int NEXT_CALL = 1;

    public interface UnansweredOwnerCallSource {

        @NonNull
        List<UnansweredOwnerCall> unansweredCallsForSession(@Nullable String sessionName);
    }

    public interface OwnerCallDialogGeometrySource {

        @NonNull
        OwnerCallDialogGeometry currentGeometry();
    }

    private final View mRoot;
    private final UnansweredOwnerCallSource mCallSource;
    private final OwnerCallDialogGeometrySource mGeometrySource;
    private final OwnerCallDialogState mState = new OwnerCallDialogState();

    public OwnerCallDialogController(@NonNull View root,
                                     @NonNull UnansweredOwnerCallSource callSource,
                                     @NonNull OwnerCallDialogGeometrySource geometrySource) {
        mRoot = root;
        mCallSource = callSource;
        mGeometrySource = geometrySource;
    }

    public void showCallsForSession(@Nullable String sessionName, long nowMillis) {
        mState.displaySession(sessionName);
        render(KEEP_DISPLAYED_CALL, nowMillis);
    }

    @Override
    public void onPreviousCallRequested() {
        render(PREVIOUS_CALL, System.currentTimeMillis());
    }

    @Override
    public void onNextCallRequested() {
        render(NEXT_CALL, System.currentTimeMillis());
    }

    @Override
    public void onCallDismissed(@NonNull UnansweredOwnerCall call) {
        mState.dismiss(call);
        render(KEEP_DISPLAYED_CALL, System.currentTimeMillis());
    }

    private void render(int offsetFromDisplayedCall, long nowMillis) {
        List<UnansweredOwnerCall> visibleCalls =
            mState.visibleCalls(mCallSource.unansweredCallsForSession(mState.getSessionName()));
        int requestedIndex = mState.indexOfDisplayedCall(visibleCalls) + offsetFromDisplayedCall;
        OwnerCallDialogPaging paging = OwnerCallDialogBinder.bind(mRoot, visibleCalls,
            requestedIndex, nowMillis, mGeometrySource.currentGeometry(), this);
        mState.displayCallAt(visibleCalls, paging.getIndex());
    }
}
