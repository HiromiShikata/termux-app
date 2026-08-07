package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.browser.OpenTagBrowserController;
import com.termux.app.terminal.OffDeviceNativeSubprocessLibrary;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalBuffer;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(
    instrumentedPackages = {"com.termux.terminal.TerminalBuffer"},
    shadows = {ViewedSessionOutputMaterializesItsTranscriptOnceTest.CountingTranscriptShadow.class})
public class ViewedSessionOutputMaterializesItsTranscriptOnceTest {

    private static final String VIEWED_SESSION_NAME = "session-the-owner-is-watching";

    private static final int TRANSCRIPT_ROWS = 2000;

    private static final int SCREEN_COLUMNS = 80;

    private static final int SCREEN_ROWS = 24;

    private static final int SCROLLBACK_FILLER_ROWS = 120;

    private static final int LIVE_SHELL_PROCESS_ID = 4242;

    private static final int MAX_TRANSCRIPT_MATERIALIZATIONS_PER_OUTPUT_EVENT = 1;

    private static final long DRAIN_HORIZON_MILLIS = 30_000L;

    private static final int DRAIN_STEP_LIMIT = 500;

    static final List<TerminalBuffer> transcriptMaterializations =
        Collections.synchronizedList(new ArrayList<>());

    @Implements(TerminalBuffer.class)
    public static class CountingTranscriptShadow {

        @RealObject
        TerminalBuffer realBuffer;

        @Implementation
        protected String getTranscriptText() {
            transcriptMaterializations.add(realBuffer);
            return Shadow.directlyOn(realBuffer, TerminalBuffer.class, "getTranscriptText");
        }

        @Implementation
        protected String getTranscriptTextWithoutJoinedLines() {
            transcriptMaterializations.add(realBuffer);
            return Shadow.directlyOn(realBuffer, TerminalBuffer.class,
                "getTranscriptTextWithoutJoinedLines");
        }

        @Implementation
        protected String getTranscriptTextWithFullLinesJoined() {
            transcriptMaterializations.add(realBuffer);
            return Shadow.directlyOn(realBuffer, TerminalBuffer.class,
                "getTranscriptTextWithFullLinesJoined");
        }
    }

    private TermuxActivity activity;

    private TermuxTerminalSessionActivityClient sessionActivityClient;

    private TermuxShellManager shellManager;

    private TerminalSession viewedSession;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context appContext = RuntimeEnvironment.getApplication();

