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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.browser.BrowserUrlInput;
import com.termux.app.browser.BrowserViewMode;
import com.termux.app.browser.TermuxBrowserController;
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

public class TermuxSessionsListViewController extends RecyclerView.Adapter<TermuxSessionsListViewController.SessionRowViewHolder> {

    static final Object RELATIVE_TIME_PAYLOAD = new Object();

    private static final long HEADER_ITEM_ID_NAMESPACE = 0x1_0000_0000L;
    private static final long PROJECT_HEADER_ITEM_ID_NAMESPACE = 0x2_0000_0000L;
    private static final long SESSION_ITEM_ID_NAMESPACE = 0x3_0000_0000L;

    private static final int VIEW_TYPE_SESSION = 0;
    private static final int VIEW_TYPE_PROJECT_HEADER = 1;
    private static final int VIEW_TYPE_STORY_HEADER = 2;

    private static final int SESSION_ROW_LEFT_PADDING_DP = 6;
    private static final int SESSION_ROW_VERTICAL_PADDING_DP = 6;
    private static final int SESSION_ROW_TITLE_TO_TIMES_BOTTOM_PADDING_DP = 0;
    private static final int SESSION_ROW_BELL_ICON_PADDING_DP = 4;
    private static final int SESSION_ROW_BELL_ICON_WIDTH_DP = 16;

    private static final float DEFINITION_TITLE_RELATIVE_SIZE = 0.7f;
    private static final int DEFINITION_TITLE_ALPHA = 0xA6;
    static final int SUBDUED_TEXT_ALPHA = 0x99;
    private static final float BELL_NOTIFICATION_LABEL_RELATIVE_SIZE = 0.75f;
    private static final float EXPLICIT_CALL_REASON_RELATIVE_SIZE = 0.5f;

    private static final String PROJECT_EXPANDED_INDICATOR = "▾";
    private static final String PROJECT_COLLAPSED_INDICATOR = "▸";

    private static final String EXPLICIT_CALL_REASON_PREFIX = "⚠ ";

    static final long REFRESH_DEBOUNCE_WINDOW_MILLIS = 200L;

    static final long POST_RECONNECT_REFRESH_DEBOUNCE_WINDOW_MILLIS = 800L;

    static final long POST_RECONNECT_RESCAN_WINDOW_DURATION_MILLIS = 5_000L;

    private static final String CONTENT_FIELD_DELIMITER = "\u0001";

    final TermuxActivity mActivity;

    private final Handler mRefreshDebounceHandler = new Handler(Looper.getMainLooper());

    private final Runnable mRefreshDebounceRunnable = this::onRefreshCooldownElapsed;

    @Nullable
    private Runnable mCoalescedRefreshRunnable;

    private boolean mRefreshCooldownActive;

    private boolean mRefreshPendingDuringCooldown;

    private boolean mPostReconnectRescanWindowActive;

    private final Runnable mPostReconnectRescanWindowEndRunnable = this::onPostReconnectRescanWindowElapsed;

    final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
    final StyleSpan italicSpan = new StyleSpan(Typeface.ITALIC);

    private final List<TermuxSession> mSessionList;

    private final SessionHierarchyBuilder mHierarchyBuilder = new SessionHierarchyBuilder();

    private final SessionDefinitionEntryMatcher mEntryMatcher = new SessionDefinitionEntryMatcher();

    private List<SessionDefinitionEntry> mEntries = Collections.emptyList();

    private List<SessionHierarchyRow> mRows = Collections.emptyList();

    private Map<Integer, SessionRow> mSessionRowsByIndex = Collections.emptyMap();

    private Map<Integer, SessionNewActivityIndicator> mSessionActivityIndicatorsByIndex = Collections.emptyMap();

    private Map<Long, SessionRowContent> mRowContentByItemId = Collections.emptyMap();

    private int mVisibleSessionCount;

    private int mPendingCallSessionCount;

    private Map<String, Integer> mSessionCountByProjectLabel = Collections.emptyMap();

    private Map<String, Integer> mPendingCallSessionCountByProjectLabel = Collections.emptyMap();

    private final Set<String> mCollapsedProjectKeys = new LinkedHashSet<>();

    @Nullable
    private SessionClickHost mSessionClickHost;

    public interface SessionClickHost {
        void onSessionSelected();
    }

    public TermuxSessionsListViewController(TermuxActivity activity, List<TermuxSession> sessionList) {
        this.mActivity = activity;
        this.mSessionList = sessionList;
        setHasStableIds(true);
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
        refreshSessionList();
    }

    @NonNull
    public List<SessionDefinitionEntry> getEntries() {
        return Collections.unmodifiableList(mEntries);
    }

    public void setCoalescedRefreshRunnable(@Nullable Runnable coalescedRefreshRunnable) {
        this.mCoalescedRefreshRunnable = coalescedRefreshRunnable;
    }

