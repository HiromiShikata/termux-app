package com.termux.app.terminal;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
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
import androidx.core.content.ContextCompat;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.app.sessiondefinition.SessionDefinitionEntryMatcher;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.shared.theme.NightMode;
import com.termux.shared.theme.ThemeUtils;
import com.termux.terminal.TerminalSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TermuxSessionsListViewController extends BaseAdapter implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private static final int VIEW_TYPE_SESSION = 0;
    private static final int VIEW_TYPE_PROJECT_HEADER = 1;
    private static final int VIEW_TYPE_STORY_HEADER = 2;
    private static final int VIEW_TYPE_COUNT = 3;

    private static final int SESSION_ROW_FLAT_INDENT_DP = 6;
    private static final int SESSION_ROW_GROUPED_INDENT_DP = 24;
    private static final int SESSION_ROW_VERTICAL_PADDING_DP = 6;

    final TermuxActivity mActivity;

    final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
    final StyleSpan italicSpan = new StyleSpan(Typeface.ITALIC);

    private final List<TermuxSession> mSessionList;

    private final SessionHierarchyBuilder mHierarchyBuilder = new SessionHierarchyBuilder();

    private final SessionDefinitionEntryMatcher mEntryMatcher = new SessionDefinitionEntryMatcher();

    private List<SessionDefinitionEntry> mEntries = Collections.emptyList();

    private List<SessionHierarchyRow> mRows = Collections.emptyList();

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

    private void rebuildRows() {
        List<String> sessionNames = new ArrayList<>(mSessionList.size());
        for (TermuxSession session : mSessionList) {
            TerminalSession terminalSession = session.getTerminalSession();
            sessionNames.add(terminalSession == null ? null : terminalSession.mSessionName);
        }
        mRows = mHierarchyBuilder.build(sessionNames, mEntries,
            mActivity.getString(R.string.session_list_other_group_header));
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
        return !mRows.get(position).isHeader();
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        SessionHierarchyRow row = mRows.get(position);
        switch (row.getType()) {
            case PROJECT_HEADER:
                return getHeaderView(row, convertView, parent,
                    R.layout.item_terminal_sessions_project_header, R.id.session_project_header_title);
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

        if (shouldEnableDarkTheme) {
            sessionTitleView.setBackground(
                ContextCompat.getDrawable(mActivity, R.drawable.session_background_black_selected)
            );
        }

        String name = sessionAtRow.mSessionName;
        String sessionTitle = sessionAtRow.getTitle();
        String definitionTitle = mEntryMatcher.findTitleForSessionName(mEntries, name);

        String numberPart = "[" + (sessionIndex + 1) + "] ";
        String sessionNamePart = (TextUtils.isEmpty(name) ? "" : name);
        String namePart = numberPart + sessionNamePart;

        StringBuilder fullSessionTitleBuilder = new StringBuilder(namePart);
        int definitionTitleStart = -1;
        int definitionTitleEnd = -1;
        if (!TextUtils.isEmpty(definitionTitle)) {
            if (fullSessionTitleBuilder.length() > 0) {
                fullSessionTitleBuilder.append("\n");
            }
            definitionTitleStart = fullSessionTitleBuilder.length();
            fullSessionTitleBuilder.append(definitionTitle);
            definitionTitleEnd = fullSessionTitleBuilder.length();
        }
        if (!TextUtils.isEmpty(sessionTitle)) {
            if (fullSessionTitleBuilder.length() > 0) {
                fullSessionTitleBuilder.append("\n");
            }
            fullSessionTitleBuilder.append(sessionTitle);
        }

        String fullSessionTitle = fullSessionTitleBuilder.toString();
        SpannableString fullSessionTitleStyled = new SpannableString(fullSessionTitle);
        fullSessionTitleStyled.setSpan(boldSpan, 0, namePart.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (definitionTitleStart >= 0) {
            fullSessionTitleStyled.setSpan(new RelativeSizeSpan(0.85f), definitionTitleStart, definitionTitleEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (namePart.length() < fullSessionTitle.length()) {
            fullSessionTitleStyled.setSpan(italicSpan, namePart.length(), fullSessionTitle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        sessionTitleView.setText(fullSessionTitleStyled);

        boolean sessionRunning = sessionAtRow.isRunning();

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

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
            mActivity.getResources().getDisplayMetrics()));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SessionHierarchyRow row = mRows.get(position);
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
            mActivity.getString(R.string.action_rename_session),
            mActivity.getString(R.string.action_delete_session)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        if (!TextUtils.isEmpty(session.mSessionName)) {
            builder.setTitle(session.mSessionName);
        }
        builder.setItems(actions, (dialog, which) -> {
            if (which == 0) {
                mActivity.getTermuxTerminalSessionClient().renameSession(session);
            } else if (which == 1) {
                mActivity.getTermuxTerminalSessionClient().deleteSession(session);
            }
        });
        builder.show();
    }

}
