package com.termux.app.terminal;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.browser.TermuxBrowserController;
import com.termux.app.sessiondefinition.DefaultProjectManagerSessionPlanner;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.shared.view.KeyboardUtils;
import com.termux.terminal.TerminalSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SessionListBottomSheetController {

    static final long SLIDE_ANIMATION_DURATION_MILLISECONDS = 220;

    static final long RELATIVE_TIME_REFRESH_INTERVAL_MILLISECONDS = 1000L;

    static final String GOOGLE_URL = "https://www.google.com";

    private final TermuxActivity mActivity;
    private final View mSheetView;
    private final View mScrimView;
    private final View mSessionInfoBottomContainer;
    private final View mDragHandleView;
    private final TextView mTitleView;
    private final RecyclerView mSessionListView;
    private final OnScreenSessionRowSelector mOnScreenSessionRowSelector = new OnScreenSessionRowSelector();
    private final View mSettingsButton;
    private final View mNewSessionButton;
    private final View mLoadSessionButton;
    private final View mGoogleButton;
    private final ImageButton mHiddenToggleButton;
    private final ShortcutFlowLayout mAlwaysSessionShortcutsContainer;
    private final ShortcutFlowLayout mProjectManagerSessionShortcutsContainer;
    private final SessionShortcutBarPlanner mSessionShortcutBarPlanner =
        new SessionShortcutBarPlanner(new DefaultProjectManagerSessionPlanner());

    private boolean mAdapterBound;
    private float mDragStartRawY;
    private int mDragStartHeightPixels;
    private int mMinHeightPixels;
    private int mMaxHeightPixels;
    private VelocityTracker mVelocityTracker;

    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    private final Runnable mRelativeTimeRefreshRunnable = this::onRelativeTimeRefreshTick;
    private boolean mRelativeTimeRefreshScheduled;
    private boolean mSessionListTouchInProgress;
    private boolean mRelativeTimeRefreshDeferredByTouch;

    public SessionListBottomSheetController(@NonNull TermuxActivity activity) {
        this.mActivity = activity;
        this.mSheetView = activity.findViewById(R.id.session_list_bottom_sheet);
        this.mScrimView = activity.findViewById(R.id.session_list_bottom_sheet_scrim);
        this.mSessionInfoBottomContainer = activity.findViewById(R.id.session_info_bottom_container);
        this.mDragHandleView = activity.findViewById(R.id.session_list_bottom_sheet_drag_handle);
        this.mTitleView = activity.findViewById(R.id.session_list_bottom_sheet_title);
        this.mSessionListView = activity.findViewById(R.id.session_list_bottom_sheet_list);
        this.mSettingsButton = activity.findViewById(R.id.session_list_bottom_sheet_settings_button);
        this.mNewSessionButton = activity.findViewById(R.id.session_list_bottom_sheet_new_session_button);
        this.mLoadSessionButton = activity.findViewById(R.id.session_list_bottom_sheet_load_session_button);
        this.mGoogleButton = activity.findViewById(R.id.session_list_bottom_sheet_google_button);
        this.mHiddenToggleButton = activity.findViewById(R.id.session_list_bottom_sheet_hidden_toggle_button);
        this.mAlwaysSessionShortcutsContainer =
            activity.findViewById(R.id.session_list_bottom_sheet_always_session_shortcuts_container);
        this.mProjectManagerSessionShortcutsContainer =
            activity.findViewById(R.id.session_list_bottom_sheet_project_manager_session_shortcuts_container);
        bindActionButtons();
        bindHiddenToggleButton();
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
            mActivity.reloadSessionsAndRefreshAllState();
        });
        mGoogleButton.setOnClickListener(v -> {
            hideSoftKeyboard();
            hide();
            openGoogle();
        });
    }

    private void bindHiddenToggleButton() {
        mHiddenToggleButton.setOnClickListener(v -> {
            TermuxSessionsListViewController listController = mActivity.getTermuxSessionListViewController();
            if (listController == null) {
                return;
            }
            listController.toggleHideHiddenSessions();
            applyHiddenToggleButtonState(listController);
            applySessionCountTitle(listController);
        });
    }

    private void applyHiddenToggleButtonState(@NonNull TermuxSessionsListViewController listController) {
        boolean hidingHiddenSessions = listController.isHidingHiddenSessions();
        mHiddenToggleButton.setImageResource(hiddenToggleIconResource(hidingHiddenSessions));
        mHiddenToggleButton.setContentDescription(
            mActivity.getString(hiddenToggleContentDescriptionResource(hidingHiddenSessions)));
    }

    static int hiddenToggleIconResource(boolean hidingHiddenSessions) {
        return hidingHiddenSessions ? R.drawable.ic_filter_alt : R.drawable.ic_filter_alt_off;
    }

    static int hiddenToggleContentDescriptionResource(boolean hidingHiddenSessions) {
        return hidingHiddenSessions ? R.string.action_session_filter_on : R.string.action_session_filter_off;
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

    @NonNull
    public List<String> getOnScreenSessionNames() {
        if (!isOpen()) {
            return Collections.emptyList();
        }
        TermuxSessionsListViewController listController = mActivity.getTermuxSessionListViewController();
        if (listController == null) {
            return Collections.emptyList();
        }
        RecyclerView.LayoutManager layoutManager = mSessionListView.getLayoutManager();
        if (!(layoutManager instanceof LinearLayoutManager)) {
            return Collections.emptyList();
        }
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
        return mOnScreenSessionRowSelector.selectOnScreenSessionNames(listController.getVisibleRows(),
            linearLayoutManager.findFirstVisibleItemPosition(),
            linearLayoutManager.findLastVisibleItemPosition());
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
        hideSoftKeyboard();
        applyTitleColor();
        applyHorizontalBounds();
        applySheetDefaultHeight();
        bindSessionList(listController);
        applyHiddenToggleButtonState(listController);
        applySessionCountTitle(listController);
        rebuildSessionShortcuts(listController);
        mSheetView.animate().cancel();
        mScrimView.setVisibility(scrimVisibilityForSheet(View.VISIBLE));
        applySessionInfoVisibilityForSheet(View.VISIBLE);
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
        if (!shouldRefreshNow(mSessionListTouchInProgress)) {
            mRelativeTimeRefreshDeferredByTouch = true;
            return;
        }
        refreshSessionRowsForRelativeTime();
        scheduleRelativeTimeRefresh();
    }

    private void refreshSessionRowsForRelativeTime() {
        TermuxSessionsListViewController listController = mActivity.getTermuxSessionListViewController();
        if (listController == null) {
            return;
        }
        listController.dispatchRelativeTimeTick();
    }

    private void onSessionListTouchStarted() {
        mSessionListTouchInProgress = true;
    }

    private void onSessionListTouchEnded() {
        mSessionListTouchInProgress = false;
        if (!mRelativeTimeRefreshDeferredByTouch) {
            return;
        }
        mRelativeTimeRefreshDeferredByTouch = false;
        if (!shouldKeepRefreshing(mSheetView.getVisibility())) {
            return;
        }
        scheduleRelativeTimeRefresh();
    }

    static boolean shouldRefreshNow(boolean sessionListTouchInProgress) {
        return !sessionListTouchInProgress;
    }

    static boolean isSessionListTouchInteractionEnd(int maskedAction) {
        return maskedAction == MotionEvent.ACTION_UP
            || maskedAction == MotionEvent.ACTION_CANCEL;
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
        mRelativeTimeRefreshDeferredByTouch = false;
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
            scrollSessionRowToTop(rowPosition);
        });
    }

    private void scrollSessionRowToTop(int rowPosition) {
        RecyclerView.LayoutManager layoutManager = mSessionListView.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(rowPosition, 0);
            return;
        }
        mSessionListView.scrollToPosition(rowPosition);
    }

    public void hide() {
        if (mSheetView.getVisibility() != View.VISIBLE) {
            return;
        }
        mScrimView.setVisibility(scrimVisibilityForSheet(View.GONE));
        applySessionInfoVisibilityForSheet(View.GONE);
        animateDismiss();
    }

    private void applySessionInfoVisibilityForSheet(int sheetVisibility) {
        if (mSessionInfoBottomContainer == null) {
            return;
        }
        mSessionInfoBottomContainer.setVisibility(sessionInfoVisibilityForSheet(sheetVisibility));
    }

    static int sessionInfoVisibilityForSheet(int sheetVisibility) {
        return sheetVisibility == View.VISIBLE ? View.INVISIBLE : View.VISIBLE;
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
        mTitleView.setText(sessionCountTitle(baseTitle, listController.getPendingCallSessionCount(),
            listController.getVisibleSessionCount()));
    }

    @NonNull
    static String sessionCountTitle(@NonNull String baseTitle, int pendingCallSessionCount,
                                    int visibleSessionCount) {
        return baseTitle + " " + SessionCountFraction.of(pendingCallSessionCount, visibleSessionCount);
    }

    private void rebuildSessionShortcuts(@NonNull TermuxSessionsListViewController listController) {
        mAlwaysSessionShortcutsContainer.removeAllViews();
        mProjectManagerSessionShortcutsContainer.removeAllViews();
        TermuxService service = mActivity.getTermuxService();
        if (service == null) {
            return;
        }
        Set<String> alwaysNaSessionNames = mActivity.getPreferences().getAlwaysNaSessionNames();
        SessionShortcutRows rightToLeftShortcutRows =
            mSessionShortcutBarPlanner.planRightToLeftShortcutRows(alwaysNaSessionNames,
                listController.getEntries(), liveSessionNames(service));
        fillShortcutRow(mAlwaysSessionShortcutsContainer,
            rightToLeftShortcutRows.getAlwaysSessionShortcuts(), service, listController);
        fillShortcutRow(mProjectManagerSessionShortcutsContainer,
            rightToLeftShortcutRows.getProjectManagerSessionShortcuts(), service, listController);
    }

    private void fillShortcutRow(@NonNull ShortcutFlowLayout row,
                                 @NonNull List<SessionShortcut> rightToLeftShortcuts,
                                 @NonNull TermuxService service,
                                 @NonNull TermuxSessionsListViewController listController) {
        List<SessionShortcut> renderOrderShortcuts =
            SessionShortcutBarPlanner.renderOrderShortcuts(rightToLeftShortcuts);
        for (SessionShortcut shortcut : renderOrderShortcuts) {
            TermuxSession targetSession = service.getTermuxSessionForSessionName(shortcut.getTargetSessionName());
            row.addView(createShortcutButton(shortcut, targetSession, listController));
        }
    }

    @NonNull
    private List<String> liveSessionNames(@NonNull TermuxService service) {
        List<String> liveSessionNames = new ArrayList<>();
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (terminalSession != null && terminalSession.mSessionName != null) {
                liveSessionNames.add(terminalSession.mSessionName);
            }
        }
        return liveSessionNames;
    }

    @NonNull
    private View createShortcutButton(@NonNull SessionShortcut shortcut,
                                      @Nullable TermuxSession targetSession,
                                      @NonNull TermuxSessionsListViewController listController) {
        TextView shortcutButton = newShortcutButtonView(mActivity, shortcut.getLabel());
        shortcutButton.setOnClickListener(targetSession == null
            ? v -> openShortcutSessionFromDefinition(shortcut, listController)
            : v -> switchToShortcutSession(targetSession));
        return shortcutButton;
    }

    private void openShortcutSessionFromDefinition(
            @NonNull SessionShortcut shortcut,
            @NonNull TermuxSessionsListViewController listController) {
        listController.openDefinitionBackedSession(shortcut.getTargetSessionName());
        hideSoftKeyboard();
        hide();
    }

    @NonNull
    static TextView newShortcutButtonView(@NonNull android.content.Context context, @NonNull String label) {
        TextView shortcutButton = new TextView(context);
        shortcutButton.setText(label);
        shortcutButton.setTextColor(ContextCompat.getColor(context,
            com.termux.shared.R.color.schema_text_primary));
        shortcutButton.setSingleLine(true);
        shortcutButton.setContentDescription(label);
        int horizontalPaddingPixels = dpToPixels(context, 8);
        int verticalPaddingPixels = dpToPixels(context, 6);
        shortcutButton.setPadding(horizontalPaddingPixels, verticalPaddingPixels,
            horizontalPaddingPixels, verticalPaddingPixels);
        ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int horizontalMarginPixels = dpToPixels(context, 1);
        int rowGapPixels = dpToPixels(context, 3);
        layoutParams.leftMargin = horizontalMarginPixels;
        layoutParams.rightMargin = horizontalMarginPixels;
        layoutParams.topMargin = rowGapPixels;
        shortcutButton.setLayoutParams(layoutParams);
        shortcutButton.setBackgroundResource(shortcutBackgroundResource(context));
        return shortcutButton;
    }

    private void switchToShortcutSession(@NonNull TermuxSession targetSession) {
        unhideNavigatedShortcutSession(targetSession);
        expandNavigatedShortcutSessionProject(targetSession);
        TermuxTerminalSessionActivityClient sessionClient = mActivity.getTermuxTerminalSessionClient();
        if (sessionClient != null) {
            sessionClient.switchToSessionReconnectingIfDead(targetSession.getTerminalSession());
        }
        hideSoftKeyboard();
        hide();
    }

    private void unhideNavigatedShortcutSession(@NonNull TermuxSession targetSession) {
        TerminalSession terminalSession = targetSession.getTerminalSession();
        if (terminalSession == null) {
            return;
        }
        TermuxAppSharedPreferences preferences = mActivity.getPreferences();
        if (preferences == null) {
            return;
        }
        OpenedSessionUnhider.unhideOpenedSession(preferences, terminalSession.mSessionName);
    }

    private void expandNavigatedShortcutSessionProject(@NonNull TermuxSession targetSession) {
        TerminalSession terminalSession = targetSession.getTerminalSession();
        if (terminalSession == null) {
            return;
        }
        TermuxSessionsListViewController listController = mActivity.getTermuxSessionListViewController();
        if (listController == null) {
            return;
        }
        listController.expandCollapsedProjectForSession(terminalSession.mSessionName);
    }

    private static int shortcutBackgroundResource(@NonNull android.content.Context context) {
        int[] attribute = new int[]{android.R.attr.selectableItemBackground};
        android.content.res.TypedArray typedArray = context.obtainStyledAttributes(attribute);
        int backgroundResource = typedArray.getResourceId(0, 0);
        typedArray.recycle();
        return backgroundResource;
    }

    private static int dpToPixels(@NonNull android.content.Context context, int densityIndependentPixels) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(densityIndependentPixels * density);
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

    public void applyOrientationBounds() {
        applyHorizontalBounds();
    }

    private void applyHorizontalBounds() {
        boolean landscape = isLandscape();
        int containerWidthPixels = resolveContainerWidthPixels();
        int widthPixels = SessionListBottomSheetHorizontalBounds.resolveWidthPixels(landscape, containerWidthPixels);
        boolean alignEnd = SessionListBottomSheetHorizontalBounds.alignToEnd(landscape);
        applyHorizontalBoundsToView(mSheetView, widthPixels, alignEnd);
        applyHorizontalBoundsToView(mScrimView, widthPixels, alignEnd);
    }

    private void applyHorizontalBoundsToView(@NonNull View view, int widthPixels, boolean alignEnd) {
        ViewGroup.LayoutParams rawParams = view.getLayoutParams();
        if (!(rawParams instanceof RelativeLayout.LayoutParams)) {
            return;
        }
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) rawParams;
        params.width = widthPixels;
        if (alignEnd) {
            params.addRule(RelativeLayout.ALIGN_PARENT_END);
            params.removeRule(RelativeLayout.ALIGN_PARENT_START);
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_START);
            params.removeRule(RelativeLayout.ALIGN_PARENT_END);
        }
        view.setLayoutParams(params);
    }

    private int resolveContainerWidthPixels() {
        View parent = (View) mSheetView.getParent();
        if (parent != null && parent.getWidth() > 0) {
            return parent.getWidth();
        }
        return mActivity.getResources().getDisplayMetrics().widthPixels;
    }

    private boolean isLandscape() {
        return mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    private void bindSessionList(@NonNull TermuxSessionsListViewController listController) {
        if (mAdapterBound) {
            return;
        }
        bindSessionListAdapter(mSessionListView, listController);
        listController.setSessionClickHost(this::dismissAfterSessionSelected);
        bindSessionListTouchTracking();
        mAdapterBound = true;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void bindSessionListTouchTracking() {
        mSessionListView.setOnTouchListener((view, event) -> {
            int maskedAction = event.getActionMasked();
            if (maskedAction == MotionEvent.ACTION_DOWN) {
                onSessionListTouchStarted();
            } else if (isSessionListTouchInteractionEnd(maskedAction)) {
                onSessionListTouchEnded();
            }
            return false;
        });
    }

    private void dismissAfterSessionSelected() {
        mActivity.getDrawer().closeDrawers();
        hideSoftKeyboard();
        hide();
    }

    static void bindSessionListAdapter(@NonNull RecyclerView recyclerView,
                                       @NonNull RecyclerView.Adapter<?> adapter) {
        if (recyclerView.getLayoutManager() == null) {
            recyclerView.setLayoutManager(new SafeLinearLayoutManager(recyclerView.getContext()));
        }
        disableChangeAnimations(recyclerView);
        recyclerView.setAdapter(adapter);
    }

    static void disableChangeAnimations(@NonNull RecyclerView recyclerView) {
        RecyclerView.ItemAnimator itemAnimator = recyclerView.getItemAnimator();
        if (itemAnimator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
        }
    }

    static int nextSheetVisibility(int currentVisibility) {
        return currentVisibility == View.VISIBLE ? View.GONE : View.VISIBLE;
    }

    static int scrimVisibilityForSheet(int sheetVisibility) {
        return sheetVisibility == View.VISIBLE ? View.VISIBLE : View.GONE;
    }
}
