package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.terminal.TerminalSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
public class KillHostSessionDoesNotWriteToAttachedSessionTest {

    private TermuxActivity activity;
    private TerminalSession attachedSession;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context appContext = RuntimeEnvironment.getApplication();

        TermuxService service = Robolectric.buildService(TermuxService.class).get();
        TermuxShellManager shellManager = new TermuxShellManager(appContext);
        set(service, TermuxService.class, "mShellManager", shellManager);
        set(service, TermuxService.class, "mProperties",
            com.termux.shared.termux.settings.properties.TermuxAppSharedProperties.init(appContext));

        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient",
            new TermuxTerminalSessionActivityClient(activity));
        service.setTermuxTerminalSessionClient(activity.getTermuxTerminalSessionClient());

        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);

        attachedSession = new TerminalSession(null, null, null, null, null, null);
        attachedSession.mSessionName = "attached-host-session";
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(attachedSession, 12345);
    }

    @Test
    public void killHostSessionMustNotWriteKillCommandBytesIntoCurrentlyAttachedSession() throws Exception {
        assertEquals("test setup sanity check: no bytes must be queued before the action runs",
            0, storedByteCountInAttachedSessionIoQueue());

        activity.getTermuxTerminalSessionClient().killHostSession(attachedSession);

        assertEquals("Kill host session must route the composed command out-of-band as a new short-lived "
                + "session instead of writing it into the pty of the currently attached session",
            0, storedByteCountInAttachedSessionIoQueue());
    }

    private int storedByteCountInAttachedSessionIoQueue() throws Exception {
        Field ioQueueField = TerminalSession.class.getDeclaredField("mTerminalToProcessIOQueue");
        ioQueueField.setAccessible(true);
        Object ioQueue = ioQueueField.get(attachedSession);
        assertNotNull(ioQueue);
        Field storedBytesField = ioQueue.getClass().getDeclaredField("mStoredBytes");
        storedBytesField.setAccessible(true);
        return storedBytesField.getInt(ioQueue);
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
