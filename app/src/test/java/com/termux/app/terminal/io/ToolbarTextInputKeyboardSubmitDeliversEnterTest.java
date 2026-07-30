package com.termux.app.terminal.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;

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
public class ToolbarTextInputKeyboardSubmitDeliversEnterTest {

    private static final int LIVE_SHELL_PROCESS_ID = 4242;

    private static final String TYPED_TEXT = "echo toolbar-keyboard-submit";

    private TermuxActivity activity;
    private TerminalSession currentSession;
    private EditText toolbarTextInput;
    private ImageButton toolbarSendButton;

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
        toolbarSendButton = textInputRow.findViewById(R.id.terminal_toolbar_enter_button);
        assertNotNull(toolbarSendButton);
    }

    @Test
    public void imeSendActionDeliversTheEnterSequenceSoTheTypedTextIsSubmittedInsteadOfLeftAtThePrompt()
            throws Exception {
        toolbarTextInput.setText(TYPED_TEXT);
        drainBytesDeliveredToSession();

        toolbarTextInput.onEditorAction(EditorInfo.IME_ACTION_SEND);

        assertEquals("confirming non-empty toolbar text with the keyboard send action must deliver the "
                + "typed text followed by the enter sequence, so the foreground program actually runs it "
                + "instead of leaving it sitting at its prompt",
            TYPED_TEXT + expectedEnterSequence(), bytesDeliveredToSession());
    }

    @Test
    public void enterKeyDeliversTheEnterSequenceSoTheTypedTextIsSubmittedInsteadOfLeftAtThePrompt()
            throws Exception {
        toolbarTextInput.setText(TYPED_TEXT);
        drainBytesDeliveredToSession();

        boolean enterKeyHandled = toolbarTextInput.dispatchKeyEvent(
            new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));

        assertTrue("the toolbar text input must handle an unmodified enter key press itself",
            enterKeyHandled);
        assertEquals("confirming non-empty toolbar text with an unmodified enter key press must deliver "
                + "the typed text followed by the enter sequence, so the foreground program actually runs "
                + "it instead of leaving it sitting at its prompt",
            TYPED_TEXT + expectedEnterSequence(), bytesDeliveredToSession());
    }

    @Test
    public void imeSendActionOnAnEmptyFieldStillDeliversABareEnterSequence() throws Exception {
        toolbarTextInput.setText("");
        drainBytesDeliveredToSession();

        toolbarTextInput.onEditorAction(EditorInfo.IME_ACTION_SEND);

        assertEquals("confirming an empty toolbar field with the keyboard send action must still deliver a "
                + "bare enter sequence, so the foreground program receives the blank line it received "
                + "before the keyboard paths started going through the shared submit",
            expectedEnterSequence(), bytesDeliveredToSession());
    }

    @Test
    public void oneSendButtonClickDeliversExactlyOneEnterSequence() throws Exception {
        drainBytesDeliveredToSession();

        toolbarSendButton.performClick();

        assertEquals("a single send button click must deliver exactly one enter sequence, otherwise "
                + "routing the keyboard paths through the same submit would have made the button submit "
                + "twice for one tap",
            expectedEnterSequence(), bytesDeliveredToSession());
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
        session.mSessionName = "toolbar-keyboard-submit-session";
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
