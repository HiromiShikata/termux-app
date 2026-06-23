package com.termux.app.terminal;

import android.os.Handler;
import android.os.Looper;
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
import androidx.core.content.ContextCompat;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.browser.ProjectBrowserOverlayController;
import com.termux.app.browser.TermuxBrowserController;
import com.termux.shared.view.KeyboardUtils;
import com.termux.terminal.TerminalSession;

public class SessionListBottomSheetController {

    static final long SLIDE_ANIMATION_DURATION_MILLISECONDS = 220;

    static final long RELATIVE_TIME_REFRESH_INTERVAL_MILLISECONDS = 1000L;

    static final String GOOGLE_URL = "https://www.google.com";

    private final TermuxActivity mActivity;
    private final View mSheetView;
    private final View mScrimView;
    private final View mDragHandleView;
    private final TextView mTitleView;
    private final ListView mSessionListView;
    private final View mSettingsButton;
    private final View mNewSessionButton;
    private final View mLoadSessionButton;
    private final View mGoogleButton;

    private boolean mAdapterBound;
    private float mDragStartRawY;
    private int mDragStartHeightPixels;
    private int mMinHeightPixels;
    private int mMaxHeightPixels;
    private VelocityTracker mVelocityTracker;

    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    private final Runnable mRelativeTimeRefreshRunnable = this::onRelativeTimeRefreshTick;
    private boolean mRelativeTimeRefreshScheduled;

    public SessionListBottomSheetController(@NonNull TermuxActivity activity) {
        this.mActivity = activity;
        this.mSheetView = activity.findViewById(R.id.session_list_bottom_sheet);
        this.mScrimView = activity.findViewById(R.id.session_list_bottom_sheet_scrim);
        this.mDragHandleView = activity.findViewById(R.id.session_list_bottom_sheet_drag_handle);
        this.mTitleView = activity.findViewById(R.id.session_list_bottom_sheet_title);
        this.mSessionListView = activity.findViewById(R.id.session_list_bottom_sheet_list);
        this.mSettingsButton = activity.findViewById(R.id.session_list_bottom_sheet_settings_button);
        this.mNewSessionButton = activity.findViewById(R.id.session_list_bottom_sheet_new_session_button);
        this.mLoadSessionButton = activity.findViewById(R.id.session_list_bottom_sheet_load_session_button);
        this.mGoogleButton = activity.findViewById(R.id.session_list_bottom_sheet_google_button);
        bindActionButtons();
        bindDragToDismiss();
        bindScrimTapToDismiss();
    }

    private void bindScrimTapToDismiss() {
        mScrimView.setOnClickListener(v -> hide());
    }

    private void bindActionButtons() {
        mSettingsButton.setOnClickListener(v -> {
            hideSoftKeyboard();
            hide();
            mActivity.openSettingsActivity();
        });
        mNewSessionButton.setOnClickListener(v -> {
            hideSoftKeyboard();
            hide();
            mActivity.promptAndCreateNewSession();
        });
        mLoadSessionButton.setOnClickListener(v -> {
            hideSoftKeyboard();
            hide();
            mActivity.loadSessionsFromDefinition();
        });
        mGoogleButton.setOnClickListener(v -> {
            hideSoftKeyboard();
            hide();
            openGoogle();
        });
    }

    private void hideBrowserIfShowing() {
        TermuxBrowserController browserController = mActivity.getTermuxBrowserController();
        if (browserController == null) {
            return;
        }
        if (shouldHideBrowserOnOpen(browserController.isBrowserVisible())) {
            browserController.showTerminal();
        }
        ProjectBrowserOverlayController projectBrowserOverlayController = mActivity.getProjectBrowserOverlayController();
        if (projectBrowserOverlayController != null && projectBrowserOverlayController.isVisible()) {
            projectBrowserOverlayController.hide();
        }
    }

    static boolean shouldHideBrowserOnOpen(boolean browserVisible) {
        return browserVisible;
    }

    private void openGoogle() {
        TermuxBrowserController browserController = mActivity.getTermuxBrowserController();
        if (browserController == null) {
            return;
        }
        browserController.openUrlInNewTab(GOOGLE_URL);
    }