    public void requestSessionListRefresh() {
        if (mRefreshCooldownActive) {
            mRefreshPendingDuringCooldown = true;
            return;
        }
        mRefreshCooldownActive = true;
        runCoalescedRefresh();
        mRefreshDebounceHandler.postDelayed(mRefreshDebounceRunnable,
            refreshDebounceWindowMillis(mPostReconnectRescanWindowActive));
    }

    private void onRefreshCooldownElapsed() {
        mRefreshCooldownActive = false;
        if (mRefreshPendingDuringCooldown) {
            mRefreshPendingDuringCooldown = false;
            requestSessionListRefresh();
        }
    }

    public void beginPostReconnectRescanWindow() {
        if (postToMainThreadIfOffThread(this::beginPostReconnectRescanWindow)) {
            return;
        }
        mPostReconnectRescanWindowActive = true;
        mRefreshDebounceHandler.removeCallbacks(mPostReconnectRescanWindowEndRunnable);
        mRefreshDebounceHandler.postDelayed(mPostReconnectRescanWindowEndRunnable,
            POST_RECONNECT_RESCAN_WINDOW_DURATION_MILLIS);
    }

    private void onPostReconnectRescanWindowElapsed() {
        mPostReconnectRescanWindowActive = false;
    }

    static long refreshDebounceWindowMillis(boolean postReconnectRescanWindowActive) {
        return postReconnectRescanWindowActive
            ? POST_RECONNECT_REFRESH_DEBOUNCE_WINDOW_MILLIS
            : REFRESH_DEBOUNCE_WINDOW_MILLIS;
    }

    private void runCoalescedRefresh() {
        if (mCoalescedRefreshRunnable != null) {
            mCoalescedRefreshRunnable.run();
            return;
        }
        refreshSessionList();
    }

