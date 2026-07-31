package com.termux.app.terminal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class DeletedSessionRowLeavesTheListInstrumentedTest {

    private static final long ACTIVITY_READY_TIMEOUT_MILLIS = 60_000L;

    private static final long LIST_SETTLE_TIMEOUT_MILLIS = 30_000L;

    private static final long POLL_INTERVAL_MILLIS = 100L;

    private static final String SYSTEM_SHELL_PATH = "/system/bin/sh";

    private static final String ROOT_WORKING_DIRECTORY = "/";

    private static final String PROJECT_LABEL = "deleterowproject";

    private static final String STORY_LABEL = "deleterowstory";

    private static final String SESSION_UNDER_TEST_NAME = "https://example.test/i1621-delete-removes-the-row";

    private static final String SESSION_KEEPING_THE_SERVICE_ALIVE_NAME =
        "i1621-session-keeping-the-service-alive";

    private ActivityScenario<TermuxActivity> mScenario;

    private String mAutosshCommandBeforeTest;

    private List<SessionDefinitionEntry> mEntriesBeforeTest;

    @Before
    public void launchActivityAndNameTheSessionInTheSessionDefinition() throws Exception {
        mScenario = ActivityScenario.launch(TermuxActivity.class);
        awaitConditionOnMainThread(ACTIVITY_READY_TIMEOUT_MILLIS, "the activity to bind its session list",
            activity -> activity.getTermuxService() != null
                && activity.getTermuxTerminalSessionClient() != null
                && activity.getPreferences() != null
                && activity.getTermuxSessionListViewController() != null);

        mAutosshCommandBeforeTest = readOnMainThread(activity -> activity.getPreferences().getAutosshCommand());
        mEntriesBeforeTest = readOnMainThread(activity ->
            activity.getTermuxSessionListViewController().getEntries());

        runOnMainThread(activity -> {
            TermuxAppSharedPreferences preferences = activity.getPreferences();
            preferences.setAutosshCommand("");
            preferences.setSessionUserRemoved(SESSION_UNDER_TEST_NAME, false);
            preferences.setSessionDisabled(SESSION_UNDER_TEST_NAME, false);
            activity.getTermuxSessionListViewController().setEntries(Collections.singletonList(
                new SessionDefinitionEntry(PROJECT_LABEL, STORY_LABEL,
                    Collections.singletonList(SESSION_UNDER_TEST_NAME))));
        });
        createSessionNamed(SESSION_KEEPING_THE_SERVICE_ALIVE_NAME);
    }

    @After
    public void restoreWhatThisTestChanged() {
        if (mScenario == null) return;
        runOnMainThread(activity -> {
            TermuxAppSharedPreferences preferences = activity.getPreferences();
            if (preferences != null) {
                preferences.setAutosshCommand(mAutosshCommandBeforeTest == null ? "" : mAutosshCommandBeforeTest);
                preferences.setSessionUserRemoved(SESSION_UNDER_TEST_NAME, false);
                preferences.setSessionDisabled(SESSION_UNDER_TEST_NAME, false);
            }
            TermuxSessionsListViewController listController = activity.getTermuxSessionListViewController();
            if (listController != null && mEntriesBeforeTest != null) {
                listController.setEntries(mEntriesBeforeTest);
            }
            TermuxService service = activity.getTermuxService();
            if (service == null) return;
            removeSessionNamed(service, activity, SESSION_UNDER_TEST_NAME);
            removeSessionNamed(service, activity, SESSION_KEEPING_THE_SERVICE_ALIVE_NAME);
        });
    }

    @Test
    public void deletingTheSessionRemovesItsRowAndOpeningItAgainBringsTheRowBack() throws Exception {
        TerminalSession sessionUnderTest = createTheSessionUnderTest();
        refreshTheList();
        awaitRowForTheSessionUnderTest(true, "a live session whose name the session definition carries must"
            + " draw a row, otherwise this test cannot tell whether deleting removed anything");

        runOnMainThread(activity ->
            activity.getTermuxTerminalSessionClient().deleteSession(sessionUnderTest));
        refreshTheList();

        assertNull("the Delete action must end the session, not only redraw the list",
            readOnMainThread(this::liveSessionUnderTest));
        awaitRowForTheSessionUnderTest(false, "the owner deleted this session, so its row must leave the"
            + " list; leaving it there makes the delete action look as if it did nothing");

        TerminalSession sessionOpenedAgain = createTheSessionUnderTest();
        assertNotNull("the session must be creatable again under the same name", sessionOpenedAgain);
        refreshTheList();
        awaitRowForTheSessionUnderTest(true, "a running session must always be reachable from the list, so"
            + " a name whose session exists again must draw its row even while the removal record stands");
    }

    @NonNull
    private TerminalSession createTheSessionUnderTest() {
        return createSessionNamed(SESSION_UNDER_TEST_NAME);
    }

    @NonNull
    private TerminalSession createSessionNamed(@NonNull String sessionName) {
        TerminalSession createdSession = readOnMainThread(activity -> {
            TermuxService service = activity.getTermuxService();
            if (service == null) return null;
            TermuxSession termuxSession = service.createTermuxSession(SYSTEM_SHELL_PATH, new String[0], null,
                ROOT_WORKING_DIRECTORY, true, sessionName);
            return termuxSession == null ? null : termuxSession.getTerminalSession();
        });
        assertNotNull("the service must create the session named " + sessionName, createdSession);
        return createdSession;
    }

    private void refreshTheList() {
        runOnMainThread(activity -> activity.getTermuxSessionListViewController().refreshSessionList());
    }

    private void awaitRowForTheSessionUnderTest(boolean expectedDrawn, @NonNull String requirement)
        throws Exception {
        long deadlineMillis = System.currentTimeMillis() + LIST_SETTLE_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadlineMillis) {
            if (Boolean.TRUE.equals(readOnMainThread(activity ->
                    rowCountForTheSessionUnderTest(activity) > 0)) == expectedDrawn) {
                return;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        fail(requirement + ". Rows actually drawn:\n" + readOnMainThread(this::describeRows));
    }

    private int rowCountForTheSessionUnderTest(@NonNull TermuxActivity activity) {
        int rowCount = 0;
        for (SessionHierarchyRow row : activity.getTermuxSessionListViewController().getVisibleRows()) {
            if (!row.isHeader() && SESSION_UNDER_TEST_NAME.equals(row.getSessionName())) {
                rowCount++;
            }
        }
        return rowCount;
    }

    @NonNull
    private String describeRows(@NonNull TermuxActivity activity) {
        StringBuilder description = new StringBuilder();
        for (SessionHierarchyRow row : activity.getTermuxSessionListViewController().getVisibleRows()) {
            description.append(row.getType())
                .append(row.isHeader() ? "|" + row.getLabel()
                    : "|index=" + row.getSessionIndex() + "|name=" + row.getSessionName())
                .append('\n');
        }
        return description.toString();
    }

    @Nullable
    private TerminalSession liveSessionUnderTest(@NonNull TermuxActivity activity) {
        return liveSessionNamed(activity, SESSION_UNDER_TEST_NAME);
    }

    @Nullable
    private TerminalSession liveSessionNamed(@NonNull TermuxActivity activity, @NonNull String sessionName) {
        TermuxService service = activity.getTermuxService();
        if (service == null) return null;
        TermuxSession termuxSession = service.getTermuxSessionForSessionName(sessionName);
        return termuxSession == null ? null : termuxSession.getTerminalSession();
    }

    private void removeSessionNamed(@NonNull TermuxService service, @NonNull TermuxActivity activity,
                                    @NonNull String sessionName) {
        TerminalSession leftoverSession = liveSessionNamed(activity, sessionName);
        if (leftoverSession == null) return;
        leftoverSession.finishIfRunning();
        service.removeTermuxSession(leftoverSession);
    }

    private void runOnMainThread(@NonNull MainThreadAction action) {
        mScenario.onActivity(action::run);
    }

    private <T> T readOnMainThread(@NonNull MainThreadReader<T> reader) {
        AtomicReference<T> readValue = new AtomicReference<>();
        mScenario.onActivity(activity -> readValue.set(reader.read(activity)));
        return readValue.get();
    }

    private void awaitConditionOnMainThread(long timeoutMillis, @NonNull String description,
                                            @NonNull MainThreadReader<Boolean> condition) throws Exception {
        long deadlineMillis = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadlineMillis) {
            if (Boolean.TRUE.equals(readOnMainThread(condition))) return;
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        fail("Timed out after " + timeoutMillis + "ms waiting for " + description);
    }

    private interface MainThreadAction {
        void run(@NonNull TermuxActivity activity);
    }

    private interface MainThreadReader<T> {
        T read(@NonNull TermuxActivity activity);
    }
}
