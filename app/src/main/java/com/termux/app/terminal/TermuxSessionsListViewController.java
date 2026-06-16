package com.termux.app.terminal;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.browser.ProjectBrowserOverlayController;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.app.sessiondefinition.SessionDefinitionEntryMatcher;
import com.termux.shared.interact.DialogUtils;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import java.util.ArrayList;
import java.util.Collection;
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

    private static final float DEFINITION_TITLE_RELATIVE_SIZE = 0.7f;
    private static final int DEFINITION_TITLE_ALPHA = 0xA6;

    static final float SESSION_NAME_RELATIVE_SIZE = 0.85f;

    private static final String PROJECT_EXPANDED_INDICATOR = "▾";
    private static final String PROJECT_COLLAPSED_INDICATOR = "▸";

    final TermuxActivity mActivity;

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
        restoreCollapsedProjectKeys();
        rebuildRows();
    }

    private void restoreCollapsedProjectKeys() {
        TermuxAppSharedPreferences preferences = mActivity.getPreferences();
        if (preferences == null) {
            return;
        }
        mCollapsedProjectKeys.addAll(preferences.getCollapsedProjectKeys());
    }

    private void persistCollapsedProjectKeys() {
        TermuxAppSharedPreferences preferences = mActivity.getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.setCollapsedProjectKeys(mCollapsedProjectKeys);
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
            mActivity.getString(R.string.session_list_na_group_header),
            alwaysNaSessionNames());
    }

    @NonNull
    private Set<String> alwaysNaSessionNames() {
        TermuxAppSharedPreferences preferences = mActivity.getPreferences();
        if (preferences == null) {
            return Collections.emptySet();
        }
        return preferences.getAlwaysNaSessionNames();
    }

    private void rebuildRows() {
        mRows = mHierarchyBuilder.filterCollapsedProjects(buildAllRows(), mCollapsedProjectKeys);
    }

    public int getFirstVisibleSessionIndexAfterRebuild() {
        return SessionHierarchyBuilder.firstSessionIndex(buildAllRows());
    }

    public int getNextVisibleSessionIndex(int currentSessionIndex, boolean forward) {
        return VisibleSessionNavigator.nextSessionIndex(
            getOrderedSessionIndexes(), getNavigableSessionIndexes(), currentSessionIndex, forward);
    }

    @NonNull
    public List<Integer> getOrderedSessionIndexes() {
        return SessionHierarchyBuilder.visibleSessionIndexes(buildAllRows());
    }

    public int getRowPositionForSessionIndex(int sessionIndex) {
        return SessionHierarchyBuilder.rowPositionForSessionIndex(mRows, sessionIndex);
    }

    @NonNull
    public List<SessionHierarchyRow> getVisibleRows() {
        return Collections.unmodifiableList(mRows);
    }

    @NonNull
    public List<Integer> getVisibleSessionIndexes() {
        return SessionHierarchyBuilder.visibleSessionIndexes(mRows);
    }

    @NonNull
    public List<Integer> getNavigableSessionIndexes() {
        return navigableSessionIndexes(getVisibleSessionIndexes(), sessionNamesByIndex(), disabledSessionNames());
    }

    @NonNull
    static List<Integer> navigableSessionIndexes(@NonNull List<Integer> visibleSessionIndexes,
                                                 @NonNull List<String> sessionNamesByIndex,
                                                 @NonNull Set<String> disabledSessionNames) {
        if (disabledSessionNames.isEmpty()) {
            return new ArrayList<>(visibleSessionIndexes);
        }
        List<Integer> navigableSessionIndexes = new ArrayList<>(visibleSessionIndexes.size());
        for (int sessionIndex : visibleSessionIndexes) {
            String sessionName = sessionIndex >= 0 && sessionIndex < sessionNamesByIndex.size()
                ? sessionNamesByIndex.get(sessionIndex) : null;
            if (sessionName != null && disabledSessionNames.contains(sessionName)) {
                continue;
            }
            navigableSessionIndexes.add(sessionIndex);
        }
        return navigableSessionIndexes;
    }

    @NonNull
    private List<String> sessionNamesByIndex() {
        List<String> sessionNames = new ArrayList<>(mSessionList.size());
        for (TermuxSession session : mSessionList) {
            TerminalSession terminalSession = session.getTerminalSession();
            sessionNames.add(terminalSession == null ? null : terminalSession.mSessionName);
        }
        return sessionNames;
    }

    @NonNull
    private Set<String> disabledSessionNames() {
        TermuxAppSharedPreferences preferences = mActivity.getPreferences();
        if (preferences == null) {
            return Collections.emptySet();
        }
        return preferences.getDisabledSessionNames();
    }

    private boolean isSessionDisabled(@Nullable String sessionName) {
        return sessionName != null && disabledSessionNames().contains(sessionName);
    }

    private void toggleSessionDisabled(@Nullable String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) {
            return;
        }
        TermuxAppSharedPreferences preferences = mActivity.getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.toggleSessionDisabled(sessionName);
        notifyDataSetChanged();
    }

    @NonNull
    public List<String> getSessionRawNames() {
        List<String> rawNames = new ArrayList<>(mSessionList.size());
        for (TermuxSession session : mSessionList) {
            TerminalSession terminalSession = session.getTerminalSession();
            String name = terminalSession == null ? null : terminalSession.mSessionName;
            rawNames.add(name == null ? "" : name);
        }
        return rawNames;
    }

    @NonNull
    public Set<Integer> getMarkedSessionIndexes() {
        Set<Integer> markedSessionIndexes = new LinkedHashSet<>();
        SessionBellNotificationStore store = mActivity.getSessionBellNotificationStore();
        if (store == null) {
            return markedSessionIndexes;
        }
        for (int sessionIndex : getVisibleSessionIndexes()) {
            if (!isSessionIndexInRange(sessionIndex, mSessionList.size())) {
                continue;
            }
            TerminalSession terminalSession = mSessionList.get(sessionIndex).getTerminalSession();
            String sessionHandle = terminalSession == null ? null : terminalSession.mHandle;
            if (bellArrivalTimeMillis(store, sessionHandle) != null) {
                markedSessionIndexes.add(sessionIndex);
            }
        }
        return markedSessionIndexes;
    }

    @NonNull
    public List<String> getSessionTitles() {
        List<String> titles = new ArrayList<>(mSessionList.size());
        for (TermuxSession session : mSessionList) {
            titles.add(sessionDefinitionTitle(session));
        }
        return titles;
    }

    @NonNull
    private String sessionDefinitionTitle(@NonNull TermuxSession session) {
        TerminalSession terminalSession = session.getTerminalSession();
        if (terminalSession == null) {
            return "";
        }
        String name = terminalSession.mSessionName;
        if (name == null || name.isEmpty()) {
            return "";
        }
        String definitionTitle = mEntryMatcher.findTitleForSessionName(mEntries, name);
        return definitionTitle == null ? "" : definitionTitle;
    }

    static boolean isSessionIndexInRange(int sessionIndex, int sessionCount) {
        return sessionIndex >= 0 && sessionIndex < sessionCount;
    }

    @Nullable
    private TermuxSession sessionAtRowOrNull(@NonNull SessionHierarchyRow row) {
        int sessionIndex = row.getSessionIndex();
        if (!isSessionIndexInRange(sessionIndex, mSessionList.size())) {
            return null;
        }
        return mSessionList.get(sessionIndex);
    }

    public void applyExpandedProjectsAllowlist(@NonNull Collection<String> expandedProjectTokens) {
        if (expandedProjectTokens.isEmpty()) {
            return;
        }
        List<String> projectLabels = new ArrayList<>();
        for (SessionHierarchyRow row : buildAllRows()) {
            if (row.getType() == SessionHierarchyRow.Type.PROJECT_HEADER && row.getLabel() != null) {
                projectLabels.add(row.getLabel());
            }
        }
        Set<String> labelsToCollapse = collapsedProjectLabels(projectLabels, expandedProjectTokens);
        for (String projectLabel : projectLabels) {
            if (labelsToCollapse.contains(projectLabel)) {
                mCollapsedProjectKeys.add(projectLabel);
            } else {
                mCollapsedProjectKeys.remove(projectLabel);
            }
        }
        persistCollapsedProjectKeys();
        notifyDataSetChanged();
    }

    public void applyProjectActionTokens(@NonNull List<ProjectActionToken> projectActionTokens) {
        if (projectActionTokens.isEmpty()) {
            return;
        }
        List<SessionHierarchyRow> allRows = buildAllRows();
        for (ProjectActionToken projectActionToken : projectActionTokens) {
            String url = SessionHierarchyBuilder.projectActionUrl(allRows,
                projectActionToken.getNormalizedProjectName(), projectActionToken.getAction());
            if (url != null && !url.isEmpty()) {
                openProjectUrlInNewTab(url);
            }
            int topSessionIndex = SessionHierarchyBuilder.firstSessionIndexForProject(allRows,
                projectActionToken.getNormalizedProjectName());
            selectSessionAtIndex(topSessionIndex);
        }
    }

    private void selectSessionAtIndex(int sessionIndex) {
        if (!isSessionIndexInRange(sessionIndex, mSessionList.size())) {
            return;
        }
        TerminalSession terminalSession = mSessionList.get(sessionIndex).getTerminalSession();
        if (terminalSession == null) {
            return;
        }
        mActivity.getTermuxTerminalSessionClient().setCurrentSession(terminalSession);
    }

    @NonNull
    static Set<String> collapsedProjectLabels(@NonNull List<String> projectLabels,
                                              @NonNull Collection<String> expandedProjectTokens) {
        Set<String> normalizedAllowedTokens = new LinkedHashSet<>();
        for (String expandedProjectToken : expandedProjectTokens) {
            normalizedAllowedTokens.add(ExpandedProjectsAllowlistParser.normalize(expandedProjectToken));
        }
        Set<String> labelsToCollapse = new LinkedHashSet<>();
        for (String projectLabel : projectLabels) {
            if (!normalizedAllowedTokens.contains(ExpandedProjectsAllowlistParser.normalize(projectLabel))) {
                labelsToCollapse.add(projectLabel);
            }
        }
        return labelsToCollapse;
    }

    private void toggleProjectCollapsed(@Nullable String projectKey) {
        if (projectKey == null) {
            return;
        }
        if (!mCollapsedProjectKeys.remove(projectKey)) {
            mCollapsedProjectKeys.add(projectKey);
        }
        persistCollapsedProjectKeys();
        notifyDataSetChanged();
    }

    private boolean isGrouped() {
        return !mEntries.isEmpty();
    }

    @Override
    public int getCount() {
        return mRows.size();
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
                bindProjectTdpmConsoleIcon(projectHeaderView, row);
                bindProjectNewIssueIcon(projectHeaderView, row);
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
        headerTitleView.setTextColor(surfacePrimaryTextColor());
        return headerRowView;
    }

    private void bindProjectCollapseIndicator(@NonNull View projectHeaderView, @NonNull SessionHierarchyRow row) {
        TextView collapseIndicatorView = projectHeaderView.findViewById(R.id.session_project_header_collapse_indicator);
        boolean collapsed = mCollapsedProjectKeys.contains(row.getLabel());
        collapseIndicatorView.setText(collapsed ? PROJECT_COLLAPSED_INDICATOR : PROJECT_EXPANDED_INDICATOR);
        collapseIndicatorView.setTextColor(surfacePrimaryTextColor());
    }

    private void bindProjectOverviewBrowserIcon(@NonNull View projectHeaderView, @NonNull SessionHierarchyRow row) {
        View overviewBrowserIconView = projectHeaderView.findViewById(R.id.session_project_header_overview_browser_icon);
        String overviewUrl = row.getOverviewUrl();
        Runnable openAction = (overviewUrl == null || overviewUrl.isEmpty())
            ? null
            : () -> openProjectUrlInNewTab(overviewUrl);
        applyProjectHeaderIconVisibility(overviewBrowserIconView, openAction);
    }

    private void bindProjectTdpmConsoleIcon(@NonNull View projectHeaderView, @NonNull SessionHierarchyRow row) {
        View tdpmConsoleIconView = projectHeaderView.findViewById(R.id.session_project_header_tdpm_console_icon);
        String tdpmConsoleUrl = row.getTdpmConsoleUrl();
        Runnable openAction = (tdpmConsoleUrl == null || tdpmConsoleUrl.isEmpty())
            ? null
            : () -> openProjectUrlInNewTab(tdpmConsoleUrl);
        applyProjectHeaderIconVisibility(tdpmConsoleIconView, openAction);
    }

    private void bindProjectNewIssueIcon(@NonNull View projectHeaderView, @NonNull SessionHierarchyRow row) {
        View newIssueIconView = projectHeaderView.findViewById(R.id.session_project_header_new_issue_icon);
        String newIssueUrl = row.getNewIssueUrl();
        Runnable openAction = (newIssueUrl == null || newIssueUrl.isEmpty())
            ? null
            : () -> openProjectUrlInNewTab(newIssueUrl);
        applyProjectHeaderIconVisibility(newIssueIconView, openAction);
    }

    static void applyProjectHeaderIconVisibility(@NonNull View projectHeaderIconView,
                                                 @Nullable Runnable openAction) {
        if (openAction == null) {
            projectHeaderIconView.setVisibility(View.GONE);
            projectHeaderIconView.setOnClickListener(null);
            return;
        }
        projectHeaderIconView.setVisibility(View.VISIBLE);
        projectHeaderIconView.setOnClickListener(v -> openAction.run());
    }

    static void applyActiveIndicatorBarVisibility(@NonNull View activeIndicatorBar, boolean showAccentBar,
                                                  int activeIndicatorColor) {
        if (showAccentBar) {
            activeIndicatorBar.setBackgroundColor(activeIndicatorColor);
            activeIndicatorBar.setVisibility(View.VISIBLE);
        } else {
            activeIndicatorBar.setBackgroundColor(Color.TRANSPARENT);
            activeIndicatorBar.setVisibility(View.INVISIBLE);
        }
    }

    private void openProjectUrlInNewTab(@NonNull String url) {
        ProjectBrowserOverlayController projectBrowserController = mActivity.getProjectBrowserOverlayController();
        if (projectBrowserController == null) {
            return;
        }
        projectBrowserController.route(url);
    }

    static void applySessionNameStyling(@NonNull SpannableString styled, int sessionNameLength, @NonNull StyleSpan boldSpan) {
        if (sessionNameLength <= 0) {
            return;
        }
        styled.setSpan(boldSpan, 0, sessionNameLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        styled.setSpan(new RelativeSizeSpan(SESSION_NAME_RELATIVE_SIZE), 0, sessionNameLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
        if (!isSessionIndexInRange(sessionIndex, mSessionList.size())) {
            sessionTitleView.setText("null session");
            return sessionRowView;
        }
        TerminalSession sessionAtRow = mSessionList.get(sessionIndex).getTerminalSession();
        if (sessionAtRow == null) {
            sessionTitleView.setText("null session");
            return sessionRowView;
        }

        boolean isCurrentSession = sessionAtRow == mActivity.getCurrentSession();
        boolean sessionRunning = sessionAtRow.isRunning();
        SessionRowActiveIndicator activeIndicator = computeActiveIndicator(isCurrentSession, sessionRunning);

        int rowBackgroundResId = sessionRowBackgroundRes(isCurrentSession);
        sessionRowView.setBackground(ContextCompat.getDrawable(mActivity, rowBackgroundResId));

        int activeIndicatorColor = ContextCompat.getColor(mActivity, R.color.session_active_indicator);
        View activeIndicatorBar = sessionRowView.findViewById(R.id.session_active_indicator_bar);
        applyActiveIndicatorBarVisibility(activeIndicatorBar, activeIndicator.showAccentBar, activeIndicatorColor);

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
        applySessionNameStyling(fullSessionTitleStyled, sessionNamePart.length(), boldSpan);
        if (activeIndicator.useAccentNameColor) {
            fullSessionTitleStyled.setSpan(new ForegroundColorSpan(activeIndicatorColor), 0, sessionNamePart.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (bellNotificationLabelStart >= 0) {
            fullSessionTitleStyled.setSpan(italicSpan, bellNotificationLabelStart, bellNotificationLabelEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (definitionTitleStart >= 0) {
            int definitionTitleColor = (DEFINITION_TITLE_ALPHA << 24)
                | (surfacePrimaryTextColor() & 0x00FFFFFF);
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
        int defaultColor = surfacePrimaryTextColor();
        int color = sessionRunning || sessionAtRow.getExitStatus() == 0 ? defaultColor : Color.RED;
        sessionTitleView.setTextColor(color);

        bindSessionDisableToggle(sessionRowView, sessionAtRow.mSessionName);
        return sessionRowView;
    }

    private void bindSessionDisableToggle(@NonNull View sessionRowView, @Nullable String sessionName) {
        ImageView disableToggleView = sessionRowView.findViewById(R.id.session_disable_toggle);
        boolean disabled = isSessionDisabled(sessionName);
        disableToggleView.setImageResource(sessionDisableToggleIconRes(disabled));
        disableToggleView.setActivated(disabled);
        disableToggleView.setOnClickListener(v -> toggleSessionDisabled(sessionName));
    }

    static int sessionDisableToggleIconRes(boolean disabled) {
        return disabled
            ? R.drawable.ic_session_navigation_disabled
            : R.drawable.ic_session_navigation_enabled;
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

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
            mActivity.getResources().getDisplayMetrics()));
    }

    private int surfacePrimaryTextColor() {
        return ContextCompat.getColor(mActivity, com.termux.shared.R.color.schema_text_primary);
    }

    static SessionRowActiveIndicator computeActiveIndicator(boolean isCurrentSession, boolean sessionRunning) {
        return new SessionRowActiveIndicator(isCurrentSession, isCurrentSession && sessionRunning);
    }

    static int sessionRowBackgroundRes(boolean isCurrentSession) {
        return isCurrentSession ? R.drawable.current_session : R.drawable.session_ripple;
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
        TermuxSession clickedSession = sessionAtRowOrNull(row);
        if (clickedSession == null) {
            return;
        }
        mActivity.getTermuxTerminalSessionClient().setCurrentSession(clickedSession.getTerminalSession());
        mActivity.getDrawer().closeDrawers();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        SessionHierarchyRow row = mRows.get(position);
        if (row.isHeader()) {
            return false;
        }
        final TermuxSession selectedSession = sessionAtRowOrNull(row);
        if (selectedSession == null) {
            return false;
        }
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
        DialogUtils.showDismissibleOnTouchOutside(builder);
    }

}
