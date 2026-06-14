package com.termux.app.terminal;

import android.graphics.Color;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.theme.NightMode;
import com.termux.shared.theme.ThemeUtils;

public class SessionListBottomSheetController {

    static final long SLIDE_ANIMATION_DURATION_MILLISECONDS = 220;

    private final TermuxActivity mActivity;
    private final View mSheetView;
    private final View mScrimView;
    private final View mDragHandleView;
    private final TextView mTitleView;
    private final ListView mSessionListView;
    private final View mNewSessionButton;
    private final View mLoadSessionButton;

    private boolean mAdapterBound;
    private float mDragStartRawY;
    private VelocityTracker mVelocityTracker;

    public SessionListBottomSheetController(@NonNull TermuxActivity activity) {
        this.mActivity = activity;
        this.mSheetView = activity.findViewById(R.id.session_list_bottom_sheet);
        this.mScrimView = activity.findViewById(R.id.session_list_bottom_sheet_scrim);
        this.mDragHandleView = activity.findViewById(R.id.session_list_bottom_sheet_drag_handle);
        this.mTitleView = activity.findViewById(R.id.session_list_bottom_sheet_title);
        this.mSessionListView = activity.findViewById(R.id.session_list_bottom_sheet_list);
        this.mNewSessionButton = activity.findViewById(R.id.session_list_bottom_sheet_new_session_button);
        this.mLoadSessionButton = activity.findViewById(R.id.session_list_bottom_sheet_load_session_button);
        bindActionButtons();
        bindDragToDismiss();
        bindScrimTapToDismiss();
    }

    private void bindScrimTapToDismiss() {
        mScrimView.setOnClickListener(v -> hide());
    }

    private void bindActionButtons() {
        mNewSessionButton.setOnClickListener(v -> {
            hide();
            mActivity.promptAndCreateNewSession();
        });
        mLoadSessionButton.setOnClickListener(v -> {
            hide();
            mActivity.loadSessionsFromDefinition();
        });
    }

    private void bindDragToDismiss() {
        View.OnTouchListener dragListener = (view, event) -> handleDragTouch(event);
        mDragHandleView.setOnTouchListener(dragListener);
        mTitleView.setOnTouchListener(dragListener);
    }

    private boolean handleDragTouch(@NonNull MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mSheetView.animate().cancel();
                mDragStartRawY = event.getRawY();
                mVelocityTracker = VelocityTracker.obtain();
                mVelocityTracker.addMovement(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mVelocityTracker != null) {
                    mVelocityTracker.addMovement(event);
                }
                float dragDelta = event.getRawY() - mDragStartRawY;
                mSheetView.setTranslationY(
                    SessionListBottomSheetDragDecision.clampDragTranslation(dragDelta, sheetHeightPixels()));
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                float verticalVelocity = computeVerticalVelocityAndRecycle();
                if (SessionListBottomSheetDragDecision.shouldDismissAfterDrag(
                        mSheetView.getTranslationY(), verticalVelocity, sheetHeightPixels())) {
                    animateDismiss();
                } else {
                    animateSpringBack();
                }
                return true;
            default:
                return false;
        }
    }

    private float computeVerticalVelocityAndRecycle() {
        if (mVelocityTracker == null) {
            return 0f;
        }
        mVelocityTracker.computeCurrentVelocity(1000);
        float verticalVelocity = mVelocityTracker.getYVelocity();
        mVelocityTracker.recycle();
        mVelocityTracker = null;
        return verticalVelocity;
    }

    private float sheetHeightPixels() {
        int measuredHeight = mSheetView.getHeight();
        return measuredHeight > 0 ? measuredHeight : mSheetView.getLayoutParams().height;
    }

    private void animateSpringBack() {
        mSheetView.animate()
            .translationY(0f)
            .setInterpolator(new DecelerateInterpolator())
            .setDuration(SLIDE_ANIMATION_DURATION_MILLISECONDS)
            .start();
    }

    private void animateDismiss() {
        mSheetView.animate()
            .translationY(sheetHeightPixels())
            .setInterpolator(new DecelerateInterpolator())
            .setDuration(SLIDE_ANIMATION_DURATION_MILLISECONDS)
            .withEndAction(() -> {
                mSheetView.setVisibility(View.GONE);
                mSheetView.setTranslationY(0f);
            })
            .start();
    }

    public boolean isOpen() {
        return mSheetView.getVisibility() == View.VISIBLE;
    }

    public void toggle() {
        if (nextSheetVisibility(mSheetView.getVisibility()) == View.VISIBLE) {
            show();
        } else {
            hide();
        }
    }

    public void show() {
        TermuxSessionsListViewController listController = mActivity.getTermuxSessionListViewController();
        if (listController == null) {
            return;
        }
        applyTitleColor();
        applySheetHeightCap();
        bindSessionList(listController);
        mSheetView.animate().cancel();
        mScrimView.setVisibility(scrimVisibilityForSheet(View.VISIBLE));
        mSheetView.setVisibility(View.VISIBLE);
        mSheetView.setTranslationY(sheetHeightPixels());
        mSheetView.animate()
            .translationY(0f)
            .setInterpolator(new DecelerateInterpolator())
            .setDuration(SLIDE_ANIMATION_DURATION_MILLISECONDS)
            .start();
    }

    public void hide() {
        if (mSheetView.getVisibility() != View.VISIBLE) {
            return;
        }
        mScrimView.setVisibility(scrimVisibilityForSheet(View.GONE));
        animateDismiss();
    }

    public static void hideIfPresent(@Nullable SessionListBottomSheetController controller) {
        if (controller != null) {
            controller.hide();
        }
    }

    private void applyTitleColor() {
        boolean darkTheme = ThemeUtils.shouldEnableDarkTheme(mActivity, NightMode.getAppNightMode().getName());
        mTitleView.setTextColor(darkTheme ? Color.WHITE : Color.BLACK);
    }

    private void applySheetHeightCap() {
        int maxHeight = computeSheetMaxHeight(mActivity.getResources().getDisplayMetrics().heightPixels);
        ViewGroup.LayoutParams params = mSheetView.getLayoutParams();
        if (params.height != maxHeight) {
            params.height = maxHeight;
            mSheetView.setLayoutParams(params);
        }
    }

    private void bindSessionList(@NonNull TermuxSessionsListViewController listController) {
        if (mAdapterBound) {
            return;
        }
        bindSessionListAdapter(mSessionListView, listController);
        mSessionListView.setOnItemClickListener((parent, view, position, id) -> {
            boolean isSessionRow = !((SessionHierarchyRow) listController.getItem(position)).isHeader();
            listController.onItemClick(parent, view, position, id);
            if (isSessionRow) {
                hide();
            }
        });
        mSessionListView.setOnItemLongClickListener((parent, view, position, id) ->
            listController.onItemLongClick(parent, view, position, id));
        mAdapterBound = true;
    }

    static void bindSessionListAdapter(@NonNull ListView listView, @NonNull BaseAdapter adapter) {
        listView.setAdapter(adapter);
    }

    static int nextSheetVisibility(int currentVisibility) {
        return currentVisibility == View.VISIBLE ? View.GONE : View.VISIBLE;
    }

    static int scrimVisibilityForSheet(int sheetVisibility) {
        return sheetVisibility == View.VISIBLE ? View.VISIBLE : View.GONE;
    }

    static int computeSheetMaxHeight(int screenHeightPixels) {
        return screenHeightPixels / 3;
    }
}