    private void hideSoftKeyboard() {
        KeyboardUtils.hideSoftKeyboard(mActivity, mActivity.getTerminalView());
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
                mDragStartHeightPixels = (int) sheetHeightPixels();
                mVelocityTracker = VelocityTracker.obtain();
                mVelocityTracker.addMovement(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mVelocityTracker != null) {
                    mVelocityTracker.addMovement(event);
                }
                float downwardDrag = event.getRawY() - mDragStartRawY;
                setSheetHeight(SessionListBottomSheetDragDecision.resolveDragHeight(
                    downwardDrag, mDragStartHeightPixels, mMinHeightPixels, mMaxHeightPixels));
                mSheetView.setTranslationY(SessionListBottomSheetDragDecision.resolveDragTranslation(
                    downwardDrag, mDragStartHeightPixels, mMinHeightPixels));
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                float verticalVelocity = computeVerticalVelocityAndRecycle();
                float releaseDownwardDrag = event.getRawY() - mDragStartRawY;
                if (SessionListBottomSheetDragDecision.shouldDismissAfterDrag(
                        releaseDownwardDrag, mDragStartHeightPixels, mMinHeightPixels, verticalVelocity)) {
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
        stopRelativeTimeRefresh();
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
        hideBrowserIfShowing();
        hideSoftKeyboard();
        applyTitleColor();
        applySheetDefaultHeight();
        bindSessionList(listController);
        applySessionCountTitle(listController);
        mSheetView.animate().cancel();
        mScrimView.setVisibility(scrimVisibilityForSheet(View.VISIBLE));
        mSheetView.setVisibility(View.VISIBLE);
        mSheetView.setTranslationY(sheetHeightPixels());
        mSheetView.animate()
            .translationY(0f)
            .setInterpolator(new DecelerateInterpolator())
            .setDuration(SLIDE_ANIMATION_DURATION_MILLISECONDS)
            .start();
        revealCurrentSessionRow(listController);
        startRelativeTimeRefresh();
    }

    private void onRelativeTimeRefreshTick() {
        mRelativeTimeRefreshScheduled = false;
        if (!shouldKeepRefreshing(mSheetView.getVisibility())) {
            return;
        }
        TermuxSessionsListViewController listController = mActivity.getTermuxSessionListViewController();
        if (listController != null) {
            listController.notifyDataSetChanged();
        }
        scheduleRelativeTimeRefresh();
    }

    private void startRelativeTimeRefresh() {
        scheduleRelativeTimeRefresh();
    }

    private void scheduleRelativeTimeRefresh() {
        if (mRelativeTimeRefreshScheduled) {
            return;
        }
        if (!shouldKeepRefreshing(mSheetView.getVisibility())) {
            return;
        }
        mRelativeTimeRefreshScheduled = true;
        mMainThreadHandler.postDelayed(mRelativeTimeRefreshRunnable,
            RELATIVE_TIME_REFRESH_INTERVAL_MILLISECONDS);
    }

    private void stopRelativeTimeRefresh() {
        mRelativeTimeRefreshScheduled = false;
        mMainThreadHandler.removeCallbacks(mRelativeTimeRefreshRunnable);
    }

    static boolean shouldKeepRefreshing(int sheetVisibility) {
        return sheetVisibility == View.VISIBLE;
    }

    public void revealCurrentSessionRowIfShowing() {
        if (!shouldRevealForVisibility(mSheetView.getVisibility())) {
            return;
        }
        TermuxSessionsListViewController listController = mActivity.getTermuxSessionListViewController();
        if (listController == null) {
            return;
        }
        revealCurrentSessionRow(listController);
    }

    static boolean shouldRevealForVisibility(int sheetVisibility) {
        return sheetVisibility == View.VISIBLE;
    }

    private void revealCurrentSessionRow(@NonNull TermuxSessionsListViewController listController) {
        mSessionListView.post(() -> {
            TermuxService service = mActivity.getTermuxService();
            if (service == null) {
                return;
            }
            TerminalSession currentSession = mActivity.getCurrentSession();
            int currentSessionIndex = service.getIndexOfSession(currentSession);
            if (currentSessionIndex < 0) {
                return;
            }
            int rowPosition = listController.getRowPositionForSessionIndex(currentSessionIndex);
            if (rowPosition < 0) {
                return;
            }
            mSessionListView.setSelection(rowPosition);
        });
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
        mTitleView.setTextColor(ContextCompat.getColor(mActivity, com.termux.shared.R.color.schema_text_primary));
    }

    public void refreshSessionCountTitleIfShowing() {
        if (mSheetView.getVisibility() != View.VISIBLE) {
            return;
        }
        TermuxSessionsListViewController listController = mActivity.getTermuxSessionListViewController();
        if (listController == null) {
            return;
        }
        applySessionCountTitle(listController);
    }

    private void applySessionCountTitle(@NonNull TermuxSessionsListViewController listController) {
        String baseTitle = mActivity.getString(R.string.title_session_list_bottom_sheet);
        mTitleView.setText(sessionCountTitle(baseTitle, listController.getTotalSessionCount()));
    }

    @NonNull
    static String sessionCountTitle(@NonNull String baseTitle, int totalSessionCount) {
        return baseTitle + " (" + totalSessionCount + ")";
    }

    private void applySheetDefaultHeight() {
        int screenHeight = mActivity.getResources().getDisplayMetrics().heightPixels;
        mMinHeightPixels = SessionListBottomSheetDragDecision.computeMinHeight(screenHeight);
        mMaxHeightPixels = SessionListBottomSheetDragDecision.computeMaxHeight(screenHeight);
        setSheetHeight(SessionListBottomSheetDragDecision.computeDefaultHeight(screenHeight));
    }

    private void setSheetHeight(int heightPixels) {
        ViewGroup.LayoutParams params = mSheetView.getLayoutParams();
        if (params.height != heightPixels) {
            params.height = heightPixels;
            mSheetView.setLayoutParams(params);
        }
    }

    private void bindSessionList(@NonNull TermuxSessionsListViewController listController) {
        if (mAdapterBound) {
            return;
        }
        bindSessionListAdapter(mSessionListView, listController);
        listController.setSessionClickHost(this::dismissAfterSessionSelected);
        mSessionListView.setOnItemClickListener(listController);
        mSessionListView.setOnItemLongClickListener(listController);
        mAdapterBound = true;
    }

    private void dismissAfterSessionSelected() {
        mActivity.getDrawer().closeDrawers();
        hideSoftKeyboard();
        hide();
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
}
