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
public class RepeatSendTapsOnASilentSessionWriteOneEnterTest {

    private static final int LIVE_SHELL_PROCESS_ID = 4344;

    private static final String SESSION_NAME = "repeat-send-tap-session";

    private static final String OWNER_MESSAGE = "which check went red";

    private static final long CLEARLY_LATER_MILLIS = 60_000L;

    private TermuxActivity activity;
    private TerminalSession currentSession;
    private EditText toolbarTextInput;

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
        new TerminalToolbarViewPager.PageAdapter(activity, null).setupTextInputRow(textInputRow);
        toolbarTextInput = textInputRow.findViewById(R.id.terminal_toolbar_text_input);
        assertNotNull(toolbarTextInput);
        assertNotNull("the session activity store must be reachable, otherwise this test could not tell"
            + " the difference between a session that answered and one that stayed silent",
            activity.getSessionNewActivityStore());
    }

    @Test
    public void repeatTapsOnASessionThatStaysSilentWriteOnlyTheFirstEnter() throws Exception {
        drainBytesDeliveredToSession();

        tapSendWithAnEmptyField();
        tapSendWithAnEmptyField();
        tapSendWithAnEmptyField();

        assertEquals("a session that produced no output since the last bare enter is not consuming input,"
                + " so every further bare enter can only sit in the terminal input queue until something"
                + " reads it later and delivers it as a blank line the owner never typed",
            expectedEnterSequence(), bytesDeliveredToSession());
    }

    @Test
    public void aBareEnterIsWrittenAgainOnceTheSessionHasAnswered() throws Exception {
        drainBytesDeliveredToSession();

        tapSendWithAnEmptyField();
        recordThatTheSessionAnswered();
        tapSendWithAnEmptyField();

        assertEquals("a session that produced output after the previous bare enter is consuming input, so"
                + " the owner must still be able to confirm one prompt after another",
            expectedEnterSequence() + expectedEnterSequence(), bytesDeliveredToSession());
    }

    @Test
    public void repeatTapsThatCarryOwnerTextAreNeverSuppressed() throws Exception {
        drainBytesDeliveredToSession();

        toolbarTextInput.setText(OWNER_MESSAGE);
        toolbarTextInput.onEditorAction(EditorInfo.IME_ACTION_SEND);
        toolbarTextInput.setText(OWNER_MESSAGE);
        toolbarTextInput.onEditorAction(EditorInfo.IME_ACTION_SEND);

        assertEquals("text the owner actually typed must always be submitted, however silent the session"
                + " has been, otherwise a message would be left sitting unsent in the foreground program",
            OWNER_MESSAGE + expectedEnterSequence() + OWNER_MESSAGE + expectedEnterSequence(),
            bytesDeliveredToSession());
    }

    private void tapSendWithAnEmptyField() {
        toolbarTextInput.setText("");
        toolbarTextInput.onEditorAction(EditorInfo.IME_ACTION_SEND);
    }

    private void recordThatTheSessionAnswered() {
        activity.getSessionNewActivityStore().recordOutputActivity(SESSION_NAME,
            System.currentTimeMillis() + CLEARLY_LATER_MILLIS);
    }

    private String expectedEnterSequence() {
        TerminalEmulator emulator = currentSession.getEmulator();
        assertNotNull("the session under test must hold an emulator, otherwise the enter sequence this "
            + "test expects would not be the one the terminal would really receive", emulator);
        return TerminalEnterKeyEncoder.enterSequence(
            emulator.isCursorKeysApplicationMode(), emulator.isKeypadApplicationMode());
    }

    private TerminalSession liveSessionHoldingAnEmulator() throws Exception {
        TerminalSession session = new TerminalSession("/system/bin/sh", "/", new String[0],
            new String[0], 2000, activity.getTermuxTerminalSessionClient());
        session.mSessionName = SESSION_NAME;
        try {
            session.initializeEmulator(80, 24, 10, 20);
        } catch (LinkageError deviceOnlyNativeSubprocessLibraryIsAbsent) {
            assertItIsOnlyTheAbsentNativeSubprocessLibrary(deviceOnlyNativeSubprocessLibraryIsAbsent);
        }
        Field shellProcessId = TerminalSession.class.getDeclaredField("mShellPid");
        shellProcessId.setAccessible(true);
        shellProcessId.setInt(session, LIVE_SHELL_PROCESS_ID);
        assertTrue("the session under test must look running, otherwise nothing would ever be written "
            + "to it and this test would pass vacuously", session.isRunning());
        return session;
    }

    private void assertItIsOnlyTheAbsentNativeSubprocessLibrary(LinkageError error) {
        Throwable rootCause = error;
        while (rootCause.getCause() != null) rootCause = rootCause.getCause();
        String absorbedFailure = rootCause.getClass().getName() + ": " + rootCause.getMessage();
        assertTrue("a Java virtual machine run can only absorb the absence of the device-only native "
                + "subprocess library that TerminalSession.initializeEmulator loads after it has already "
                + "constructed the terminal emulator; every other failure is a real one and must surface "
                + "instead of being discarded, yet this run absorbed " + absorbedFailure,
            absorbedFailure.contains("UnsatisfiedLinkError")
                || absorbedFailure.contains("com.termux.terminal.JNI"));
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
        assertNotNull(ioQueue);
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
