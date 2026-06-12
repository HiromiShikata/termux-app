package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SessionDefinitionEntryMatcherTest {

    private final SessionDefinitionEntryMatcher matcher = new SessionDefinitionEntryMatcher();

    @Test
    public void matchesEntryWhenSessionNameEqualsEntrySessionName() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2")));

        SessionDefinitionEntry matched = matcher.findEntryForSessionName(entries, "groupOne/entryA");

        Assert.assertNotNull(matched);
        Assert.assertEquals("groupOne/entryA", matched.getSessionName());
        Assert.assertEquals(Arrays.asList("https://example.test/a1", "https://example.test/a2"), matched.getUrls());
    }

    @Test
    public void matchesEntryWhenSessionNameHasIndexSuffix() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2")));

        SessionDefinitionEntry matched = matcher.findEntryForSessionName(entries, "groupOne/entryA 2");

        Assert.assertNotNull(matched);
        Assert.assertEquals("groupOne/entryA", matched.getSessionName());
    }

    @Test
    public void prefersTheLongestMatchingEntrySessionName() {
        SessionDefinitionEntry shortEntry = new SessionDefinitionEntry("groupOne", "entryA",
            Collections.singletonList("https://example.test/short"));
        SessionDefinitionEntry longEntry = new SessionDefinitionEntry("groupOne", "entryA extra",
            Collections.singletonList("https://example.test/long"));

        SessionDefinitionEntry matched = matcher.findEntryForSessionName(
            Arrays.asList(shortEntry, longEntry), "groupOne/entryA extra 1");

        Assert.assertNotNull(matched);
        Assert.assertEquals("groupOne/entryA extra", matched.getSessionName());
        Assert.assertEquals(Collections.singletonList("https://example.test/long"), matched.getUrls());
    }

    @Test
    public void returnsNullWhenNoEntryMatches() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Collections.singletonList("https://example.test/a1")));

        Assert.assertNull(matcher.findEntryForSessionName(entries, "groupTwo/entryB"));
    }

    @Test
    public void returnsNullWhenSessionNameIsNull() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Collections.singletonList("https://example.test/a1")));

        Assert.assertNull(matcher.findEntryForSessionName(entries, null));
    }

    @Test
    public void returnsNullWhenSessionNameIsEmpty() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Collections.singletonList("https://example.test/a1")));

        Assert.assertNull(matcher.findEntryForSessionName(entries, ""));
    }

    @Test
    public void doesNotMatchOnPartialTokenWithoutSpaceBoundary() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Collections.singletonList("https://example.test/a1")));

        Assert.assertNull(matcher.findEntryForSessionName(entries, "groupOne/entryAB"));
    }
}
