package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import android.content.Context;
import android.view.View;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.JniSpawnCounter;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class BackgroundDeadSessionReconnectTest {

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TermuxAppSharedPreferences preferences;
    private TermuxTerminalSessionActivityClient client;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context appContext = RuntimeEnvironment.getApplication();

        service = Robolectric.buildService(TermuxService.class).get();
        shellManager = new TermuxShellManager(appContext);
        set(service, TermuxService.class, "mShellManager", shellManager);
        set(service, TermuxService.class, "mProperties",
            com.termux.shared.termux.settings.properties.TermuxAppSharedProperties.init(appContext));

        client = new TermuxTerminalSessionActivityClient(activity);
        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient", client);
        service.setTermuxTerminalSessionClient(activity.getTermuxTerminalSessionClient());

        preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        preferences.setAutosshCommand("ssh {name}");

        set(activity, TermuxActivity.class, "mProperties",
            com.termux.shared.termux.settings.properties.TermuxAppSharedProperties.init(appContext));
        set(activity, TermuxActivity.class, "mIsVisible", true);

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        TerminalView terminalView = new TerminalView(appContext, null);
        terminalView.setTextSize(24);
        terminalView.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY));
        terminalView.layout(0, 0, 1080, 1920);
        forceNonZeroFontMetrics(terminalView);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);
    }

    private void forceNonZeroFontMetrics(TerminalView terminalView) throws Exception {
        Object renderer = terminalView.mRenderer;
        assertNotNull("terminal view must have a renderer after a real layout pass", renderer);
        setDeclared(renderer, "mFontWidth", 10f);
        setDeclared(renderer, "mFontLineSpacing", 20);
        setDeclared(renderer, "mFontLineSpacingAndAscent", 15);
    }

    private void setDeclared(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void reconnectingAHiddenBackgroundDeadSessionCreatesNothingAtAll() throws Exception {
        TermuxSession displayedDeadSession = deadSession("displayed-session");
        TermuxSession hiddenBackgroundDeadSession = deadSession("hidden-background-session");
        shellManager.mTermuxSessions.add(displayedDeadSession);
        shellManager.mTermuxSessions.add(hiddenBackgroundDeadSession);
        activity.getTerminalView().mTermSession = displayedDeadSession.getTerminalSession();
        preferences.setDisabledSessionNames(TermuxAppSharedPreferences.serializeDisabledSessionNames(
            Collections.singleton("hidden-background-session")));
        int liveSessionCountBeforeTheReconnect = shellManager.mTermuxSessions.size();

        TerminalSession reconnectedBackgroundSession =
            invokeReconnect(hiddenBackgroundDeadSession.getTerminalSession());

        assertNull(
            "a hidden session holds no runtime resource at all, so a reconnect must not replace it with a "
                + "live session. This assertion previously required the opposite, that a hidden session is "
                + "reconnected with its emulator left uninitialized, and that contract is superseded: a "
                + "session that is never reconnected cannot spawn a subprocess on the main thread either, "
                + "so the freeze this test was written to prevent is prevented more strongly than before",
            reconnectedBackgroundSession);
        assertEquals(
            "a refused reconnect must add no live session object to the service session list, otherwise "
                + "the hidden session still occupies a slot in the session cap and is still walked by "
                + "every selection that reads that list",
            liveSessionCountBeforeTheReconnect, shellManager.mTermuxSessions.size());
    }

    @Test
    public void reconnectingShownBackgroundDeadSessionStartsTheReplacementSessionProcess()
            throws Exception {
        TermuxSession displayedDeadSession = deadSession("displayed-session");
        TermuxSession shownBackgroundDeadSession = deadSession("shown-background-session");
        shellManager.mTermuxSessions.add(displayedDeadSession);
        shellManager.mTermuxSessions.add(shownBackgroundDeadSession);
        activity.getTerminalView().mTermSession = displayedDeadSession.getTerminalSession();

        try {
            invokeReconnect(shownBackgroundDeadSession.getTerminalSession());
            fail("a reconnected background session that is displayed (not hidden, project not collapsed) "
                + "must have its process started, otherwise it writes no statusline, the reconnecting row "
                + "is never cleared and the row settles on the reload button; starting it reaches "
                + "TerminalSession.updateSize() -> JNI.createSubprocess() and fails to load the native "
                + "library that only exists on a real device, and that failure is the proof the process "
                + "start ran");
        } catch (InvocationTargetException expected) {
            assertContainsJniEvidence(causeChainDescription(expected.getCause()));
        }
    }

    private String causeChainDescription(Throwable throwable) {
        StringBuilder description = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            description.append(current.getClass().getName()).append('\n');
            for (StackTraceElement element : current.getStackTrace()) {
                description.append("    at ").append(element).append('\n');
            }
            current = current.getCause();
        }
        return description.toString();
    }

    private void assertContainsJniEvidence(String causeChainDescription) {
        if (!causeChainDescription.contains("com.termux.terminal.JNI")) {
            fail("expected the failure to originate from com.termux.terminal.JNI (proving eager "
                + "initialization attempted a real subprocess spawn), but got:\n" + causeChainDescription);
        }
    }

    private TerminalSession invokeReconnect(TerminalSession deadSession) throws Exception {
        Method method = TermuxTerminalSessionActivityClient.class.getDeclaredMethod(
            "reconnectDeadSessionPreservingDisplayedSession", TerminalSession.class);
        method.setAccessible(true);
        return (TerminalSession) method.invoke(client, deadSession);
    }

    private TermuxSession deadSession(String name) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = name;
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(terminalSession, -1);
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class, boolean.class);
        constructor.setAccessible(true);
        TermuxSession termuxSession = constructor.newInstance(terminalSession, new ExecutionCommand(), null, false);
        assertNotNull(termuxSession.getTerminalSession());
        return termuxSession;
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
