package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public class SessionDefinitionAlwaysPresentPriorityPlannerTest {

    private final SessionDefinitionAlwaysPresentPriorityPlanner planner =
        new SessionDefinitionAlwaysPresentPriorityPlanner();

    private static List<String> names(List<SessionDefinitionPlannedSession> plannedSessions) {
        List<String> names = new ArrayList<>();
        for (SessionDefinitionPlannedSession plannedSession : plannedSessions) {
            names.add(plannedSession.getName());
        }
        return names;
    }

    private static SessionDefinitionPlannedSession definitionSession(String name) {
        return new SessionDefinitionPlannedSession(name, "autossh " + name);
    }

    @Test
    public void alwaysPresentSessionsArePlacedAheadOfDefinitionSessions() {
        List<SessionDefinitionPlannedSession> definitionSessions = Arrays.asList(
            definitionSession("uminopm"), definitionSession("xcarepm"));

        List<SessionDefinitionPlannedSession> merged = planner.planPrioritizedSessions(
            definitionSessions, new LinkedHashSet<>(Collections.singletonList("secretary")),
            "autossh {name}");

        Assert.assertEquals(Arrays.asList("secretary", "uminopm", "xcarepm"), names(merged));
    }

    @Test
    public void alwaysPresentSessionAlreadyInDefinitionListIsNotDuplicated() {
        List<SessionDefinitionPlannedSession> definitionSessions = Arrays.asList(
            definitionSession("uminopm"), definitionSession("secretary"));

        List<SessionDefinitionPlannedSession> merged = planner.planPrioritizedSessions(
            definitionSessions, new LinkedHashSet<>(Collections.singletonList("secretary")),
            "autossh {name}");

        Assert.assertEquals(Arrays.asList("secretary", "uminopm"), names(merged));
    }

    @Test
    public void blankAndWhitespaceAlwaysPresentNamesAreSkipped() {
        List<SessionDefinitionPlannedSession> definitionSessions =
            Collections.singletonList(definitionSession("uminopm"));

        List<SessionDefinitionPlannedSession> merged = planner.planPrioritizedSessions(
            definitionSessions, new LinkedHashSet<>(Arrays.asList("  ", "secretary", "")),
            "autossh {name}");

        Assert.assertEquals(Arrays.asList("secretary", "uminopm"), names(merged));
    }

    @Test
    public void plannedAlwaysPresentSessionCarriesItsAutosshCommandBuiltFromTheTemplate() {
        List<SessionDefinitionPlannedSession> merged = planner.planPrioritizedSessions(
            Collections.emptyList(), new LinkedHashSet<>(Collections.singletonList("secretary")),
            "tmux attach -t {name}");

        Assert.assertEquals(1, merged.size());
        SessionDefinitionPlannedSession secretary = merged.get(0);
        Assert.assertEquals("secretary", secretary.getName());
        Assert.assertTrue(secretary.hasCommand());
        Assert.assertTrue(secretary.getCommand().contains("secretary"));
    }

    @Test
    public void noAlwaysPresentNamesLeavesDefinitionListUnchanged() {
        List<SessionDefinitionPlannedSession> definitionSessions = Arrays.asList(
            definitionSession("uminopm"), definitionSession("xcarepm"));

        List<SessionDefinitionPlannedSession> merged = planner.planPrioritizedSessions(
            definitionSessions, Collections.emptySet(), "autossh {name}");

        Assert.assertEquals(Arrays.asList("uminopm", "xcarepm"), names(merged));
    }

    @Test
    public void alwaysPresentNamesKeepTheirConfiguredOrderAheadOfDefinitions() {
        List<SessionDefinitionPlannedSession> definitionSessions =
            Collections.singletonList(definitionSession("uminopm"));

        List<SessionDefinitionPlannedSession> merged = planner.planPrioritizedSessions(
            definitionSessions, new LinkedHashSet<>(Arrays.asList("secretary", "inbox")),
            "autossh {name}");

        Assert.assertEquals(Arrays.asList("secretary", "inbox", "uminopm"), names(merged));
    }
}
