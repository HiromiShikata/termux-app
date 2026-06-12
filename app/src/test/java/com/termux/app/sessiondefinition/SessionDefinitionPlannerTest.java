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
    public void planCreatesOnePlannedSessionPerUrlPreservingOrder() {
        List<SessionDefinitionEntry> entries = new ArrayList<>();
        entries.add(new SessionDefinitionEntry("groupOne", "entryA",
            Arrays.asList("https://example.test/a1", "https://example.test/a2")));
        entries.add(new SessionDefinitionEntry("groupTwo", "entryB",
            Collections.singletonList("https://example.test/b1")));

        List<SessionDefinitionPlannedSession> plannedSessions = planner.plan(entries, "");

        Assert.assertEquals(3, plannedSessions.size());
        Assert.assertEquals("groupOne/entryA 1", plannedSessions.get(0).getName());
        Assert.assertEquals("groupOne/entryA 2", plannedSessions.get(1).getName());
        Assert.assertEquals("groupTwo/entryB", plannedSessions.get(2).getName());
        Assert.assertFalse(plannedSessions.get(0).hasCommand());
    }

    @Test
    public void planWithoutTemplateProducesPlainSessions() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Collections.singletonList("https://example.test/a1")));

        List<SessionDefinitionPlannedSession> plannedSessions = planner.plan(entries, "   ");

        Assert.assertEquals(1, plannedSessions.size());
        Assert.assertNull(plannedSessions.get(0).getCommand());
        Assert.assertFalse(plannedSessions.get(0).hasCommand());
    }

    @Test
    public void planWithTemplateSubstitutesShellQuotedUrlAndName() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Collections.singletonList("https://example.test/a1")));

        List<SessionDefinitionPlannedSession> plannedSessions =
            planner.plan(entries, "connect {url} as {name}");

        Assert.assertEquals(1, plannedSessions.size());
        Assert.assertTrue(plannedSessions.get(0).hasCommand());
        Assert.assertEquals(
            "connect 'https://example.test/a1' as 'groupOne/entryA'",
            plannedSessions.get(0).getCommand());
    }

    @Test
    public void planCreatesPlainSessionForEntryWithoutUrls() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA", Collections.emptyList()));

        List<SessionDefinitionPlannedSession> plannedSessions =
            planner.plan(entries, "connect {url}");

        Assert.assertEquals(1, plannedSessions.size());
        Assert.assertEquals("groupOne/entryA", plannedSessions.get(0).getName());
        Assert.assertFalse(plannedSessions.get(0).hasCommand());
    }

    @Test
    public void shellQuoteEscapesSingleQuotes() {
        Assert.assertEquals("'a'\\''b'", SessionDefinitionPlanner.shellQuote("a'b"));
    }
}
