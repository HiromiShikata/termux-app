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
import com.termux.app.sessiondefinition.SessionDefinitionCapCountPlanner;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.app.terminal.session.SessionEagerLoadPacer;
import com.termux.app.terminal.session.SessionReconnectPacer;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class HiddenSessionStaysDisconnectedWhileAReconnectIsInFlightTest {

    private static final String CURRENT_SESSION_NAME = "session-current";

    private static final String HIDDEN_SESSION_NAME = "session-hidden";

    private static final String RECONNECTABLE_SESSION_NAME = "session-reconnectable";

    private static final int TERMINAL_COLUMNS = 80;

    private static final int TERMINAL_ROWS = 40;

    private static final int TERMINAL_CELL_WIDTH_PIXELS = 12;

    private static final int TERMINAL_CELL_HEIGHT_PIXELS = 24;

    private static final Integer TRANSCRIPT_ROWS = 2000;

    private static final int UNREACHABLE_SHELL_PROCESS_ID = 2000000000;

    private static final int RELEASED_SHELL_PROCESS_ID = -1;

    private static final long IDLE_PAST_PACED_RECONNECT_MILLIS = 2000L;

    private static final long IDLE_PAST_RECONNECT_TIMEOUT_MILLIS = 600000L;

    private static final int MAIN_THREAD_TASK_DRAIN_LIMIT = 500;

    private static final String KEYSTROKE_PROBE = "reconnect-keystroke-probe";

    private static final String SHELL_OUTPUT_PROBE = "reconnect-shell-output-probe";

    private static final String TERMINAL_TO_PROCESS_QUEUE = "mTerminalToProcessIOQueue";

    private static final String PROCESS_TO_TERMINAL_QUEUE = "mProcessToTerminalIOQueue";

    private static final int PROBE_READ_BUFFER_BYTES = 4096;

    private static final String DEVICE_ONLY_NATIVE_LIBRARY_OWNER = "com.termux.terminal.JNI";

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
    public void reconnectAlreadyInFlightCreatesNoLiveSessionObjectForAHiddenSession() throws Exception {
        hideWhileTheReconnectIsStillPending();

        assertNull("a hidden session must not connect at all, so a staggered reconnect that was "
                + "already in flight when the session was hidden must not create a live session object "
                + "for it; the reconnect runnable is posted with a delay and never stored, so nothing "
                + "cancels it, and the owner sees a session he hid come back with a running shell",
            service.getTermuxSessionForSessionName(HIDDEN_SESSION_NAME));
    }

    @Test
    public void reconnectAlreadyInFlightGivesAHiddenSessionNoTerminalEmulator() throws Exception {
        hideWhileTheReconnectIsStillPending();

        assertEquals("the reconnect must add no session to the service session list, because a session "
                + "object created there is what the size update is then driven against; asserting only "
                + "that the emulator is absent would pass off-device for the unrelated reason that the "
                + "shell process fork cannot complete without its native library",
            1, service.getTermuxSessionsSize());
        assertNull("a reconnect that fires after the hide must not give the hidden session a terminal "
                + "emulator; the reconnect drives the session size update, which on a session with no "
                + "emulator enters emulator initialisation and native subprocess creation, so the "
                + "scrollback buffer the hide released is allocated again",
            emulatorForSessionName(HIDDEN_SESSION_NAME));
    }

    @Test
    public void reconnectAlreadyInFlightPutsAHiddenSessionBackInNoSessionCapSlot() throws Exception {
        hideWhileTheReconnectIsStillPending();

        assertEquals("a hidden session must occupy no slot in the session cap count even when a "
                + "reconnect was already in flight when it was hidden, so only the displayed session "
                + "may be counted", 1, cappedSessionCount());
        assertEquals("the hidden session must be absent from the cap count because no session object "
                + "for it exists, not merely because the caller remembered to pass the hidden-name "
                + "set; a caller that counts without that set must reach the same figure, otherwise a "
                + "revived hidden session silently consumes a cap slot wherever the set is omitted",
            1, cappedSessionCountWithoutTheHiddenNameFilter());
    }

    @Test
    public void reconnectAlreadyInFlightPutsAHiddenSessionBackInNoScanSet() throws Exception {
        openSessionListWithOnScreenRows(CURRENT_SESSION_NAME, HIDDEN_SESSION_NAME);

        hideWhileTheReconnectIsStillPending();

        Set<String> scanSessionNames = visibleSessionNames();
        assertFalse("a hidden session must never be included in the set that drives statusline "
                + "re-parsing and reconnect selection, including after a reconnect that was already in "
                + "flight when it was hidden; the set contained " + scanSessionNames,
            scanSessionNames.contains(HIDDEN_SESSION_NAME));
        assertEquals("the on-screen row for the hidden session must be backed by no live session at "
                + "all; excluding the name from the scan set while a revived session object keeps a "
                + "shell process alive behind it is worse than including it, because the process then "
                + "runs unscanned and unaccounted for",
            1, service.getTermuxSessionsSize());
    }

    @Test
    public void sessionBroughtBackAfterAReleaseIsANewObjectWhoseChannelsCarryKeystrokesAndOutput()
            throws Exception {
        shellManager.mTermuxSessions.add(liveSessionHoldingAnEmulator(HIDDEN_SESSION_NAME));
        TerminalSession releasedSession =
            service.getTermuxSessionForSessionName(HIDDEN_SESSION_NAME).getTerminalSession();

        hideSessionThroughProductionEntryPoint(HIDDEN_SESSION_NAME);

        assertEquals("the release must close the keystroke queue of the released object, otherwise the "
                + "assertions below would not be proving that the object which came back is a "
                + "different one", 0, keystrokeByteCountDeliveredTo(releasedSession));

        unhideSessionThroughProductionEntryPoint(HIDDEN_SESSION_NAME);
        drainMainThreadTasksToleratingTheDeviceOnlyNativeLibrary();

        TermuxSession revivedTermuxSession = service.getTermuxSessionForSessionName(HIDDEN_SESSION_NAME);
        assertNotNull("bringing a released session back must produce a live session object for it",
            revivedTermuxSession);
        TerminalSession revivedSession = revivedTermuxSession.getTerminalSession();
        assertNotSame("a session brought back after a release must be a new object; the released "
                + "object's byte queues were closed permanently and its shell stream close also closed "
                + "the pseudo-teletype descriptor and zeroed the field, so reviving that object yields "
                + "a terminal that looks alive and silently swallows every byte in both directions",
            releasedSession, revivedSession);

        Object keystrokeQueue = queueOf(revivedSession, TERMINAL_TO_PROCESS_QUEUE);
        assertEquals("a keystroke typed into the session that came back must reach the queue the "
                + "writer thread drains into the shell process, byte for byte",
            KEYSTROKE_PROBE.length(), keystrokeByteCountDeliveredTo(revivedSession));

        assertTrue("shell output arriving for the session that came back must be accepted by the queue "
                + "the reader thread fills; a permanently closed queue discards it and reports false",
            queueAcceptsBytes(queueOf(revivedSession, PROCESS_TO_TERMINAL_QUEUE), SHELL_OUTPUT_PROBE));
        deliverPendingShellOutputOnMainThread(revivedSession);

        TerminalEmulator revivedEmulator = revivedSession.getEmulator();
        assertNotNull("the session that came back must hold a terminal emulator for its output to land "
            + "in", revivedEmulator);
        String transcript = revivedEmulator.getScreen().getTranscriptText();
        assertTrue("the accepted output must actually reach the terminal emulator screen of the session "
                + "that came back, otherwise the owner is looking at a terminal that swallows every "
                + "byte the shell writes; the transcript was " + transcript.trim() + " and the "
                + "keystroke queue object was " + keystrokeQueue,
            transcript.contains(SHELL_OUTPUT_PROBE));
    }

    @Test
    public void aRefusedReconnectLeavesTheHiddenRowNoSpinnerAndNoQueuedTimeout() throws Exception {
        hideWhileTheReconnectIsStillPending();

        assertFalse("a refused reconnect must leave no reconnecting mark behind; the mark draws a "
                + "spinner on the row, and a hidden session consumes nothing, so a row the owner hid "
                + "must not acquire one from a reconnect that never happened",
            activity.getSessionNewActivityStore().isReconnecting(HIDDEN_SESSION_NAME));
        assertTrue("the reconnect timeout runnable posted alongside the mark must not be queued for a "
                + "session that was never reconnected; it fires later and flips the row into a "
                + "reconnect-failed state for a session that does not exist",
            queuedReconnectTimeoutSessionNames().isEmpty());

        idlePastTheReconnectTimeout();

        assertFalse("with no queued timeout the hidden row must never reach the reconnect-failed state "
                + "either, which is what the owner would otherwise see minutes after hiding a session",
            activity.getSessionNewActivityStore().isReconnectFailed(HIDDEN_SESSION_NAME));
    }

    @Test
    public void aSessionThatIsActuallyReconnectedStillAcquiresItsReconnectingMark() throws Exception {
        set(activity, TermuxActivity.class, "mIsVisible", false);
        TermuxSession deadSession = deadSessionAwaitingReconnect(RECONNECTABLE_SESSION_NAME);
        shellManager.mTermuxSessions.add(deadSession);

        enqueueReconnectThroughProductionPath(deadSession.getTerminalSession());
        idlePastThePacedReconnectWindow();

        assertNotNull("this assertion only means something if the reconnect genuinely produced a live "
                + "session object, otherwise it would pass for the same reason the hidden case does. The "
                + "reconnect is driven with the activity not in the foreground, which is the arrangement "
                + "in which the replacement session is created without eagerly building its emulator, so "
                + "the decision under test is reachable without the device-only native library",
            service.getTermuxSessionForSessionName(RECONNECTABLE_SESSION_NAME));
        assertTrue("suppressing the reconnecting mark for a refused reconnect must not suppress it for "
                + "a reconnect that actually happened; the owner relies on that mark to see which "
                + "sessions are coming back",
            activity.getSessionNewActivityStore().isReconnecting(RECONNECTABLE_SESSION_NAME));
        assertFalse("the reconnect timeout that accompanies the mark must be queued for a session that "
                + "really is reconnecting", queuedReconnectTimeoutSessionNames().isEmpty());
    }

    @Test
    public void aHiddenNameLeavesTheReconnectListEvenWhileItsSessionObjectIsStillLive()
            throws Exception {
        TermuxSession stillLiveSession = deadSessionAwaitingReconnect(HIDDEN_SESSION_NAME);
        shellManager.mTermuxSessions.add(stillLiveSession);
        preferences.setDisabledSessionNames(HIDDEN_SESSION_NAME);

        assertFalse("the paced reconnect asks, at the moment each queued session comes up, whether that "
                + "session is still one the reconnect list covers, and a name the owner hid must answer "
                + "no. The hidden name is written straight to storage here rather than through the hide "
                + "entry point, because that entry point also tears the live session down, and the state "
                + "under test is the one where the session object is still live and the name is already "
                + "hidden",
            isSessionStillInTheReconnectListThroughProductionPath(
                stillLiveSession.getTerminalSession()));
    }

    @Test
    public void aNameThatIsNotHiddenStaysInTheReconnectListWhileItsSessionObjectIsLive()
            throws Exception {
        TermuxSession stillLiveSession = deadSessionAwaitingReconnect(RECONNECTABLE_SESSION_NAME);
        shellManager.mTermuxSessions.add(stillLiveSession);

        assertTrue("refusing hidden names must not refuse every name, otherwise the paced reconnect "
                + "would never reconnect anything and the previous assertion would hold for the wrong "
                + "reason",
            isSessionStillInTheReconnectListThroughProductionPath(
                stillLiveSession.getTerminalSession()));
    }

    @Test
    public void theInPlaceReconnectOfAFinishedSessionRefusesAHiddenName() throws Exception {
        TermuxSession deadSession = deadSessionAwaitingReconnect(HIDDEN_SESSION_NAME);
        shellManager.mTermuxSessions.add(deadSession);
        TerminalSession finishedSession = deadSession.getTerminalSession();
        hideSessionThroughProductionEntryPoint(HIDDEN_SESSION_NAME);

        boolean reconnected = reconnectFinishedSessionInPlaceThroughProductionPath(finishedSession);

        assertFalse("the in-place reconnect of a finished session must refuse a hidden name like every "
                + "other reconnect path; its callers reach only the on-screen session today, so the "
                + "requirement would otherwise be resting on an invariant kept in another component "
                + "rather than on a check at the point of effect",
            reconnected);
        assertNull("a refused in-place reconnect must create no live session object for the hidden "
                + "name", service.getTermuxSessionForSessionName(HIDDEN_SESSION_NAME));
        assertNotSame("a refused in-place reconnect must not take the owner off the session he is "
                + "working in, because that call sets the current session unconditionally once it "
                + "proceeds", finishedSession, activity.getCurrentSession());
    }

    @Test
    public void theInPlaceReconnectStillReconnectsAFinishedSessionThatIsNotHidden() throws Exception {
        TermuxSession deadSession = deadSessionAwaitingReconnect(RECONNECTABLE_SESSION_NAME);
        shellManager.mTermuxSessions.add(deadSession);

        reconnectFinishedSessionInPlaceThroughProductionPath(deadSession.getTerminalSession());

        assertNotNull("refusing the in-place reconnect for hidden names must not refuse it for a "
                + "finished session the owner can see; pressing Enter on such a session is how it comes "
                + "back, and it must exist as a live session object again afterwards",
            service.getTermuxSessionForSessionName(RECONNECTABLE_SESSION_NAME));
    }

    private boolean reconnectFinishedSessionInPlaceThroughProductionPath(TerminalSession finishedSession)
            throws Exception {
        Method reconnectFinishedSessionInPlace = TermuxTerminalSessionActivityClient.class
            .getDeclaredMethod("reconnectFinishedSessionInPlace", TerminalSession.class, String.class);
        reconnectFinishedSessionInPlace.setAccessible(true);
        try {
            return (Boolean) reconnectFinishedSessionInPlace.invoke(
                activity.getTermuxTerminalSessionClient(), finishedSession, null);
        } catch (Throwable missingNativeLibrary) {
            assertTrue("the only failure an in-place reconnect may raise in a Java virtual machine test "
                    + "is the absent device-only native library reached once the replacement session is "
                    + "displayed, but it raised " + causeChainDescription(missingNativeLibrary),
                causeChainDescription(missingNativeLibrary).contains(DEVICE_ONLY_NATIVE_LIBRARY_OWNER));
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> queuedReconnectTimeoutSessionNames() throws Exception {
        Field runnablesByName = TermuxTerminalSessionActivityClient.class
            .getDeclaredField("mReconnectTimeoutRunnableByName");
        runnablesByName.setAccessible(true);
        return ((Map<String, Runnable>) runnablesByName.get(activity.getTermuxTerminalSessionClient()))
            .keySet();
    }

    private void idlePastTheReconnectTimeout() {
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(IDLE_PAST_RECONNECT_TIMEOUT_MILLIS));
    }

    private void hideWhileTheReconnectIsStillPending() throws Exception {
        TermuxSession deadSession = deadSessionAwaitingReconnect(HIDDEN_SESSION_NAME);
        shellManager.mTermuxSessions.add(deadSession);

        enqueueReconnectThroughProductionPath(deadSession.getTerminalSession());

        hideSessionThroughProductionEntryPoint(HIDDEN_SESSION_NAME);
        assertTrue("the hide must be recorded before the reconnect fires, otherwise this test would be "
                + "measuring a reconnect of a session that was never hidden",
            preferences.getDisabledSessionNames().contains(HIDDEN_SESSION_NAME));

        idlePastThePacedReconnectWindow();
    }

    private boolean isSessionStillInTheReconnectListThroughProductionPath(TerminalSession session)
            throws Exception {
        Method isSessionStillInTheReconnectList = TermuxTerminalSessionActivityClient.class
            .getDeclaredMethod("isSessionStillInTheReconnectList", TerminalSession.class);
        isSessionStillInTheReconnectList.setAccessible(true);
        return (Boolean) isSessionStillInTheReconnectList.invoke(
            activity.getTermuxTerminalSessionClient(), session);
    }

    private void enqueueReconnectThroughProductionPath(TerminalSession deadSession) throws Exception {
        Field pacerField = TermuxTerminalSessionActivityClient.class
            .getDeclaredField("mSessionReconnectPacer");
        pacerField.setAccessible(true);
        SessionReconnectPacer pacer =
            (SessionReconnectPacer) pacerField.get(activity.getTermuxTerminalSessionClient());
        pacer.enqueueSession(deadSession);
    }

    private void idlePastThePacedReconnectWindow() {
        ShadowLooper mainLooper = Shadows.shadowOf(Looper.getMainLooper());
        try {
            mainLooper.idleFor(Duration.ofMillis(IDLE_PAST_PACED_RECONNECT_MILLIS));
        } catch (Throwable missingNativeLibrary) {
            assertTrue("the only failure the reconnect may raise in a Java virtual machine test is the "
                    + "absent device-only native library reached from the shell process fork, but it "
                    + "raised " + causeChainDescription(missingNativeLibrary),
                causeChainDescription(missingNativeLibrary).contains(DEVICE_ONLY_NATIVE_LIBRARY_OWNER));
        }
    }

    private void drainMainThreadTasksToleratingTheDeviceOnlyNativeLibrary() {
        ShadowLooper mainLooper = Shadows.shadowOf(Looper.getMainLooper());
        for (int task = 0; task < MAIN_THREAD_TASK_DRAIN_LIMIT; task++) {
            try {
                mainLooper.idleFor(Duration.ofMillis(
                    SessionEagerLoadPacer.MAIN_THREAD_FRAME_YIELD_INTERVAL_MILLIS));
            } catch (Throwable missingNativeLibrary) {
                assertTrue("the only failure this may raise in a Java virtual machine test is the "
                        + "absent device-only native library reached from the shell process fork, but "
                        + "it raised " + causeChainDescription(missingNativeLibrary),
                    causeChainDescription(missingNativeLibrary)
                        .contains(DEVICE_ONLY_NATIVE_LIBRARY_OWNER));
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

    private TerminalEmulator emulatorForSessionName(String sessionName) {
        TermuxSession termuxSession = service.getTermuxSessionForSessionName(sessionName);
        if (termuxSession == null) return null;
        TerminalSession terminalSession = termuxSession.getTerminalSession();
        return terminalSession == null ? null : terminalSession.getEmulator();
    }

    private int cappedSessionCount() throws Exception {
        Method cappedSessionCount = TermuxTerminalSessionActivityClient.class
            .getDeclaredMethod("cappedSessionCount", TermuxService.class);
        cappedSessionCount.setAccessible(true);
        return (int) cappedSessionCount.invoke(activity.getTermuxTerminalSessionClient(), service);
    }

    private int cappedSessionCountWithoutTheHiddenNameFilter() {
        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = new ArrayList<>();
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            countedSessions.add(new SessionDefinitionCapCountPlanner.CountedSession(
                terminalSession == null ? null : terminalSession.mSessionName,
                terminalSession != null && terminalSession.isRunning()));
        }
        return new SessionDefinitionCapCountPlanner()
            .countSessionsTowardCap(countedSessions, Collections.emptySet());
    }

    @SuppressWarnings("unchecked")
    private Set<String> visibleSessionNames() throws Exception {
        Method visibleSessionNames =
            TermuxTerminalSessionActivityClient.class.getDeclaredMethod("visibleSessionNames");
        visibleSessionNames.setAccessible(true);
        return (Set<String>) visibleSessionNames.invoke(activity.getTermuxTerminalSessionClient());
    }

    private void openSessionListWithOnScreenRows(String... onScreenSessionNames) throws Exception {
        OnScreenSessionListBottomSheetController bottomSheetController =
            new OnScreenSessionListBottomSheetController(activity, Arrays.asList(onScreenSessionNames));
        set(activity, TermuxActivity.class, "mSessionListBottomSheetController", bottomSheetController);
        activity.findViewById(R.id.session_list_bottom_sheet).setVisibility(View.VISIBLE);
        assertTrue("the session list must report itself open so the on-screen rows reach the reconnect "
            + "scheduler and statusline re-parse selection", bottomSheetController.isOpen());
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

    private static int keystrokeByteCountDeliveredTo(TerminalSession session) throws Exception {
        Object queue = queueOf(session, TERMINAL_TO_PROCESS_QUEUE);
        if (!queueAcceptsBytes(queue, KEYSTROKE_PROBE)) {
            return 0;
        }
        Method read = queue.getClass().getDeclaredMethod("read", byte[].class, boolean.class);
        read.setAccessible(true);
        byte[] buffer = new byte[PROBE_READ_BUFFER_BYTES];
        int readBytes = (Integer) read.invoke(queue, buffer, false);
        return Math.max(readBytes, 0);
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
        TerminalSession terminalSession = terminalSessionHoldingAnEmulator(sessionName);
        setShellProcessId(terminalSession, UNREACHABLE_SHELL_PROCESS_ID);
        return termuxSession(terminalSession);
    }

    private TermuxSession deadSessionAwaitingReconnect(String sessionName) throws Exception {
        TerminalSession terminalSession = terminalSessionHoldingAnEmulator(sessionName);
        setShellProcessId(terminalSession, RELEASED_SHELL_PROCESS_ID);
        return termuxSession(terminalSession);
    }

    private TerminalSession terminalSessionHoldingAnEmulator(String sessionName) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null,
            activity.getTermuxTerminalSessionClient());
        terminalSession.mSessionName = sessionName;
        Field emulator = TerminalSession.class.getDeclaredField("mEmulator");
        emulator.setAccessible(true);
        emulator.set(terminalSession, new TerminalEmulator(terminalSession, TERMINAL_COLUMNS,
            TERMINAL_ROWS, TERMINAL_CELL_WIDTH_PIXELS, TERMINAL_CELL_HEIGHT_PIXELS, TRANSCRIPT_ROWS,
            activity.getTermuxTerminalSessionClient()));
        return terminalSession;
    }

    private static void setShellProcessId(TerminalSession terminalSession, int shellProcessId)
            throws Exception {
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(terminalSession, shellProcessId);
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

    private static final class OnScreenSessionListBottomSheetController
            extends SessionListBottomSheetController {

        private final List<String> onScreenSessionNames;

        private OnScreenSessionListBottomSheetController(TermuxActivity activity,
                                                         List<String> onScreenSessionNames) {
            super(activity);
            this.onScreenSessionNames = new ArrayList<>(onScreenSessionNames);
        }

        @Override
        public List<String> getOnScreenSessionNames() {
            return new ArrayList<>(onScreenSessionNames);
        }
    }
}
