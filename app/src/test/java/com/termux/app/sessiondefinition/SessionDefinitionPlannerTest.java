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

        Assert.assertEquals(5, plannedSessions.size());
        Assert.assertEquals("groupOnepm", plannedSessions.get(0).getName());
        Assert.assertEquals("https://example.test/a1", plannedSessions.get(1).getName());
        Assert.assertEquals("https://example.test/a2", plannedSessions.get(2).getName());
        Assert.assertEquals("groupTwopm", plannedSessions.get(3).getName());
        Assert.assertEquals("https://example.test/b1", plannedSessions.get(4).getName());
        Assert.assertFalse(plannedSessions.get(1).hasCommand());
    }

    @Test
    public void planAddsDefaultProjectManagerSessionAtTopOfEachProjectWithAutosshCommandAndNoTask() {
        List<SessionDefinitionEntry> entries = new ArrayList<>();
        entries.add(new SessionDefinitionEntry("umino", "storyA",
            Collections.singletonList("https://example.test/u1")));
        entries.add(new SessionDefinitionEntry("xmile", "storyB",
            Collections.singletonList("https://example.test/x1")));

        List<SessionDefinitionPlannedSession> plannedSessions = planner.plan(entries, "autossh {name}");

        Assert.assertEquals(4, plannedSessions.size());

        Assert.assertEquals("uminopm", plannedSessions.get(0).getName());
        Assert.assertTrue(plannedSessions.get(0).hasCommand());
        Assert.assertEquals("AUTOSSH_GATETIME=0 autossh 'uminopm'", plannedSessions.get(0).getCommand());

        Assert.assertEquals("https://example.test/u1", plannedSessions.get(1).getName());

        Assert.assertEquals("xmilepm", plannedSessions.get(2).getName());
        Assert.assertTrue(plannedSessions.get(2).hasCommand());
        Assert.assertEquals("AUTOSSH_GATETIME=0 autossh 'xmilepm'", plannedSessions.get(2).getCommand());

        Assert.assertEquals("https://example.test/x1", plannedSessions.get(3).getName());
    }

    @Test
    public void planPlacesDefaultProjectManagerSessionOnceAtTopWhenProjectSpansMultipleStories() {
        List<SessionDefinitionEntry> entries = new ArrayList<>();
        entries.add(new SessionDefinitionEntry("umino", "storyA",
            Collections.singletonList("https://example.test/u1")));
        entries.add(new SessionDefinitionEntry("umino", "storyB",
            Collections.singletonList("https://example.test/u2")));

        List<SessionDefinitionPlannedSession> plannedSessions = planner.plan(entries, "autossh {name}");

        Assert.assertEquals(3, plannedSessions.size());
        Assert.assertEquals("uminopm", plannedSessions.get(0).getName());
        Assert.assertEquals("https://example.test/u1", plannedSessions.get(1).getName());
        Assert.assertEquals("https://example.test/u2", plannedSessions.get(2).getName());
    }

    @Test
    public void planDefaultProjectManagerSessionHasNoCommandWhenTemplateBlank() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("umino", "storyA",
                Collections.singletonList("https://example.test/u1")));

        List<SessionDefinitionPlannedSession> plannedSessions = planner.plan(entries, "   ");

        Assert.assertEquals("uminopm", plannedSessions.get(0).getName());
        Assert.assertFalse(plannedSessions.get(0).hasCommand());
    }

    @Test
    public void planWithoutTemplateProducesPlainSessions() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Collections.singletonList("https://example.test/a1")));

        List<SessionDefinitionPlannedSession> plannedSessions = planner.plan(entries, "   ");

        Assert.assertEquals(2, plannedSessions.size());
        Assert.assertEquals("https://example.test/a1", plannedSessions.get(1).getName());
        Assert.assertNull(plannedSessions.get(1).getCommand());
        Assert.assertFalse(plannedSessions.get(1).hasCommand());
    }

    @Test
    public void planWithTemplateSubstitutesShellQuotedUrlIntoNamePlaceholder() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Collections.singletonList("https://example.test/a1")));

        List<SessionDefinitionPlannedSession> plannedSessions =
            planner.plan(entries, "connect {name}");

        Assert.assertEquals(2, plannedSessions.size());
        Assert.assertEquals("https://example.test/a1", plannedSessions.get(1).getName());
        Assert.assertTrue(plannedSessions.get(1).hasCommand());
        Assert.assertEquals(
            "connect 'https://example.test/a1'",
            plannedSessions.get(1).getCommand());
    }

    @Test
    public void planForEntryWithoutUrlsStillProducesProjectManagerSession() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA", Collections.emptyList()));

        List<SessionDefinitionPlannedSession> plannedSessions =
            planner.plan(entries, "connect {name}");

        Assert.assertEquals(1, plannedSessions.size());
        Assert.assertEquals("groupOnepm", plannedSessions.get(0).getName());
        Assert.assertEquals("connect 'groupOnepm'", plannedSessions.get(0).getCommand());
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

    @Test
    public void planNamedSessionAppliesConfiguredCommandToArbitraryBrowserLinkUrlNotInAnyDefinition() {
        String clickedLinkUrl = "https://example.test/deep/link/not-a-configured-definition-url";

        SessionDefinitionPlannedSession plannedSession =
            planner.planNamedSession(clickedLinkUrl, "connect {name}");

        Assert.assertEquals(clickedLinkUrl, plannedSession.getName());
        Assert.assertTrue(plannedSession.hasCommand());
        Assert.assertEquals(
            "connect '" + clickedLinkUrl + "'",
            plannedSession.getCommand());
    }

    @Test
    public void planInjectsSshKeepaliveOptionsIntoSshCommandTemplate() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Collections.singletonList("https://example.test/a1")));

        List<SessionDefinitionPlannedSession> plannedSessions =
            planner.plan(entries, "ssh {name}");

        Assert.assertEquals(
            "ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes -o ConnectTimeout=10 'https://example.test/a1'",
            plannedSessions.get(1).getCommand());
    }

    @Test
    public void planNamedSessionInjectsSshKeepaliveOptionsIntoAutosshSshCommandTemplate() {
        SessionDefinitionPlannedSession plannedSession =
            planner.planNamedSession("https://example.test/a1", "autossh -M 0 ssh {name}");

        Assert.assertEquals(
            "AUTOSSH_GATETIME=0 autossh -M 0 ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes -o ConnectTimeout=10 'https://example.test/a1'",
            plannedSession.getCommand());
    }

    @Test
    public void planDoesNotInjectKeepaliveIntoNonSshCommandTemplate() {
        SessionDefinitionPlannedSession plannedSession =
            planner.planNamedSession("https://example.test/a1", "connect {name}");

        Assert.assertEquals(
            "connect 'https://example.test/a1'",
            plannedSession.getCommand());
    }
}
