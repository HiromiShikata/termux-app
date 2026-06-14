package com.termux.app.terminal;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.browser.TermuxBrowserController;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.app.sessiondefinition.SessionDefinitionEntryMatcher;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.shared.theme.NightMode;
import com.termux.shared.theme.ThemeUtils;
import com.termux.terminal.TerminalSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TermuxSessionsListViewController extends BaseAdapter implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private static final int VIEW_TYPE_SESSION = 0;
    private static final int VIEW_TYPE_PROJECT_HEADER = 1;
    private static final int VIEW_TYPE_STORY_HEADER = 2;
    private static final int VIEW_TYPE_COUNT = 3;

    private static final int SESSION_ROW_FLAT_INDENT_DP = 6;
    private static final int SESSION_ROW_GROUPED_INDENT_DP = 24;
    private static final int SESSION_ROW_VERTICAL_PADDING_DP = 6;
    private static final int SESSION_ROW_BELL_ICON_PADDING_DP = 4;

    private static final long RELATIVE_TIME_REFRESH_INTERVAL_MS = 5000L;

    private static final float DEFINITION_TITLE_RELATIVE_SIZE = 0.7f;
    private static final int DEFINITION_TITLE_ALPHA = 0xA6;

    private static final String PROJECT_EXPANDED_INDICATOR = "▾";
    private static final String PROJECT_COLLAPSED_INDICATOR = "▸";

    final TermuxActivity mActivity;

    private final Handler mPeriodicRefreshHandler = new Handler(Looper.getMainLooper());

    private final Runnable mPeriodicRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            notifyDataSetChanged();
            mPeriodicRefreshHandler.postDelayed(this, RELATIVE_TIME_REFRESH_INTERVAL_MS);
        }
    };

    final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
    final StyleSpan italicSpan = new StyleSpan(Typeface.ITALIC);

    private final List<TermuxSession> mSessionList;

    private final SessionHierarchyBuilder mHierarchyBuilder = new SessionHierarchyBuilder();

    private final SessionDefinitionEntryMatcher mEntryMatcher = new SessionDefinitionEntryMatcher();

    private List<SessionDefinitionEntry> mEntries = Collections.emptyList();

    private List<SessionHierarchyRow> mRows = Collections.emptyList();

    private final Set<String> mCollapsedProjectKeys = new LinkedHashSet<>();

    public TermuxSessionsListViewController(TermuxActivity activity, List<TermuxSession> sessionList) {
        this.mActivity = activity;
        this.mSessionList = sessionList;
        rebuildRows();
    }

    public void setEntries(@NonNull List<SessionDefinitionEntry> entries) {
        this.mEntries = entries;
        notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetChanged() {
        rebuildRows();
        super.notifyDataSetChanged();
    }

    private List<SessionHierarchyRow> buildAllRows() {
        List<String> sessionNames = new ArrayList<>(mSessionList.size());
        for (TermuxSession session : mSessionList) {
            TerminalSession terminalSession = session.getTerminalSession();
            sessionNames.add(terminalSession == null ? null : terminalSession.mSessionName);
        }
        return mHierarchyBuilder.build(sessionNames, mEntries,
            mActivity.getString(R.string.session_list_na_group_header));
    }

    private void rebuildRows() {
        mRows = mHierarchyBuilder.filterCollapsedProjects(buildAllRows(), mCollapsedProjectKeys);
    }

    public int getFirstVisibleSessionIndexAfterRebuild() {
        return SessionHierarchyBuilder.firstSessionIndex(buildAllRows());
    }

    private void toggleProjectCollapsed(@Nullable String projectKey) {
        if (projectKey == null) {
            return;
        }
        if (!mCollapsedProjectKeys.remove(projectKey)) {
            mCollapsedProjectKeys.add(projectKey);
        }
        notifyDataSetChanged();
    }

    private boolean isGrouped() {
        return !mEntries.isEmpty();
    }

    @Override
    public int getCount() {
        return mRows.size();
    }

    public int getRowPositionForSessionIndex(int sessionIndex) {
        for (int position = 0; position < mRows.size(); position++) {
            SessionHierarchyRow row = mRows.get(position);
            if (!row.isHeader() && row.getSessionIndex() == sessionIndex) {
                return position;
            }
        }
        return -1;
    }

    @Override
    public Object getItem(int position) {
        return mRows.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        switch (mRows.get(position).getType()) {
            case PROJECT_HEADER:
                return VIEW_TYPE_PROJECT_HEADER;
            case STORY_HEADER:
                return VIEW_TYPE_STORY_HEADER;
            default:
                return VIEW_TYPE_SESSION;
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return mRows.get(position).getType() != SessionHierarchyRow.Type.STORY_HEADER;
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        SessionHierarchyRow row = mRows.get(position);
        switch (row.getType()) {
            case PROJECT_HEADER:
                View projectHeaderView = getHeaderView(row, convertView, parent,
                    R.layout.item_terminal_sessions_project_header, R.id.session_project_header_title);
                bindProjectCollapseIndicator(projectHeaderView, row);
                bindProjectOverviewBrowserIcon(projectHeaderView, row);
                return projectHeaderView;
            case STORY_HEADER:
                return getHeaderView(row, convertView, parent,
                    R.layout.item_terminal_sessions_story_header, R.id.session_story_header_title);
            default:
                return getSessionView(row, convertView, parent);
        }
    }

    private View getHeaderView(@NonNull SessionHierarchyRow row, View convertView, @NonNull ViewGroup parent,
                              int layoutResId, int titleViewId) {
        View headerRowView = convertView;
        if (headerRowView == null) {
            LayoutInflater inflater = mActivity.getLayoutInflater();
            headerRowView = inflater.inflate(layoutResId, parent, false);
        }
        TextView headerTitleView = headerRowView.findViewById(titleViewId);
        headerTitleView.setText(row.getLabel());
        boolean shouldEnableDarkTheme = ThemeUtils.shouldEnableDarkTheme(mActivity, NightMode.getAppNightMode().getName());
        headerTitleView.setTextColor(shouldEnableDarkTheme ? Color.WHITE : Color.BLACK);
        return headerRowView;
    }

    private void bindProjectCollapseIndicator(@NonNull View projectHeaderView, @NonNull SessionHierarchyRow row) {
        TextView collapseIndicatorView = projectHeaderView.findViewById(R.id.session_project_header_collapse_indicator);
        boolean collapsed = mCollapsedProjectKeys.contains(row.getLabel());
        collapseIndicatorView.setText(collapsed ? PROJECT_COLLAPSED_INDICATOR : PROJECT_EXPANDED_INDICATOR);
        boolean shouldEnableDarkTheme = ThemeUtils.shouldEnableDarkTheme(mActivity, NightMode.getAppNightMode().getName());
        collapseIndicatorView.setTextColor(shouldEnableDarkTheme ? Color.WHITE : Color.BLACK);
    }

    private void bindProjectOverviewBrowserIcon(@NonNull View projectHeaderView, @NonNull SessionHierarchyRow row) {
        View overviewBrowserIconView = projectHeaderView.findViewById(R.id.session_project_header_overview_browser_icon);
        String overviewUrl = row.getOverviewUrl();
        Runnable openAction = (overviewUrl == null || overviewUrl.isEmpty())
            ? null
            : () -> openProjectOverview(overviewUrl);
        applyProjectOverviewBrowserIconVisibility(overviewBrowserIconView, openAction);
    }

    static void applyProjectOverviewBrowserIconVisibility(@NonNull View overviewBrowserIconView,
                                                          @Nullable Runnable openAction) {
        if (openAction == null) {
            overviewBrowserIconView.setVisibility(View.GONE);
            overviewBrowserIconView.setOnClickListener(null);
            return;
        }
        overviewBrowserIconView.setVisibility(View.VISIBLE);
        overviewBrowserIconView.setOnClickListener(v -> openAction.run());
    }

    private void openProjectOverview(@NonNull String overviewUrl) {
        TermuxBrowserController browserController = mActivity.getTermuxBrowserController();
        if (browserController == null) {
            return;
        }
        browserController.openUrlInNewTab(overviewUrl);
    }

    @SuppressLint("SetTextI18n")
    private View getSessionView(@NonNull SessionHierarchyRow row, View convertView, @NonNull ViewGroup parent) {
        View sessionRowView = convertView;
        if (sessionRowView == null) {
            LayoutInflater inflater = mActivity.getLayoutInflater();
            sessionRowView = inflater.inflate(R.layout.item_terminal_sessions_list, parent, false);
        }

        TextView sessionTitleView = sessionRowView.findViewById(R.id.session_title);

        int verticalPadding = dpToPx(SESSION_ROW_VERTICAL_PADDING_DP);
        int startPadding = dpToPx(isGrouped() ? SESSION_ROW_GROUPED_INDENT_DP : SESSION_ROW_FLAT_INDENT_DP);
        sessionTitleView.setPadding(startPadding, verticalPadding, verticalPadding, verticalPadding);

        int sessionIndex = row.getSessionIndex();
        TerminalSession sessionAtRow = mSessionList.get(sessionIndex).getTerminalSession();
        if (sessionAtRow == null) {
            sessionTitleView.setText("null session");
            return sessionRowView;
        }

        boolean shouldEnableDarkTheme = ThemeUtils.shouldEnableDarkTheme(mActivity, NightMode.getAppNightMode().getName());

        boolean isCurrentSession = sessionAtRow == mActivity.getCurrentSession();
        boolean sessionRunning = sessionAtRow.isRunning();
        SessionRowActiveIndicator activeIndicator = computeActiveIndicator(isCurrentSession, sessionRunning);

        int rowBackgroundResId = shouldEnableDarkTheme
            ? R.drawable.session_background_black_selected
            : R.drawable.session_background_selected;
        sessionRowView.setBackground(ContextCompat.getDrawable(mActivity, rowBackgroundResId));

        int activeIndicatorColor = ContextCompat.getColor(mActivity,
            shouldEnableDarkTheme ? R.color.session_active_indicator_dark : R.color.session_active_indicator_light);
        View activeIndicatorBar = sessionRowView.findViewById(R.id.session_active_indicator_bar);
        if (activeIndicator.showAccentBar) {
            activeIndicatorBar.setBackgroundColor(activeIndicatorColor);
            activeIndicatorBar.setVisibility(View.VISIBLE);
        } else {
            activeIndicatorBar.setVisibility(View.GONE);
        }

        String name = sessionAtRow.mSessionName;
        String sessionTitle = sessionAtRow.getTitle();
        String definitionTitle = mEntryMatcher.findTitleForSessionName(mEntries, name);

        String sessionNamePart = (TextUtils.isEmpty(name) ? "" : name);

        String bellNotificationLabelPart = buildBellNotificationLabel(sessionAtRow);

        StringBuilder fullSessionTitleBuilder = new StringBuilder(sessionNamePart);
        int bellNotificationLabelStart = -1;
        int bellNotificationLabelEnd = -1;
        if (!bellNotificationLabelPart.isEmpty()) {
            bellNotificationLabelStart = fullSessionTitleBuilder.length();
            fullSessionTitleBuilder.append(bellNotificationLabelPart);
            bellNotificationLabelEnd = fullSessionTitleBuilder.length();
        }
        boolean hasSecondaryLine = !sessionNamePart.isEmpty();
        int definitionTitleStart = -1;
        int definitionTitleEnd = -1;
        if (!TextUtils.isEmpty(definitionTitle)) {
            if (hasSecondaryLine) {
                fullSessionTitleBuilder.append("\n");
            }
            definitionTitleStart = fullSessionTitleBuilder.length();
            fullSessionTitleBuilder.append(definitionTitle);
            definitionTitleEnd = fullSessionTitleBuilder.length();
            hasSecondaryLine = true;
        }
        int sessionTitleStart = -1;
        int sessionTitleEnd = -1;
        if (!TextUtils.isEmpty(sessionTitle)) {
            if (hasSecondaryLine) {
                fullSessionTitleBuilder.append("\n");
            }
            sessionTitleStart = fullSessionTitleBuilder.length();
            fullSessionTitleBuilder.append(sessionTitle);
            sessionTitleEnd = fullSessionTitleBuilder.length();
        }

        String fullSessionTitle = fullSessionTitleBuilder.toString();
        SpannableString fullSessionTitleStyled = new SpannableString(fullSessionTitle);
        fullSessionTitleStyled.setSpan(boldSpan, 0, sessionNamePart.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (activeIndicator.useAccentNameColor) {
            fullSessionTitleStyled.setSpan(new ForegroundColorSpan(activeIndicatorColor), 0, sessionNamePart.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (bellNotificationLabelStart >= 0) {
            fullSessionTitleStyled.setSpan(italicSpan, bellNotificationLabelStart, bellNotificationLabelEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (definitionTitleStart >= 0) {
            int definitionTitleColor = (DEFINITION_TITLE_ALPHA << 24)
                | ((shouldEnableDarkTheme ? Color.WHITE : Color.BLACK) & 0x00FFFFFF);
            fullSessionTitleStyled.setSpan(new RelativeSizeSpan(DEFINITION_TITLE_RELATIVE_SIZE), definitionTitleStart, definitionTitleEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            fullSessionTitleStyled.setSpan(new ForegroundColorSpan(definitionTitleColor), definitionTitleStart, definitionTitleEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (sessionTitleStart >= 0) {
            fullSessionTitleStyled.setSpan(italicSpan, sessionTitleStart, sessionTitleEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        sessionTitleView.setText(fullSessionTitleStyled);

        applyBellNotificationIcon(sessionTitleView, sessionAtRow);

        if (sessionRunning) {
            sessionTitleView.setPaintFlags(sessionTitleView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            sessionTitleView.setPaintFlags(sessionTitleView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        int defaultColor = shouldEnableDarkTheme ? Color.WHITE : Color.BLACK;
        int color = sessionRunning || sessionAtRow.getExitStatus() == 0 ? defaultColor : Color.RED;
        sessionTitleView.setTextColor(color);
        return sessionRowView;
    }

    private String buildBellNotificationLabel(@NonNull TerminalSession session) {
        Long bellArrivalTimeMillis = getBellArrivalTimeMillis(session);
        if (bellArrivalTimeMillis == null) {
            return "";
        }
        return "  " + SessionBellNotificationStore.formatRelativeTime(System.currentTimeMillis() - bellArrivalTimeMillis);
    }

    private void applyBellNotificationIcon(@NonNull TextView sessionTitleView, @NonNull TerminalSession session) {
        if (getBellArrivalTimeMillis(session) != null) {
            sessionTitleView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_session_bell_notification, 0, 0, 0);
            sessionTitleView.setCompoundDrawablePadding(dpToPx(SESSION_ROW_BELL_ICON_PADDING_DP));
        } else {
            sessionTitleView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    @Nullable
    private Long getBellArrivalTimeMillis(@NonNull TerminalSession session) {
        return bellArrivalTimeMillis(mActivity.getSessionBellNotificationStore(), session.mHandle);
    }

    @Nullable
    static Long bellArrivalTimeMillis(@NonNull SessionBellNotificationStore store, @Nullable String sessionHandle) {
        if (sessionHandle == null) {
            return null;
        }
        return store.getBellArrivalTimeMillis(sessionHandle);
    }

    public void startPeriodicRefresh() {
        mPeriodicRefreshHandler.removeCallbacks(mPeriodicRefreshRunnable);
        mPeriodicRefreshHandler.postDelayed(mPeriodicRefreshRunnable, RELATIVE_TIME_REFRESH_INTERVAL_MS);
    }

    public void stopPeriodicRefresh() {
        mPeriodicRefreshHandler.removeCallbacks(mPeriodicRefreshRunnable);
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
            mActivity.getResources().getDisplayMetrics()));
    }

    static SessionRowActiveIndicator computeActiveIndicator(boolean isCurrentSession, boolean sessionRunning) {
        return new SessionRowActiveIndicator(isCurrentSession, isCurrentSession && sessionRunning);
    }

    static final class SessionRowActiveIndicator {
        final boolean showAccentBar;
        final boolean useAccentNameColor;

        SessionRowActiveIndicator(boolean showAccentBar, boolean useAccentNameColor) {
            this.showAccentBar = showAccentBar;
            this.useAccentNameColor = useAccentNameColor;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SessionHierarchyRow row = mRows.get(position);
        if (row.getType() == SessionHierarchyRow.Type.PROJECT_HEADER) {
            toggleProjectCollapsed(row.getLabel());
            return;
        }
        if (row.isHeader()) {
            return;
        }
        TermuxSession clickedSession = mSessionList.get(row.getSessionIndex());
        mActivity.getTermuxTerminalSessionClient().setCurrentSession(clickedSession.getTerminalSession());
        mActivity.getDrawer().closeDrawers();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        SessionHierarchyRow row = mRows.get(position);
        if (row.isHeader()) {
            return false;
        }
        final TermuxSession selectedSession = mSessionList.get(row.getSessionIndex());
        showSessionActionChooser(selectedSession.getTerminalSession());
        return true;
    }

    private void showSessionActionChooser(final TerminalSession session) {
        if (session == null) {
            return;
        }

        CharSequence[] actions = {
            mActivity.getString(R.string.action_copy_session_name),
            mActivity.getString(R.string.action_rename_session),
            mActivity.getString(R.string.action_delete_session)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        if (!TextUtils.isEmpty(session.mSessionName)) {
            builder.setTitle(session.mSessionName);
        }
        builder.setItems(actions, (dialog, which) -> {
            if (which == 0) {
                mActivity.getTermuxTerminalSessionClient().copySessionNameToClipboard(session);
            } else if (which == 1) {
                mActivity.getTermuxTerminalSessionClient().renameSession(session);
            } else if (which == 2) {
                mActivity.getTermuxTerminalSessionClient().deleteSession(session);
            }
        });
        builder.show();
    }

}
