package com.termux.app.terminal;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.terminal.TerminalSession;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SessionListBottomSheetHeaderBindNullTitleGuardTest {

    private static final String PROJECT_LABEL = "DEMOPROJECT";
    private static final String STORY_LABEL = "DemoStory";
    private static final String SESSION_NAME = "https://github.com/HiromiShikata/termux-app/issues/100?k=TESTKEY";
    private static final String LOG_TAG = "TermuxSessionsListViewController";

    @Before
    public void clearLogs() {
        ShadowLog.clear();
    }

    @Test
    public void bindingAProjectHeaderRowIntoASessionViewHolderIsSkippedWithoutCrashingAndIsLogged() throws Exception {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        TermuxSessionsListViewController adapter = mixedRowAdapter(activity);
        RecyclerView parent = recyclerViewParent(activity);

        int projectHeaderPosition = firstProjectHeaderPosition(adapter);
        Assert.assertTrue("the mixed-row adapter must contain a project-header row to reproduce the crash",
            projectHeaderPosition >= 0);

        TermuxSessionsListViewController.SessionRowViewHolder sessionViewHolder =
            adapter.createViewHolder(parent, sessionViewType(adapter));
        Assert.assertNull("a session view holder must not own the project-header title view, the absence the crash exploited",
            sessionViewHolder.itemView.findViewById(R.id.session_project_header_title));

        try {
            adapter.onBindViewHolder(sessionViewHolder, projectHeaderPosition);
        } catch (NullPointerException e) {
            Assert.fail("binding a project-header row into a session-typed view holder must not throw: " + e);
        }

        Assert.assertTrue("the dispatch-level type-mismatch guard must record a warning so a recurrence is detectable",
            loggedAMismatchWarning());
    }

    @Test
    public void bindingAProjectHeaderRowIntoAStoryHeaderViewHolderIsSkippedWithoutCrashingAndIsLogged() throws Exception {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        TermuxSessionsListViewController adapter = mixedRowAdapter(activity);
        RecyclerView parent = recyclerViewParent(activity);

        int projectHeaderPosition = firstProjectHeaderPosition(adapter);
        Assert.assertTrue("the mixed-row adapter must contain a project-header row to reproduce the crash",
            projectHeaderPosition >= 0);

        TermuxSessionsListViewController.SessionRowViewHolder storyHeaderViewHolder =
            adapter.createViewHolder(parent, storyHeaderViewType(adapter));
        Assert.assertNull("a story-header view must not own the project-header title view",
            storyHeaderViewHolder.itemView.findViewById(R.id.session_project_header_title));

        try {
            adapter.onBindViewHolder(storyHeaderViewHolder, projectHeaderPosition);
        } catch (NullPointerException e) {
            Assert.fail("binding a project-header row into a story-header view holder must not throw: " + e);
        }

        Assert.assertTrue("the dispatch-level type-mismatch guard must record a warning so a recurrence is detectable",
            loggedAMismatchWarning());
    }

    @Test
    public void anExpandedProjectHeaderRendersItsLabelAndFractionWhenBoundIntoItsOwnHeaderViewHolder() throws Exception {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        TermuxSessionsListViewController adapter = mixedRowAdapter(activity);
        RecyclerView parent = recyclerViewParent(activity);

        int projectHeaderPosition = firstProjectHeaderPosition(adapter);
        TermuxSessionsListViewController.SessionRowViewHolder headerViewHolder =
            adapter.createViewHolder(parent, adapter.getItemViewType(projectHeaderPosition));

        adapter.onBindViewHolder(headerViewHolder, projectHeaderPosition);

        TextView headerTitleView = headerViewHolder.itemView.findViewById(R.id.session_project_header_title);
        Assert.assertNotNull("the header view holder must own its title view", headerTitleView);
        String renderedExpandedTitle = headerTitleView.getText().toString();
        Assert.assertTrue("the expanded header must render its project label: " + renderedExpandedTitle,
            renderedExpandedTitle.contains(PROJECT_LABEL));
        Assert.assertNotEquals("an expanded header must render the session-count fraction after the label",
            PROJECT_LABEL, renderedExpandedTitle);
        Assert.assertFalse("a correctly-typed header bind must not log a type-mismatch warning",
            loggedAMismatchWarning());
    }

    @Test
    public void aCollapsedProjectHeaderRendersOnlyItsLabelSoTheCollapsedBindPathIsCovered() {
        Assert.assertEquals("a collapsed project header must render only its label, omitting the fraction",
            PROJECT_LABEL,
            TermuxSessionsListViewController.projectHeaderTitle(PROJECT_LABEL, 1, 2, true));
        Assert.assertNotEquals("an expanded project header must append the fraction after the label",
            PROJECT_LABEL,
            TermuxSessionsListViewController.projectHeaderTitle(PROJECT_LABEL, 1, 2, false));
    }

    private static boolean loggedAMismatchWarning() {
        for (ShadowLog.LogItem logItem : ShadowLog.getLogs()) {
            if (logItem.type == android.util.Log.WARN
                && logItem.tag != null && logItem.tag.endsWith(LOG_TAG)
                && logItem.msg != null && logItem.msg.contains("mismatched view holder")) {
                return true;
            }
        }
        return false;
    }

    private static int sessionViewType(@NonNull TermuxSessionsListViewController adapter) {
        List<SessionHierarchyRow> rows = adapter.getVisibleRows();
        for (int position = 0; position < rows.size(); position++) {
            if (rows.get(position).getType() == SessionHierarchyRow.Type.SESSION) {
                return adapter.getItemViewType(position);
            }
        }
        throw new IllegalStateException("the mixed-row adapter must contain at least one session row");
    }

    private static int storyHeaderViewType(@NonNull TermuxSessionsListViewController adapter) {
        List<SessionHierarchyRow> rows = adapter.getVisibleRows();
        for (int position = 0; position < rows.size(); position++) {
            if (rows.get(position).getType() == SessionHierarchyRow.Type.STORY_HEADER) {
                return adapter.getItemViewType(position);
            }
        }
        throw new IllegalStateException("the mixed-row adapter must contain at least one story-header row");
    }

    private static int firstProjectHeaderPosition(@NonNull TermuxSessionsListViewController adapter) {
        List<SessionHierarchyRow> rows = adapter.getVisibleRows();
        for (int position = 0; position < rows.size(); position++) {
            if (rows.get(position).getType() == SessionHierarchyRow.Type.PROJECT_HEADER) {
                return position;
            }
        }
        return -1;
    }

    @NonNull
    private static RecyclerView recyclerViewParent(@NonNull TermuxActivity activity) {
        RecyclerView recyclerView = new RecyclerView(activity);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        return recyclerView;
    }

    @NonNull
    private static TermuxSessionsListViewController mixedRowAdapter(@NonNull TermuxActivity activity) throws Exception {
        TerminalSession terminalSession =
            new TerminalSession("/system/bin/sh", "/", new String[0], new String[0], 100, null);
        terminalSession.mSessionName = SESSION_NAME;

        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class, boolean.class);
        constructor.setAccessible(true);
        TermuxSession termuxSession = constructor.newInstance(terminalSession, null, null, false);

        TermuxSessionsListViewController adapter =
            new TermuxSessionsListViewController(activity, Collections.singletonList(termuxSession));
        adapter.setEntries(Collections.singletonList(
            new SessionDefinitionEntry(PROJECT_LABEL, STORY_LABEL, Collections.singletonList(SESSION_NAME))));
        return adapter;
    }
}
