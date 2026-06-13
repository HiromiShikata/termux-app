package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SessionDefinitionEntryMatcherTest {

    private final SessionDefinitionEntryMatcher matcher = new SessionDefinitionEntryMatcher();

    @Test
    public void matchesEntryWhenSessionNameEqualsOneOfEntryUrls() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2")));

        SessionDefinitionEntry matched = matcher.findEntryForSessionName(entries, "https://example.test/a1");

        Assert.assertNotNull(matched);
        Assert.assertEquals(Arrays.asList("https://example.test/a1", "https://example.test/a2"), matched.getUrls());
    }

    @Test
    public void matchesEntryForAnyUrlInTheEntry() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2")));

        SessionDefinitionEntry matched = matcher.findEntryForSessionName(entries, "https://example.test/a2");

        Assert.assertNotNull(matched);
        Assert.assertEquals(Arrays.asList("https://example.test/a1", "https://example.test/a2"), matched.getUrls());
    }

    @Test
    public void selectsTheEntryThatContainsTheUrlAmongMultipleEntries() {
        SessionDefinitionEntry firstEntry = new SessionDefinitionEntry("groupOne", "entryA",
            Collections.singletonList("https://example.test/a1"));
        SessionDefinitionEntry secondEntry = new SessionDefinitionEntry("groupTwo", "entryB",
            Collections.singletonList("https://example.test/b1"));

        SessionDefinitionEntry matched = matcher.findEntryForSessionName(
            Arrays.asList(firstEntry, secondEntry), "https://example.test/b1");

        Assert.assertNotNull(matched);
        Assert.assertEquals(Collections.singletonList("https://example.test/b1"), matched.getUrls());
    }

    @Test
    public void returnsNullWhenNoEntryContainsTheUrl() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Collections.singletonList("https://example.test/a1")));

        Assert.assertNull(matcher.findEntryForSessionName(entries, "https://example.test/other"));
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
    public void doesNotMatchOnPartialUrl() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "entryA",
                Collections.singletonList("https://example.test/a1")));

        Assert.assertNull(matcher.findEntryForSessionName(entries, "https://example.test/a"));
    }
}
