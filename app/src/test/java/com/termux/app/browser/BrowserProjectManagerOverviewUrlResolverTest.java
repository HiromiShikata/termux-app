package com.termux.app.browser;

import com.termux.app.sessiondefinition.DefaultProjectManagerSessionPlanner;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BrowserProjectManagerOverviewUrlResolverTest {

    private static final DefaultProjectManagerSessionPlanner MANAGER_SESSION_PLANNER =
        new DefaultProjectManagerSessionPlanner();

    private static SessionDefinitionEntry entry(String groupLabel, String entryLabel, String sessionUrl,
                                                String overviewUrl) {
        return new SessionDefinitionEntry(groupLabel, entryLabel,
            Collections.singletonList(sessionUrl), Collections.emptyMap(), overviewUrl, null, null);
    }

    private static String managerSessionName(String groupLabel) {
        return MANAGER_SESSION_PLANNER.sessionNameForProjectLabel(groupLabel);
    }

    private BrowserProjectManagerOverviewUrlResolver resolverFor(List<SessionDefinitionEntry> entries) {
        return new BrowserProjectManagerOverviewUrlResolver(() -> entries);
    }

    @Test
    public void resolvesOverviewUrlForManagerSession() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            entry("project-a", "story-1", "session-a", "https://overview.a/"));

        String overviewUrl = resolverFor(entries)
            .resolveOverviewUrlForManagerSessionName(managerSessionName("project-a"));

        Assert.assertEquals("https://overview.a/", overviewUrl);
    }

    @Test
    public void inheritsOverviewUrlFromSiblingEntryInSameProject() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            entry("project-a", "story-1", "session-a", null),
            entry("project-a", "story-2", "session-a2", "https://overview.a/"));

        String overviewUrl = resolverFor(entries)
            .resolveOverviewUrlForManagerSessionName(managerSessionName("project-a"));

        Assert.assertEquals("https://overview.a/", overviewUrl);
    }

    @Test
    public void returnsNullForNonManagerSessionName() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            entry("project-a", "story-1", "session-a", "https://overview.a/"));

        String overviewUrl = resolverFor(entries)
            .resolveOverviewUrlForManagerSessionName("session-a");

        Assert.assertNull(overviewUrl);
    }

    @Test
    public void returnsNullForManagerSessionWithoutOverviewUrl() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            entry("project-a", "story-1", "session-a", null));

        String overviewUrl = resolverFor(entries)
            .resolveOverviewUrlForManagerSessionName(managerSessionName("project-a"));

        Assert.assertNull(overviewUrl);
    }

    @Test
    public void resolvesOverviewUrlForCorrectProjectOnly() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            entry("project-a", "story-1", "session-a", "https://overview.a/"),
            entry("project-b", "story-1", "session-b", "https://overview.b/"));

        String overviewUrl = resolverFor(entries)
            .resolveOverviewUrlForManagerSessionName(managerSessionName("project-b"));

        Assert.assertEquals("https://overview.b/", overviewUrl);
    }

    @Test
    public void returnsNullForNullSessionName() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            entry("project-a", "story-1", "session-a", "https://overview.a/"));

        String overviewUrl = resolverFor(entries)
            .resolveOverviewUrlForManagerSessionName(null);

        Assert.assertNull(overviewUrl);
    }

    @Test
    public void returnsNullForEmptyEntries() {
        String overviewUrl = resolverFor(Collections.emptyList())
            .resolveOverviewUrlForManagerSessionName(managerSessionName("project-a"));

        Assert.assertNull(overviewUrl);
    }
}
