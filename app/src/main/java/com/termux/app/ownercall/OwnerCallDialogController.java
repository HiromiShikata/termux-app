package com.termux.app.ownercall;

import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

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

    @Nullable
    private Integer mChosenBottomMargin = null;

    private boolean mDragListenerAttached = false;

    private boolean mDragging = false;

    private float mDragStartY = 0f;

    private int mDragStartBottomMargin = 0;

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

    @Override
    public void onDragPositionChanged(int bottomMarginPixels) {
        OwnerCallDialogGeometry geometry = mGeometrySource.currentGeometry();
        mChosenBottomMargin = OwnerCallDialogPositionResolver.resolve(
            bottomMarginPixels,
            geometry.getBottomMarginPixels(),
            geometry.getMinBottomMarginPixels(),
            geometry.getMaxBottomMarginPixels());
        applyChosenBottomMarginToDialog();
    }

    private void render(int offsetFromDisplayedCall, long nowMillis) {
        if (mState.isDialogClosed()) {
            hideDialog();
            return;
        }
        OwnerCallDialogGeometry geometry = mGeometrySource.currentGeometry();
        int resolvedBottomMargin = OwnerCallDialogPositionResolver.resolve(
            mChosenBottomMargin,
            geometry.getBottomMarginPixels(),
            geometry.getMinBottomMarginPixels(),
            geometry.getMaxBottomMarginPixels());
        if (mChosenBottomMargin != null) {
            mChosenBottomMargin = resolvedBottomMargin;
        }
        List<OwnerCall> calls = mCallSource.callsForSession(mState.getSessionName());
        int requestedIndex = mState.indexOfDisplayedCall(calls) + offsetFromDisplayedCall;
        OwnerCallDialogPaging paging = OwnerCallDialogBinder.bind(mRoot, calls,
            requestedIndex, nowMillis, geometry.withBottomMargin(resolvedBottomMargin), this);
        mState.displayCallAt(calls, paging.getIndex());
        ensureDragListenerAttached();
    }

    private void applyChosenBottomMarginToDialog() {
        View dialog = mRoot.findViewById(R.id.owner_call_dialog);
        if (dialog == null || mChosenBottomMargin == null) {
            return;
        }
        ViewGroup.MarginLayoutParams params =
            (ViewGroup.MarginLayoutParams) dialog.getLayoutParams();
        if (params.bottomMargin == mChosenBottomMargin) {
            return;
        }
        params.bottomMargin = mChosenBottomMargin;
        dialog.setLayoutParams(params);
    }

    private void ensureDragListenerAttached() {
        if (mDragListenerAttached) {
            return;
        }
        View header = mRoot.findViewById(R.id.owner_call_dialog_header);
        if (header == null) {
            return;
        }
        attachDragListenerTo(header);
        mDragListenerAttached = true;
    }

    private void attachDragListenerTo(@NonNull View header) {
        GestureDetector gestureDetector = new GestureDetector(header.getContext(),
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public void onLongPress(@NonNull MotionEvent e) {
                    mDragging = true;
                    mDragStartY = e.getRawY();
                    View dialog = mRoot.findViewById(R.id.owner_call_dialog);
                    mDragStartBottomMargin = dialog == null ? 0
                        : ((ViewGroup.MarginLayoutParams) dialog.getLayoutParams()).bottomMargin;
                    header.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                }
            });
        header.setOnTouchListener((view, event) -> {
            gestureDetector.onTouchEvent(event);
            if (mDragging) {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_MOVE) {
                    int newBottomMargin = mDragStartBottomMargin
                        + (int) (mDragStartY - event.getRawY());
                    onDragPositionChanged(newBottomMargin);
                } else if (action == MotionEvent.ACTION_UP
                    || action == MotionEvent.ACTION_CANCEL) {
                    mDragging = false;
                }
            }
            return true;
        });
    }
}
