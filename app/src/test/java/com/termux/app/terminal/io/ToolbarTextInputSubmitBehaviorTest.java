package com.termux.app.terminal.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

@RunWith(RobolectricTestRunner.class)
public class ToolbarTextInputSubmitBehaviorTest {

    private static final int LIVE_SHELL_PROCESS_ID = 4545;
    private static final String OWNER_TEXT = "echo toolbar text";

    private TermuxActivity activity;
    private TerminalSession currentSession;
    private EditText toolbarTextInput;
    private TerminalToolbarViewPager.PageAdapter pageAdapter;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context appContext = RuntimeEnvironment.getApplication();
        TermuxService service = Robolectric.buildService(TermuxService.class).get();
        set(service, TermuxService.class, "mShellManager", new TermuxShellManager(appContext));
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(appContext));
        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient",
            new TermuxTerminalSessionActivityClient(activity));
        service.setTermuxTerminalSessionClient(activity.getTermuxTerminalSessionClient());
        set(activity, TermuxActivity.class, "mPreferences",
            TermuxAppSharedPreferences.build(appContext, true));
        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));
        currentSession = liveSessionHoldingAnEmulator();
        TerminalView terminalView = new TerminalView(appContext, null);
        terminalView.mTermSession = currentSession;
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);
        View textInputRow = LayoutInflater.from(activity)
            .inflate(R.layout.view_terminal_toolbar_text_input, null);
        pageAdapter = new TerminalToolbarViewPager.PageAdapter(activity, null);
        pageAdapter.setupTextInputRow(textInputRow);
        toolbarTextInput = textInputRow.findViewById(R.id.terminal_toolbar_text_input);
        assertNotNull(toolbarTextInput);
    }

    @Test
    public void submittingNonEmptyToolbarTextWritesExactTextAndClearsFieldAndSessionDraft() throws Exception {
        toolbarTextInput.setText(OWNER_TEXT);
        pageAdapter.saveTextInputForSession(currentSession);
        drainBytesDeliveredToSession();

        toolbarTextInput.onEditorAction(EditorInfo.IME_ACTION_SEND);

        assertEquals(OWNER_TEXT + expectedEnterSequence(), bytesDeliveredToSession());
        assertEquals("", toolbarTextInput.getText().toString());
        pageAdapter.restoreTextInputForSession(currentSession);
        assertEquals("", toolbarTextInput.getText().toString());
    }

    @Test
    public void submittingEmptyToolbarTextWritesNoOwnerContent() throws Exception {
        toolbarTextInput.setText("");
        drainBytesDeliveredToSession();

        toolbarTextInput.onEditorAction(EditorInfo.IME_ACTION_SEND);

        assertEquals(expectedEnterSequence(), bytesDeliveredToSession());
    }

    private String expectedEnterSequence() {
        TerminalEmulator emulator = currentSession.getEmulator();
        assertNotNull(emulator);
        return TerminalEnterKeyEncoder.enterSequence(
            emulator.isCursorKeysApplicationMode(), emulator.isKeypadApplicationMode());
    }

    private TerminalSession liveSessionHoldingAnEmulator() throws Exception {
        TerminalSession session = new TerminalSession("/system/bin/sh", "/", new String[0],
            new String[0], 2000, activity.getTermuxTerminalSessionClient());
        session.mSessionName = "toolbar-submit-behavior-session";
        try {
            session.initializeEmulator(80, 24, 10, 20);
        } catch (LinkageError error) {
            Throwable rootCause = error;
            while (rootCause.getCause() != null) rootCause = rootCause.getCause();
            assertTrue(rootCause.toString(), rootCause.toString().contains("UnsatisfiedLinkError")
                || rootCause.toString().contains("com.termux.terminal.JNI"));
        }
        Field shellProcessId = TerminalSession.class.getDeclaredField("mShellPid");
        shellProcessId.setAccessible(true);
        shellProcessId.setInt(session, LIVE_SHELL_PROCESS_ID);
        assertTrue(session.isRunning());
        return session;
    }

    private String bytesDeliveredToSession() throws Exception {
        byte[] buffer = new byte[4096];
        int readByteCount = readFromSessionIoQueue(buffer);
        return readByteCount <= 0 ? "" : new String(buffer, 0, readByteCount, StandardCharsets.UTF_8);
    }

    private void drainBytesDeliveredToSession() throws Exception {
        readFromSessionIoQueue(new byte[4096]);
    }

    private int readFromSessionIoQueue(byte[] buffer) throws Exception {
        Field ioQueueField = TerminalSession.class.getDeclaredField("mTerminalToProcessIOQueue");
        ioQueueField.setAccessible(true);
        Object ioQueue = ioQueueField.get(currentSession);
        Method read = ioQueue.getClass().getDeclaredMethod("read", byte[].class, boolean.class);
        read.setAccessible(true);
        return (Integer) read.invoke(ioQueue, buffer, false);
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value)
            throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
