package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Looper;
import android.view.View;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.session.SessionEagerLoader;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class HiddenSessionStartupProcessStartTest {

    private static final String DISPLAYED_SESSION = "displayed-session";
    private static final String SHOWN_SESSION = "shown-session";
    private static final String HIDDEN_SESSION = "hidden-session";

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TermuxAppSharedPreferences preferences;
    private TerminalView terminalView;

    private final List<String> startedSessionNames = new ArrayList<>();

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

        preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        terminalView = new TerminalView(appContext, null);
        terminalView.setTextSize(12);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        for (String sessionName : Arrays.asList(DISPLAYED_SESSION, SHOWN_SESSION, HIDDEN_SESSION)) {
            shellManager.mTermuxSessions.add(liveSession(sessionName));
        }
        terminalView.mTermSession = terminalSession(DISPLAYED_SESSION);

        preferences.setDisabledSessionNames(HIDDEN_SESSION);
        recordStartedSessionsInsteadOfSpawningProcesses();
    }

    @Test
    public void startupDoesNotStartTheProcessOfAHiddenSession() throws Exception {
        giveTheTerminalViewMeasurableDimensions();

        activity.eagerLoadAllSessions();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue("a session the user has not hidden must still be started at startup",
            startedSessionNames.contains(SHOWN_SESSION));
        assertFalse("a hidden session must not have its process started at startup, because its startup "
                + "command would otherwise begin doing the real work the user deliberately suppressed",
            startedSessionNames.contains(HIDDEN_SESSION));
    }

    @Test
    public void openingAHiddenSessionDisplaysItAndAttachesItToTheTerminalView() {
        activity.getTermuxTerminalSessionClient().setCurrentSession(terminalSession(HIDDEN_SESSION));
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertEquals("opening a hidden session must display it like any other session",
            HIDDEN_SESSION, activity.getCurrentSession().mSessionName);
        assertEquals("opening a hidden session must attach it to the terminal view, because the hidden set "
                + "gates only the automatic startup pass",
            HIDDEN_SESSION, terminalView.getCurrentSession().mSessionName);
    }

    @Test
    public void openingAHiddenSessionStartsItWithTheDimensionsOfTheLaidOutTerminalView() throws Exception {
        giveTheTerminalViewMeasurableDimensions();
        int[] laidOutDimensions = terminalView.computeSessionEmulatorDimensions();
        TerminalSession hiddenSession = terminalSession(HIDDEN_SESSION);
        assertNull("the hidden session must still be unstarted before the user opens it, otherwise this "
                + "test cannot tell the opening apart from the startup pass", hiddenSession.getEmulator());

        Throwable deviceOnlySubprocessFailure = openUntilTheDeviceOnlySubprocessCall(hiddenSession);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertNotNull("opening a hidden session must run the session start path as far as the native "
                + "subprocess creation, which is the last step a Java virtual machine can reach",
            deviceOnlySubprocessFailure);
        TerminalEmulator startedEmulator = hiddenSession.getEmulator();
        assertNotNull("opening a hidden session must reach the session size update that creates the "
                + "emulator; attachment on its own leaves the emulator null", startedEmulator);
        assertEquals("the session must be started with the columns computed from the laid out terminal "
                + "view, not left unstarted by the zero dimension guard",
            laidOutDimensions[0], startedEmulator.mColumns);
        assertEquals("the session must be started with the rows computed from the laid out terminal "
                + "view, not left unstarted by the zero dimension guard",
            laidOutDimensions[1], startedEmulator.mRows);
    }

    @Test
    public void startupStartsOnlyTheVisibleSessionsInARealisticallySizedSessionPopulation() throws Exception {
        List<String> visibleSessionNames = new ArrayList<>();
        List<String> hiddenSessionNames = new ArrayList<>();
        shellManager.mTermuxSessions.clear();
        startedSessionNames.clear();
        for (int sessionNumber = 0; sessionNumber < 16; sessionNumber++) {
            String sessionName = "visible-" + sessionNumber;
            visibleSessionNames.add(sessionName);
            shellManager.mTermuxSessions.add(liveSession(sessionName));
        }
        for (int sessionNumber = 0; sessionNumber < 15; sessionNumber++) {
            String sessionName = "hidden-" + sessionNumber;
            hiddenSessionNames.add(sessionName);
            shellManager.mTermuxSessions.add(liveSession(sessionName));
        }
        preferences.setDisabledSessionNames(String.join("\n", hiddenSessionNames));
        String displayedSessionName = visibleSessionNames.get(0);
        terminalView.mTermSession = terminalSession(displayedSessionName);

        assertEquals("the fixture must hold the reported population of 31 rows",
            31, service.getTermuxSessionsSize());

        giveTheTerminalViewMeasurableDimensions();
        activity.eagerLoadAllSessions();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        List<String> expectedStartedSessionNames = new ArrayList<>(visibleSessionNames);
        expectedStartedSessionNames.remove(displayedSessionName);
        assertEquals("the startup pass must start every visible session except the displayed one, which "
                + "is started by being attached to the terminal view, and no hidden session at all",
            expectedStartedSessionNames, startedSessionNames);
        for (String hiddenSessionName : hiddenSessionNames) {
            assertFalse("no hidden session may be started at startup, however many of them there are",
                startedSessionNames.contains(hiddenSessionName));
        }
    }

    private Throwable openUntilTheDeviceOnlySubprocessCall(TerminalSession session) {
        try {
            activity.getTermuxTerminalSessionClient().setCurrentSession(session);
            return null;
        } catch (Throwable deviceOnlySubprocessFailure) {
            return deviceOnlySubprocessFailure;
        }
    }

    private void giveTheTerminalViewMeasurableDimensions() throws Exception {
        set(terminalView, View.class, "mLeft", 0);
        set(terminalView, View.class, "mTop", 0);
        set(terminalView, View.class, "mRight", 800);
        set(terminalView, View.class, "mBottom", 600);

        Field rendererField = TerminalView.class.getDeclaredField("mRenderer");
        rendererField.setAccessible(true);
        Object renderer = rendererField.get(terminalView);
        set(renderer, renderer.getClass(), "mFontWidth", 10f);
        set(renderer, renderer.getClass(), "mFontLineSpacing", 20);
        set(renderer, renderer.getClass(), "mFontLineSpacingAndAscent", 24);

        assertTrue("the terminal view must be able to compute emulator dimensions for the eager load pass",
            terminalView.isLaidOutForSizeComputation());
        assertNotNull(terminalView.computeSessionEmulatorDimensions());
    }

    private void recordStartedSessionsInsteadOfSpawningProcesses() throws Exception {
        SessionEagerLoader productionEagerLoader = get(activity, "mSessionEagerLoader");
        SessionEagerLoader.SessionListSupplier productionSupplier =
            get(productionEagerLoader, "sessionListSupplier");
        SessionEagerLoader.SessionInitializationState productionInitializationState =
            get(productionEagerLoader, "sessionInitializationState");
        set(activity, TermuxActivity.class, "mSessionEagerLoader",
            new SessionEagerLoader(productionSupplier, productionInitializationState,
                session -> startedSessionNames.add(session.mSessionName)));
    }

    private TerminalSession terminalSession(String sessionName) {
        TermuxSession termuxSession = service.getTermuxSessionForSessionName(sessionName);
        assertNotNull(termuxSession);
        return termuxSession.getTerminalSession();
    }

    private TermuxSession liveSession(String sessionName) throws Exception {
        TerminalSession terminalSession = new TerminalSession("/system/bin/sh", "/", new String[0],
            new String[0], null, activity.getTermuxTerminalSessionClient());
        terminalSession.mSessionName = sessionName;
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(terminalSession, new ExecutionCommand(), null, false);
    }

    @SuppressWarnings("unchecked")
    private <T> T get(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
