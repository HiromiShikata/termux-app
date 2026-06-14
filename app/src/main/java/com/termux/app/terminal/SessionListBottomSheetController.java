package com.termux.app.terminal;

import android.graphics.Color;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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

    static final int DISMISS_SWIPE_MIN_DISTANCE_PIXELS = 48;
    static final int DISMISS_SWIPE_MIN_VELOCITY_PIXELS_PER_SECOND = 200;

    private final TermuxActivity mActivity;
    private final View mSheetView;
    private final View mDragHandleView;
    private final TextView mTitleView;
    private final ListView mSessionListView;
    private final View mNewSessionButton;
    private final View mLoadSessionButton;

    private boolean mAdapterBound;

    public SessionListBottomSheetController(@NonNull TermuxActivity activity) {
        this.mActivity = activity;
        this.mSheetView = activity.findViewById(R.id.session_list_bottom_sheet);
        this.mDragHandleView = activity.findViewById(R.id.session_list_bottom_sheet_drag_handle);
        this.mTitleView = activity.findViewById(R.id.session_list_bottom_sheet_title);
        this.mSessionListView = activity.findViewById(R.id.session_list_bottom_sheet_list);
        this.mNewSessionButton = activity.findViewById(R.id.session_list_bottom_sheet_new_session_button);
        this.mLoadSessionButton = activity.findViewById(R.id.session_list_bottom_sheet_load_session_button);
        bindActionButtons();
        bindSwipeDownToDismiss();
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

    private void bindSwipeDownToDismiss() {
        GestureDetector gestureDetector =
            new GestureDetector(mActivity, new SwipeDownGestureListener());
        View.OnTouchListener swipeToDismissListener = (view, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        };
        mDragHandleView.setOnTouchListener(swipeToDismissListener);
        mTitleView.setOnTouchListener(swipeToDismissListener);
    }

    private final class SwipeDownGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(@NonNull MotionEvent event) {
            return true;
        }

        @Override
        public boolean onFling(@Nullable MotionEvent start, @NonNull MotionEvent end,
                               float velocityX, float velocityY) {
            float verticalDistance = start == null ? end.getY() : end.getY() - start.getY();
            if (isDownwardDismissSwipe(verticalDistance, velocityY)) {
                hide();
                return true;
            }
            return false;
        }
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
        mSheetView.setVisibility(View.VISIBLE);
    }

    public void hide() {
        mSheetView.setVisibility(View.GONE);
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

    static int computeSheetMaxHeight(int screenHeightPixels) {
        return screenHeightPixels / 3;
    }

    static boolean isDownwardDismissSwipe(float verticalDistancePixels, float verticalVelocityPixelsPerSecond) {
        return verticalDistancePixels >= DISMISS_SWIPE_MIN_DISTANCE_PIXELS
            && verticalVelocityPixelsPerSecond >= DISMISS_SWIPE_MIN_VELOCITY_PIXELS_PER_SECOND;
    }
}
