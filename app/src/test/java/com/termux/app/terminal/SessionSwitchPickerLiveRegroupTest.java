package com.termux.app.terminal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Looper;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.sessiondefinition.SessionDefinitionDocumentFetcher;
import com.termux.app.sessiondefinition.SessionDefinitionLoader;
import com.termux.app.sessiondefinition.SessionDefinitionParser;
import com.termux.app.sessiondefinition.SessionDefinitionRepository;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class SessionSwitchPickerLiveRegroupTest {

    private static final String INDEX_URL = "https://example.test/index.json";
    private static final String GROUP_URL = "https://example.test/group.json";
    private static final String SESSION_URL = "https://example.test/live";
    private static final String SECOND_SESSION_URL = "https://example.test/second";

    private TermuxActivity activity;
    private MutableFetcher fetcher;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        activity.setContentView(LayoutInflater.from(activity).inflate(
            R.layout.activity_termux, new FrameLayout(activity), false));
        set(activity, TermuxActivity.class, "mSessionSwitchPickerController",
            new SessionSwitchPickerController(activity));
        TermuxService service = Robolectric.buildService(TermuxService.class).get();
        Context appContext = RuntimeEnvironment.getApplication();
        set(service, TermuxService.class, "mShellManager", new TermuxShellManager(appContext));
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(appContext));
        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mTermuxSessionListViewController",
            new TermuxSessionsListViewController(activity, service.getTermuxSessions()));
        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(appContext, true);
        preferences.setSessionSwitchOverlayEnabled(true);
        preferences.setSessionSwitchPreviewFirstEnabled(true);
        preferences.setSessionDefinitionUrl(INDEX_URL);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        fetcher = new MutableFetcher();
        fetcher.documents.put(INDEX_URL, "{\"projects\":[\"group\"]}");
        fetcher.documents.put(GROUP_URL, groupDocument("storyA"));
        set(activity, TermuxActivity.class, "mSessionDefinitionRepository",
            new SessionDefinitionRepository(new SessionDefinitionLoader(fetcher,
                new SessionDefinitionParser())));
        service.getTermuxSessions().add(session(SESSION_URL));
        loadDefinition();
    }

    @Test
    public void pickerShowsNewStoryAfterGroupingRefresh() throws Exception {
        fetcher.documents.put(GROUP_URL, groupDocument("storyB"));
        SessionSwitchPickerController controller = activity.getSessionSwitchPickerController();
        controller.onVolumeKeyDirection(true);
        assertTrue(controller.isShowing());
        TextView structureView = activity.findViewById(R.id.session_switch_picker_structure);
        assertTrue(structureView.getText().toString().contains("storyA"));

        long deadlineNanos = System.nanoTime() + 10000L * 1000000L;
        while (!structureView.getText().toString().contains("storyB")
            && System.nanoTime() < deadlineNanos) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(10);
        }

        String structureText = structureView.getText().toString();
        assertTrue(structureText.contains("storyB"));
        assertFalse(structureText.contains("storyA"));
        assertTrue(controller.isShowing());
    }

    @Test
    public void pickerKeepsHighlightOnSessionIdentityAfterGroupingRefresh() throws Exception {
        fetcher.documents.put(GROUP_URL, groupDocument("storyA", SESSION_URL, SECOND_SESSION_URL));
        activity.getTermuxService().getTermuxSessions().add(session(SECOND_SESSION_URL));
        SessionSwitchPickerController controller = activity.getSessionSwitchPickerController();
        controller.onVolumeKeyDirection(true);

        TextView structureView = activity.findViewById(R.id.session_switch_picker_structure);
        waitForStructureText(structureView, SECOND_SESSION_URL);

        fetcher.documents.put(GROUP_URL, groupDocument("storyA", SECOND_SESSION_URL, SESSION_URL));
        activity.refreshSessionGroupingFromDefinition();

        long deadlineNanos = System.nanoTime() + 10000L * 1000000L;
        List<SessionPickerOverlayLine> lines = pickerLines(controller);
        while ((!sessionNames(lines).equals(List.of(SECOND_SESSION_URL, SESSION_URL))
            || !highlightedSessionName(lines).equals(SESSION_URL))
            && System.nanoTime() < deadlineNanos) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(10);
            lines = pickerLines(controller);
        }

        assertEquals(List.of(SECOND_SESSION_URL, SESSION_URL), sessionNames(lines));
        assertEquals(SESSION_URL, highlightedSessionName(lines));
    }

    @Test
    public void pickerCommitTimerStillHidesAfterGroupingRefresh() throws Exception {
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient",
            new TermuxTerminalSessionActivityClient(activity));
        SessionSwitchPickerController controller = activity.getSessionSwitchPickerController();
        controller.onVolumeKeyDirection(true);
        activity.getPreferences().setSessionSwitchPreviewFirstEnabled(false);
        drainMainLooperForCommit();
        assertFalse(controller.isShowing());

        activity.getPreferences().setSessionSwitchPreviewFirstEnabled(true);
        controller.onVolumeKeyDirection(true);
        activity.getPreferences().setSessionSwitchPreviewFirstEnabled(false);
        activity.refreshSessionGroupingFromDefinition();
        drainMainLooperForCommit();
        assertFalse(controller.isShowing());
    }

    private void loadDefinition() throws Exception {
        SessionDefinitionRepository repository = (SessionDefinitionRepository) get(
            activity, TermuxActivity.class, "mSessionDefinitionRepository");
        repository.loadForRebuild(INDEX_URL, false,
            result -> activity.refreshSessionGroupingEntries(result.getEntries()),
            exception -> { throw new AssertionError(exception); });
        drainMainLooper(1);
        fetcher.indexFetchCalls = 0;
    }

    private void drainMainLooper(int expectedFetchCount) throws Exception {
        SessionDefinitionRepository repository = (SessionDefinitionRepository) get(
            activity, TermuxActivity.class, "mSessionDefinitionRepository");
        long deadlineNanos = System.nanoTime() + 10000L * 1000000L;
        while ((fetcher.indexFetchCalls < expectedFetchCount || !repository.isLoaded()
            || repository.isLoading()) && System.nanoTime() < deadlineNanos) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(10);
        }
    }

    private static String groupDocument(String story) {
        return groupDocument(story, SESSION_URL);
    }

    private static String groupDocument(String story, String... sessionUrls) {
        StringBuilder document = new StringBuilder("[{\"story\":\"" + story + "\",\"urls\":[");
        for (int index = 0; index < sessionUrls.length; index++) {
            if (index > 0) document.append(',');
            document.append("\"").append(sessionUrls[index]).append("\"");
        }
        return document.append("]}]").toString();
    }

    private void waitForStructureText(TextView structureView, String expectedText) throws Exception {
        long deadlineNanos = System.nanoTime() + 10000L * 1000000L;
        while (!structureView.getText().toString().contains(expectedText)
            && System.nanoTime() < deadlineNanos) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(10);
        }
        assertTrue(structureView.getText().toString().contains(expectedText));
    }

    private void drainMainLooperForCommit() {
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(
            SessionSwitchPickerController.COMMIT_DELAY_MILLISECONDS + 100L,
            TimeUnit.MILLISECONDS);
    }

    private List<SessionPickerOverlayLine> pickerLines(SessionSwitchPickerController controller)
        throws Exception {
        TermuxSessionsListViewController listController = activity.getTermuxSessionListViewController();
        int highlightedSessionIndex = (int) get(controller,
            SessionSwitchPickerController.class, "mHighlightedSessionIndex");
        return SessionPickerOverlayRenderModel.build(listController.getVisibleRows(),
            listController.getSessionRows(), highlightedSessionIndex);
    }

    private static List<String> sessionNames(List<SessionPickerOverlayLine> lines) {
        List<String> names = new ArrayList<>();
        for (SessionPickerOverlayLine line : lines) {
            if (line.getKind() == SessionPickerOverlayLine.Kind.SESSION) {
                names.add(line.getText());
            }
        }
        return names;
    }

    private static String highlightedSessionName(List<SessionPickerOverlayLine> lines) {
        for (SessionPickerOverlayLine line : lines) {
            if (line.getKind() == SessionPickerOverlayLine.Kind.SESSION && line.isHighlighted()) {
                return line.getText();
            }
        }
        return "";
    }

    private static TermuxSession session(String name) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = name;
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(terminalSession, -1);
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class,
            boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(terminalSession, new ExecutionCommand(), null, false);
    }

    private static Object get(Object target, Class<?> declaringClass, String fieldName) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void set(Object target, Class<?> declaringClass, String fieldName, Object value)
        throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class MutableFetcher implements SessionDefinitionDocumentFetcher {
        private final Map<String, String> documents = new HashMap<>();
        private volatile int indexFetchCalls;

        @Override
        public String fetch(String url) throws IOException {
            if (INDEX_URL.equals(url)) {
                indexFetchCalls++;
            }
            String document = documents.get(url);
            if (document == null) {
                throw new IOException("missing document: " + url);
            }
            return document;
        }
    }
}
