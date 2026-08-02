package com.termux.app.terminal.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
public class ToolbarSendUsesBracketedPasteSoTheEnterIsNotSwallowedTest {

    private static final int LIVE_SHELL_PROCESS_ID = 4343;

    private static final String BRACKETED_PASTE_REQUEST_SEQUENCE = "\033[?2004h";

    private static final String PASTE_START_MARKER = "\033[200~";

    private static final String PASTE_END_MARKER = "\033[201~";

    private static final String OWNER_MESSAGE =
        "please look at the failing build and tell me which check went red";

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
    }

    @Test
    public void sendingWhileTheForegroundProgramAsksForBracketedPasteMarksWhereTheOwnerTextEnds()
            throws Exception {
        askForBracketedPaste();
        toolbarTextInput.setText(OWNER_MESSAGE);
        drainBytesDeliveredToSession();

        toolbarTextInput.onEditorAction(EditorInfo.IME_ACTION_SEND);

        assertEquals("a foreground program that asked for bracketed paste decides where pasted content"
                + " ends from the end marker; without it the program reads the owner text and the enter"
                + " sequence as one pasted chunk, keeps the text sitting in its input box and never"
                + " submits it, which is why the owner has to press send a second time",
            PASTE_START_MARKER + OWNER_MESSAGE + PASTE_END_MARKER + expectedEnterSequence(),
            bytesDeliveredToSession());
    }

    @Test
    public void sendingWhileTheForegroundProgramDidNotAskForBracketedPasteDeliversThePlainOwnerText()
            throws Exception {
        toolbarTextInput.setText(OWNER_MESSAGE);
        drainBytesDeliveredToSession();

        toolbarTextInput.onEditorAction(EditorInfo.IME_ACTION_SEND);

        assertEquals("a program that never asked for bracketed paste must keep receiving the owner text"
                + " exactly as before, so a shell prompt does not start seeing escape sequences it would"
                + " print as literal characters",
            OWNER_MESSAGE + expectedEnterSequence(), bytesDeliveredToSession());
    }

    private void askForBracketedPaste() throws Exception {
        TerminalEmulator emulator = currentSession.getEmulator();
        assertNotNull("the session under test must hold an emulator, otherwise it could not record that"
            + " the foreground program asked for bracketed paste", emulator);
        byte[] request = BRACKETED_PASTE_REQUEST_SEQUENCE.getBytes(StandardCharsets.UTF_8);
        assertFalse("the emulator must start without bracketed paste, otherwise this test could not tell"
            + " that the foreground program's request is what turned it on", bracketedPasteIsOn(emulator));
        emulator.append(request, request.length);
        assertTrue("the emulator must record the foreground program's bracketed paste request, otherwise"
            + " this test would assert against a mode the terminal is not actually in",
            bracketedPasteIsOn(emulator));
    }

    private boolean bracketedPasteIsOn(TerminalEmulator emulator) throws Exception {
        emulator.paste("x");
        return bytesDeliveredToSession().startsWith(PASTE_START_MARKER);
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
        session.mSessionName = "toolbar-bracketed-paste-session";
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
