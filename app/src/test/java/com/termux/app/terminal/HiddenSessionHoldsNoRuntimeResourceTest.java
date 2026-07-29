package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class HiddenSessionHoldsNoRuntimeResourceTest {

    private static final String CURRENT_SESSION_NAME = "session-current";

    private static final String HIDDEN_SESSION_NAME = "session-hidden";

    private static final int TERMINAL_COLUMNS = 80;

    private static final int TERMINAL_ROWS = 40;

    private static final int TERMINAL_CELL_WIDTH_PIXELS = 12;

    private static final int TERMINAL_CELL_HEIGHT_PIXELS = 24;

    private static final Integer TRANSCRIPT_ROWS = 2000;

    private static final int UNREACHABLE_SHELL_PROCESS_ID = 2000000000;

    private static final int VISIBLE_SESSION_COUNT = 16;

    private static final int HIDDEN_SESSION_COUNT = 15;

    private static final int MAIN_THREAD_TASK_DRAIN_LIMIT = 500;

    private static final String PROJECT_LABEL = "project-label";

    private static final String STORY_LABEL = "story-label";

    private static final String NOT_APPLICABLE_PROJECT_LABEL = "N/A";

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
    public void hidingASessionReleasesItsTerminalEmulatorAndScrollbackBuffer() throws Exception {
        TermuxSession hiddenSession = liveSessionHoldingAnEmulator(HIDDEN_SESSION_NAME);
        shellManager.mTermuxSessions.add(hiddenSession);
        assertNotNull("the session must hold a terminal emulator before it is hidden, otherwise this "
                + "test would not be measuring the release at all",
            hiddenSession.getTerminalSession().getEmulator());

        hideSessionThroughProductionEntryPoint(HIDDEN_SESSION_NAME);

        assertNull("a hidden session must retain no terminal emulator and no scrollback buffer at "
                + "runtime, so hiding must release the emulator whose transcript is sized by the "
                + "terminal transcript rows setting",
            hiddenSession.getTerminalSession().getEmulator());
    }

    @Test
    public void hidingASessionTerminatesItsShellProcess() throws Exception {
        TermuxSession hiddenSession = liveSessionHoldingAnEmulator(HIDDEN_SESSION_NAME);
        shellManager.mTermuxSessions.add(hiddenSession);
        assertTrue("the session must report a running shell process before it is hidden, otherwise "
                + "this test would not be measuring the termination at all",
            hiddenSession.getTerminalSession().isRunning());

        hideSessionThroughProductionEntryPoint(HIDDEN_SESSION_NAME);

        assertFalse("a hidden session must retain no shell process at runtime, so hiding must "
                + "terminate the shell process rather than leave it running",
            hiddenSession.getTerminalSession().isRunning());
    }

    @Test
    public void hidingASessionSignalsTheShellProcessGroupRatherThanOnlyForgettingTheProcessId()
            throws Exception {
        TermuxSession hiddenSession = liveSessionHoldingAnEmulator(HIDDEN_SESSION_NAME);
        shellManager.mTermuxSessions.add(hiddenSession);

        hideSessionThroughProductionEntryPoint(HIDDEN_SESSION_NAME);

        assertEquals("reporting no running shell is satisfied by forgetting the process id alone, so it "
                + "cannot show that the shell was terminated. The kill must be addressed at the shell's "
                + "own process group, which is the negated process id, otherwise the process survives "
                + "the hide and keeps the device resource this change exists to free. The target is "
                + "recorded only after the signal call returns, so this value cannot exist unless the "
                + "signal was delivered",
            -UNREACHABLE_SHELL_PROCESS_ID,
            sigkilledShellProcessGroupTarget(hiddenSession.getTerminalSession()));
    }

    @Test
    public void hidingASessionRemovesItsLiveSessionObjectFromTheServiceSessionList() throws Exception {
        shellManager.mTermuxSessions.add(liveSessionHoldingAnEmulator(HIDDEN_SESSION_NAME));

        hideSessionThroughProductionEntryPoint(HIDDEN_SESSION_NAME);

        assertNull("a hidden session must retain nothing at runtime, so no live session object for it "
                + "may remain in the service session list that every scan, every reconnect selection "
                + "and every cap count iterates",
            service.getTermuxSessionForSessionName(HIDDEN_SESSION_NAME));
    }

    @Test
    public void hiddenSessionOccupiesNoSlotInTheSessionCapCount() throws Exception {
        shellManager.mTermuxSessions.add(liveSessionHoldingAnEmulator(HIDDEN_SESSION_NAME));
        assertEquals("both seeded sessions must count toward the cap before the hide, otherwise this "
                + "test would not be measuring the released slot at all", 2, cappedSessionCount());

        hideSessionThroughProductionEntryPoint(HIDDEN_SESSION_NAME);

        assertEquals("a hidden session must occupy no slot in the session cap count, so only the "
            + "displayed session may be counted", 1, cappedSessionCount());
    }

    @Test
    public void hiddenSessionIsNeverSelectedByTheReconnectSchedulerPathWhileItIsAnOnScreenRow()
            throws Exception {
        TermuxSession hiddenSession = deadSessionHoldingAnEmulator(HIDDEN_SESSION_NAME);
        shellManager.mTermuxSessions.add(hiddenSession);
        markSessionNamesHidden(HIDDEN_SESSION_NAME);
        openSessionListWithOnScreenRows(CURRENT_SESSION_NAME, HIDDEN_SESSION_NAME);

        List<String> reconnectedSessionNames =
            activity.getTermuxTerminalSessionClient().reconnectDeadDefinitionBackedSessionsInBackground();

        assertFalse("a hidden session must never enter the reconnect scheduler selection, including "
                + "when the show-hidden toggle is on and the session is therefore an on-screen row; "
                + "the scheduler path selects through VisibleSessionSelector, which takes no "
                + "hidden-name argument at all, so it selected " + reconnectedSessionNames,
            reconnectedSessionNames.contains(HIDDEN_SESSION_NAME));
    }

    @Test
    public void hiddenSessionIsNeverIncludedInTheSetThatDrivesStatuslineReparsing() throws Exception {
        shellManager.mTermuxSessions.add(liveSessionHoldingAnEmulator(HIDDEN_SESSION_NAME));
        markSessionNamesHidden(HIDDEN_SESSION_NAME);
        openSessionListWithOnScreenRows(CURRENT_SESSION_NAME, HIDDEN_SESSION_NAME);

        Set<?> statuslineReparseSessionNames = visibleSessionNames();

        assertFalse("a hidden session must never be included in the set that drives statusline "
                + "re-parsing; that set is the same on-screen set the reconnect scheduler uses and it "
                + "contained " + statuslineReparseSessionNames,
            statuslineReparseSessionNames.contains(HIDDEN_SESSION_NAME));
    }

    @Test
    public void listRowForAHiddenSessionIsStillProducedFromTheStoredSessionDefinition() {
        List<String> liveSessionNames = Collections.singletonList(CURRENT_SESSION_NAME);
        List<SessionDefinitionEntry> storedSessionDefinitions = Collections.singletonList(
            new SessionDefinitionEntry(PROJECT_LABEL, STORY_LABEL,
                Arrays.asList(CURRENT_SESSION_NAME, HIDDEN_SESSION_NAME)));

        List<SessionHierarchyRow> rows = new SessionHierarchyBuilder()
            .build(liveSessionNames, storedSessionDefinitions, NOT_APPLICABLE_PROJECT_LABEL);

        assertTrue("the row of a hidden session must still be produced from the stored session "
                + "definition after its live session object, its shell process, its terminal emulator "
                + "and its scrollback buffer have all been released, so the owner still sees the row "
                + "and can unhide it without any manual action; the built rows carried "
                + SessionHierarchyBuilder.totalSessionCount(rows) + " session rows for "
                + liveSessionNames.size() + " live session names",
            SessionHierarchyBuilder.totalSessionCount(rows) > liveSessionNames.size());
    }

    @Test
    public void unhidingAHiddenSessionRecreatesTheSession() throws Exception {
        TermuxSession sessionBeforeHiding = liveSessionHoldingAnEmulator(HIDDEN_SESSION_NAME);
        shellManager.mTermuxSessions.add(sessionBeforeHiding);
        hideSessionThroughProductionEntryPoint(HIDDEN_SESSION_NAME);

        unhideSessionThroughProductionEntryPoint(HIDDEN_SESSION_NAME);

        TermuxSession sessionAfterUnhiding = service.getTermuxSessionForSessionName(HIDDEN_SESSION_NAME);
        assertNotNull("unhiding a hidden session must recreate it, so a session object for it must "
            + "exist again after the unhide", sessionAfterUnhiding);
        assertNotSame("unhiding a hidden session must recreate it exactly as if it were being opened "
                + "for the first time, so the session object after the unhide must be a newly created "
                + "one and not the object that was alive before the hide",
            sessionBeforeHiding.getTerminalSession(), sessionAfterUnhiding.getTerminalSession());
    }

    @Test
    public void eagerSessionLoadOnForegroundTransitionCollectsNoHiddenSession() throws Exception {
        seedRealisticSessionPopulation();

        List<String> eagerlyLoadedSessionNames = sessionNamesOf(collectSessionsToEagerLoad());

        List<String> hiddenSessionNamesCollected = new ArrayList<>(eagerlyLoadedSessionNames);
        hiddenSessionNamesCollected.retainAll(hiddenSessionNames());
        assertEquals("the eager session load runs on every foreground transition and starts a shell "
                + "process for every session it collects, so it must collect no hidden session; a "
                + "hidden session must not connect at all, yet the collection contained these hidden "
                + "session names: " + hiddenSessionNamesCollected,
            Collections.emptyList(), hiddenSessionNamesCollected);
    }

    @Test
    public void eagerSessionLoadOnForegroundTransitionStartsNoHiddenSessionEmulator() throws Exception {
        seedRealisticSessionPopulation();

        activity.eagerLoadAllSessions();
        drainMainThreadTasksIgnoringMissingNativeLibrary();

        List<String> hiddenSessionNamesThatStartedAnEmulator = new ArrayList<>();
        for (String hiddenSessionName : hiddenSessionNames()) {
            TermuxSession termuxSession = service.getTermuxSessionForSessionName(hiddenSessionName);
            if (termuxSession != null && termuxSession.getTerminalSession() != null
                    && termuxSession.getTerminalSession().getEmulator() != null) {
                hiddenSessionNamesThatStartedAnEmulator.add(hiddenSessionName);
            }
        }
        assertEquals("terminal emulator construction is the first statement of the same method that "
                + "then forks the shell process, so a hidden session holding a freshly built emulator "
                + "after the eager session load is the proof that the eager load started a shell "
                + "process for it; a hidden session must not connect at all and must consume no "
                + "resources at all, yet the eager load started emulators for "
                + hiddenSessionNamesThatStartedAnEmulator.size() + " of " + HIDDEN_SESSION_COUNT
                + " hidden sessions: " + hiddenSessionNamesThatStartedAnEmulator,
            Collections.emptyList(), hiddenSessionNamesThatStartedAnEmulator);
    }

    private void seedRealisticSessionPopulation() throws Exception {
        for (int visibleIndex = 1; visibleIndex < VISIBLE_SESSION_COUNT; visibleIndex++) {
            shellManager.mTermuxSessions.add(uninitialisedSession(visibleSessionName(visibleIndex)));
        }
        for (int hiddenIndex = 1; hiddenIndex <= HIDDEN_SESSION_COUNT; hiddenIndex++) {
            shellManager.mTermuxSessions.add(uninitialisedSession(hiddenSessionName(hiddenIndex)));
        }
        markSessionNamesHidden(hiddenSessionNames().toArray(new String[0]));
        assertEquals("the seeded population must match the device the owner reports running",
            VISIBLE_SESSION_COUNT + HIDDEN_SESSION_COUNT, service.getTermuxSessionsSize());
    }

    private static List<String> hiddenSessionNames() {
        List<String> hiddenSessionNames = new ArrayList<>();
        for (int hiddenIndex = 1; hiddenIndex <= HIDDEN_SESSION_COUNT; hiddenIndex++) {
            hiddenSessionNames.add(hiddenSessionName(hiddenIndex));
        }
        return hiddenSessionNames;
    }

    private static String visibleSessionName(int index) {
        return String.format("session-visible-%02d", index);
    }

    private static String hiddenSessionName(int index) {
        return String.format("session-hidden-%02d", index);
    }

    private static List<String> sessionNamesOf(List<?> terminalSessions) {
        List<String> sessionNames = new ArrayList<>();
        for (Object terminalSession : terminalSessions) {
            sessionNames.add(terminalSession == null
                ? null
                : ((TerminalSession) terminalSession).mSessionName);
        }
        return sessionNames;
    }

    private List<?> collectSessionsToEagerLoad() throws Exception {
        Method collectSessionsToEagerLoad =
            TermuxActivity.class.getDeclaredMethod("collectSessionsToEagerLoad");
        collectSessionsToEagerLoad.setAccessible(true);
        return (List<?>) collectSessionsToEagerLoad.invoke(activity);
    }

    private void drainMainThreadTasksIgnoringMissingNativeLibrary() {
        ShadowLooper mainLooper = Shadows.shadowOf(Looper.getMainLooper());
        for (int task = 0; task < MAIN_THREAD_TASK_DRAIN_LIMIT && !mainLooper.isIdle(); task++) {
            try {
                mainLooper.runOneTask();
            } catch (Throwable missingNativeLibrary) {
                assertTrue("the only failure the eager load may raise in a Java virtual machine test "
                        + "is the absent device-only native library reached from the shell process "
                        + "fork, but it raised " + missingNativeLibrary,
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

    private TermuxSession uninitialisedSession(String sessionName) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, TRANSCRIPT_ROWS,
            activity.getTermuxTerminalSessionClient());
        terminalSession.mSessionName = sessionName;
        return termuxSession(terminalSession);
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

    private int cappedSessionCount() throws Exception {
        Method cappedSessionCount = TermuxTerminalSessionActivityClient.class
            .getDeclaredMethod("cappedSessionCount", TermuxService.class);
        cappedSessionCount.setAccessible(true);
        return (int) cappedSessionCount.invoke(activity.getTermuxTerminalSessionClient(), service);
    }

    private Set<?> visibleSessionNames() throws Exception {
        Method visibleSessionNames =
            TermuxTerminalSessionActivityClient.class.getDeclaredMethod("visibleSessionNames");
        visibleSessionNames.setAccessible(true);
        return (Set<?>) visibleSessionNames.invoke(activity.getTermuxTerminalSessionClient());
    }

    private void markSessionNamesHidden(String... sessionNames) {
        Set<String> hiddenSessionNames = new LinkedHashSet<>(Arrays.asList(sessionNames));
        preferences.setDisabledSessionNames(
            TermuxAppSharedPreferences.serializeDisabledSessionNames(hiddenSessionNames));
    }

    private void openSessionListWithOnScreenRows(String... onScreenSessionNames) throws Exception {
        OnScreenSessionListBottomSheetController bottomSheetController =
            new OnScreenSessionListBottomSheetController(activity, Arrays.asList(onScreenSessionNames));
        set(activity, TermuxActivity.class, "mSessionListBottomSheetController", bottomSheetController);
        activity.findViewById(R.id.session_list_bottom_sheet).setVisibility(View.VISIBLE);
        assertTrue("the session list must report itself open so the on-screen rows reach the "
            + "reconnect scheduler and statusline re-parse selection", bottomSheetController.isOpen());
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

    private int sigkilledShellProcessGroupTarget(TerminalSession terminalSession) throws Exception {
        Field target = TerminalSession.class.getDeclaredField("mSigkilledShellProcessGroupTarget");
        target.setAccessible(true);
        return target.getInt(terminalSession);
    }

    private TermuxSession liveSessionHoldingAnEmulator(String sessionName) throws Exception {
        TerminalSession terminalSession = terminalSessionHoldingAnEmulator(sessionName);
        setShellProcessId(terminalSession, UNREACHABLE_SHELL_PROCESS_ID);
        return termuxSession(terminalSession);
    }

    private TermuxSession deadSessionHoldingAnEmulator(String sessionName) throws Exception {
        TerminalSession terminalSession = terminalSessionHoldingAnEmulator(sessionName);
        setShellProcessId(terminalSession, -1);
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
}
