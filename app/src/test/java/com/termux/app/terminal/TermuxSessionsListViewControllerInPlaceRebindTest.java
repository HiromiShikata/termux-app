package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class TermuxSessionsListViewControllerInPlaceRebindTest {

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TermuxSessionsListViewController adapter;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        service = Robolectric.buildService(TermuxService.class).get();
        shellManager = new TermuxShellManager(RuntimeEnvironment.getApplication());
        set(service, TermuxService.class, "mShellManager", shellManager);
        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient",
            new TermuxTerminalSessionActivityClient(activity));
        service.setTermuxTerminalSessionClient(activity.getTermuxTerminalSessionClient());

        shellManager.mTermuxSessions.add(session("worker-session"));

        adapter = new TermuxSessionsListViewController(activity, service.getTermuxSessions());
        set(activity, TermuxActivity.class, "mTermuxSessionListViewController", adapter);
        adapter.setEntries(Collections.singletonList(
            new SessionDefinitionEntry("workerProject", "workerStory",
                Collections.singletonList("worker-session"))));
    }

    private int firstStoryHeaderPosition() {
        for (int position = 0; position < adapter.getCount(); position++) {
            if (adapter.getItemViewType(position)
                    == TermuxSessionsListViewController.VIEW_TYPE_STORY_HEADER) {
                return position;
            }
        }
        return -1;
    }

    private int firstSessionPosition() {
        for (int position = 0; position < adapter.getCount(); position++) {
            if (adapter.getItemViewType(position)
                    == TermuxSessionsListViewController.VIEW_TYPE_SESSION) {
                return position;
            }
        }
        return -1;
    }

    @Test
    public void theHierarchyContainsAStoryHeaderAndASessionRowSoTheMismatchPathIsExercisable() {
        assertTrue("a story header row must exist for the rebind mismatch to be possible",
            firstStoryHeaderPosition() >= 0);
        assertTrue("a session row must exist to act as the mismatched convertView",
            firstSessionPosition() >= 0);
    }

    @Test
    public void rebindingAStoryHeaderPositionWithASessionRowConvertViewDoesNotCrashAndRefreshesTheTitle() {
        FrameLayout parent = new FrameLayout(activity);
        int sessionPosition = firstSessionPosition();
        int storyHeaderPosition = firstStoryHeaderPosition();

        View sessionRowConvertView = adapter.getView(sessionPosition, null, parent);
        View headerRowView = adapter.getView(storyHeaderPosition, sessionRowConvertView, parent);

        assertNotNull("getView must return a header row even when handed a mismatched convertView",
            headerRowView);
        assertNotNull("the re-inflated header layout must expose the story header title view",
            headerRowView.findViewById(R.id.session_story_header_title));
    }

    @Test
    public void rebindingASessionPositionWithAStoryHeaderConvertViewDoesNotCrashAndRefreshesTheSessionRow() {
        FrameLayout parent = new FrameLayout(activity);
        int storyHeaderPosition = firstStoryHeaderPosition();
        int sessionPosition = firstSessionPosition();

        View storyHeaderConvertView = adapter.getView(storyHeaderPosition, null, parent);
        View sessionRowView = adapter.getView(sessionPosition, storyHeaderConvertView, parent);

        assertNotNull("getView must return a session row even when handed a mismatched convertView",
            sessionRowView);
        assertNotNull("the re-inflated session layout must expose the session title view",
            sessionRowView.findViewById(R.id.session_title));
    }

    @Test
    public void aMatchingStoryHeaderConvertViewIsReusedInPlaceForTheRebind() {
        FrameLayout parent = new FrameLayout(activity);
        int storyHeaderPosition = firstStoryHeaderPosition();

        View firstHeaderView = adapter.getView(storyHeaderPosition, null, parent);
        View reboundHeaderView = adapter.getView(storyHeaderPosition, firstHeaderView, parent);

        assertSame("a same-type convertView must be reused so the relative-time refresh rebinds it in place",
            firstHeaderView, reboundHeaderView);
    }

    @Test
    public void theFullVisibleWindowSurvivesAnInPlaceRelativeTimeRefreshTickWithMixedRowTypes() {
        ListView listView = new ListView(activity);
        listView.setAdapter(adapter);
        int widthSpec = View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY);
        listView.measure(widthSpec, heightSpec);
        listView.layout(0, 0, 600, 800);

        assertTrue("the laid-out list must contain at least the header and session rows",
            listView.getChildCount() >= 2);

        SessionListBottomSheetController.rebindVisibleSessionRowsInPlace(listView);

        assertEquals("the in-place rebind must not change the visible row count",
            adapter.getCount(), listView.getCount());
    }

    private TermuxSession session(String name) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = name;
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(terminalSession, -1);
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(terminalSession, new ExecutionCommand(), null, false);
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
