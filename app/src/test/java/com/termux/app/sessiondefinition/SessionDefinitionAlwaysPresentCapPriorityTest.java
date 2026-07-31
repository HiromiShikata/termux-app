package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import com.termux.app.TermuxService;
import com.termux.app.terminal.SessionShortcut;
import com.termux.app.terminal.SessionShortcutBarPlanner;
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
public class SessionDefinitionAlwaysPresentCapPriorityTest {

    private TermuxService service;
    private TermuxShellManager shellManager;
    private final SessionDefinitionAlwaysPresentPriorityPlanner alwaysPresentPriorityPlanner =
        new SessionDefinitionAlwaysPresentPriorityPlanner();
    private final SessionShortcutBarPlanner shortcutBarPlanner =
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
    public void underCapPressureTheLeftoverCapacitySecondPassStarvesTheAlwaysNaSecretarySession() {
        // Reproduces the pre-fix behavior: definition/PM sessions consume the whole cap budget in the
        // first pass, so the Always-N/A "secretary" session, created only in the leftover-capacity second
        // pass, is never created and therefore has no live session to render a shortcut for.
        List<SessionDefinitionPlannedSession> definitionSessions = Arrays.asList(
            definitionSession("uminopm"), definitionSession("xcarepm"));
        Set<String> alwaysNa = new LinkedHashSet<>(Collections.singletonList("secretary"));
        int cap = 2;

        List<String> createdInFirstPass = createFirstPassSessions(definitionSessions, cap);
        int remainingCapacity = Math.max(0, cap - createdInFirstPass.size());

        assertTrue("definition/PM sessions must consume the whole budget", createdInFirstPass.contains("uminopm"));
        assertTrue("definition/PM sessions must consume the whole budget", createdInFirstPass.contains("xcarepm"));
        assertFalse("pre-fix: secretary is not created in the capacity-consuming first pass",
            createdInFirstPass.contains("secretary"));
        // Second pass has no leftover capacity, so secretary stays starved.
        boolean secretaryCreatedInSecondPass = remainingCapacity > 0 && alwaysNa.contains("secretary");
        assertFalse("pre-fix: secretary starves because the second pass has no leftover capacity",
            secretaryCreatedInSecondPass);
        assertTrue("the shortcut bar draws every configured shortcut, so the secretary shortcut renders"
                + " even while the cap starved its session; tapping it opens the session from the"
                + " session definition",
            renderedShortcutLabels(alwaysNa, definitionSessions).contains("secretary"));
    }

    @Test
    public void prioritizingAlwaysPresentSessionsMakesSecretaryLiveUnderCapAndItsShortcutRenders() {
        // Post-fix behavior: the Always-N/A "secretary" session is placed ahead of a marginal definition
        // session in the same capacity budget, so under cap pressure secretary is created (becomes a live
        // TermuxSession named "secretary") and the existing shortcut bar renders its button.
        List<SessionDefinitionPlannedSession> definitionSessions = Arrays.asList(
            definitionSession("uminopm"), definitionSession("xcarepm"));
        Set<String> alwaysNa = new LinkedHashSet<>(Collections.singletonList("secretary"));
        int cap = 2;

        List<SessionDefinitionPlannedSession> prioritized = alwaysPresentPriorityPlanner.planPrioritizedSessions(
            definitionSessions, alwaysNa, "autossh {name}");
        List<String> created = createSessionsWithinCap(prioritized, cap);

        assertTrue("post-fix: secretary wins the budget and is created", created.contains("secretary"));
        assertNotNull("post-fix: secretary is a live tracked TermuxSession named exactly secretary",
            service.getTermuxSessionForSessionName("secretary"));

        List<String> labels = renderedShortcutLabels(alwaysNa, definitionSessions);
        assertTrue("post-fix: the Always-N/A secretary shortcut renders", labels.contains("secretary"));
    }

    private List<String> createFirstPassSessions(List<SessionDefinitionPlannedSession> definitionSessions,
                                                 int cap) {
        return createSessionsWithinCap(definitionSessions, cap);
    }

    private List<String> createSessionsWithinCap(List<SessionDefinitionPlannedSession> plannedSessions, int cap) {
        List<String> created = new ArrayList<>();
        for (SessionDefinitionPlannedSession plannedSession : plannedSessions) {
            if (service.getTermuxSessionsSize() >= cap) {
                break;
            }
            try {
                shellManager.mTermuxSessions.add(session(plannedSession.getName()));
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
            created.add(plannedSession.getName());
        }
        return created;
    }

    private List<String> renderedShortcutLabels(Set<String> alwaysNa,
                                                List<SessionDefinitionPlannedSession> definitionSessions) {
        List<SessionDefinitionEntry> entries = new ArrayList<>();
        for (SessionDefinitionPlannedSession definitionSession : definitionSessions) {
            String pmName = definitionSession.getName();
            String projectLabel = pmName.endsWith("pm") ? pmName.substring(0, pmName.length() - 2) : pmName;
            entries.add(new SessionDefinitionEntry(projectLabel, "story",
                Collections.singletonList("https://example.test/" + projectLabel)));
        }
        List<String> liveSessionNames = new ArrayList<>();
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            liveSessionNames.add(termuxSession.getTerminalSession().mSessionName);
        }
        List<SessionShortcut> rightToLeftShortcuts =
            shortcutBarPlanner.planRightToLeftShortcuts(alwaysNa, entries, liveSessionNames);
        List<SessionShortcut> renderOrderShortcuts =
            SessionShortcutBarPlanner.renderOrderShortcuts(rightToLeftShortcuts);
        List<String> labels = new ArrayList<>();
        for (SessionShortcut shortcut : renderOrderShortcuts) {
            labels.add(shortcut.getLabel());
        }
        return labels;
    }

    private static SessionDefinitionPlannedSession definitionSession(String name) {
        return new SessionDefinitionPlannedSession(name, "autossh " + name);
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
