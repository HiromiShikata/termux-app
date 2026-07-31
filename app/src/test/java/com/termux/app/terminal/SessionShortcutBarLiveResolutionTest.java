package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
public class SessionShortcutBarLiveResolutionTest {

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
    public void compositeConfiguredAlwaysNaSessionThatExistsRendersAButtonThroughTheServiceLookup()
            throws Exception {
        String url = "https://github.com/HiromiShikata/secretary";
        shellManager.mTermuxSessions.add(session("uminopm"));
        shellManager.mTermuxSessions.add(session(url));
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("umino", "story", Collections.singletonList(url)));
        Set<String> alwaysNa = namesInOrder("umino/story");

        List<String> labels = renderedShortcutLabels(alwaysNa, entries);

        assertEquals(Arrays.asList("umino", "umino/story"), labels);
    }

    @Test
    public void bareConfiguredAlwaysNaSessionThatExistsStillRendersAButton() throws Exception {
        shellManager.mTermuxSessions.add(session("uminopm"));
        shellManager.mTermuxSessions.add(session("na-inbox"));
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("umino", "story",
                Collections.singletonList("https://example.test/x")));
        Set<String> alwaysNa = namesInOrder("na-inbox");

        List<String> labels = renderedShortcutLabels(alwaysNa, entries);

        assertEquals(Arrays.asList("umino", "na-inbox"), labels);
    }

    @Test
    public void compositeConfiguredAlwaysNaSessionThatIsNotLiveRendersNoButton() throws Exception {
        String url = "https://github.com/HiromiShikata/secretary";
        shellManager.mTermuxSessions.add(session("uminopm"));
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("umino", "story", Collections.singletonList(url)));
        Set<String> alwaysNa = namesInOrder("umino/story");

        List<String> labels = renderedShortcutLabels(alwaysNa, entries);

        assertEquals(Collections.singletonList("umino"), labels);
    }

    private List<String> renderedShortcutLabels(Set<String> alwaysNaSessionNames,
                                                List<SessionDefinitionEntry> entries) {
        List<String> liveSessionNames = new ArrayList<>();
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            liveSessionNames.add(termuxSession.getTerminalSession().mSessionName);
        }
        List<SessionShortcut> rightToLeftShortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries, liveSessionNames);
        Set<String> presentSessionNames = new LinkedHashSet<>();
        for (SessionShortcut shortcut : rightToLeftShortcuts) {
            if (service.getTermuxSessionForSessionName(shortcut.getTargetSessionName()) != null) {
                presentSessionNames.add(shortcut.getTargetSessionName());
            }
        }
        List<SessionShortcut> renderOrderShortcuts =
            SessionShortcutBarPlanner.renderOrderShortcuts(rightToLeftShortcuts);
        List<String> labels = new ArrayList<>();
        for (SessionShortcut shortcut : renderOrderShortcuts) {
            if (service.getTermuxSessionForSessionName(shortcut.getTargetSessionName()) != null) {
                labels.add(shortcut.getLabel());
            }
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
        TermuxSession termuxSession = constructor.newInstance(terminalSession, new ExecutionCommand(), null, false);
        assertNotNull(termuxSession.getTerminalSession());
        return termuxSession;
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
