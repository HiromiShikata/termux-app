package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class HiddenSessionRowTapUnhidesExactlyThatNameTest {

    private static final String DISPLAYED_SESSION_NAME = "displayed-agent";
    private static final String TAPPED_HIDDEN_SESSION_NAME = "tapped-hidden-agent";
    private static final String UNTOUCHED_HIDDEN_SESSION_NAME = "untouched-hidden-agent";

    private static final int TERMINAL_COLUMNS = 80;
    private static final int TERMINAL_ROWS = 40;
    private static final int TERMINAL_CELL_WIDTH_PIXELS = 12;
    private static final int TERMINAL_CELL_HEIGHT_PIXELS = 24;
    private static final Integer TRANSCRIPT_ROWS = 2000;
    private static final int UNREACHABLE_SHELL_PROCESS_ID = 2000000000;
    private static final int NO_ROW_POSITION = -1;

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
        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));
        set(activity, TermuxActivity.class, "mIsVisible", true);

        preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        preferences.setAutosshCommand("ssh {name}");
        preferences.setDisabledSessionNames(TermuxAppSharedPreferences.serializeDisabledSessionNames(
            namesInOrder(TAPPED_HIDDEN_SESSION_NAME, UNTOUCHED_HIDDEN_SESSION_NAME)));

        activity.setContentView(bottomSheetStubContentView(appContext));

        TerminalView terminalView = new TerminalView(appContext, null);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);
        layOutTerminalViewForEmulatorSizeComputation();

        TermuxSession displayedSession = liveSessionHoldingAnEmulator(DISPLAYED_SESSION_NAME);
        shellManager.mTermuxSessions.add(displayedSession);
        terminalView.mTermSession = displayedSession.getTerminalSession();

        listViewController = new TermuxSessionsListViewController(activity, service.getTermuxSessions());
        set(activity, TermuxActivity.class, "mTermuxSessionListViewController", listViewController);
        listViewController.setEntries(Collections.emptyList());
    }

    @Test
    public void tappingTheRowOfAHiddenNameClearsTheHiddenMarkOfThatNameAndOfNoOtherName()
            throws Exception {
        listViewController.refreshSessionList();

        assertTrue("the arrangement must record both names as hidden, otherwise the assertions below "
                + "would hold without the tap doing anything; the stored hidden set was "
                + preferences.getDisabledSessionNames(),
            preferences.getDisabledSessionNames().containsAll(
                Arrays.asList(TAPPED_HIDDEN_SESSION_NAME, UNTOUCHED_HIDDEN_SESSION_NAME)));

        int tappedRowPosition = rowPositionOfSessionName(TAPPED_HIDDEN_SESSION_NAME);
        assertTrue("the row the owner taps to unhide a name must exist while the entry list is empty, "
                + "because otherwise he has nothing to act on and has to retype the name; the rows were "
                + rowSessionNames(), tappedRowPosition != NO_ROW_POSITION);

        tapRowThroughProductionEntryPoint(tappedRowPosition);

        Set<String> hiddenSessionNamesAfterTheTap = preferences.getDisabledSessionNames();
        assertFalse("acting on the row of a hidden name is the owner's way of unhiding it without "
                + "retyping it, so the tap must clear the hidden mark of that name; leaving it set keeps "
                + "the name out of the always-present restore and out of the session creation call while "
                + "the owner is looking at the row he just tapped; the stored hidden set was "
                + hiddenSessionNamesAfterTheTap,
            hiddenSessionNamesAfterTheTap.contains(TAPPED_HIDDEN_SESSION_NAME));
        assertTrue("unhiding one name must leave every other name the owner hid still hidden, because "
                + "clearing them all would bring back sessions he deliberately put away and give each "
                + "of them a shell process, a terminal emulator and a session cap slot; the stored "
                + "hidden set was " + hiddenSessionNamesAfterTheTap,
            hiddenSessionNamesAfterTheTap.contains(UNTOUCHED_HIDDEN_SESSION_NAME));
        assertEquals("the tap must clear exactly one hidden mark, so the stored hidden set must hold "
                + "exactly the names the owner did not act on; the stored hidden set was "
                + hiddenSessionNamesAfterTheTap,
            Collections.singleton(UNTOUCHED_HIDDEN_SESSION_NAME), hiddenSessionNamesAfterTheTap);
    }

    private void tapRowThroughProductionEntryPoint(int rowPosition) throws Exception {
        Method onSessionRowClicked = TermuxSessionsListViewController.class
            .getDeclaredMethod("onSessionRowClicked", int.class);
        onSessionRowClicked.setAccessible(true);
        try {
            onSessionRowClicked.invoke(listViewController, rowPosition);
        } catch (Throwable missingNativeLibrary) {
            assertTrue("the only failure the tap may raise in a Java virtual machine test is the absent "
                    + "device-only native library reached from the shell process fork, but it raised "
                    + missingNativeLibrary,
                causeChainDescription(missingNativeLibrary).contains("com.termux.terminal.JNI"));
        }
    }

    private int rowPositionOfSessionName(String sessionName) throws Exception {
        List<SessionHierarchyRow> rows = rows();
        for (int position = 0; position < rows.size(); position++) {
            SessionHierarchyRow row = rows.get(position);
            if (!row.isHeader() && sessionName.equals(row.getSessionName())) {
                return position;
            }
        }
        return NO_ROW_POSITION;
    }

    private List<String> rowSessionNames() throws Exception {
        List<String> rowSessionNames = new ArrayList<>();
        for (SessionHierarchyRow row : rows()) {
            rowSessionNames.add(row.isHeader() ? row.getLabel() : row.getSessionName());
        }
        return rowSessionNames;
    }

    @SuppressWarnings("unchecked")
    private List<SessionHierarchyRow> rows() throws Exception {
        Field rows = TermuxSessionsListViewController.class.getDeclaredField("mRows");
        rows.setAccessible(true);
        return (List<SessionHierarchyRow>) rows.get(listViewController);
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
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class,
            boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(terminalSession, new ExecutionCommand(), null, false);
    }

    private static Set<String> namesInOrder(String... names) {
        return new LinkedHashSet<>(Arrays.asList(names));
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value)
            throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