        TermuxService service = Robolectric.buildService(TermuxService.class).get();
        shellManager = new TermuxShellManager(appContext);
        set(service, TermuxService.class, "mShellManager", shellManager);
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        sessionActivityClient = new TermuxTerminalSessionActivityClient(activity);
        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient",
            sessionActivityClient);
        service.setTermuxTerminalSessionClient(activity.getTermuxTerminalSessionClient());

        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        preferences.setOpenTagAutoOpenEnabled(true);
        set(service, TermuxService.class, "mOpenTagBrowserController",
            new OpenTagBrowserController(preferences, (sessionHandle, url) -> { }));

        activity.setContentView(R.layout.activity_termux);
        set(activity, TermuxActivity.class, "mTerminalView",
            activity.findViewById(R.id.terminal_view));

        viewedSession = addSessionHoldingARenderedEmulator(VIEWED_SESSION_NAME);
        activity.getTerminalView().mTermSession = viewedSession;
        set(activity, TermuxActivity.class, "mIsVisible", true);

        drainEveryLooperToTheHorizon();
        transcriptMaterializations.clear();
    }

    @Test
    public void oneOutputEventOnTheViewedSessionMaterializesItsWholeScrollbackOnlyOnce() {
        sessionActivityClient.onTextChanged(viewedSession);
        drainEveryLooperToTheHorizon();

        int materializationsOfTheViewedSession = materializationsOfTheViewedSession();

        assertTrue("materializing a session's whole scrollback allocates a string as large as the "
                + "scrollback and copies every row into it, and the terminal emulator can only be read "
                + "from the thread that owns it, so every one of those materializations happens on the "
                + "main thread where the drawing happens. One output event on the session the owner is "
                + "watching needs that text once: the open-tag scan and the background output-tag scan "
                + "consume the same screen in the same main-thread message, so the second read is a "
                + "duplicate of the first and doubles what an output event costs the drawing thread. At "
                + "most " + MAX_TRANSCRIPT_MATERIALIZATIONS_PER_OUTPUT_EVENT + " materialization is "
                + "allowed per output event, yet this one cost " + materializationsOfTheViewedSession,
            materializationsOfTheViewedSession <= MAX_TRANSCRIPT_MATERIALIZATIONS_PER_OUTPUT_EVENT);

        assertEquals("an output event that materializes nothing satisfies the bound above without either "
                + "consumer having read anything, so the same run must also show the text really was "
                + "read once; the viewed session's transcript must be materialized exactly once",
            MAX_TRANSCRIPT_MATERIALIZATIONS_PER_OUTPUT_EVENT, materializationsOfTheViewedSession);
    }

    private int materializationsOfTheViewedSession() {
        Set<TerminalBuffer> buffersOfTheViewedSession = buffersOf(viewedSession);
        int materializations = 0;
        for (TerminalBuffer materializedBuffer : new ArrayList<>(transcriptMaterializations)) {
            if (buffersOfTheViewedSession.contains(materializedBuffer)) materializations++;
        }
        return materializations;
    }

    private Set<TerminalBuffer> buffersOf(TerminalSession session) {
        Set<TerminalBuffer> buffers = new LinkedHashSet<>();
        TerminalEmulator emulator = session.getEmulator();
        if (emulator == null) return buffers;
        buffers.add(emulator.getScreen());
        for (String bufferFieldName : new String[]{"mMainBuffer", "mAltBuffer"}) {
            try {
                Field bufferField = TerminalEmulator.class.getDeclaredField(bufferFieldName);
                bufferField.setAccessible(true);
                Object buffer = bufferField.get(emulator);
                if (buffer instanceof TerminalBuffer) buffers.add((TerminalBuffer) buffer);
            } catch (ReflectiveOperationException theEmulatorNoLongerHoldsThatBuffer) {
                continue;
            }
        }
        return buffers;
    }

    private TerminalSession addSessionHoldingARenderedEmulator(String sessionName) throws Exception {
        TerminalSession terminalSession =
            new TerminalSession("/system/bin/sh", "/", new String[0], new String[0], TRANSCRIPT_ROWS,
                activity.getTermuxTerminalSessionClient());
        terminalSession.mSessionName = sessionName;
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class,
            boolean.class);
        constructor.setAccessible(true);
        shellManager.mTermuxSessions.add(
            constructor.newInstance(terminalSession, new ExecutionCommand(), null, false));
        OffDeviceNativeSubprocessLibrary.tolerateItsAbsence(
            () -> terminalSession.initializeEmulator(SCREEN_COLUMNS, SCREEN_ROWS, 10, 20));
        Field shellProcessId = TerminalSession.class.getDeclaredField("mShellPid");
        shellProcessId.setAccessible(true);
        shellProcessId.setInt(terminalSession, LIVE_SHELL_PROCESS_ID);
        renderAScrollback(terminalSession);
        return terminalSession;
    }

    private void renderAScrollback(TerminalSession session) {
        StringBuilder rendered = new StringBuilder();
        for (int row = 0; row < SCROLLBACK_FILLER_ROWS; row++) {
            rendered.append("scrollback row ").append(row).append(" of ").append(session.mSessionName)
                .append("\r\n");
        }
        byte[] renderedBytes = rendered.toString().getBytes(StandardCharsets.UTF_8);
        session.getEmulator().append(renderedBytes, renderedBytes.length);
    }

    private void drainEveryLooperToTheHorizon() {
        long horizonUptimeMillis = SystemClock.uptimeMillis() + DRAIN_HORIZON_MILLIS;
        for (int step = 0; step < DRAIN_STEP_LIMIT; step++) {
            for (Looper looper : loopersTheClientOwnsBesidesTheMainOne()) {
                OffDeviceNativeSubprocessLibrary.tolerateItsAbsence(() -> shadowOf(looper).idle());
            }
            OffDeviceNativeSubprocessLibrary.tolerateItsAbsence(
                () -> shadowOf(Looper.getMainLooper()).idle());
            if (!everyLooperTheClientOwnsIsIdle()) continue;
            long nextTaskDueUptimeMillis =
                shadowOf(Looper.getMainLooper()).getNextScheduledTaskTime().toMillis();
            if (nextTaskDueUptimeMillis <= 0L) return;
            if (nextTaskDueUptimeMillis > horizonUptimeMillis) return;
            long millisToAdvance = Math.max(0L, nextTaskDueUptimeMillis - SystemClock.uptimeMillis());
            OffDeviceNativeSubprocessLibrary.tolerateItsAbsence(() -> shadowOf(Looper.getMainLooper())
                .idleFor(Duration.ofMillis(millisToAdvance)));
        }
    }

    private boolean everyLooperTheClientOwnsIsIdle() {
        for (Looper looper : loopersTheClientOwnsBesidesTheMainOne()) {
            if (!shadowOf(looper).isIdle()) return false;
        }
        return shadowOf(Looper.getMainLooper()).isIdle();
    }

    @NonNull
    private List<Looper> loopersTheClientOwnsBesidesTheMainOne() {
        List<Looper> loopers = new ArrayList<>();
        for (Field field : TermuxTerminalSessionActivityClient.class.getDeclaredFields()) {
            field.setAccessible(true);
            Object value;
            try {
                value = field.get(sessionActivityClient);
            } catch (ReflectiveOperationException theFieldIsNotReadable) {
                continue;
            }
            Looper looper = null;
            if (value instanceof HandlerThread) looper = ((HandlerThread) value).getLooper();
            if (value instanceof Handler) looper = ((Handler) value).getLooper();
            if (looper == null) continue;
            if (looper == Looper.getMainLooper()) continue;
            if (!loopers.contains(looper)) loopers.add(looper);
        }
        return loopers;
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value)
            throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
