package com.termux.app.ownercall;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.sessiondefinition.UnansweredOwnerCall;

import java.util.List;

public final class OwnerCallDialogController implements OwnerCallDialogBinder.OwnerCallDialogActions {

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

    @Nullable
    private String mSessionName;

    public OwnerCallDialogController(@NonNull View root,
                                     @NonNull UnansweredOwnerCallSource callSource,
                                     @NonNull OwnerCallDialogGeometrySource geometrySource) {
        mRoot = root;
        mCallSource = callSource;
        mGeometrySource = geometrySource;
    }

    public void showCallsForSession(@Nullable String sessionName, long nowMillis) {
        mSessionName = sessionName;
        render(nowMillis);
    }

    @Override
    public void onPreviousCallRequested() {
        mState.showPreviousCall();
        render(System.currentTimeMillis());
    }

    @Override
    public void onNextCallRequested() {
        mState.showNextCall();
        render(System.currentTimeMillis());
    }

    @Override
    public void onCallDismissed(@NonNull UnansweredOwnerCall call) {
        mState.dismiss(call);
        render(System.currentTimeMillis());
    }

    private void render(long nowMillis) {
        List<UnansweredOwnerCall> visibleCalls =
            mState.visibleCalls(mCallSource.unansweredCallsForSession(mSessionName));
        OwnerCallDialogPaging paging = OwnerCallDialogBinder.bind(mRoot, visibleCalls,
            mState.getIndex(), nowMillis, mGeometrySource.currentGeometry(), this);
        mState.applyResolvedIndex(paging.getIndex());
    }
}
