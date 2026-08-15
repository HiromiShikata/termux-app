package com.termux.app.ownercall;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;

public final class OwnerCallDialogFrame extends LinearLayout {

    private static final float EDGE_BAND_DP = 20f;

    public interface OwnerCallDialogFrameResizeActions {

        void onDialogResizedBy(@NonNull OwnerCallDialogEdgeGrip grip, int horizontalPixels,
                               int verticalPixels);

        void onDialogPlacementCommitted();
    }

    private final int mEdgeBandPixels;

    @NonNull
    private final OwnerCallDialogDragGesture mDragGesture;

    @Nullable
    private OwnerCallDialogFrameResizeActions mResizeActions;

    @NonNull
    private OwnerCallDialogEdgeGrip mGrip = OwnerCallDialogEdgeGrip.nothingGripped();

    public OwnerCallDialogFrame(@NonNull Context context) {
        this(context, null);
    }

    public OwnerCallDialogFrame(@NonNull Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        mEdgeBandPixels = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            EDGE_BAND_DP, getResources().getDisplayMetrics()));
        mDragGesture = new OwnerCallDialogDragGesture(
            ViewConfiguration.get(context).getScaledTouchSlop());
    }

    public void setResizeActions(@Nullable OwnerCallDialogFrameResizeActions resizeActions) {
        mResizeActions = resizeActions;
    }

    public int getEdgeBandPixels() {
        return mEdgeBandPixels;
    }

    @NonNull
    public OwnerCallDialogEdgeGrip gripAt(int touchXPixels, int touchYPixels) {
        return OwnerCallDialogEdgeGrip.resolve(touchXPixels, touchYPixels, getWidth(), getHeight(),
            mEdgeBandPixels, headerBottomPixels());
    }

    private int headerBottomPixels() {
        View header = findViewById(R.id.owner_call_dialog_header);
        return header == null ? 0 : header.getBottom();
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (mGrip.isAnyEdgeGripped()) {
            return;
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startGripAt(event);
                return false;
            case MotionEvent.ACTION_MOVE:
                return mGrip.isAnyEdgeGripped() && applyDragStepOf(event);
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                finishGrip();
                return false;
            default:
                return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startGripAt(event);
                return mGrip.isAnyEdgeGripped() || super.onTouchEvent(event);
            case MotionEvent.ACTION_MOVE:
                if (!mGrip.isAnyEdgeGripped()) {
                    return super.onTouchEvent(event);
                }
                applyDragStepOf(event);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                boolean wasResizing = finishGrip();
                return wasResizing || super.onTouchEvent(event);
            default:
                return super.onTouchEvent(event);
        }
    }

    private void startGripAt(@NonNull MotionEvent event) {
        mGrip = gripAt((int) event.getX(), (int) event.getY());
        if (mGrip.isAnyEdgeGripped()) {
            mDragGesture.onTouchDown(event.getRawX(), event.getRawY());
        }
    }

    private boolean applyDragStepOf(@NonNull MotionEvent event) {
        OwnerCallDialogDragStep step =
            mDragGesture.onTouchMoved(event.getRawX(), event.getRawY());
        if (step == null) {
            return false;
        }
        if (mResizeActions != null) {
            mResizeActions.onDialogResizedBy(mGrip, step.getHorizontalPixels(),
                step.getVerticalPixels());
        }
        return true;
    }

    private boolean finishGrip() {
        boolean wasResizing = mDragGesture.onTouchFinished();
        mGrip = OwnerCallDialogEdgeGrip.nothingGripped();
        if (wasResizing && mResizeActions != null) {
            mResizeActions.onDialogPlacementCommitted();
        }
        return wasResizing;
    }
}
