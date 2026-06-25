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
import android.text.style.TypefaceSpan;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    private static final float BELL_NOTIFICATION_LABEL_RELATIVE_SIZE = 0.75f;
    private static final float TIMESTAMP_ROW_RELATIVE_SIZE = 0.65f;
    private static final float EXPLICIT_CALL_REASON_RELATIVE_SIZE = 0.5f;
    private static final int TIMESTAMP_VALUE_FIELD_WIDTH = 8;

    private static final String PROJECT_EXPANDED_INDICATOR = "▾";
    private static final String PROJECT_COLLAPSED_INDICATOR = "▸";

    private static final String EXPLICIT_CALL_REASON_PREFIX = "⚠ ";

    final TermuxActivity mActivity;

    final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
    final StyleSpan italicSpan = new StyleSpan(Typeface.ITALIC);

    private final List<TermuxSession> mSessionList;

    private final SessionHierarchyBuilder mHierarchyBuilder = new SessionHierarchyBuilder();

    private final SessionDefinitionEntryMatcher mEntryMatcher = new SessionDefinitionEntryMatcher();

    private List<SessionDefinitionEntry> mEntries = Collections.emptyList();

    private List<SessionHierarchyRow> mRows = Collections.emptyList();

    private Map<Integer, SessionRow> mSessionRowsByIndex = Collections.emptyMap();

    private int mTotalSessionCount;

    private Map<String, Integer> mSessionCountByProjectLabel = Collections.emptyMap();

    private final Set<String> mCollapsedProjectKeys = new LinkedHashSet<>();

    @Nullable
    private SessionClickHost mSessionClickHost;

    public interface SessionClickHost {
        void onSessionSelected();
    }

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

    public void setSessionClickHost(@Nullable SessionClickHost sessionClickHost) {
        this.mSessionClickHost = sessionClickHost;
    }

    public void setEntries(@NonNull List<SessionDefinitionEntry> entries) {
        this.mEntries = new ArrayList<>(entries);
        notifyDataSetChanged();
    }

    @NonNull
    public List<SessionDefinitionEntry> getEntries() {
        return Collections.unmodifiableList(mEntries);
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
        List<SessionHierarchyRow> allRows = buildAllRows();
        mRows = mHierarchyBuilder.filterCollapsedProjects(allRows, mCollapsedProjectKeys);
        mSessionRowsByIndex = getSessionRows();
        mTotalSessionCount = SessionHierarchyBuilder.totalSessionCount(allRows);
        mSessionCountByProjectLabel = SessionHierarchyBuilder.sessionCountByProjectLabel(allRows);
    }

    public int getTotalSessionCount() {
        return mTotalSessionCount;
    }

    public int getFirstVisibleSessionIndexAfterRebuild() {
        return SessionHierarchyBuilder.firstSessionIndex(buildAllRows());
    }

    public int getNextVisibleSessionIndex(int currentSessionIndex, boolean forward) {
        return VisibleSessionNavigator.nextSessionIndex(
            getOrderedSessionIndexes(), getNavigationCandidateSessionIndexes(), currentSessionIndex, forward);
    }

    @NonNull
    public List<Integer> getNavigationCandidateSessionIndexes() {
        return NotifiedSessionNavigationCandidates.restrictToActiveTier(
            getNavigableSessionIndexes(), getSessionTiersByIndex());
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

    private void hideSession(@Nullable String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) {
            return;
        }
        TermuxAppSharedPreferences preferences = mActivity.getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.setSessionDisabled(sessionName, true);
        notifyDataSetChanged();
    }

    @NonNull
    public Set<Integer> getDisabledSessionIndexes() {
        Set<Integer> disabledSessionIndexes = new LinkedHashSet<>();
        Set<String> disabledSessionNames = disabledSessionNames();
        if (disabledSessionNames.isEmpty()) {
            return disabledSessionIndexes;
        }
        List<String> sessionNames = sessionNamesByIndex();
        for (int sessionIndex : getVisibleSessionIndexes()) {
            String sessionName = sessionIndex >= 0 && sessionIndex < sessionNames.size()
                ? sessionNames.get(sessionIndex) : null;
            if (sessionName != null && disabledSessionNames.contains(sessionName)) {
                disabledSessionIndexes.add(sessionIndex);
            }
        }
        return disabledSessionIndexes;
    }

    @NonNull
    public Set<Integer> getMarkedSessionIndexes() {
        return new LinkedHashSet<>(sessionActivityIndicatorsByIndex().keySet());
    }

    @NonNull
    public Map<Integer, SessionNewActivityTier> getSessionTiersByIndex() {
        Map<Integer, SessionNewActivityTier> tiersByIndex = new LinkedHashMap<>();
        for (Map.Entry<Integer, SessionNewActivityIndicator> entry
                : sessionActivityIndicatorsByIndex().entrySet()) {
            tiersByIndex.put(entry.getKey(), entry.getValue().getTier());
        }
        return tiersByIndex;
    }

    @NonNull
    public SessionRow getCurrentSessionRow() {
        int currentSessionIndex = indexOfSession(mActivity.getCurrentSession());
        return SessionRow.rowOrEmpty(getSessionRows(), currentSessionIndex);
    }

    @NonNull
    public Map<Integer, SessionRow> getSessionRows() {
        List<String> namesByIndex = new ArrayList<>(mSessionList.size());
        List<String> titlesByIndex = new ArrayList<>(mSessionList.size());
        List<String> projectsByIndex = new ArrayList<>(mSessionList.size());
        List<String> storiesByIndex = new ArrayList<>(mSessionList.size());
        for (TermuxSession session : mSessionList) {
            TerminalSession terminalSession = session.getTerminalSession();
            String name = terminalSession == null ? null : terminalSession.mSessionName;
            SessionDefinitionEntry entry = (name == null || name.isEmpty())
                ? null : mEntryMatcher.findEntryForSessionName(mEntries, name);
            namesByIndex.add(name);
            titlesByIndex.add(entry == null ? "" : definitionTitleOrEmpty(name));
            projectsByIndex.add(entry == null || entry.getGroupLabel() == null ? "" : entry.getGroupLabel());
            storiesByIndex.add(entry == null || entry.getEntryLabel() == null ? "" : entry.getEntryLabel());
        }
        TerminalSession currentSession = mActivity.getCurrentSession();
        return SessionRow.project(namesByIndex, titlesByIndex, projectsByIndex, storiesByIndex,
            getSessionTiersByIndex(), sessionActivityAgeLabelsByIndex(), getDisabledSessionIndexes(),
            indexOfSession(currentSession));
    }

    @NonNull
    private String definitionTitleOrEmpty(@NonNull String name) {
        String definitionTitle = mEntryMatcher.findTitleForSessionName(mEntries, name);
        return definitionTitle == null ? "" : definitionTitle;
    }

    private int indexOfSession(@Nullable TerminalSession session) {
        if (session == null) {
            return -1;
        }
        for (int sessionIndex = 0; sessionIndex < mSessionList.size(); sessionIndex++) {
            if (mSessionList.get(sessionIndex).getTerminalSession() == session) {
                return sessionIndex;
            }
        }
        return -1;
    }

    @NonNull
    private Map<Integer, SessionNewActivityIndicator> sessionActivityIndicatorsByIndex() {
        Map<Integer, SessionNewActivityIndicator> indicatorsBySessionIndex = new LinkedHashMap<>();
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) {
            return indicatorsBySessionIndex;
        }
        long nowMillis = System.currentTimeMillis();
        Set<String> disabledSessionNames = disabledSessionNames();
        for (int sessionIndex : getVisibleSessionIndexes()) {
            if (!isSessionIndexInRange(sessionIndex, mSessionList.size())) {
                continue;
            }
            TerminalSession terminalSession = mSessionList.get(sessionIndex).getTerminalSession();
            String sessionName = terminalSession == null ? null : terminalSession.mSessionName;
            if (isExcludedFromActivityIndicators(sessionName, disabledSessionNames)) {
                continue;
            }
            SessionNewActivityIndicator indicator =
                newActivityIndicator(store, sessionName, nowMillis);
            if (indicator.isVisible()) {
                indicatorsBySessionIndex.put(sessionIndex, indicator);
            }
        }
        return indicatorsBySessionIndex;
    }

    static boolean isExcludedFromActivityIndicators(@Nullable String sessionName,
                                                    @NonNull Set<String> disabledSessionNames) {
        return sessionName != null && disabledSessionNames.contains(sessionName);
    }

    @NonNull
    private Map<Integer, String> sessionActivityAgeLabelsByIndex() {
        Map<Integer, String> ageLabelsBySessionIndex = new LinkedHashMap<>();
        for (Map.Entry<Integer, SessionNewActivityIndicator> entry
                : sessionActivityIndicatorsByIndex().entrySet()) {
            ageLabelsBySessionIndex.put(entry.getKey(), entry.getValue().getLabel());
        }
        return ageLabelsBySessionIndex;
    }

    @NonNull
    static SessionNewActivityIndicator newActivityIndicator(@NonNull SessionNewActivityStore store,
                                                            @Nullable String sessionName,
                                                            long nowMillis) {
        if (sessionName == null) {
            return SessionNewActivityIndicator.indicatorFor(null, null, null, null, nowMillis);
        }
        return SessionNewActivityIndicator.indicatorFor(
            store.getLastOutputActivityTimeMillis(sessionName),
            store.getLastExplicitCallTimeMillis(sessionName),
            store.getLastUserInputTimeMillis(sessionName),
            store.getLastSeenTimeMillis(sessionName), nowMillis);
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
                bindProjectHeaderTitle(projectHeaderView, row);
                bindProjectCollapseIndicator(projectHeaderView, row);
                bindProjectOverviewBrowserIcon(projectHeaderView, row);
                bindProjectTdpmConsoleIcon(projectHeaderView, row);
                bindProjectNewIssueIcon(projectHeaderView, row);
                return projectHeaderView;
            case STORY_HEADER:
                return getHeaderView(row, convertView, parent,
                    R.layout.item_terminal_sessions_story_header, R.id.session_story_header_title);
            default:
                return getSessionView(row, convertView, parent, position);
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

    private void bindProjectHeaderTitle(@NonNull View projectHeaderView, @NonNull SessionHierarchyRow row) {
        TextView headerTitleView = projectHeaderView.findViewById(R.id.session_project_header_title);
        headerTitleView.setText(projectHeaderTitle(row.getLabel(), projectSessionCount(row.getLabel())));
        headerTitleView.setTextColor(surfacePrimaryTextColor());
    }

    private int projectSessionCount(@Nullable String projectLabel) {
        Integer projectSessionCount = mSessionCountByProjectLabel.get(projectLabel);
        return projectSessionCount == null ? 0 : projectSessionCount;
    }

    @NonNull
    static String projectHeaderTitle(@Nullable String projectLabel, int sessionCount) {
        String label = projectLabel == null ? "" : projectLabel;
        return label + " (" + sessionCount + ")";
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

    static void applySessionNameStyling(@NonNull SpannableString styled, int start, int end, @NonNull StyleSpan boldSpan) {
        if (end <= start) {
            return;
        }
        styled.setSpan(boldSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        styled.setSpan(new RelativeSizeSpan(SessionRow.SESSION_NAME_RELATIVE_SIZE), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    static void applyBellNotificationLabelStyling(@NonNull SpannableString styled, int start, int end,
                                                   @NonNull StyleSpan italicSpan) {
        if (start < 0 || end <= start) {
            return;
        }
        styled.setSpan(italicSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        styled.setSpan(new RelativeSizeSpan(BELL_NOTIFICATION_LABEL_RELATIVE_SIZE), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    static void applyExplicitCallReasonStyling(@NonNull SpannableString styled, int start, int end,
                                                @NonNull StyleSpan boldSpan) {
        if (start < 0 || end <= start) {
            return;
        }
        styled.setSpan(new RelativeSizeSpan(EXPLICIT_CALL_REASON_RELATIVE_SIZE), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        styled.setSpan(boldSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    @SuppressLint("SetTextI18n")
    private View getSessionView(@NonNull SessionHierarchyRow row, View convertView, @NonNull ViewGroup parent,
                                int position) {
        View sessionRowView = convertView;
        if (sessionRowView == null) {
            LayoutInflater inflater = mActivity.getLayoutInflater();
            sessionRowView = inflater.inflate(R.layout.item_terminal_sessions_list, parent, false);
        }

        applySessionRowGroupDividerVisibility(sessionRowView, position);

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

        SessionRow sessionRow = SessionRow.rowOrEmpty(mSessionRowsByIndex, sessionIndex);

        boolean isCurrentSession = sessionRow.isCurrent();
        boolean sessionRunning = sessionAtRow.isRunning();
        SessionRowActiveIndicator activeIndicator = computeActiveIndicator(isCurrentSession, sessionRunning);

        int rowBackgroundResId = sessionRowBackgroundRes(isCurrentSession);
        sessionRowView.setBackground(ContextCompat.getDrawable(mActivity, rowBackgroundResId));

        int activeIndicatorColor = ContextCompat.getColor(mActivity, R.color.session_active_indicator);
        View activeIndicatorBar = sessionRowView.findViewById(R.id.session_active_indicator_bar);
        applyActiveIndicatorBarVisibility(activeIndicatorBar, activeIndicator.showAccentBar, activeIndicatorColor);

        String sessionTitle = sessionAtRow.getTitle();
        String definitionTitle = sessionRow.getResolvedTitle();

        String sessionNamePart = sessionRow.getName();

        String bellNotificationLabelPart = buildBellNotificationLabel(sessionRow);
        String timestampLine = buildTimestampLine(sessionAtRow.mSessionName);
        String explicitCallReasonPart = explicitCallReasonLabel(sessionRow);

        SessionInfoBlock sessionInfoBlock = SessionInfoBlock.compose(bellNotificationLabelPart, sessionNamePart,
            timestampLine, definitionTitle, sessionTitle, explicitCallReasonPart);

        int bellNotificationLabelStart = sessionInfoBlock.startOf(SessionInfoLine.BELL_NOTIFICATION_LABEL);
        int bellNotificationLabelEnd = sessionInfoBlock.endOf(SessionInfoLine.BELL_NOTIFICATION_LABEL);
        int sessionNameStart = sessionInfoBlock.startOf(SessionInfoLine.SESSION_NAME);
        int timestampLineStart = sessionInfoBlock.startOf(SessionInfoLine.TIMESTAMP);
        int timestampLineEnd = sessionInfoBlock.endOf(SessionInfoLine.TIMESTAMP);
        int definitionTitleStart = sessionInfoBlock.startOf(SessionInfoLine.DEFINITION_TITLE);
        int definitionTitleEnd = sessionInfoBlock.endOf(SessionInfoLine.DEFINITION_TITLE);
        int sessionTitleStart = sessionInfoBlock.startOf(SessionInfoLine.SESSION_TITLE);
        int sessionTitleEnd = sessionInfoBlock.endOf(SessionInfoLine.SESSION_TITLE);
        int explicitCallReasonStart = sessionInfoBlock.startOf(SessionInfoLine.EXPLICIT_CALL_REASON);
        int explicitCallReasonEnd = sessionInfoBlock.endOf(SessionInfoLine.EXPLICIT_CALL_REASON);

        String fullSessionTitle = sessionInfoBlock.text();
        SpannableString fullSessionTitleStyled = new SpannableString(fullSessionTitle);
        if (sessionNameStart >= 0) {
            applySessionNameStyling(fullSessionTitleStyled, sessionNameStart, sessionNameStart + sessionNamePart.length(), boldSpan);
            if (activeIndicator.useAccentNameColor) {
                fullSessionTitleStyled.setSpan(new ForegroundColorSpan(activeIndicatorColor), sessionNameStart, sessionNameStart + sessionNamePart.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        applyBellNotificationLabelStyling(fullSessionTitleStyled, bellNotificationLabelStart, bellNotificationLabelEnd, italicSpan);
        if (definitionTitleStart >= 0) {
            int definitionTitleColor = (DEFINITION_TITLE_ALPHA << 24)
                | (surfacePrimaryTextColor() & 0x00FFFFFF);
            fullSessionTitleStyled.setSpan(new RelativeSizeSpan(DEFINITION_TITLE_RELATIVE_SIZE), definitionTitleStart, definitionTitleEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            fullSessionTitleStyled.setSpan(new ForegroundColorSpan(definitionTitleColor), definitionTitleStart, definitionTitleEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (sessionTitleStart >= 0) {
            fullSessionTitleStyled.setSpan(italicSpan, sessionTitleStart, sessionTitleEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (explicitCallReasonStart >= 0) {
            int reasonColor = ContextCompat.getColor(mActivity, R.color.session_explicit_call_reason_text);
            applyExplicitCallReasonStyling(fullSessionTitleStyled, explicitCallReasonStart, explicitCallReasonEnd, boldSpan);
            fullSessionTitleStyled.setSpan(new ForegroundColorSpan(reasonColor), explicitCallReasonStart, explicitCallReasonEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (timestampLineStart >= 0) {
            int fadedColor = (0x99 << 24) | (surfacePrimaryTextColor() & 0x00FFFFFF);
            fullSessionTitleStyled.setSpan(new RelativeSizeSpan(TIMESTAMP_ROW_RELATIVE_SIZE), timestampLineStart, timestampLineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            fullSessionTitleStyled.setSpan(new ForegroundColorSpan(fadedColor), timestampLineStart, timestampLineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            fullSessionTitleStyled.setSpan(new TypefaceSpan("monospace"), timestampLineStart, timestampLineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        sessionTitleView.setText(fullSessionTitleStyled);

        applyBellNotificationIcon(sessionTitleView, sessionRow);

        if (sessionRunning) {
            sessionTitleView.setPaintFlags(sessionTitleView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            sessionTitleView.setPaintFlags(sessionTitleView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        int defaultColor = surfacePrimaryTextColor();
        int color = sessionRunning || sessionAtRow.getExitStatus() == 0 ? defaultColor : Color.RED;
        sessionTitleView.setTextColor(color);

        bindSessionDisableToggle(sessionRowView, sessionRow, sessionAtRow.mSessionName);
        return sessionRowView;
    }

    private void applySessionRowGroupDividerVisibility(@NonNull View sessionRowView, int position) {
        View groupDividerView = sessionRowView.findViewById(R.id.session_row_group_divider);
        SessionHierarchyRow.Type previousRowType = position > 0
            ? mRows.get(position - 1).getType()
            : null;
        boolean showDivider = shouldShowSessionRowGroupDivider(previousRowType);
        groupDividerView.setVisibility(showDivider ? View.VISIBLE : View.GONE);
    }

    static boolean shouldShowSessionRowGroupDivider(@Nullable SessionHierarchyRow.Type previousRowType) {
        return previousRowType == SessionHierarchyRow.Type.SESSION;
    }

    private void bindSessionDisableToggle(@NonNull View sessionRowView, @NonNull SessionRow sessionRow,
                                          @Nullable String sessionName) {
        ImageView disableToggleView = sessionRowView.findViewById(R.id.session_disable_toggle);
        boolean disabled = sessionRow.isDisabled();
        disableToggleView.setImageResource(sessionDisableToggleIconRes(disabled));
        disableToggleView.setActivated(disabled);
        disableToggleView.setOnClickListener(v -> toggleSessionDisabled(sessionName));
    }

    static int sessionDisableToggleIconRes(boolean disabled) {
        return disabled
            ? R.drawable.ic_session_navigation_disabled
            : R.drawable.ic_session_navigation_enabled;
    }

    private String buildBellNotificationLabel(@NonNull SessionRow sessionRow) {
        return "";
    }

    @NonNull
    private String lastUpdatedAgeLabel(@Nullable String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) {
            return "";
        }
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) {
            return "";
        }
        String label = store.lastOutputActivityAgeLabel(sessionName, System.currentTimeMillis());
        return label == null ? "" : label;
    }

    @NonNull
    private String buildTimestampLine(@Nullable String sessionName) {
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) return "";
        return buildTimestampLine(store, sessionName, System.currentTimeMillis());
    }

    @NonNull
    static String buildTimestampLine(@NonNull SessionNewActivityStore store,
                                     @Nullable String sessionName,
                                     long nowMillis) {
        if (sessionName == null || sessionName.isEmpty()) return "";
        return "call: " + padTimestampValue(relativeAgeOrDash(store.getLastExplicitCallTimeMillis(sessionName), nowMillis))
            + "  out: " + padTimestampValue(relativeAgeOrDash(store.getLastOutputActivityTimeMillis(sessionName), nowMillis))
            + "  seen: " + relativeAgeOrDash(store.getLastSeenTimeMillis(sessionName), nowMillis);
    }

    @NonNull
    static String padTimestampValue(@NonNull String value) {
        if (value.length() >= TIMESTAMP_VALUE_FIELD_WIDTH) {
            return value;
        }
        StringBuilder padded = new StringBuilder(value);
        while (padded.length() < TIMESTAMP_VALUE_FIELD_WIDTH) {
            padded.append(' ');
        }
        return padded.toString();
    }

    @NonNull
    private static String relativeAgeOrDash(@Nullable Long timeMillis, long nowMillis) {
        return timeMillis == null ? "-" : SessionNewActivityStore.formatRelativeTime(nowMillis - timeMillis);
    }

    @NonNull
    private String explicitCallReasonLabel(@NonNull SessionRow sessionRow) {
        if (sessionRow.getTier() != SessionNewActivityTier.RED) {
            return "";
        }
        String sessionName = sessionRow.getName();
        if (sessionName.isEmpty()) {
            return "";
        }
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) {
            return "";
        }
        String reason = store.getLastExplicitCallReason(sessionName);
        if (reason.isEmpty()) {
            return "";
        }
        return EXPLICIT_CALL_REASON_PREFIX + reason;
    }

    private void applyBellNotificationIcon(@NonNull TextView sessionTitleView, @NonNull SessionRow sessionRow) {
        int indicatorDrawableRes = newActivityIndicatorDrawableRes(sessionRow.getTier());
        sessionTitleView.setCompoundDrawablesRelativeWithIntrinsicBounds(indicatorDrawableRes, 0, 0, 0);
        sessionTitleView.setCompoundDrawablePadding(dpToPx(SESSION_ROW_BELL_ICON_PADDING_DP));
    }

    static int newActivityIndicatorDrawableRes(@NonNull SessionNewActivityTier tier) {
        return SessionActivityTierDrawables.dotDrawableRes(tier);
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
        if (mSessionClickHost != null) {
            mSessionClickHost.onSessionSelected();
        }
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

    enum SessionAction {
        COPY_NAME(R.string.action_copy_session_name),
        RENAME(R.string.action_rename_session),
        HIDE(R.string.action_hide_session),
        KILL_HOST_SESSION(R.string.action_kill_host_session),
        DELETE(R.string.action_delete_session);

        final int labelResId;

        SessionAction(int labelResId) {
            this.labelResId = labelResId;
        }

        static SessionAction atIndex(int index) {
            return values()[index];
        }
    }

    private void showSessionActionChooser(final TerminalSession session) {
        if (session == null) {
            return;
        }

        SessionAction[] sessionActions = SessionAction.values();
        CharSequence[] actionLabels = new CharSequence[sessionActions.length];
        for (int index = 0; index < sessionActions.length; index++) {
            actionLabels[index] = mActivity.getString(sessionActions[index].labelResId);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        if (!TextUtils.isEmpty(session.mSessionName)) {
            builder.setTitle(session.mSessionName);
        }
        builder.setItems(actionLabels, (dialog, which) -> onSessionActionSelected(SessionAction.atIndex(which), session));
        DialogUtils.showDismissibleOnTouchOutside(builder);
    }

    private void onSessionActionSelected(SessionAction action, TerminalSession session) {
        switch (action) {
            case COPY_NAME:
                mActivity.getTermuxTerminalSessionClient().copySessionNameToClipboard(session);
                break;
            case RENAME:
                mActivity.getTermuxTerminalSessionClient().renameSession(session);
                break;
            case HIDE:
                hideSession(session.mSessionName);
                break;
            case KILL_HOST_SESSION:
                mActivity.getTermuxTerminalSessionClient().killHostSession(session);
                break;
            case DELETE:
                mActivity.getTermuxTerminalSessionClient().deleteSession(session);
                break;
        }
    }

}
