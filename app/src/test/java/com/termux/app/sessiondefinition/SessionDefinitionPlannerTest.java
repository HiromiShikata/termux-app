package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SessionDefinitionPlannerTest {

    private final SessionDefinitionPlanner planner = new SessionDefinitionPlanner();

    @Test
    public void planCreatesOnePlannedSessionPerUrlNamedByUrlPreservingOrder() {
        List<SessionDefinitionEntry> entries = new ArrayList<>();
        entries.add(new SessionDefinitionEntry("groupOne", "entryA",
            Arrays.asList("https://example.test/a1", "https://example.test/a2")));
        entries.add(new SessionDefinitionEntry("groupTwo", "entryB",
            Collections.singletonList("https://example.test/b1")));

        List<SessionDefinitionPlannedSession> plannedSessions = planner.plan(entries, "");

        Assert.assertEquals(3, plannedSessions.size());
        Assert.assertEquals("https://example.test/a1", plannedSessions.get(0).getName());
        Assert.assertEquals("https://example.test/a2", plannedSessions.get(1).getName());
        Assert.assertEquals("https://example.test/b1", plannedSessions.get(2).getName());
        Assert.assertFalse(plannedSessions.get(0).hasCommand());
    }

    @Test
    public void planWithoutTemplateProducesPlainSessions() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Collections.singletonList("https://example.test/a1")));

        List<SessionDefinitionPlannedSession> plannedSessions = planner.plan(entries, "   ");

        Assert.assertEquals(1, plannedSessions.size());
        Assert.assertEquals("https://example.test/a1", plannedSessions.get(0).getName());
        Assert.assertNull(plannedSessions.get(0).getCommand());
        Assert.assertFalse(plannedSessions.get(0).hasCommand());
    }

    @Test
    public void planWithTemplateSubstitutesShellQuotedUrlIntoNamePlaceholder() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Collections.singletonList("https://example.test/a1")));

        List<SessionDefinitionPlannedSession> plannedSessions =
            planner.plan(entries, "connect {name}");

        Assert.assertEquals(1, plannedSessions.size());
        Assert.assertEquals("https://example.test/a1", plannedSessions.get(0).getName());
        Assert.assertTrue(plannedSessions.get(0).hasCommand());
        Assert.assertEquals(
            "connect 'https://example.test/a1'",
            plannedSessions.get(0).getCommand());
    }

    @Test
    public void planSkipsEntryWithoutUrls() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA", Collections.emptyList()));

        List<SessionDefinitionPlannedSession> plannedSessions =
            planner.plan(entries, "connect {name}");

        Assert.assertTrue(plannedSessions.isEmpty());
    }

    @Test
    public void shellQuoteEscapesSingleQuotes() {
        Assert.assertEquals("'a'\\''b'", SessionDefinitionPlanner.shellQuote("a'b"));
    }

    @Test
    public void planNamedSessionBuildsAutosshCommandWhenTemplateConfigured() {
        SessionDefinitionPlannedSession plannedSession =
            planner.planNamedSession("https://example.test/a1", "connect {name}");

        Assert.assertEquals("https://example.test/a1", plannedSession.getName());
        Assert.assertTrue(plannedSession.hasCommand());
        Assert.assertEquals("connect 'https://example.test/a1'", plannedSession.getCommand());
    }

    @Test
    public void planNamedSessionTrimsTemplateBeforeBuildingCommand() {
        SessionDefinitionPlannedSession plannedSession =
            planner.planNamedSession("https://example.test/a1", "  connect {name}  ");

        Assert.assertEquals("connect 'https://example.test/a1'", plannedSession.getCommand());
    }

    @Test
    public void planNamedSessionProducesPlainSessionWhenTemplateBlank() {
        SessionDefinitionPlannedSession plannedSession =
            planner.planNamedSession("https://example.test/a1", "   ");

        Assert.assertEquals("https://example.test/a1", plannedSession.getName());
        Assert.assertFalse(plannedSession.hasCommand());
    }

    @Test
    public void planNamedSessionProducesPlainSessionWhenTemplateNull() {
        SessionDefinitionPlannedSession plannedSession =
            planner.planNamedSession("https://example.test/a1", null);

        Assert.assertFalse(plannedSession.hasCommand());
    }

    @Test
    public void planNamedSessionProducesPlainSessionWhenNameNull() {
        SessionDefinitionPlannedSession plannedSession =
            planner.planNamedSession(null, "connect {name}");

        Assert.assertNull(plannedSession.getName());
        Assert.assertFalse(plannedSession.hasCommand());
    }
}
