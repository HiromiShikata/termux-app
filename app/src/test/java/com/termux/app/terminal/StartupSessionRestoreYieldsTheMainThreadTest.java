package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.session.SessionReconnectPacer;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.view.TerminalView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class StartupSessionRestoreYieldsTheMainThreadTest {

    private static final String FIRST_PERSISTED_SESSION_NAME = "first-persisted-session";

    private static final String SECOND_PERSISTED_SESSION_NAME = "second-persisted-session";

    private static final String THIRD_PERSISTED_SESSION_NAME = "third-persisted-session";

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context appContext = RuntimeEnvironment.getApplication();

        service = Robolectric.buildService(TermuxService.class).get();
        shellManager = new TermuxShellManager(appContext);
        set(service, TermuxService.class, "mShellManager", shellManager);
        set(service, TermuxService.class, "mProperties",
            com.termux.shared.termux.settings.properties.TermuxAppSharedProperties.init(appContext));

        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient",
            new TermuxTerminalSessionActivityClient(activity));
        service.setTermuxTerminalSessionClient(activity.getTermuxTerminalSessionClient());

        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        preferences.setSessionDefinitionMaxSessions(10);
        preferences.setPersistedSessions(persistedSessionsJson());

        set(activity, TermuxActivity.class, "mProperties",
            com.termux.shared.termux.settings.properties.TermuxAppSharedProperties.init(appContext));
        set(activity, TermuxActivity.class, "mIsVisible", true);

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        set(activity, TermuxActivity.class, "mTerminalView", new TerminalView(appContext, null));
    }

    @Test
    public void restoringThePersistedSessionsReleasesTheMainThreadBetweenSessions() {
        boolean restored = activity.getTermuxTerminalSessionClient().restorePersistedSessions();

        assertTrue("a cold start with persisted sessions must report that it restored them, otherwise the"
                + " caller creates an unwanted default session on top of them",
            restored);
        assertEquals("restoring every persisted session in one uninterrupted loop holds the main thread for"
                + " the whole restore, which is what makes the first launch after an update unscrollable;"
                + " only the session the owner is shown may be built before the main thread is released",
            1, service.getTermuxSessionsSize());
    }

    @Test
    public void everyPersistedSessionIsRestoredOnceTheMainThreadHasRunTheQueuedWork() {
        activity.getTermuxTerminalSessionClient().restorePersistedSessions();

        idlePastTheRestorePacer();

        assertEquals("releasing the main thread between sessions must still restore all of them",
            3, service.getTermuxSessionsSize());
        assertNotNull(service.getTermuxSessionForSessionName(FIRST_PERSISTED_SESSION_NAME));
        assertNotNull(service.getTermuxSessionForSessionName(SECOND_PERSISTED_SESSION_NAME));
        assertNotNull(service.getTermuxSessionForSessionName(THIRD_PERSISTED_SESSION_NAME));
    }

    @Test
    public void anAlwaysPresentSessionStillQueuedForRestoreIsNotCreatedASecondTime() {
        activity.getPreferences().setAutosshCommand("ssh {name}");
        activity.getPreferences().setAlwaysNaSessionNames(
            SECOND_PERSISTED_SESSION_NAME + "\n" + THIRD_PERSISTED_SESSION_NAME);

        activity.getTermuxTerminalSessionClient().restorePersistedSessions();
        activity.getTermuxTerminalSessionClient().restoreAlwaysPresentSessions();

        idlePastTheRestorePacer();

        assertEquals("a persisted session that is queued to be restored on a later frame is already"
                + " accounted for, so the always-present pass that runs straight after the restore must not"
                + " build a second session under the same name; the live session names were "
                + liveSessionNames(),
            3, service.getTermuxSessionsSize());
        assertEquals("exactly one session may carry each name; the live session names were "
                + liveSessionNames(),
            1, liveSessionCountNamed(SECOND_PERSISTED_SESSION_NAME));
        assertEquals("exactly one session may carry each name; the live session names were "
                + liveSessionNames(),
            1, liveSessionCountNamed(THIRD_PERSISTED_SESSION_NAME));
    }

    private int liveSessionCountNamed(String sessionName) {
        int count = 0;
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            if (sessionName.equals(termuxSession.getTerminalSession().mSessionName)) count++;
        }
        return count;
    }

    private List<String> liveSessionNames() {
        List<String> names = new ArrayList<>();
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            names.add(termuxSession.getTerminalSession().mSessionName);
        }
        return names;
    }

    private void idlePastTheRestorePacer() {
        ShadowLooper.idleMainLooper(
            SessionReconnectPacer.MAIN_THREAD_FRAME_YIELD_INTERVAL_MILLIS * 10, TimeUnit.MILLISECONDS);
    }

    private String persistedSessionsJson() {
        return "["
            + persistedSessionJson(FIRST_PERSISTED_SESSION_NAME) + ","
            + persistedSessionJson(SECOND_PERSISTED_SESSION_NAME) + ","
            + persistedSessionJson(THIRD_PERSISTED_SESSION_NAME)
            + "]";
    }

    private String persistedSessionJson(String name) {
        return "{\"name\":\"" + name + "\",\"isFailSafe\":false}";
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
