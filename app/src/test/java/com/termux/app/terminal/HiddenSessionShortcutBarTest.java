package com.termux.app.terminal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import com.termux.app.TermuxService;
import com.termux.app.sessiondefinition.DefaultProjectManagerSessionPlanner;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class HiddenSessionShortcutBarTest {

    private static final String PROJECT_LABEL = "umino";
    private static final String PROJECT_MANAGER_SESSION_NAME = "uminopm";
    private static final String STORY_LABEL = "story";
    private static final String STORY_SESSION_URL = "https://github.com/HiromiShikata/secretary";
    private static final String COMPOSITE_ENTRY_SESSION_NAME = PROJECT_LABEL + "/" + STORY_LABEL;
    private static final String HIDDEN_AGENT_SESSION_NAME = "hidden-agent";
    private static final String OTHER_AGENT_SESSION_NAME = "other-agent";

    private TermuxService service;
    private TermuxShellManager shellManager;
    private final SessionShortcutBarPlanner planner =
        new SessionShortcutBarPlanner(new DefaultProjectManagerSessionPlanner());

    @Before
    public void setUp() throws Exception {
        Context appContext = RuntimeEnvironment.getApplication();
        service = Robolectric.buildService(TermuxService.class).get();
        shellManager = new TermuxShellManager(appContext);
        set(service, TermuxService.class, "mShellManager", shellManager);
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(appContext));
    }

    @Test
    public void hidingOneSessionLeavesEveryOtherShortcutDisplayedIncludingTheProjectManagerShortcut()
            throws Exception {
        shellManager.mTermuxSessions.add(session(PROJECT_MANAGER_SESSION_NAME));
        shellManager.mTermuxSessions.add(session(OTHER_AGENT_SESSION_NAME));
        shellManager.mTermuxSessions.add(session(STORY_SESSION_URL));
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry(PROJECT_LABEL, STORY_LABEL,
                Collections.singletonList(STORY_SESSION_URL)));
        Set<String> alwaysNotApplicableSessionNames =
            namesInOrder(HIDDEN_AGENT_SESSION_NAME, OTHER_AGENT_SESSION_NAME);

        List<SessionShortcut> renderedShortcuts =
            renderedShortcuts(alwaysNotApplicableSessionNames, entries);

        assertTrue("hiding one session must leave the project manager shortcut of every project displayed, "
                + "because that shortcut targets a different session that hiding never touched, and its "
                + "label is the project label: " + labelsOf(renderedShortcuts),
            labelsOf(renderedShortcuts).contains(PROJECT_LABEL));
        assertTrue("hiding one session must leave every other session's own shortcut displayed: "
                + labelsOf(renderedShortcuts),
            labelsOf(renderedShortcuts).contains(OTHER_AGENT_SESSION_NAME));
    }

    @Test
    public void hidingACompositeAlwaysNotApplicableSessionKeepsTheOwnShortcutOfTheLiveSessionItsEntryNames()
            throws Exception {
        shellManager.mTermuxSessions.add(session(PROJECT_MANAGER_SESSION_NAME));
        shellManager.mTermuxSessions.add(session(STORY_SESSION_URL));
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry(PROJECT_LABEL, STORY_LABEL,
                Collections.singletonList(STORY_SESSION_URL)));
        Set<String> alwaysNotApplicableSessionNames =
            namesInOrder(COMPOSITE_ENTRY_SESSION_NAME, STORY_SESSION_URL);

        List<SessionShortcut> renderedShortcuts =
            renderedShortcuts(alwaysNotApplicableSessionNames, entries);

        SessionShortcut shortcutForTheStillLiveSession =
            shortcutLabelled(renderedShortcuts, STORY_SESSION_URL);
        assertNotNull("hiding the composite always-not-applicable session must not take away the shortcut of "
                + "the still live session its entry names, yet the hidden name is retargeted onto that live "
                + "session and the bar then deduplicates by target, so the live session loses its own "
                + "shortcut: " + labelsOf(renderedShortcuts), shortcutForTheStillLiveSession);
        assertTrue("the shortcut of the still live session must target that same session",
            STORY_SESSION_URL.equals(shortcutForTheStillLiveSession.getTargetSessionName()));
    }

    private List<SessionShortcut> renderedShortcuts(Set<String> alwaysNotApplicableSessionNames,
                                                    List<SessionDefinitionEntry> entries) {
        List<String> liveSessionNames = new ArrayList<>();
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            liveSessionNames.add(termuxSession.getTerminalSession().mSessionName);
        }
        List<SessionShortcut> rightToLeftShortcuts =
            planner.planRightToLeftShortcuts(alwaysNotApplicableSessionNames, entries, liveSessionNames);
        Set<String> presentSessionNames = new LinkedHashSet<>();
        for (SessionShortcut shortcut : rightToLeftShortcuts) {
            if (service.getTermuxSessionForSessionName(shortcut.getTargetSessionName()) != null) {
                presentSessionNames.add(shortcut.getTargetSessionName());
            }
        }
        List<SessionShortcut> renderOrderShortcuts =
            SessionShortcutBarPlanner.renderOrderPresentShortcuts(rightToLeftShortcuts, presentSessionNames);
        List<SessionShortcut> renderedShortcuts = new ArrayList<>();
        for (SessionShortcut shortcut : renderOrderShortcuts) {
            if (service.getTermuxSessionForSessionName(shortcut.getTargetSessionName()) != null) {
                renderedShortcuts.add(shortcut);
            }
        }
        return renderedShortcuts;
    }

    private static SessionShortcut shortcutLabelled(List<SessionShortcut> shortcuts, String label) {
        for (SessionShortcut shortcut : shortcuts) {
            if (label.equals(shortcut.getLabel())) {
                return shortcut;
            }
        }
        return null;
    }

    private static List<String> labelsOf(List<SessionShortcut> shortcuts) {
        List<String> labels = new ArrayList<>();
        for (SessionShortcut shortcut : shortcuts) {
            labels.add(shortcut.getLabel());
        }
        return labels;
    }

    private static Set<String> namesInOrder(String... names) {
        return new LinkedHashSet<>(Arrays.asList(names));
    }

    private TermuxSession session(String name) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = name;
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(terminalSession, -1);
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(terminalSession, new ExecutionCommand(), null, false);
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
