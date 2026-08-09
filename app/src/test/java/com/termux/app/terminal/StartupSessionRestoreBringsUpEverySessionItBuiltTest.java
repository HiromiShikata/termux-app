package com.termux.app.terminal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.session.SessionReconnectPacer;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
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
public class StartupSessionRestoreBringsUpEverySessionItBuiltTest {

    private static final String DISPLAYED_PERSISTED_SESSION_NAME = "displayed-persisted-session";

    private static final String SECOND_PERSISTED_SESSION_NAME = "second-persisted-session";

    private static final String THIRD_PERSISTED_SESSION_NAME = "third-persisted-session";

    private static final int DRAIN_ATTEMPT_BOUND = 24;

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
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient",
            new TermuxTerminalSessionActivityClient(activity));
        service.setTermuxTerminalSessionClient(activity.getTermuxTerminalSessionClient());

        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        preferences.setSessionDefinitionMaxSessions(10);
        preferences.setPersistedSessions(persistedSessionsJson());

        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));
        set(activity, TermuxActivity.class, "mIsVisible", true);

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        TerminalView terminalView = new TerminalView(appContext, null);
        terminalView.setTextSize(12);
        terminalView.layout(0, 0, 1080, 1920);
        giveTheViewDeviceLikeFontMetrics(terminalView);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);
    }

    @Test
    public void aSessionBuiltOnALaterFrameIsBroughtUpLikeOneBuiltBeforeTheMainThreadWasReleased() {
        OffDeviceNativeSubprocessLibrary.tolerateItsAbsence(
            () -> activity.getTermuxTerminalSessionClient().restorePersistedSessions());
        OffDeviceNativeSubprocessLibrary.tolerateItsAbsence(activity::eagerLoadAllSessions);

        drainTheMainThreadPastBothPacers();

        assertNotNull("the caller runs the eager session load once, immediately after the restore returns,"
                + " and under the old uninterrupted loop that single run saw every restored session; a"
                + " session built on a later frame is invisible to it, so the restore has to bring its own"
                + " later sessions up as well, otherwise they hold no terminal emulator and render nothing"
                + " until the next foreground transition; the live session names were " + liveSessionNames(),
            emulatorOf(SECOND_PERSISTED_SESSION_NAME));
        assertNotNull("every session built on a later frame has to be brought up, not only the first of"
                + " them; the live session names were " + liveSessionNames(),
            emulatorOf(THIRD_PERSISTED_SESSION_NAME));
    }

    private Object emulatorOf(String sessionName) {
        TermuxSession termuxSession = service.getTermuxSessionForSessionName(sessionName);
        assertNotNull("the session must exist before its terminal emulator means anything; the live"
            + " session names were " + liveSessionNames(), termuxSession);
        return termuxSession.getTerminalSession().getEmulator();
    }

    private void drainTheMainThreadPastBothPacers() {
        for (int attempt = 0; attempt < DRAIN_ATTEMPT_BOUND; attempt++) {
            try {
                ShadowLooper.idleMainLooper(
                    SessionReconnectPacer.MAIN_THREAD_FRAME_YIELD_INTERVAL_MILLIS * 4,
                    TimeUnit.MILLISECONDS);
            } catch (LinkageError deviceOnlyNativeSubprocessLibraryAbsent) {
                OffDeviceNativeSubprocessLibrary.assertItIsTheOnlyAbsence(
                    deviceOnlyNativeSubprocessLibraryAbsent);
            }
        }
        assertTrue("the arrangement only means something once every persisted session has been built;"
                + " the live session names were " + liveSessionNames(),
            service.getTermuxSessionsSize() == 3);
    }

    private List<String> liveSessionNames() {
        List<String> names = new ArrayList<>();
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            names.add(termuxSession.getTerminalSession().mSessionName);
        }
        return names;
    }

    private void giveTheViewDeviceLikeFontMetrics(TerminalView terminalView) throws Exception {
        Field rendererField = TerminalView.class.getDeclaredField("mRenderer");
        rendererField.setAccessible(true);
        Object renderer = rendererField.get(terminalView);
        setField(renderer, "mFontWidth", 10.0f);
        setField(renderer, "mFontLineSpacing", 20);
        setField(renderer, "mFontLineSpacingAndAscent", 15);
    }

    private String persistedSessionsJson() {
        return "["
            + persistedSessionJson(DISPLAYED_PERSISTED_SESSION_NAME) + ","
            + persistedSessionJson(SECOND_PERSISTED_SESSION_NAME) + ","
            + persistedSessionJson(THIRD_PERSISTED_SESSION_NAME)
            + "]";
    }

    private String persistedSessionJson(String name) {
        return "{\"name\":\"" + name + "\",\"isFailSafe\":false}";
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
