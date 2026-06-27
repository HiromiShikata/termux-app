package com.termux.app.terminal;

import android.view.View;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SessionListBottomSheetHeaderRebindTypeMismatchTest {

    private static final String PROJECT_LABEL = "DEMOPROJECT";
    private static final String STORY_LABEL = "DemoStory";
    private static final String SESSION_NAME = "https://github.com/HiromiShikata/termux-app/issues/100?k=TESTKEY";

    @Test
    public void headerPositionBindsIntoAHeaderViewHolderSoTheTypeMismatchedRebindCrashCannotRecur() throws Exception {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        TermuxSessionsListViewController adapter = mixedRowAdapter(activity);
        RecyclerView parent = recyclerViewParent(activity);

        int headerPosition = firstHeaderPosition(adapter);
        Assert.assertTrue("the mixed-row adapter must contain at least one header row to reproduce the original crash",
            headerPosition >= 0);
        Assert.assertTrue("the mixed-row adapter must also contain at least one session row so the list is genuinely mixed",
            hasSessionRow(adapter));

        int headerViewType = adapter.getItemViewType(headerPosition);
        TermuxSessionsListViewController.SessionRowViewHolder headerViewHolder =
            adapter.onCreateViewHolder(parent, headerViewType);
        Assert.assertNotNull("a header view type must create a header-typed view holder that owns the header title view",
            headerViewHolder.itemView.findViewById(R.id.session_project_header_title));

        try {
            adapter.onBindViewHolder(headerViewHolder, headerPosition);
        } catch (NullPointerException e) {
            Assert.fail("binding a header position into its own header view holder must not throw: " + e);
            return;
        }

        TextView headerTitleView = headerViewHolder.itemView.findViewById(R.id.session_project_header_title);
        Assert.assertNotNull("the header view holder must own its title view", headerTitleView);
        Assert.assertTrue("the bound header must render its project label",
            headerTitleView.getText().toString().contains(PROJECT_LABEL));
    }

    @Test
    public void everyViewTypeCreatesAViewHolderWhoseViewMatchesThatTypeSoBindNeverSeesAMismatchedView() throws Exception {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        RecyclerView parent = recyclerViewParent(activity);
        TermuxSessionsListViewController adapter = mixedRowAdapter(activity);
        List<SessionHierarchyRow> rows = adapter.getVisibleRows();

        for (int position = 0; position < rows.size(); position++) {
            TermuxSessionsListViewController.SessionRowViewHolder viewHolder =
                adapter.onCreateViewHolder(parent, adapter.getItemViewType(position));
            if (rows.get(position).getType() == SessionHierarchyRow.Type.PROJECT_HEADER) {
                Assert.assertNotNull("a project-header view type must own the header title view",
                    viewHolder.itemView.findViewById(R.id.session_project_header_title));
            }
            if (rows.get(position).getType() == SessionHierarchyRow.Type.SESSION) {
                Assert.assertNull("a session view type must not own the header title view, the absence the original crash exploited",
                    viewHolder.itemView.findViewById(R.id.session_project_header_title));
                Assert.assertNotNull("a session view type must own the session title view",
                    viewHolder.itemView.findViewById(R.id.session_title));
            }
        }
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

    private static int firstHeaderPosition(@NonNull TermuxSessionsListViewController adapter) {
        List<SessionHierarchyRow> rows = adapter.getVisibleRows();
        for (int position = 0; position < rows.size(); position++) {
            if (rows.get(position).isHeader()) {
                return position;
            }
        }
        return -1;
    }

    private static boolean hasSessionRow(@NonNull TermuxSessionsListViewController adapter) {
        for (SessionHierarchyRow row : adapter.getVisibleRows()) {
            if (!row.isHeader()) {
                return true;
            }
        }
        return false;
    }
}
