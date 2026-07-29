package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
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
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

@RunWith(RobolectricTestRunner.class)
public class UnhiddenSessionStartsItsShellProcessTest {

    private static final String CURRENT_SESSION_NAME = "session-current";

    private static final String HIDDEN_SESSION_NAME = "session-hidden";

    private static final int TERMINAL_COLUMNS = 80;

    private static final int TERMINAL_ROWS = 40;

    private static final int TERMINAL_CELL_WIDTH_PIXELS = 12;

    private static final int TERMINAL_CELL_HEIGHT_PIXELS = 24;

    private static final Integer TRANSCRIPT_ROWS = 2000;

    private static final int UNREACHABLE_SHELL_PROCESS_ID = 2000000000;

    private static final int MAIN_THREAD_TASK_DRAIN_LIMIT = 500;

    private static final String KEYSTROKE_PROBE = "unhidden-keystroke-probe";

    private static final String SHELL_OUTPUT_PROBE = "unhidden-shell-output-probe";

    private static final String TERMINAL_TO_PROCESS_QUEUE = "mTerminalToProcessIOQueue";

    private static final String PROCESS_TO_TERMINAL_QUEUE = "mProcessToTerminalIOQueue";

    private static final int QUEUE_CLOSED = -1;

    private static final int PROBE_READ_BUFFER_BYTES = 4096;

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TermuxAppSharedPreferences preferences;
    private TermuxSessionsListViewController listViewController;

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
        preferences.setAutosshCommand("ssh {name}");

        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));
        set(activity, TermuxActivity.class, "mIsVisible", true);

        activity.setContentView(bottomSheetStubContentView(appContext));

        TerminalView terminalView = new TerminalView(appContext, null);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);
        layOutTerminalViewForEmulatorSizeComputation();

        TermuxSession currentSession = liveSessionHoldingAnEmulator(CURRENT_SESSION_NAME);
        shellManager.mTermuxSessions.add(currentSession);
        terminalView.mTermSession = currentSession.getTerminalSession();

        listViewController = new TermuxSessionsListViewController(activity, service.getTermuxSessions());
        set(activity, TermuxActivity.class, "mTermuxSessionListViewController", listViewController);
    }

    @Test
    public void unhidingASessionDrivesItsSizeUpdateWithNonZeroDimensions() throws Exception {
        shellManager.mTermuxSessions.add(liveSessionHoldingAnEmulator(HIDDEN_SESSION_NAME));
        hideSessionThroughProductionEntryPoint(HIDDEN_SESSION_NAME);
        assertNull("the hide must leave no live session object behind, otherwise this test would be "
                + "measuring the session that was alive before the hide rather than the recreated one",
            service.getTermuxSessionForSessionName(HIDDEN_SESSION_NAME));

        unhideSessionThroughProductionEntryPoint(HIDDEN_SESSION_NAME);
        drainMainThreadTasksIgnoringMissingNativeLibrary();

        TermuxSession recreatedSession = service.getTermuxSessionForSessionName(HIDDEN_SESSION_NAME);
        assertNotNull("unhiding must recreate the session, so a live session object for it must exist "
            + "again after the unhide", recreatedSession);
        TerminalEmulator recreatedEmulator = recreatedSession.getTerminalSession().getEmulator();
        assertNotNull("a hidden session holds no shell process, no terminal emulator and no live "
                + "session object at all, so unhiding must drive the recreated session through the "
                + "session size update that constructs its terminal emulator and then forks its shell "
                + "process; without that the owner unhides a session and is left with a dead terminal",
            recreatedEmulator);
        assertTrue("the session size update must be driven with the real laid-out terminal dimensions, "
                + "because a zero-dimension update returns at its guard without ever reaching the "
                + "session; the recreated emulator was sized " + recreatedEmulator.mColumns + "x"
                + recreatedEmulator.mRows,
            recreatedEmulator.mColumns > 0 && recreatedEmulator.mRows > 0);
    }

    @Test
    public void unhidingASessionRestoresAChannelThatCarriesBothKeystrokesAndShellOutput()
            throws Exception {
        shellManager.mTermuxSessions.add(liveSessionHoldingAnEmulator(HIDDEN_SESSION_NAME));
        TerminalSession releasedSession =
            service.getTermuxSessionForSessionName(HIDDEN_SESSION_NAME).getTerminalSession();

        hideSessionThroughProductionEntryPoint(HIDDEN_SESSION_NAME);

        assertFalse("hiding closes both byte queues of the released session, and a closed queue never "
                + "becomes open again, so a session revived from the released object would spawn a "
                + "shell whose keystrokes are dropped and whose output is silently discarded; this "
                + "assertion pins that the released object really is unusable, which is what makes the "
                + "assertions below about the recreated object meaningful rather than vacuous",
            queueAcceptsBytes(queueOf(releasedSession, TERMINAL_TO_PROCESS_QUEUE), KEYSTROKE_PROBE));

        unhideSessionThroughProductionEntryPoint(HIDDEN_SESSION_NAME);
        drainMainThreadTasksIgnoringMissingNativeLibrary();

        TermuxSession recreatedTermuxSession = service.getTermuxSessionForSessionName(HIDDEN_SESSION_NAME);
        assertNotNull("unhiding must recreate the session, so a live session object for it must exist "
            + "again after the unhide", recreatedTermuxSession);
        TerminalSession recreatedSession = recreatedTermuxSession.getTerminalSession();
        assertNotSame("unhiding must build a new session object rather than revive the released one, "
                + "because the released one's byte queues can never be reopened",
            releasedSession, recreatedSession);

        Object keystrokeQueue = queueOf(recreatedSession, TERMINAL_TO_PROCESS_QUEUE);
        assertTrue("a keystroke typed into the unhidden session must be accepted by the queue the "
                + "writer thread drains into the shell process; a permanently closed queue accepts "
                + "nothing and reports false while the terminal still looks alive",
            queueAcceptsBytes(keystrokeQueue, KEYSTROKE_PROBE));
        assertEquals("the keystroke must come back off the queue byte for byte, because that is "
                + "exactly what the writer thread hands to the shell process",
            KEYSTROKE_PROBE, drainQueueAsText(keystrokeQueue));

        Object shellOutputQueue = queueOf(recreatedSession, PROCESS_TO_TERMINAL_QUEUE);
        assertTrue("shell output arriving for the unhidden session must be accepted by the queue the "
                + "reader thread fills; a permanently closed queue discards it and reports false",
            queueAcceptsBytes(shellOutputQueue, SHELL_OUTPUT_PROBE));

        deliverPendingShellOutputOnMainThread(recreatedSession);

        TerminalEmulator recreatedEmulator = recreatedSession.getEmulator();
        assertNotNull("the recreated session must hold a terminal emulator for its output to land in",
            recreatedEmulator);
        String transcript = recreatedEmulator.getScreen().getTranscriptText();
        assertTrue("the accepted output must actually reach the recreated terminal emulator screen, "
                + "otherwise the owner unhides a session that looks alive and swallows every byte the "
                + "shell writes; the transcript was " + transcript.trim(),
            transcript.contains(SHELL_OUTPUT_PROBE));
    }

    @Test
    public void hidingASessionLeavesTheDisplayedSessionChannelUntouched() throws Exception {
        shellManager.mTermuxSessions.add(liveSessionHoldingAnEmulator(HIDDEN_SESSION_NAME));
        TerminalSession displayedSession =
            service.getTermuxSessionForSessionName(CURRENT_SESSION_NAME).getTerminalSession();

        hideSessionThroughProductionEntryPoint(HIDDEN_SESSION_NAME);

        Object displayedKeystrokeQueue = queueOf(displayedSession, TERMINAL_TO_PROCESS_QUEUE);
        assertTrue("hiding one session must not close the byte queues of the session the owner is "
                + "looking at, because a displayed session must stay connected and current",
            queueAcceptsBytes(displayedKeystrokeQueue, KEYSTROKE_PROBE));
        assertEquals("the displayed session's keystrokes must still be readable back off its queue "
                + "after another session was hidden", KEYSTROKE_PROBE,
            drainQueueAsText(displayedKeystrokeQueue));

        Object displayedShellOutputQueue = queueOf(displayedSession, PROCESS_TO_TERMINAL_QUEUE);
        assertTrue("hiding one session must not stop shell output reaching the displayed session",
            queueAcceptsBytes(displayedShellOutputQueue, SHELL_OUTPUT_PROBE));

        deliverPendingShellOutputOnMainThread(displayedSession);

        String transcript = displayedSession.getEmulator().getScreen().getTranscriptText();
        assertTrue("output written for the displayed session must still land on its emulator screen "
                + "after another session was hidden; the transcript was " + transcript.trim(),
            transcript.contains(SHELL_OUTPUT_PROBE));
    }

    @Test
    public void openingADefinitionBackedRowWhileTheServiceIsUnboundLeavesTheStoredHiddenStateAlone()
            throws Exception {
        shellManager.mTermuxSessions.add(liveSessionHoldingAnEmulator(HIDDEN_SESSION_NAME));
        hideSessionThroughProductionEntryPoint(HIDDEN_SESSION_NAME);
        assertTrue("the hide must record the session as hidden before this assertion means anything",
            preferences.getDisabledSessionNames().contains(HIDDEN_SESSION_NAME));

        set(activity, TermuxActivity.class, "mTermuxService", null);
        openDefinitionBackedRowThroughProductionEntryPoint(HIDDEN_SESSION_NAME);

        assertTrue("recreating a session needs the service that owns the session list, so with the "
                + "service unbound the stored hidden state must be left alone; clearing it would "
                + "record the session as shown while nothing had been created for it, and the owner "
                + "would be looking at a row whose toggle no longer describes its state",
            preferences.getDisabledSessionNames().contains(HIDDEN_SESSION_NAME));
    }

    private void openDefinitionBackedRowThroughProductionEntryPoint(String sessionName)
            throws Exception {
        Method openDefinitionBackedSession = TermuxSessionsListViewController.class
            .getDeclaredMethod("openDefinitionBackedSession", String.class);
        openDefinitionBackedSession.setAccessible(true);
        openDefinitionBackedSession.invoke(listViewController, sessionName);
    }

    private static Object queueOf(TerminalSession session, String queueFieldName) throws Exception {
        Field queueField = TerminalSession.class.getDeclaredField(queueFieldName);
        queueField.setAccessible(true);
        return queueField.get(session);
    }

    private static boolean queueAcceptsBytes(Object queue, String text) throws Exception {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        Method write = queue.getClass().getDeclaredMethod("write", byte[].class, int.class, int.class);
        write.setAccessible(true);
        return (Boolean) write.invoke(queue, bytes, 0, bytes.length);
    }

    private static String drainQueueAsText(Object queue) throws Exception {
        Method read = queue.getClass().getDeclaredMethod("read", byte[].class, boolean.class);
        read.setAccessible(true);
        byte[] buffer = new byte[PROBE_READ_BUFFER_BYTES];
        int readBytes = (Integer) read.invoke(queue, buffer, false);
        if (readBytes == QUEUE_CLOSED) return "<the queue is closed>";
        return new String(buffer, 0, readBytes, StandardCharsets.UTF_8);
    }

    private void deliverPendingShellOutputOnMainThread(TerminalSession session) throws Exception {
        Field handlerField = TerminalSession.class.getDeclaredField("mMainThreadHandler");
        handlerField.setAccessible(true);
        Handler mainThreadHandler = (Handler) handlerField.get(session);
        Field newInputMessage = TerminalSession.class.getDeclaredField("MSG_NEW_INPUT");
        newInputMessage.setAccessible(true);
        mainThreadHandler.sendEmptyMessage(newInputMessage.getInt(null));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    private void drainMainThreadTasksIgnoringMissingNativeLibrary() {
        ShadowLooper mainLooper = Shadows.shadowOf(Looper.getMainLooper());
        for (int task = 0; task < MAIN_THREAD_TASK_DRAIN_LIMIT && !mainLooper.isIdle(); task++) {
            try {
                mainLooper.runOneTask();
            } catch (Throwable missingNativeLibrary) {
                assertTrue("the only failure the unhide may raise in a Java virtual machine test is the "
                        + "absent device-only native library reached from the shell process fork, but it "
                        + "raised " + missingNativeLibrary,
                    causeChainDescription(missingNativeLibrary).contains("com.termux.terminal.JNI"));
            }
        }
    }

    private static String causeChainDescription(Throwable throwable) {
        StringBuilder description = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            description.append(current).append('\n');
            for (StackTraceElement element : current.getStackTrace()) {
                description.append("    at ").append(element).append('\n');
            }
            current = current.getCause();
        }
        return description.toString();
    }

    private void layOutTerminalViewForEmulatorSizeComputation() throws Exception {
        TerminalView terminalView = activity.getTerminalView();
        terminalView.setTextSize(24);
        terminalView.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY));
        terminalView.layout(0, 0, 1080, 1920);
        Object renderer = terminalView.mRenderer;
        assertNotNull("the terminal view must have a renderer after a real layout pass", renderer);
        setDeclared(renderer, "mFontWidth", 10f);
        setDeclared(renderer, "mFontLineSpacing", 20);
        setDeclared(renderer, "mFontLineSpacingAndAscent", 15);
    }

    private static void setDeclared(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void hideSessionThroughProductionEntryPoint(String sessionName) throws Exception {
        Method hideSession =
            TermuxSessionsListViewController.class.getDeclaredMethod("hideSession", String.class);
        hideSession.setAccessible(true);
        hideSession.invoke(listViewController, sessionName);
    }

    private void unhideSessionThroughProductionEntryPoint(String sessionName) throws Exception {
        Method toggleSessionDisabled = TermuxSessionsListViewController.class
            .getDeclaredMethod("toggleSessionDisabled", String.class);
        toggleSessionDisabled.setAccessible(true);
        toggleSessionDisabled.invoke(listViewController, sessionName);
    }

    private static View bottomSheetStubContentView(Context appContext) {
        FrameLayout contentView = new FrameLayout(appContext);
        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        contentView.addView(drawerLayout);
        contentView.addView(viewWithId(appContext, R.id.session_list_bottom_sheet));
        contentView.addView(viewWithId(appContext, R.id.session_list_bottom_sheet_scrim));
        contentView.addView(viewWithId(appContext, R.id.session_info_bottom_container));
        contentView.addView(viewWithId(appContext, R.id.session_list_bottom_sheet_drag_handle));
        contentView.addView(viewWithId(appContext, R.id.session_list_bottom_sheet_settings_button));
        contentView.addView(viewWithId(appContext, R.id.session_list_bottom_sheet_new_session_button));
        contentView.addView(viewWithId(appContext, R.id.session_list_bottom_sheet_load_session_button));
        contentView.addView(viewWithId(appContext, R.id.session_list_bottom_sheet_google_button));
        TextView titleView = new TextView(appContext);
        titleView.setId(R.id.session_list_bottom_sheet_title);
        contentView.addView(titleView);
        ImageButton hiddenToggleButton = new ImageButton(appContext);
        hiddenToggleButton.setId(R.id.session_list_bottom_sheet_hidden_toggle_button);
        contentView.addView(hiddenToggleButton);
        return contentView;
    }

    private static View viewWithId(Context appContext, int viewId) {
        View view = new View(appContext);
        view.setId(viewId);
        return view;
    }

    private TermuxSession liveSessionHoldingAnEmulator(String sessionName) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null,
            activity.getTermuxTerminalSessionClient());
        terminalSession.mSessionName = sessionName;
        Field emulator = TerminalSession.class.getDeclaredField("mEmulator");
        emulator.setAccessible(true);
        emulator.set(terminalSession, new TerminalEmulator(terminalSession, TERMINAL_COLUMNS,
            TERMINAL_ROWS, TERMINAL_CELL_WIDTH_PIXELS, TERMINAL_CELL_HEIGHT_PIXELS, TRANSCRIPT_ROWS,
            activity.getTermuxTerminalSessionClient()));
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(terminalSession, UNREACHABLE_SHELL_PROCESS_ID);
        return termuxSession(terminalSession);
    }

    private static TermuxSession termuxSession(TerminalSession terminalSession) throws Exception {
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class,
            boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(terminalSession, new ExecutionCommand(), null, false);
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value)
            throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
