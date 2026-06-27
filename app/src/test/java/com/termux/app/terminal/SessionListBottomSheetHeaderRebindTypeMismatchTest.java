package com.termux.app.terminal;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

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
    public void headerRowRebindWithTypeMismatchedConvertViewRendersTitleWithoutCrashing() throws Exception {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        TermuxSessionsListViewController adapter = mixedRowAdapter(activity);
        ViewGroup parent = new ListView(activity);

        int headerPosition = firstHeaderPosition(adapter);
        Assert.assertTrue("the mixed-row adapter must contain at least one header row to reproduce the crash",
            headerPosition >= 0);
        Assert.assertTrue("the mixed-row adapter must also contain at least one session row so the list is genuinely mixed",
            hasSessionRow(adapter));

        View sessionRowConvertView = LayoutInflater.from(activity)
            .inflate(R.layout.item_terminal_sessions_list, parent, false);
        Assert.assertNull("the session-row layout must not contain the header title view, which is what triggers the NPE",
            sessionRowConvertView.findViewById(R.id.session_project_header_title));

        View renderedHeader;
        try {
            renderedHeader = adapter.getView(headerPosition, sessionRowConvertView, parent);
        } catch (NullPointerException e) {
            Assert.fail("rebinding a header position with a type-mismatched session-row convertView must not throw: " + e);
            return;
        }

        TextView headerTitleView = renderedHeader.findViewById(R.id.session_project_header_title);
        Assert.assertNotNull("the header must be re-inflated so its title view exists", headerTitleView);
        Assert.assertTrue("the re-inflated header must render its project label",
            headerTitleView.getText().toString().contains(PROJECT_LABEL));
    }

    @Test
    public void controllerSkipsTypeMismatchedChildViewSoTheHeaderBinderNeverReceivesIt() {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        ListView listView = new ListView(activity);

        View sessionTypedChild = new FrameLayout(activity);
        sessionTypedChild.setTag(R.id.session_row_view_type_tag, 0);

        StubViewTypeAdapter headerPositionAdapter = new StubViewTypeAdapter(1);

        View reused = SessionListBottomSheetController.typeCompatibleConvertView(
            headerPositionAdapter, 0, sessionTypedChild);

        Assert.assertNull("a child tagged with a different view type must not be reused as the convertView", reused);
    }

    @Test
    public void controllerReusesTypeMatchedChildView() {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();

        View headerTypedChild = new FrameLayout(activity);
        headerTypedChild.setTag(R.id.session_row_view_type_tag, 1);

        StubViewTypeAdapter headerPositionAdapter = new StubViewTypeAdapter(1);

        View reused = SessionListBottomSheetController.typeCompatibleConvertView(
            headerPositionAdapter, 0, headerTypedChild);

        Assert.assertSame("a child tagged with the matching view type must be reused as the convertView",
            headerTypedChild, reused);
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
        for (int position = 0; position < adapter.getCount(); position++) {
            if (((SessionHierarchyRow) adapter.getItem(position)).isHeader()) {
                return position;
            }
        }
        return -1;
    }

    private static boolean hasSessionRow(@NonNull TermuxSessionsListViewController adapter) {
        for (int position = 0; position < adapter.getCount(); position++) {
            if (!((SessionHierarchyRow) adapter.getItem(position)).isHeader()) {
                return true;
            }
        }
        return false;
    }

    private static final class StubViewTypeAdapter extends android.widget.BaseAdapter {

        private final int viewType;

        StubViewTypeAdapter(int viewType) {
            this.viewType = viewType;
        }

        @Override
        public int getViewTypeCount() {
            return 3;
        }

        @Override
        public int getItemViewType(int position) {
            return viewType;
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            return convertView != null ? convertView : new FrameLayout(parent.getContext());
        }
    }
}
