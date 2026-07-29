package com.termux.app.terminal;

import android.app.AlertDialog;
import android.content.Context;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.TermuxSessionsListViewController.SessionAction;
import com.termux.app.terminal.session.TransientCommandSessionName;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowAlertDialog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class SessionActionChooserKillHostWiringTest {

    private static final String TARGET_SESSION_NAME = "host-session";

    private static final String KILL_TEMPLATE = "ssh host tmux kill-session -t {name}";

    private static final String COMPOSED_KILL_COMMAND = "ssh host tmux kill-session -t 'host-session'";

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
        preferences.setKillSessionCommand(KILL_TEMPLATE);
        preferences.setSessionDefinitionMaxSessions(10);

        set(activity, TermuxActivity.class, "mProperties",
            com.termux.shared.termux.settings.properties.TermuxAppSharedProperties.init(appContext));

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        TerminalView terminalView = new TerminalView(appContext, null);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        TermuxSession targetSession = liveSession(TARGET_SESSION_NAME);
        shellManager.mTermuxSessions.add(targetSession);
        terminalView.mTermSession = targetSession.getTerminalSession();
    }

    @Test
    public void killHostSessionActionSitsBetweenHideAndResetSession() {
        Assert.assertEquals(SessionAction.HIDE, SessionAction.atIndex(2));
        Assert.assertEquals(SessionAction.KILL_HOST_SESSION, SessionAction.atIndex(3));
        Assert.assertEquals(SessionAction.RESET_SESSION, SessionAction.atIndex(4));
    }

    @Test
    public void killHostSessionActionUsesKillHostSessionLabel() {
        Assert.assertEquals(R.string.action_kill_host_session,
            SessionAction.KILL_HOST_SESSION.labelResId);
    }

    @Test
    public void choosingKillHostSessionInTheChooserStartsTheHostKillCommandForTheSelectedSession() throws Exception {
        AlertDialog chooser = showSessionActionChooserForTargetSession();

        Shadows.shadowOf(chooser).clickOnItem(indexOfChooserItem(chooser, SessionAction.KILL_HOST_SESSION));

        TermuxSession killSession = service.getTermuxSessionForSessionName(
            TransientCommandSessionName.forKillOfSession(TARGET_SESSION_NAME));
        Assert.assertNotNull("choosing the kill action must reach the host kill path", killSession);
        Assert.assertTrue(Arrays.asList(killSession.getExecutionCommand().arguments)
            .contains(COMPOSED_KILL_COMMAND));
    }

    @Test
    public void choosingHideInTheChooserDoesNotStartTheHostKillCommand() throws Exception {
        AlertDialog chooser = showSessionActionChooserForTargetSession();

        Shadows.shadowOf(chooser).clickOnItem(indexOfChooserItem(chooser, SessionAction.HIDE));

        Assert.assertNull("only the kill action may reach the host kill path",
            service.getTermuxSessionForSessionName(
                TransientCommandSessionName.forKillOfSession(TARGET_SESSION_NAME)));
    }

    private int indexOfChooserItem(AlertDialog chooser, SessionAction action) {
        CharSequence[] items = Shadows.shadowOf(chooser).getItems();
        String label = activity.getString(action.labelResId);
        for (int index = 0; index < items.length; index++) {
            if (label.contentEquals(items[index])) return index;
        }
        throw new AssertionError("the chooser must offer " + action);
    }

    private AlertDialog showSessionActionChooserForTargetSession() throws Exception {
        TermuxSessionsListViewController controller =
            new TermuxSessionsListViewController(activity, new ArrayList<>(shellManager.mTermuxSessions));
        Method showChooser = TermuxSessionsListViewController.class.getDeclaredMethod(
            "showSessionActionChooser", TerminalSession.class);
        showChooser.setAccessible(true);
        showChooser.invoke(controller, targetTerminalSession());
        AlertDialog chooser = ShadowAlertDialog.getLatestAlertDialog();
        Assert.assertNotNull(chooser);
        return chooser;
    }

    private TerminalSession targetTerminalSession() {
        TermuxSession targetSession = service.getTermuxSessionForSessionName(TARGET_SESSION_NAME);
        Assert.assertNotNull(targetSession);
        return targetSession.getTerminalSession();
    }

    private TermuxSession liveSession(String name) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = name;
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
