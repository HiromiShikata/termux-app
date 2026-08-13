package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.diagnostics.SessionCreationPath;
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
public class OpeningASessionThatIsNotLiveDisplaysItInstrumentedTest {

    private static final long ACTIVITY_READY_TIMEOUT_MILLIS = 60_000L;

    private static final long SESSION_SETTLE_TIMEOUT_MILLIS = 30_000L;

    private static final long POLL_INTERVAL_MILLIS = 100L;

    private static final String SYSTEM_SHELL_PATH = "/system/bin/sh";

    private static final String ROOT_WORKING_DIRECTORY = "/";

    private static final String PROJECT_LABEL = "firsttapproject";

    private static final String STORY_LABEL = "firsttapstory";

    private static final String SESSION_THE_OWNER_TAPS_NAME =
        "https://example.test/i1629-one-tap-opens-and-displays";

    private static final String SESSION_THE_OWNER_IS_LOOKING_AT_NAME =
        "i1629-session-the-owner-is-looking-at";

    private ActivityScenario<TermuxActivity> mScenario;

    private String mAutosshCommandBeforeTest;

    private List<SessionDefinitionEntry> mEntriesBeforeTest;

    @Before
    public void launchActivityAndNameTheTappedSessionInTheSessionDefinition() throws Exception {
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
            preferences.setSessionUserRemoved(SESSION_THE_OWNER_TAPS_NAME, false);
            preferences.setSessionDisabled(SESSION_THE_OWNER_TAPS_NAME, false);
            activity.getTermuxSessionListViewController().setEntries(Collections.singletonList(
                new SessionDefinitionEntry(PROJECT_LABEL, STORY_LABEL,
                    Collections.singletonList(SESSION_THE_OWNER_TAPS_NAME))));
        });
    }

    @After
    public void restoreWhatThisTestChanged() {
        if (mScenario == null) return;
        runOnMainThread(activity -> {
            TermuxAppSharedPreferences preferences = activity.getPreferences();
            if (preferences != null) {
                preferences.setAutosshCommand(mAutosshCommandBeforeTest == null ? "" : mAutosshCommandBeforeTest);
                preferences.setSessionUserRemoved(SESSION_THE_OWNER_TAPS_NAME, false);
                preferences.setSessionDisabled(SESSION_THE_OWNER_TAPS_NAME, false);
            }
            TermuxSessionsListViewController listController = activity.getTermuxSessionListViewController();
            if (listController != null && mEntriesBeforeTest != null) {
                listController.setEntries(mEntriesBeforeTest);
            }
            TermuxService service = activity.getTermuxService();
            if (service == null) return;
            removeSessionNamed(service, activity, SESSION_THE_OWNER_TAPS_NAME);
            removeSessionNamed(service, activity, SESSION_THE_OWNER_IS_LOOKING_AT_NAME);
        });
    }

    @Test
    public void oneTapOnASessionThatIsNotLiveOpensItAndPutsItOnTheScreen() throws Exception {
        displaySomeOtherSession();
        assertNull("this test only means something while the tapped session is not live yet",
            readOnMainThread(activity -> liveSessionNamed(activity, SESSION_THE_OWNER_TAPS_NAME)));

        runOnMainThread(activity -> activity.getTermuxSessionListViewController()
            .openDefinitionBackedSession(SESSION_THE_OWNER_TAPS_NAME));

        awaitSessionIsLive(SESSION_THE_OWNER_TAPS_NAME);
        assertEquals("the owner tapped this session to look at it, so one tap must put it on the screen;"
                + " leaving the previous session displayed makes the tap look as if it did nothing and"
                + " forces a second tap. Displayed session was: "
                + readOnMainThread(this::displayedSessionName),
            SESSION_THE_OWNER_TAPS_NAME, readOnMainThread(this::displayedSessionName));
    }

    @Test
    public void bringingABackgroundSessionBackToLifeLeavesTheScreenOnTheSessionTheOwnerIsReading()
        throws Exception {
        displaySomeOtherSession();

        runOnMainThread(activity -> activity.getTermuxTerminalSessionClient()
            .recreateUnhiddenSessionWithoutDisplacingTheDisplayedSession(SESSION_THE_OWNER_TAPS_NAME));

        awaitSessionIsLive(SESSION_THE_OWNER_TAPS_NAME);
        assertEquals("re-enabling a hidden session is not a request to look at it, so it must not take the"
                + " screen away from the session the owner is reading. Displayed session was: "
                + readOnMainThread(this::displayedSessionName),
            SESSION_THE_OWNER_IS_LOOKING_AT_NAME, readOnMainThread(this::displayedSessionName));
    }

    private void displaySomeOtherSession() {
        TerminalSession sessionTheOwnerIsLookingAt = createSessionNamed(SESSION_THE_OWNER_IS_LOOKING_AT_NAME);
        runOnMainThread(activity ->
            activity.getTermuxTerminalSessionClient().setCurrentSession(sessionTheOwnerIsLookingAt));
        assertEquals("the test starts with the screen on a different session than the one it taps",
            SESSION_THE_OWNER_IS_LOOKING_AT_NAME, readOnMainThread(this::displayedSessionName));
    }

    @Nullable
    private String displayedSessionName(@NonNull TermuxActivity activity) {
        TerminalSession displayedSession = activity.getCurrentSession();
        return displayedSession == null ? null : displayedSession.mSessionName;
    }

    @NonNull
    private TerminalSession createSessionNamed(@NonNull String sessionName) {
        TerminalSession createdSession = readOnMainThread(activity -> {
            TermuxService service = activity.getTermuxService();
            if (service == null) return null;
            TermuxSession termuxSession = service.createTermuxSession(SYSTEM_SHELL_PATH, new String[0], null,
                ROOT_WORKING_DIRECTORY, true, sessionName,
                SessionCreationPath.NEW_SESSION_THE_OWNER_ASKED_FOR);
            return termuxSession == null ? null : termuxSession.getTerminalSession();
        });
        assertNotNull("the service must create the session named " + sessionName, createdSession);
        return createdSession;
    }

    private void awaitSessionIsLive(@NonNull String sessionName) throws Exception {
        awaitConditionOnMainThread(SESSION_SETTLE_TIMEOUT_MILLIS,
            "the session named " + sessionName + " to be created",
            activity -> liveSessionNamed(activity, sessionName) != null);
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
