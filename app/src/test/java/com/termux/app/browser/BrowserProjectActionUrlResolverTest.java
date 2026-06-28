package com.termux.app.browser;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BrowserProjectActionUrlResolverTest {

    private static SessionDefinitionEntry entry(String groupLabel, String entryLabel, String sessionUrl,
                                                String overviewUrl, String tdpmConsoleUrl, String newIssueUrl) {
        return new SessionDefinitionEntry(groupLabel, entryLabel,
            Collections.singletonList(sessionUrl), Collections.emptyMap(),
            overviewUrl, tdpmConsoleUrl, newIssueUrl);
    }

    private BrowserProjectActionUrlResolver resolverFor(List<SessionDefinitionEntry> entries) {
        return new BrowserProjectActionUrlResolver(() -> entries);
    }

    @Test
    public void resolvesProjectActionUrlsForMatchingSession() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            entry("project-a", "story-1", "session-a",
                "https://overview.a/", "https://console.a/", "https://newissue.a/"));

        BrowserProjectActionUrls actionUrls = resolverFor(entries).resolveForSessionName("session-a");

        Assert.assertEquals("https://overview.a/", actionUrls.getOverviewUrl());
        Assert.assertEquals("https://console.a/", actionUrls.getTdpmConsoleUrl());
        Assert.assertEquals("https://newissue.a/", actionUrls.getNewIssueUrl());
    }

    @Test
    public void inheritsProjectActionUrlsFromSiblingEntryInSameProject() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            entry("project-a", "story-1", "session-a",
                "https://overview.a/", "https://console.a/", "https://newissue.a/"),
            entry("project-a", "story-2", "session-a2", null, null, null));

        BrowserProjectActionUrls actionUrls = resolverFor(entries).resolveForSessionName("session-a2");

        Assert.assertEquals("https://overview.a/", actionUrls.getOverviewUrl());
        Assert.assertEquals("https://console.a/", actionUrls.getTdpmConsoleUrl());
        Assert.assertEquals("https://newissue.a/", actionUrls.getNewIssueUrl());
    }

    @Test
    public void doesNotLeakUrlsFromOtherProjects() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            entry("project-a", "story-1", "session-a",
                "https://overview.a/", "https://console.a/", "https://newissue.a/"),
            entry("project-b", "story-1", "session-b", null, null, null));

        BrowserProjectActionUrls actionUrls = resolverFor(entries).resolveForSessionName("session-b");

        Assert.assertNull(actionUrls.getOverviewUrl());
        Assert.assertNull(actionUrls.getTdpmConsoleUrl());
        Assert.assertNull(actionUrls.getNewIssueUrl());
    }

    @Test
    public void returnsEmptyWhenSessionNameDoesNotMatchAnyEntry() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            entry("project-a", "story-1", "session-a",
                "https://overview.a/", "https://console.a/", "https://newissue.a/"));

        BrowserProjectActionUrls actionUrls = resolverFor(entries).resolveForSessionName("unknown-session");

        Assert.assertNull(actionUrls.getOverviewUrl());
        Assert.assertNull(actionUrls.getTdpmConsoleUrl());
        Assert.assertNull(actionUrls.getNewIssueUrl());
    }

    @Test
    public void returnsEmptyForNullSessionName() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            entry("project-a", "story-1", "session-a",
                "https://overview.a/", "https://console.a/", "https://newissue.a/"));

        BrowserProjectActionUrls actionUrls = resolverFor(entries).resolveForSessionName(null);

        Assert.assertNull(actionUrls.getOverviewUrl());
        Assert.assertNull(actionUrls.getTdpmConsoleUrl());
        Assert.assertNull(actionUrls.getNewIssueUrl());
    }
}
