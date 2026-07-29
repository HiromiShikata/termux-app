package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

@RunWith(RobolectricTestRunner.class)
public class HidingTheLastVisibleSessionIsRefusedTest {

    private static final String LAST_VISIBLE_SESSION_NAME = "session-current";

    private static final String SECOND_VISIBLE_SESSION_NAME = "session-other";

    private static final int TERMINAL_COLUMNS = 80;

    private static final int TERMINAL_ROWS = 40;

    private static final int TERMINAL_CELL_WIDTH_PIXELS = 12;

    private static final int TERMINAL_CELL_HEIGHT_PIXELS = 24;

    private static final Integer TRANSCRIPT_ROWS = 2000;

    private static final int UNREACHABLE_SHELL_PROCESS_ID = 2000000000;

    private static final int RELEASED_SHELL_PROCESS_ID = -1;

    private static final String KEYSTROKE_PROBE = "last-visible-session-keystroke-probe";

    private static final String TERMINAL_TO_PROCESS_QUEUE = "mTerminalToProcessIOQueue";

    private static final int PROBE_READ_BUFFER_BYTES = 4096;

    private static final String DEVICE_ONLY_NATIVE_LIBRARY_OWNER = "com.termux.terminal.JNI";

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TermuxAppSharedPreferences preferences;
    private TermuxSessionsListViewController listViewController;
    private TerminalView terminalView;

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

