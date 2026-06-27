package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BrowserSessionDefinitionStartupPlannerTest {

    private final BrowserSessionDefinitionStartupPlanner planner =
        new BrowserSessionDefinitionStartupPlanner();

    private static final String COMMAND_TEMPLATE = "echo hello {name}";

    @Test
    public void appliesConfiguredStartupCommandWhenUrlMatchesADefinition() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Collections.singletonList("https://example.com/a1")));

        SessionDefinitionPlannedSession plannedSession =
            planner.plan(entries, "https://example.com/a1", COMMAND_TEMPLATE);

        Assert.assertEquals("https://example.com/a1", plannedSession.getName());
        Assert.assertTrue(plannedSession.hasCommand());
        Assert.assertEquals("echo hello 'https://example.com/a1'", plannedSession.getCommand());
    }

    @Test
    public void launchesPlainShellWhenUrlMatchesNoDefinition() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Collections.singletonList("https://example.com/a1")));

        SessionDefinitionPlannedSession plannedSession =
            planner.plan(entries, "https://example.com/unmatched", COMMAND_TEMPLATE);

        Assert.assertEquals("https://example.com/unmatched", plannedSession.getName());
        Assert.assertFalse(plannedSession.hasCommand());
        Assert.assertNull(plannedSession.getCommand());
    }

    @Test
    public void launchesPlainShellWhenNoTemplateConfiguredEvenIfUrlMatches() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Collections.singletonList("https://example.com/a1")));

        SessionDefinitionPlannedSession plannedSession =
            planner.plan(entries, "https://example.com/a1", "");

        Assert.assertEquals("https://example.com/a1", plannedSession.getName());
        Assert.assertFalse(plannedSession.hasCommand());
    }

    @Test
    public void launchesPlainShellWhenTemplateIsNullEvenIfUrlMatches() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Collections.singletonList("https://example.com/a1")));

        SessionDefinitionPlannedSession plannedSession =
            planner.plan(entries, "https://example.com/a1", null);

        Assert.assertFalse(plannedSession.hasCommand());
    }

    @Test
    public void matchesAnyUrlListedInTheSameDefinitionEntry() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Arrays.asList("https://example.com/a1", "https://example.com/a2")));

        SessionDefinitionPlannedSession plannedSession =
            planner.plan(entries, "https://example.com/a2", COMMAND_TEMPLATE);

        Assert.assertTrue(plannedSession.hasCommand());
        Assert.assertEquals("echo hello 'https://example.com/a2'", plannedSession.getCommand());
    }

    @Test
    public void launchesPlainShellWhenNoDefinitionsAreLoaded() {
        SessionDefinitionPlannedSession plannedSession =
            planner.plan(Collections.emptyList(), "https://example.com/a1", COMMAND_TEMPLATE);

        Assert.assertEquals("https://example.com/a1", plannedSession.getName());
        Assert.assertFalse(plannedSession.hasCommand());
    }
}
