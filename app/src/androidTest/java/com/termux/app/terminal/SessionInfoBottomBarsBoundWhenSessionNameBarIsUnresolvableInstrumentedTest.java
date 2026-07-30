package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.app.Instrumentation;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.terminal.TerminalSession;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class SessionInfoBottomBarsBoundWhenSessionNameBarIsUnresolvableInstrumentedTest {

    private static final String SESSION_NAME = "name-bar-unresolvable-session";
    private static final String REASON = "NEEDS APPROVAL TO DEPLOY";
    private static final long SERVICE_READY_TIMEOUT_MILLIS = 30_000L;
    private static final long SESSION_READY_TIMEOUT_MILLIS = 15_000L;

    @Test
    public void sessionInfoBottomBarsAreBoundWhileTheSessionNameBarViewIsUnresolvable()
        throws Exception {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);

        waitForServiceConnected(scenario);

        AtomicReference<TerminalSession> sessionRef = new AtomicReference<>();
        scenario.onActivity(activity -> {
            TermuxTerminalSessionActivityClient client = activity.getTermuxTerminalSessionClient();
            assertNotNull(client);
            client.addNewSession(true, SESSION_NAME);
            client.stopActiveSessionSeenTick();
            sessionRef.set(activity.getCurrentSession());
        });

        TerminalSession session = waitForSession(scenario, sessionRef);
        assertNotNull("a current session carrying the test session name must exist", session);

        AtomicReference<SessionNewActivityTier> tierRef =
            new AtomicReference<>(SessionNewActivityTier.NONE);
        AtomicReference<Integer> sceneVisibilityRef = new AtomicReference<>(View.GONE);
        AtomicReference<String> sceneTextRef = new AtomicReference<>("");

        scenario.onActivity(activity -> {
            TermuxTerminalSessionActivityClient client = activity.getTermuxTerminalSessionClient();
            assertNotNull(client);

            SessionNewActivityStore store = activity.getSessionNewActivityStore();
            assertNotNull(store);
            store.purgeSession(SESSION_NAME);
            store.recordExplicitCall(SESSION_NAME, System.currentTimeMillis(), REASON);
            tierRef.set(store.tierFor(SESSION_NAME));

            View sceneBar = activity.findViewById(R.id.session_pending_call_to_user_bar);
            TextView sceneText = activity.findViewById(R.id.session_pending_call_to_user_text);
            assertNotNull(sceneBar);
            assertNotNull(sceneText);
            sceneBar.setVisibility(View.GONE);
            sceneText.setText("");

            TextView sessionNameBar = activity.findViewById(R.id.session_name_bar);
            assertNotNull("the session name bar must resolve before the test unresolves it",
                sessionNameBar);
            sessionNameBar.setId(View.NO_ID);
            assertNull("the session name bar must no longer resolve by id",
                activity.findViewById(R.id.session_name_bar));

            client.updateSessionNameOverlay();

            sceneVisibilityRef.set(sceneBar.getVisibility());
            sceneTextRef.set(sceneText.getText().toString());
        });

        System.out.println("NAME_BAR_UNRESOLVABLE_RENDER tier=" + tierRef.get()
            + " sceneVisible=" + (sceneVisibilityRef.get() == View.VISIBLE)
            + " sceneText=[" + sceneTextRef.get() + "]");

        assertEquals("the store must hold the red tier for the outstanding call",
            SessionNewActivityTier.RED, tierRef.get());
        assertEquals("the scene bar must be visible even though the session name bar view did not "
            + "resolve", View.VISIBLE, (int) sceneVisibilityRef.get());
        assertEquals("the scene text must render the outstanding call reason even though the "
            + "session name bar view did not resolve", REASON, sceneTextRef.get());
    }

    private static void waitForServiceConnected(ActivityScenario<TermuxActivity> scenario)
        throws InterruptedException {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        long deadline = System.currentTimeMillis() + SERVICE_READY_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            AtomicBoolean ready = new AtomicBoolean(false);
            scenario.onActivity(activity ->
                ready.set(activity.getTermuxService() != null
                    && activity.getSessionNewActivityStore() != null));
            if (ready.get()) {
                instrumentation.waitForIdleSync();
                return;
            }
            Thread.sleep(250L);
        }
        throw new AssertionError("TermuxService did not connect within timeout");
    }

    private static TerminalSession waitForSession(ActivityScenario<TermuxActivity> scenario,
                                                  AtomicReference<TerminalSession> sessionRef)
        throws InterruptedException {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        long deadline = System.currentTimeMillis() + SESSION_READY_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            AtomicReference<TerminalSession> readyRef = new AtomicReference<>();
            scenario.onActivity(activity -> {
                TerminalSession current = activity.getCurrentSession();
                if (current != null && SESSION_NAME.equals(current.mSessionName)) {
                    readyRef.set(current);
                }
            });
            if (readyRef.get() != null) {
                instrumentation.waitForIdleSync();
                return readyRef.get();
            }
            Thread.sleep(250L);
        }
        return sessionRef.get();
    }
}
