package com.termux.app.terminal.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.widget.EditText;
import android.widget.ImageButton;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.termux.app.TermuxActivity;
import com.termux.app.ownercall.LocalOwnerCallServer;
import com.termux.app.terminal.SessionNewActivityStore;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.terminal.TerminalSession;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
public class SendButtonReplyDeletesTheAnsweredOwnerCallsInstrumentedTest {

    private static final String SESSION_URL = LocalOwnerCallServer.SESSION_URL;
    private static final String CALL_BODY = "Decide whether the release may go out.";
    private static final String CALL_REASON = "the owner is being called";
    private static final String REPLY_TEXT = "yes";
    private static final long READY_TIMEOUT_MILLIS = 30_000L;
    private static final long POLL_INTERVAL_MILLIS = 100L;

    private LocalOwnerCallServer server;
    private String previousSessionDefinitionUrl;

    @Before
    public void startTheOwnerCallServer() throws IOException {
        previousSessionDefinitionUrl = preferences().getSessionDefinitionUrl();
        server = new LocalOwnerCallServer(
            LocalOwnerCallServer.ownerCallDocument("2026-08-14T04:22:28Z", CALL_BODY));
        server.start();
    }

    @After
    public void stopTheOwnerCallServer() throws InterruptedException {
        preferences().setSessionDefinitionUrl(previousSessionDefinitionUrl);
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void submittingTheReplyThroughTheSendButtonDeletesTheSessionsOwnerCallFile()
        throws Exception {
        ActivityScenario<TermuxActivity> scenario = launchWithACallingSession();
        awaitTheToolbarTextInput(scenario);

        scenario.onActivity(activity -> {
            EditText textInput = activity.getTerminalToolbarTextInput();
            assertNotNull("the toolbar text input the owner types the reply into must exist",
                textInput);
            textInput.setText(REPLY_TEXT);
            TerminalEnterKeyController enterKeyController =
                new TerminalEnterKeyController(activity, new ImageButton(activity));
            assertTrue("the owner content must be submitted through the send button path",
                enterKeyController.submit(textInput,
                    activity.getTerminalToolbarViewPagerAdapter()));
        });

        awaitDeleteRequest();
        assertEquals(Collections.singletonList(LocalOwnerCallServer.OWNER_CALL_FILE_PATH),
            server.deletedPaths());
    }

    private ActivityScenario<TermuxActivity> launchWithACallingSession() throws Exception {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        awaitServiceConnected(scenario);

        scenario.onActivity(activity -> {
            activity.getPreferences().setSessionDefinitionUrl(server.indexUrl());
            activity.loadSessionsFromDefinition();
        });
        awaitLoadedEntries(scenario);

        scenario.onActivity(activity -> {
            TermuxTerminalSessionActivityClient sessionClient =
                activity.getTermuxTerminalSessionClient();
            assertNotNull(sessionClient);
            sessionClient.addNewSession(true, SESSION_URL);
        });
        awaitRunningDisplayedSession(scenario);

        scenario.onActivity(activity -> {
            SessionNewActivityStore store = activity.getSessionNewActivityStore();
            assertNotNull(store);
            store.recordExplicitCall(SESSION_URL, System.currentTimeMillis(), CALL_REASON,
                CALL_REASON + "-" + System.nanoTime());
            activity.showUnansweredOwnerCallsOfDisplayedSession();
        });
        return scenario;
    }

    private static void awaitTheToolbarTextInput(ActivityScenario<TermuxActivity> scenario)
        throws InterruptedException {
        scenario.onActivity(activity -> {
            if (activity.getTerminalToolbarTextInput() == null) {
                activity.toggleTerminalToolbar();
            }
        });
        awaitCondition(scenario, activity -> activity.getTerminalToolbarTextInput() != null
                && activity.getTerminalToolbarViewPagerAdapter() != null,
            "the terminal toolbar the owner types the reply into never appeared");
    }

    private static void awaitServiceConnected(ActivityScenario<TermuxActivity> scenario)
        throws InterruptedException {
        awaitCondition(scenario, activity -> activity.getTermuxService() != null
                && activity.getTermuxTerminalSessionClient() != null
                && activity.getSessionNewActivityStore() != null,
            "the termux service never connected");
    }

    private void awaitLoadedEntries(ActivityScenario<TermuxActivity> scenario)
        throws InterruptedException {
        awaitCondition(scenario,
            activity -> !activity.getSessionDefinitionEntries().isEmpty(),
            "the session definition entries never loaded; the owner call server reported "
                + server.describeFailure());
    }

    private static void awaitRunningDisplayedSession(ActivityScenario<TermuxActivity> scenario)
        throws InterruptedException {
        awaitCondition(scenario, activity -> {
            TerminalSession session = activity.getCurrentSession();
            return session != null && session.isRunning() && session.getEmulator() != null
                && SESSION_URL.equals(session.mSessionName);
        }, "the called session never started");
    }

    private void awaitDeleteRequest() throws InterruptedException {
        long deadline = System.currentTimeMillis() + READY_TIMEOUT_MILLIS;
        AtomicInteger deleteCount = new AtomicInteger();
        while (System.currentTimeMillis() < deadline) {
            deleteCount.set(server.deletedPaths().size());
            if (deleteCount.get() > 0) {
                return;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        throw new AssertionError("the send button reply never deleted the answered owner calls; "
            + "the owner call server reported " + server.describeFailure());
    }

    private static void awaitCondition(ActivityScenario<TermuxActivity> scenario,
                                       ActivityCondition condition,
                                       String failureMessage) throws InterruptedException {
        long deadline = System.currentTimeMillis() + READY_TIMEOUT_MILLIS;
        AtomicBoolean satisfied = new AtomicBoolean();
        while (System.currentTimeMillis() < deadline) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> satisfied.set(condition.isSatisfiedBy(activity)));
            if (satisfied.get()) {
                return;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        throw new AssertionError(failureMessage);
    }

    private interface ActivityCondition {
        boolean isSatisfiedBy(TermuxActivity activity);
    }

    private static TermuxAppSharedPreferences preferences() {
        return TermuxAppSharedPreferences.build(
            InstrumentationRegistry.getInstrumentation().getTargetContext(), true);
    }
}
