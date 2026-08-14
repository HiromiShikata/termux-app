package com.termux.app.ownercall;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;

import java.util.List;

public final class OwnerCallDialogController implements OwnerCallDialogBinder.OwnerCallDialogActions {

    private static final int KEEP_DISPLAYED_CALL = 0;
    private static final int PREVIOUS_CALL = -1;
    private static final int NEXT_CALL = 1;

    public interface OwnerCallSource {

        @NonNull
        List<OwnerCall> callsForSession(@Nullable String sessionName);
    }

    public interface OwnerCallDialogGeometrySource {

        @NonNull
        OwnerCallDialogGeometry currentGeometry();
    }

    @NonNull
    private final View mRoot;

    @NonNull
    private final OwnerCallSource mCallSource;

    @NonNull
    private final OwnerCallDialogGeometrySource mGeometrySource;

    @NonNull
    private final OwnerCallDialogState mState = new OwnerCallDialogState();

    public OwnerCallDialogController(@NonNull View root,
                                     @NonNull OwnerCallSource callSource,
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
    public void onDialogCloseRequested() {
        mState.closeDialog();
        hideDialog();
    }

    public void reopenDialog(long nowMillis) {
        mState.reopenDialog();
        render(KEEP_DISPLAYED_CALL, nowMillis);
    }

    private void hideDialog() {
        View dialog = mRoot.findViewById(R.id.owner_call_dialog);
        if (dialog != null && dialog.getVisibility() != View.GONE) {
            dialog.setVisibility(View.GONE);
        }
    }

    private void render(int offsetFromDisplayedCall, long nowMillis) {
        if (mState.isDialogClosed()) {
            hideDialog();
            return;
        }
        List<OwnerCall> calls =
            mCallSource.callsForSession(mState.getSessionName());
        int requestedIndex = mState.indexOfDisplayedCall(calls) + offsetFromDisplayedCall;
        OwnerCallDialogPaging paging = OwnerCallDialogBinder.bind(mRoot, calls,
            requestedIndex, nowMillis, mGeometrySource.currentGeometry(), this);
        mState.displayCallAt(calls, paging.getIndex());
    }
}