    public void refreshSessionList() {
        if (postToMainThreadIfOffThread(this::refreshSessionList)) {
            return;
        }
        List<SessionHierarchyRow> previousRows = mRows;
        Map<Long, SessionRowContent> previousRowContentByItemId = mRowContentByItemId;
        rebuildRows();
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
            new SessionRowDiffCallback(previousRows, mRows,
                previousRowContentByItemId, mRowContentByItemId), true);
        diffResult.dispatchUpdatesTo(this);
    }

    public void dispatchRelativeTimeTick() {
        if (postToMainThreadIfOffThread(this::dispatchRelativeTimeTick)) {
            return;
        }
        for (int position = 0; position < mRows.size(); position++) {
            if (mRows.get(position).getType() == SessionHierarchyRow.Type.SESSION) {
                notifyItemChanged(position, RELATIVE_TIME_PAYLOAD);
            }
        }
    }

    private boolean postToMainThreadIfOffThread(@NonNull Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return false;
        }
        mRefreshDebounceHandler.post(action);
        return true;
    }

    static final class SessionRowDiffCallback extends DiffUtil.Callback {

        private final List<SessionHierarchyRow> previousRows;
        private final List<SessionHierarchyRow> currentRows;
        private final Map<Long, SessionRowContent> previousRowContentByItemId;
        private final Map<Long, SessionRowContent> currentRowContentByItemId;

        SessionRowDiffCallback(@NonNull List<SessionHierarchyRow> previousRows,
                               @NonNull List<SessionHierarchyRow> currentRows,
                               @NonNull Map<Long, SessionRowContent> previousRowContentByItemId,
                               @NonNull Map<Long, SessionRowContent> currentRowContentByItemId) {
            this.previousRows = previousRows;
            this.currentRows = currentRows;
            this.previousRowContentByItemId = previousRowContentByItemId;
            this.currentRowContentByItemId = currentRowContentByItemId;
        }

        @Override
        public int getOldListSize() {
            return previousRows.size();
        }

        @Override
        public int getNewListSize() {
            return currentRows.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return sameRowIdentity(previousRows.get(oldItemPosition), currentRows.get(newItemPosition));
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            SessionRowContent previousContent = previousRowContentByItemId.get(
                rowItemId(previousRows.get(oldItemPosition)));
            SessionRowContent currentContent = currentRowContentByItemId.get(
                rowItemId(currentRows.get(newItemPosition)));
            return SessionRowContent.sameContent(previousContent, currentContent);
        }
    }

    static boolean sameRowIdentity(@NonNull SessionHierarchyRow firstRow, @NonNull SessionHierarchyRow secondRow) {
        if (firstRow.getType() != secondRow.getType()) {
            return false;
        }
        if (firstRow.getType() == SessionHierarchyRow.Type.SESSION) {
            String firstName = firstRow.getSessionName();
            String secondName = secondRow.getSessionName();
            if (firstName != null || secondName != null) {
                return TextUtils.equals(firstName, secondName);
            }
            return firstRow.getSessionIndex() == secondRow.getSessionIndex();
        }
        return TextUtils.equals(firstRow.getLabel(), secondRow.getLabel());
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

    @NonNull
    private List<SessionHierarchyRow> buildCountedRows() {
        List<String> sessionNamesByIndex = sessionNamesByIndex();
        Set<String> hiddenSessionNames = shouldHideHiddenSessions() ? disabledSessionNames() : Collections.emptySet();
        List<SessionHierarchyRow> renderedRows =
            SessionHierarchyBuilder.filterHiddenSessions(buildAllRows(), sessionNamesByIndex, hiddenSessionNames);
        return SessionHierarchyBuilder.filterCollapsedProjectSessions(renderedRows, mCollapsedProjectKeys);
    }

    private void rebuildRows() {
        List<SessionHierarchyRow> allRows = buildAllRows();
        List<String> sessionNamesByIndex = sessionNamesByIndex();
        Set<String> hiddenSessionNames = shouldHideHiddenSessions() ? disabledSessionNames() : Collections.emptySet();
        List<SessionHierarchyRow> renderedRows =
            SessionHierarchyBuilder.filterHiddenSessions(allRows, sessionNamesByIndex, hiddenSessionNames);
        List<SessionHierarchyRow> countedRows =
            SessionHierarchyBuilder.filterCollapsedProjectSessions(renderedRows, mCollapsedProjectKeys);
        mRows = Collections.unmodifiableList(
            mHierarchyBuilder.filterCollapsedProjects(renderedRows, mCollapsedProjectKeys));
        mSessionActivityIndicatorsByIndex = sessionActivityIndicatorsByIndex();
        mSessionRowsByIndex = getSessionRows();
        mVisibleSessionCount = SessionHierarchyBuilder.totalSessionCount(countedRows);
        mSessionCountByProjectLabel = SessionHierarchyBuilder.sessionCountByProjectLabel(countedRows);
        Set<String> pendingCallSessionNames = pendingCallToUserSessionNames(sessionNamesByIndex);
        mPendingCallSessionCount = SessionHierarchyBuilder.pendingCallSessionCount(
            countedRows, sessionNamesByIndex, pendingCallSessionNames);
        mPendingCallSessionCountByProjectLabel = SessionHierarchyBuilder.pendingCallSessionCountByProjectLabel(
            countedRows, sessionNamesByIndex, pendingCallSessionNames);
        mRowContentByItemId = buildRowContentByItemId();
    }

    @NonNull
    private Map<Long, SessionRowContent> buildRowContentByItemId() {
        Map<Long, SessionRowContent> rowContentByItemId = new LinkedHashMap<>();
        for (int position = 0; position < mRows.size(); position++) {
            SessionHierarchyRow row = mRows.get(position);
            SessionHierarchyRow.Type previousRowType = position > 0 ? mRows.get(position - 1).getType() : null;
            rowContentByItemId.put(rowItemId(row), new SessionRowContent(rowContentText(row, previousRowType)));
        }
        return rowContentByItemId;
    }

    @NonNull
    private String rowContentText(@NonNull SessionHierarchyRow row,
                                  @Nullable SessionHierarchyRow.Type previousRowType) {
        switch (row.getType()) {
            case PROJECT_HEADER:
                boolean projectCollapsed = mCollapsedProjectKeys.contains(row.getLabel());
                return joinContentFields("P",
                    projectHeaderTitle(row.getLabel(), projectPendingCallSessionCount(row.getLabel()),
                        projectSessionCount(row.getLabel()), projectCollapsed),
                    String.valueOf(projectCollapsed));
            case STORY_HEADER:
                return joinContentFields("S", nullToEmpty(row.getLabel()));
            default:
                return sessionRowContentText(row, previousRowType);
        }
    }

    @NonNull
    private String sessionRowContentText(@NonNull SessionHierarchyRow row,
                                         @Nullable SessionHierarchyRow.Type previousRowType) {
        int sessionIndex = row.getSessionIndex();
        boolean showDivider = shouldShowSessionRowGroupDivider(previousRowType);
        if (!isSessionIndexInRange(sessionIndex, mSessionList.size())) {
            return joinContentFields("X", String.valueOf(sessionIndex));
        }
        TerminalSession sessionAtRow = mSessionList.get(sessionIndex).getTerminalSession();
        if (sessionAtRow == null) {
            return joinContentFields("X", String.valueOf(sessionIndex));
        }
        SessionRow sessionRow = SessionRow.rowOrEmpty(mSessionRowsByIndex, sessionIndex);
        return joinContentFields("R",
            String.valueOf(sessionIndex),
            sessionRow.getName(),
            sessionRow.getResolvedTitle(),
            String.valueOf(sessionRow.getTier()),
            String.valueOf(sessionRow.isDisabled()),
            String.valueOf(sessionRow.isCurrent()),
            String.valueOf(sessionAtRow.isRunning()),
            String.valueOf(sessionAtRow.getExitStatus()),
            nullToEmpty(sessionAtRow.getTitle()),
            buildTimestampLine(sessionAtRow.mSessionName),
            explicitCallReasonLabel(sessionRow),
            String.valueOf(showDivider),
            String.valueOf(reconnectingIndicatorVisible(sessionAtRow.mSessionName)));
    }

    @NonNull
    static String joinContentFields(@NonNull String... fields) {
        return TextUtils.join(CONTENT_FIELD_DELIMITER, fields);
    }

    @NonNull
    private static String nullToEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    public int getVisibleSessionCount() {
        return mVisibleSessionCount;
    }

    public boolean isHidingHiddenSessions() {
        return shouldHideHiddenSessions();
    }

    private boolean shouldHideHiddenSessions() {
        TermuxAppSharedPreferences preferences = mActivity.getPreferences();
        return preferences != null && preferences.shouldHideHiddenSessions();
    }

    public boolean toggleHideHiddenSessions() {
        TermuxAppSharedPreferences preferences = mActivity.getPreferences();
        if (preferences == null) {
            return false;
        }
        boolean hideHiddenSessions = preferences.toggleHideHiddenSessions();
        refreshSessionList();
        return hideHiddenSessions;
    }

    public int getPendingCallSessionCount() {
        return mPendingCallSessionCount;
    }

    public int getFirstVisibleSessionIndexAfterRebuild() {
        return SessionHierarchyBuilder.firstSessionIndex(buildCountedRows());
    }

    public int getNextVisibleSessionIndex(int currentSessionIndex, boolean forward) {
        return VisibleSessionNavigator.nextSessionIndex(
            getOrderedSessionIndexes(), getNavigationCandidateSessionIndexes(), currentSessionIndex, forward);
    }

    @NonNull
    public List<Integer> getNavigationCandidateSessionIndexes() {
        return NotifiedSessionNavigationCandidates.restrictToCallingSessions(
            getOrderedSessionIndexes(), getNavigableSessionIndexes(), sessionNamesByIndex(),
            getPendingCallToUserSessionNames(), indexOfSession(mActivity.getCurrentSession()));
    }

    @NonNull
    public List<Integer> getOrderedSessionIndexes() {
        return SessionHierarchyBuilder.visibleSessionIndexes(buildCountedRows());
    }

    @NonNull
    public List<String> getSessionNamesByIndex() {
        return sessionNamesByIndex();
    }

    @NonNull
    public Set<String> getExpandedProjectSessionNames() {
        List<SessionHierarchyRow> expandedProjectRows =
            SessionHierarchyBuilder.filterCollapsedProjectSessions(buildAllRows(), mCollapsedProjectKeys);
        Set<String> expandedProjectSessionNames = new LinkedHashSet<>();
        for (SessionHierarchyRow row : expandedProjectRows) {
            if (row.isHeader()) {
                continue;
            }
            String sessionName = row.getSessionName();
            if (sessionName != null) {
                expandedProjectSessionNames.add(sessionName);
            }
        }
        return expandedProjectSessionNames;
    }

    @NonNull
    public Set<String> getPendingCallToUserSessionNames() {
        return pendingCallToUserSessionNames(sessionNamesByIndex());
    }

    public void switchToSessionAtIndex(int sessionIndex) {
        selectSessionAtIndex(sessionIndex);
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
        boolean wasDisabled = isSessionDisabled(sessionName);
        preferences.toggleSessionDisabled(sessionName);
        if (wasDisabled) {
            mActivity.reconnectDeadDefinitionBackedSessions();
        }
        refreshSessionList();
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
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store != null) {
            store.clearReconnecting(sessionName);
        }
        refreshSessionList();
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
        return new LinkedHashSet<>(mSessionActivityIndicatorsByIndex.keySet());
    }

    @NonNull
    public Map<Integer, SessionNewActivityTier> getSessionTiersByIndex() {
        Map<Integer, SessionNewActivityTier> tiersByIndex = new LinkedHashMap<>();
        for (Map.Entry<Integer, SessionNewActivityIndicator> entry
                : mSessionActivityIndicatorsByIndex.entrySet()) {
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
    private Set<String> pendingCallToUserSessionNames(@NonNull List<String> sessionNamesByIndex) {
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) {
            return Collections.emptySet();
        }
        Set<String> pendingCallSessionNames = new LinkedHashSet<>();
        for (String sessionName : sessionNamesByIndex) {
            if (sessionName != null && store.hasPendingExplicitCall(sessionName)) {
                pendingCallSessionNames.add(sessionName);
            }
        }
        return pendingCallSessionNames;
    }

    @NonNull
    private Map<Integer, SessionNewActivityIndicator> sessionActivityIndicatorsByIndex() {
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) {
            return Collections.emptyMap();
        }
        return sessionActivityIndicatorsByIndex(store, allSessionIndexes(),
            sessionNamesByIndex(), disabledSessionNames(), System.currentTimeMillis());
    }

    @NonNull
    private List<Integer> allSessionIndexes() {
        List<Integer> sessionIndexes = new ArrayList<>(mSessionList.size());
        for (int sessionIndex = 0; sessionIndex < mSessionList.size(); sessionIndex++) {
            sessionIndexes.add(sessionIndex);
        }
        return sessionIndexes;
    }

    @NonNull
    static Map<Integer, SessionNewActivityIndicator> sessionActivityIndicatorsByIndex(
            @NonNull SessionNewActivityStore store,
            @NonNull List<Integer> visibleSessionIndexes,
            @NonNull List<String> sessionNamesByIndex,
            @NonNull Set<String> disabledSessionNames,
            long nowMillis) {
        Map<Integer, SessionNewActivityIndicator> indicatorsBySessionIndex = new LinkedHashMap<>();
        for (int sessionIndex : visibleSessionIndexes) {
            if (!isSessionIndexInRange(sessionIndex, sessionNamesByIndex.size())) {
                continue;
            }
            String sessionName = sessionNamesByIndex.get(sessionIndex);
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
                : mSessionActivityIndicatorsByIndex.entrySet()) {
            ageLabelsBySessionIndex.put(entry.getKey(), entry.getValue().getLabel());
        }
        return ageLabelsBySessionIndex;
    }

    @NonNull
    static SessionNewActivityIndicator newActivityIndicator(@NonNull SessionNewActivityStore store,
                                                            @Nullable String sessionName,
                                                            long nowMillis) {
        if (sessionName == null) {
            return SessionNewActivityIndicator.indicatorFor(null, null, null, null, null, null, nowMillis);
        }
        return SessionNewActivityIndicator.indicatorFor(
            store.outActivityTimeMillisForDotTier(sessionName),
            store.replyActivityTimeMillisForDotTier(sessionName),
            store.pendingCallToUserTimeMillis(sessionName),
            store.statuslineCallPendingTimeMillis(sessionName),
            store.getLastUserInputTimeMillis(sessionName),
            store.getLastSeenTimeMillis(sessionName), nowMillis);
    }

    static boolean isSessionIndexInRange(int sessionIndex, int sessionCount) {
        return sessionIndex >= 0 && sessionIndex < sessionCount;
    }

    @Nullable
    private TermuxSession sessionAtRowOrNull(@NonNull SessionHierarchyRow row) {
        int sessionIndex = resolveClickedSessionIndex(row, sessionNamesByIndex());
        if (!isSessionIndexInRange(sessionIndex, mSessionList.size())) {
            return null;
        }
        return mSessionList.get(sessionIndex);
    }

    /**
     * Resolves a tapped row to the index of its session in the live session list, keyed on the row's
     * stable session name rather than on the position the row was rendered at. Resolving by name means
     * a tap always reaches the intended session even when a concurrent reconnect has shifted every
     * later index under the row. When the row carries no name (legacy rows) or the name is no longer
     * present in the live list, it falls back to the captured index so a tap is never silently lost.
     */
    static int resolveClickedSessionIndex(@NonNull SessionHierarchyRow row,
                                          @NonNull List<String> sessionNamesByIndex) {
        String sessionName = row.getSessionName();
        if (sessionName != null) {
            int indexByName = sessionNamesByIndex.indexOf(sessionName);
            if (indexByName >= 0) {
                return indexByName;
            }
        }
        return row.getSessionIndex();
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
        refreshSessionList();
    }

    public void applyProjectActionTokens(@NonNull List<ProjectActionToken> projectActionTokens) {
        if (projectActionTokens.isEmpty()) {
            return;
        }
        List<SessionHierarchyRow> allRows = buildAllRows();
        for (ProjectActionToken projectActionToken : projectActionTokens) {
            SessionHierarchyRow projectHeaderRow = SessionHierarchyBuilder.projectHeaderRowForProject(
                allRows, projectActionToken.getNormalizedProjectName());
            String url = SessionHierarchyBuilder.projectActionUrl(allRows,
                projectActionToken.getNormalizedProjectName(), projectActionToken.getAction());
            if (url != null && !url.isEmpty()) {
                openProjectUrlInNewTab(url, projectActionViewMode(projectActionToken.getAction()),
                    projectHeaderRow);
            }
            int topSessionIndex = SessionHierarchyBuilder.firstSessionIndexForProject(allRows,
                projectActionToken.getNormalizedProjectName());
            selectSessionAtIndex(topSessionIndex);
        }
    }

    static BrowserViewMode projectActionViewMode(@NonNull ProjectAction projectAction) {
        return BrowserViewMode.DESKTOP;
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
        refreshSessionList();
    }

    @Override
    public int getItemCount() {
        return mRows.size();
    }

    @Override
    public long getItemId(int position) {
        return rowItemId(mRows.get(position));
    }

    static long rowItemId(@NonNull SessionHierarchyRow row) {
        switch (row.getType()) {
            case PROJECT_HEADER:
                return PROJECT_HEADER_ITEM_ID_NAMESPACE | labelIdentityHash(row.getLabel());
            case STORY_HEADER:
                return HEADER_ITEM_ID_NAMESPACE | labelIdentityHash(row.getLabel());
            default:
                String sessionName = row.getSessionName();
                if (sessionName != null) {
                    return SESSION_ITEM_ID_NAMESPACE | labelIdentityHash(sessionName);
                }
                return row.getSessionIndex();
        }
    }

    private static long labelIdentityHash(@Nullable String label) {
        return (label == null ? 0 : label.hashCode()) & 0xFFFF_FFFFL;
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

    @NonNull
    @Override
    public SessionRowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = mActivity.getLayoutInflater();
        switch (viewType) {
            case VIEW_TYPE_PROJECT_HEADER:
                SessionRowViewHolder projectHeaderViewHolder = new SessionRowViewHolder(
                    inflater.inflate(R.layout.item_terminal_sessions_project_header, parent, false));
                projectHeaderViewHolder.itemView.setOnClickListener(
                    view -> onProjectHeaderRowClicked(projectHeaderViewHolder.getBindingAdapterPosition()));
                return projectHeaderViewHolder;
            case VIEW_TYPE_STORY_HEADER:
                return new SessionRowViewHolder(
                    inflater.inflate(R.layout.item_terminal_sessions_story_header, parent, false));
            default:
                SessionRowViewHolder sessionRowViewHolder = new SessionRowViewHolder(
                    inflater.inflate(R.layout.item_terminal_sessions_list, parent, false));
                bindSessionRowClickHandlers(sessionRowViewHolder);
                return sessionRowViewHolder;
        }
    }

    private void bindSessionRowClickHandlers(@NonNull SessionRowViewHolder sessionRowViewHolder) {
        sessionRowViewHolder.itemView.setOnClickListener(
            view -> onSessionRowClicked(sessionRowViewHolder.getBindingAdapterPosition()));
        sessionRowViewHolder.itemView.setOnLongClickListener(
            view -> onSessionRowLongClicked(sessionRowViewHolder.getBindingAdapterPosition()));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull SessionRowViewHolder holder, int position) {
        bindRowView(mRows.get(position), holder.itemView, position);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionRowViewHolder holder, int position,
                                 @NonNull List<Object> payloads) {
        if (payloads.contains(RELATIVE_TIME_PAYLOAD)) {
            bindSessionRowTimesOnly(mRows.get(position), holder.itemView);
            return;
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    private void bindSessionRowTimesOnly(@NonNull SessionHierarchyRow row, @NonNull View sessionRowView) {
        if (row.getType() != SessionHierarchyRow.Type.SESSION) {
            return;
        }
        int sessionIndex = row.getSessionIndex();
        if (!isSessionIndexInRange(sessionIndex, mSessionList.size())) {
            return;
        }
        TerminalSession sessionAtRow = mSessionList.get(sessionIndex).getTerminalSession();
        String timestampLine = sessionAtRow == null
            ? "" : buildTimestampLine(sessionAtRow.mSessionName);
        bindSessionRowTimes(sessionRowView, timestampLine);
        bindSessionRowReconnectingIndicator(sessionRowView,
            sessionAtRow == null ? null : sessionAtRow.mSessionName);
        TextView sessionTitleView = sessionRowView.findViewById(R.id.session_title);
        applySessionTitleBottomPadding(sessionTitleView, !timestampLine.isEmpty());
    }

    static final class SessionRowViewHolder extends RecyclerView.ViewHolder {
        SessionRowViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    @SuppressLint("SetTextI18n")
    private void bindRowView(@NonNull SessionHierarchyRow row, @NonNull View rowView, int position) {
        switch (row.getType()) {
            case PROJECT_HEADER:
                bindProjectHeaderTitle(rowView, row);
                bindProjectCollapseIndicator(rowView, row);
                return;
            case STORY_HEADER:
                bindHeaderTitle(rowView, row, R.id.session_story_header_title);
                return;
            default:
                bindSessionView(row, rowView, position);
        }
    }

    private void bindHeaderTitle(@NonNull View headerRowView, @NonNull SessionHierarchyRow row, int titleViewId) {
        TextView headerTitleView = headerRowView.findViewById(titleViewId);
        headerTitleView.setText(row.getLabel());
        headerTitleView.setTextColor(fadedSurfacePrimaryTextColor());
    }

    private void bindProjectHeaderTitle(@NonNull View projectHeaderView, @NonNull SessionHierarchyRow row) {
        TextView headerTitleView = projectHeaderView.findViewById(R.id.session_project_header_title);
        headerTitleView.setText(projectHeaderTitle(row.getLabel(),
            projectPendingCallSessionCount(row.getLabel()), projectSessionCount(row.getLabel()),
            mCollapsedProjectKeys.contains(row.getLabel())));
        headerTitleView.setTextColor(surfacePrimaryTextColor());
    }

    private int projectSessionCount(@Nullable String projectLabel) {
        Integer projectSessionCount = mSessionCountByProjectLabel.get(projectLabel);
        return projectSessionCount == null ? 0 : projectSessionCount;
    }

    private int projectPendingCallSessionCount(@Nullable String projectLabel) {
        Integer projectPendingCallSessionCount = mPendingCallSessionCountByProjectLabel.get(projectLabel);
        return projectPendingCallSessionCount == null ? 0 : projectPendingCallSessionCount;
    }

    @NonNull
    static String projectHeaderTitle(@Nullable String projectLabel, int pendingCallSessionCount,
                                     int sessionCount, boolean collapsed) {
        String label = projectLabel == null ? "" : projectLabel;
        if (collapsed) {
            return label;
        }
        return label + " " + SessionCountFraction.of(pendingCallSessionCount, sessionCount);
    }

    private void bindProjectCollapseIndicator(@NonNull View projectHeaderView, @NonNull SessionHierarchyRow row) {
        TextView collapseIndicatorView = projectHeaderView.findViewById(R.id.session_project_header_collapse_indicator);
        boolean collapsed = mCollapsedProjectKeys.contains(row.getLabel());
        collapseIndicatorView.setText(collapsed ? PROJECT_COLLAPSED_INDICATOR : PROJECT_EXPANDED_INDICATOR);
        collapseIndicatorView.setTextColor(surfacePrimaryTextColor());
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

    private void openProjectUrlInNewTab(@NonNull String url, @NonNull BrowserViewMode viewMode,
                                        @Nullable SessionHierarchyRow projectHeaderRow) {
        TermuxBrowserController browserController = mActivity.getTermuxBrowserController();
        if (browserController == null) {
            return;
        }
        if (projectHeaderRow != null && projectHeaderRow.getLabel() != null) {
            int topSessionIndex = SessionHierarchyBuilder.firstSessionIndexForProject(
                buildAllRows(), projectHeaderRow.getLabel());
            selectSessionAtIndex(topSessionIndex);
        }
        browserController.openUrlInNewTab(BrowserUrlInput.normalize(url), viewMode);
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
    private void bindSessionView(@NonNull SessionHierarchyRow row, @NonNull View sessionRowView, int position) {
        applySessionRowGroupDividerVisibility(sessionRowView, position);

        TextView sessionTitleView = sessionRowView.findViewById(R.id.session_title);

        int verticalPadding = dpToPx(SESSION_ROW_VERTICAL_PADDING_DP);
        int startPadding = dpToPx(SESSION_ROW_LEFT_PADDING_DP);
        sessionTitleView.setPadding(startPadding, verticalPadding, verticalPadding, verticalPadding);

        int sessionIndex = row.getSessionIndex();
        if (!isSessionIndexInRange(sessionIndex, mSessionList.size())) {
            sessionTitleView.setText("null session");
            return;
        }
        TerminalSession sessionAtRow = mSessionList.get(sessionIndex).getTerminalSession();
        if (sessionAtRow == null) {
            sessionTitleView.setText("null session");
            return;
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
            "", definitionTitle, sessionTitle, explicitCallReasonPart);

        bindSessionRowTimes(sessionRowView, timestampLine);
        bindSessionRowReconnectingIndicator(sessionRowView, sessionAtRow.mSessionName);
        applySessionTitleBottomPadding(sessionTitleView, !timestampLine.isEmpty());

        int bellNotificationLabelStart = sessionInfoBlock.startOf(SessionInfoLine.BELL_NOTIFICATION_LABEL);
        int bellNotificationLabelEnd = sessionInfoBlock.endOf(SessionInfoLine.BELL_NOTIFICATION_LABEL);
        int sessionNameStart = sessionInfoBlock.startOf(SessionInfoLine.SESSION_NAME);
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
        sessionTitleView.setText(fullSessionTitleStyled);

        applyBellNotificationIcon(sessionTitleView, sessionRow);

        sessionTitleView.setPaintFlags(sessionTitleView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        sessionTitleView.setTextColor(surfacePrimaryTextColor());

        bindSessionDisableToggle(sessionRowView, sessionRow, sessionAtRow.mSessionName);
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

    static int sessionTitleBottomPaddingDp(boolean timesVisible) {
        return timesVisible
            ? SESSION_ROW_TITLE_TO_TIMES_BOTTOM_PADDING_DP
            : SESSION_ROW_VERTICAL_PADDING_DP;
    }

    private void applySessionTitleBottomPadding(@NonNull TextView sessionTitleView, boolean timesVisible) {
        int bottomPadding = dpToPx(sessionTitleBottomPaddingDp(timesVisible));
        sessionTitleView.setPadding(sessionTitleView.getPaddingLeft(), sessionTitleView.getPaddingTop(),
            sessionTitleView.getPaddingRight(), bottomPadding);
    }

    private void bindSessionRowTimes(@NonNull View sessionRowView, @NonNull String timestampLine) {
        TextView sessionRowTimesView = sessionRowView.findViewById(R.id.session_row_times);
        alignSessionRowTimesStartWithTitleText(sessionRowTimesView, sessionTitleTextStartPaddingPx());
        if (timestampLine.isEmpty()) {
            sessionRowTimesView.setText("");
            sessionRowTimesView.setVisibility(View.GONE);
            return;
        }
        sessionRowTimesView.setTextColor(fadedSurfacePrimaryTextColor());
        sessionRowTimesView.setText(timestampLine);
        sessionRowTimesView.setVisibility(View.VISIBLE);
    }

    private void bindSessionRowReconnectingIndicator(@NonNull View sessionRowView,
                                                     @Nullable String sessionName) {
        View reconnectingIndicatorView = sessionRowView.findViewById(R.id.session_reconnecting_indicator);
        boolean showIndicator = reconnectingIndicatorVisible(sessionName);
        reconnectingIndicatorView.setVisibility(showIndicator ? View.VISIBLE : View.INVISIBLE);
    }

    private int sessionTitleTextStartPaddingPx() {
        int startPadding = dpToPx(SESSION_ROW_LEFT_PADDING_DP);
        return sessionRowTimesStartPaddingPx(startPadding,
            dpToPx(SESSION_ROW_BELL_ICON_WIDTH_DP),
            dpToPx(SESSION_ROW_BELL_ICON_PADDING_DP));
    }

    static int sessionRowTimesStartPaddingPx(int titleStartPaddingPx, int bellIconWidthPx,
                                             int bellIconPaddingPx) {
        return titleStartPaddingPx + bellIconWidthPx + bellIconPaddingPx;
    }

    static void alignSessionRowTimesStartWithTitleText(@NonNull TextView sessionRowTimesView,
                                                       int startPaddingPx) {
        sessionRowTimesView.setPaddingRelative(startPaddingPx,
            sessionRowTimesView.getPaddingTop(),
            sessionRowTimesView.getPaddingEnd(),
            sessionRowTimesView.getPaddingBottom());
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
        return SessionTimesLine.ofColumnAligned(
            store.getStatuslineCallTimeMillis(sessionName),
            store.getStatuslineOutTimeMillis(sessionName),
            store.effectiveReplyTimeMillis(sessionName),
            store.getSubagentCount(sessionName),
            nowMillis).getText();
    }

    private boolean reconnectingIndicatorVisible(@Nullable String sessionName) {
        return sessionName != null
            && SessionReconnectingIndicatorState.shouldShowReconnectingIndicator(
                sessionName, mActivity.getSessionNewActivityStore());
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

    private int fadedSurfacePrimaryTextColor() {
        return fadedColor(surfacePrimaryTextColor());
    }

    static int fadedColor(int color) {
        return (SUBDUED_TEXT_ALPHA << 24) | (color & 0x00FFFFFF);
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

    private void onProjectHeaderRowClicked(int position) {
        if (!isBoundRowPosition(position)) {
            return;
        }
        SessionHierarchyRow row = mRows.get(position);
        if (row.getType() != SessionHierarchyRow.Type.PROJECT_HEADER) {
            return;
        }
        toggleProjectCollapsed(row.getLabel());
    }

    private void onSessionRowClicked(int position) {
        if (!isBoundRowPosition(position)) {
            return;
        }
        SessionHierarchyRow row = mRows.get(position);
        if (row.isHeader()) {
            return;
        }
        TermuxSession clickedSession = sessionAtRowOrNull(row);
        if (clickedSession == null) {
            return;
        }
        mActivity.getTermuxTerminalSessionClient()
            .switchToSessionReconnectingIfDead(clickedSession.getTerminalSession());
        if (mSessionClickHost != null) {
            mSessionClickHost.onSessionSelected();
        }
    }

    private boolean onSessionRowLongClicked(int position) {
        if (!isBoundRowPosition(position)) {
            return false;
        }
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

    private boolean isBoundRowPosition(int position) {
        return position != RecyclerView.NO_POSITION && position >= 0 && position < mRows.size();
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