        terminalView = new TerminalView(appContext, null);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);
        layOutTerminalViewForEmulatorSizeComputation();

        TermuxSession lastVisibleSession = liveSessionHoldingAnEmulator(LAST_VISIBLE_SESSION_NAME);
        shellManager.mTermuxSessions.add(lastVisibleSession);
        terminalView.mTermSession = lastVisibleSession.getTerminalSession();
        terminalView.mEmulator = lastVisibleSession.getTerminalSession().getEmulator();

        listViewController = new TermuxSessionsListViewController(activity, service.getTermuxSessions());
        set(activity, TermuxActivity.class, "mTermuxSessionListViewController", listViewController);
    }

    @Test
    public void hidingTheOnlyVisibleSessionLeavesItRunningAndUnhidden() throws Exception {
        TerminalSession lastVisibleSession = activity.getCurrentSession();
        assertNotNull("the arrangement must start with the displayed session as the only live session",
            lastVisibleSession);
        assertEquals("the arrangement must hold exactly one live session, otherwise this test would be "
            + "measuring the case that already works", 1, service.getTermuxSessionsSize());

        hideThroughLongPressEntryPoint(LAST_VISIBLE_SESSION_NAME);

        assertNotNull("hiding the last visible session must not take effect, because honouring it would "
                + "leave the application with nothing to show; the session the owner is looking at must "
                + "keep its terminal emulator rather than be left rendering a screen that no longer "
                + "belongs to any session", lastVisibleSession.getEmulator());
        assertTrue("the last visible session must keep its running shell process, because releasing it "
                + "leaves the owner looking at a terminal whose shell is gone with no way back except "
                + "restarting the application", lastVisibleSession.isRunning());
        assertEquals("a keystroke typed into the last visible session must still be accepted by the "
                + "queue the writer thread drains into the shell process; a released session closes that "
                + "queue permanently and silently swallows everything the owner types",
            KEYSTROKE_PROBE.length(), keystrokeByteCountDeliveredTo(lastVisibleSession));
        assertFalse("the refused hide must leave no trace in stored state, because a session recorded as "
                + "hidden while it is still the one on screen makes its own toggle describe the opposite "
                + "of what the owner sees",
            preferences.getDisabledSessionNames().contains(LAST_VISIBLE_SESSION_NAME));
        assertSame("the view must still be attached to the session it was showing before the refused "
            + "hide", lastVisibleSession, activity.getCurrentSession());
    }

    @Test
    public void hidingTheOnlyVisibleSessionThroughTheRowToggleLeavesItRunningAndUnhidden()
            throws Exception {
        TerminalSession lastVisibleSession = activity.getCurrentSession();

        hideThroughRowToggleEntryPoint(LAST_VISIBLE_SESSION_NAME);

        assertNotNull("the row toggle reaches the same hidden state as the long press action, so it must "
            + "refuse the last visible session on the same terms", lastVisibleSession.getEmulator());
        assertTrue("the row toggle must not release the shell process of the last visible session",
            lastVisibleSession.isRunning());
        assertEquals("the row toggle must leave the last visible session's keystroke channel open",
            KEYSTROKE_PROBE.length(), keystrokeByteCountDeliveredTo(lastVisibleSession));
        assertFalse("the row toggle must leave no hidden state behind for a hide it refused",
            preferences.getDisabledSessionNames().contains(LAST_VISIBLE_SESSION_NAME));
    }

    @Test
    public void hidingTheDisplayedSessionWhileAnotherVisibleSessionRemainsStillReleasesIt()
            throws Exception {
        TermuxSession secondSession = liveSessionHoldingAnEmulator(SECOND_VISIBLE_SESSION_NAME);
        shellManager.mTermuxSessions.add(secondSession);
        TerminalSession displayedSession = activity.getCurrentSession();

        hideThroughLongPressEntryPoint(LAST_VISIBLE_SESSION_NAME);

        assertNull("the guard must refuse only the hide that would leave nothing to show; while another "
                + "visible session remains, hiding the displayed session must still release its terminal "
                + "emulator, otherwise the guard has been implemented by breaking the case that already "
                + "works", displayedSession.getEmulator());
        assertEquals("hiding the displayed session while another visible session remains must still end "
            + "its shell process", RELEASED_SHELL_PROCESS_ID, shellPidOf(displayedSession));
        assertEquals("the released session's keystroke queue must be closed, which is what makes moving "
            + "the view away from it necessary", 0, keystrokeByteCountDeliveredTo(displayedSession));
        assertTrue("the honoured hide must be recorded in stored state",
            preferences.getDisabledSessionNames().contains(LAST_VISIBLE_SESSION_NAME));
        assertSame("the view must be moved to the remaining visible session rather than left attached to "
                + "the released one", secondSession.getTerminalSession(), activity.getCurrentSession());
        assertNotNull("the remaining visible session must be untouched by the hide of another session",
            secondSession.getTerminalSession().getEmulator());
    }

    private void hideThroughLongPressEntryPoint(String sessionName) throws Exception {
        Method hideSession =
            TermuxSessionsListViewController.class.getDeclaredMethod("hideSession", String.class);
        hideSession.setAccessible(true);
        invokeToleratingTheDeviceOnlyNativeLibrary(hideSession, sessionName);
    }

    private void hideThroughRowToggleEntryPoint(String sessionName) throws Exception {
        Method toggleSessionDisabled = TermuxSessionsListViewController.class
            .getDeclaredMethod("toggleSessionDisabled", String.class);
        toggleSessionDisabled.setAccessible(true);
        invokeToleratingTheDeviceOnlyNativeLibrary(toggleSessionDisabled, sessionName);
    }

    private void invokeToleratingTheDeviceOnlyNativeLibrary(Method entryPoint, String sessionName)
            throws Exception {
        try {
            entryPoint.invoke(listViewController, sessionName);
        } catch (InvocationTargetException invocationFailure) {
            String causeChain = causeChainDescription(invocationFailure);
            assertTrue("the only failure a hide may raise in a Java virtual machine test is the absent "
                    + "device-only native library reached from the shell process fork, but it raised "
                    + causeChain,
                causeChain.contains(DEVICE_ONLY_NATIVE_LIBRARY_OWNER));
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

    private static int shellPidOf(TerminalSession session) throws Exception {
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        return shellPid.getInt(session);
    }

    private static int keystrokeByteCountDeliveredTo(TerminalSession session) throws Exception {
        Field queueField = TerminalSession.class.getDeclaredField(TERMINAL_TO_PROCESS_QUEUE);
        queueField.setAccessible(true);
        Object queue = queueField.get(session);
        byte[] bytes = KEYSTROKE_PROBE.getBytes(StandardCharsets.UTF_8);
        Method write = queue.getClass().getDeclaredMethod("write", byte[].class, int.class, int.class);
        write.setAccessible(true);
        if (!(Boolean) write.invoke(queue, bytes, 0, bytes.length)) {
            return 0;
        }
        Method read = queue.getClass().getDeclaredMethod("read", byte[].class, boolean.class);
        read.setAccessible(true);
        byte[] buffer = new byte[PROBE_READ_BUFFER_BYTES];
        int readBytes = (Integer) read.invoke(queue, buffer, false);
        return Math.max(readBytes, 0);
    }

    private void layOutTerminalViewForEmulatorSizeComputation() throws Exception {
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
