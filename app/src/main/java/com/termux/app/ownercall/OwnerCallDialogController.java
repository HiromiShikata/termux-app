package com.termux.app.ownercall;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
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

    public interface OwnerCallDialogPlacementStore {

        @Nullable
        OwnerCallDialogPlacement loadPlacement();

        void savePlacement(@NonNull OwnerCallDialogPlacement placement);
    }

    @NonNull
    private final View mRoot;

    @NonNull
    private final OwnerCallSource mCallSource;

    @NonNull
    private final OwnerCallDialogGeometrySource mGeometrySource;

    @NonNull
    private final OwnerCallBodySpannedText.OwnerCallBodyTapActions mBodyTapActions;

    @NonNull
    private final OwnerCallDialogPlacementStore mPlacementStore;

    @NonNull
    private final OwnerCallDialogState mState = new OwnerCallDialogState();

    @Nullable
    private OwnerCallDialogPlacement mChosenPlacement;

    private boolean mDragListenersAttached = false;

    public OwnerCallDialogController(
        @NonNull View root,
        @NonNull OwnerCallSource callSource,
        @NonNull OwnerCallDialogGeometrySource geometrySource,
        @NonNull OwnerCallBodySpannedText.OwnerCallBodyTapActions bodyTapActions,
        @NonNull OwnerCallDialogPlacementStore placementStore) {
        mRoot = root;
        mCallSource = callSource;
        mGeometrySource = geometrySource;
        mBodyTapActions = bodyTapActions;
        mPlacementStore = placementStore;
        mChosenPlacement = placementStore.loadPlacement();
    }

    @Override
    public void onCopyableTextTapped(@NonNull String text) {
        mBodyTapActions.onCopyableTextTapped(text);
    }

    @Override
    public void onUrlTapped(@NonNull String url) {
        mBodyTapActions.onUrlTapped(url);
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

    public void onDialogMovedBy(int horizontalPixels, int verticalPixels) {
        applyPlacement(currentPlacement().movedBy(horizontalPixels, verticalPixels));
    }

    public void onDialogResizedBy(int horizontalPixels, int verticalPixels) {
        applyPlacement(currentPlacement().resizedBy(horizontalPixels, verticalPixels));
    }

    public void onDialogPlacementCommitted() {
        if (mChosenPlacement != null) {
            mPlacementStore.savePlacement(mChosenPlacement);
        }
    }

    @NonNull
    private OwnerCallDialogPlacement currentPlacement() {
        return resolvePlacement(mGeometrySource.currentGeometry());
    }

    @NonNull
    private OwnerCallDialogPlacement resolvePlacement(@NonNull OwnerCallDialogGeometry geometry) {
        return OwnerCallDialogPlacementResolver.resolve(mChosenPlacement,
            geometry.getDefaultPlacement(), geometry.getAvailableWidthPixels(),
            geometry.getAvailableHeightPixels(), geometry.getMinimumWidthPixels(),
            geometry.getMinimumHeightPixels());
    }

    private void applyPlacement(@NonNull OwnerCallDialogPlacement requestedPlacement) {
        OwnerCallDialogGeometry geometry = mGeometrySource.currentGeometry();
        mChosenPlacement = OwnerCallDialogPlacementResolver.resolve(requestedPlacement,
            geometry.getDefaultPlacement(), geometry.getAvailableWidthPixels(),
            geometry.getAvailableHeightPixels(), geometry.getMinimumWidthPixels(),
            geometry.getMinimumHeightPixels());
        applyChosenPlacementToDialog();
    }

    private void render(int offsetFromDisplayedCall, long nowMillis) {
        if (mState.isDialogClosed()) {
            hideDialog();
            return;
        }
        OwnerCallDialogGeometry geometry = mGeometrySource.currentGeometry();
        OwnerCallDialogPlacement placement = resolvePlacement(geometry);
        if (mChosenPlacement != null) {
            mChosenPlacement = placement;
        }
        List<OwnerCall> calls = mCallSource.callsForSession(mState.getSessionName());
        int requestedIndex = mState.indexOfDisplayedCall(calls) + offsetFromDisplayedCall;
        OwnerCallDialogPaging paging = OwnerCallDialogBinder.bind(mRoot, calls,
            requestedIndex, nowMillis, geometry.withPlacement(placement), this);
        mState.displayCallAt(calls, paging.getIndex());
        ensureDragListenersAttached();
    }

    private void applyChosenPlacementToDialog() {
        View dialog = mRoot.findViewById(R.id.owner_call_dialog);
        if (dialog == null || mChosenPlacement == null) {
            return;
        }
        ViewGroup.MarginLayoutParams params =
            (ViewGroup.MarginLayoutParams) dialog.getLayoutParams();
        if (params.leftMargin == mChosenPlacement.getLeftMarginPixels()
            && params.bottomMargin == mChosenPlacement.getBottomMarginPixels()
            && params.width == mChosenPlacement.getWidthPixels()
            && params.height == mChosenPlacement.getHeightPixels()) {
            return;
        }
        params.leftMargin = mChosenPlacement.getLeftMarginPixels();
        params.bottomMargin = mChosenPlacement.getBottomMarginPixels();
        params.width = mChosenPlacement.getWidthPixels();
        params.height = mChosenPlacement.getHeightPixels();
        dialog.setLayoutParams(params);
    }

    private void ensureDragListenersAttached() {
        if (mDragListenersAttached) {
            return;
        }
        View header = mRoot.findViewById(R.id.owner_call_dialog_header);
        View resizeHandle = mRoot.findViewById(R.id.owner_call_dialog_resize_handle);
        if (header == null || resizeHandle == null) {
            return;
        }
        attachDragListenerTo(header, this::onDialogMovedBy);
        attachDragListenerTo(resizeHandle, this::onDialogResizedBy);
        mDragListenersAttached = true;
    }

    private interface OwnerCallDialogDragAction {

        void applyStep(int horizontalPixels, int verticalPixels);
    }

    private void attachDragListenerTo(@NonNull View handle,
                                      @NonNull OwnerCallDialogDragAction dragAction) {
        OwnerCallDialogDragGesture gesture = new OwnerCallDialogDragGesture(
            ViewConfiguration.get(handle.getContext()).getScaledTouchSlop());
        handle.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    gesture.onTouchDown(event.getRawX(), event.getRawY());
                    break;
                case MotionEvent.ACTION_MOVE:
                    OwnerCallDialogDragStep step =
                        gesture.onTouchMoved(event.getRawX(), event.getRawY());
                    if (step != null) {
                        dragAction.applyStep(step.getHorizontalPixels(), step.getVerticalPixels());
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (gesture.onTouchFinished()) {
                        onDialogPlacementCommitted();
                    }
                    break;
                default:
                    break;
            }
            return true;
        });
    }
}
