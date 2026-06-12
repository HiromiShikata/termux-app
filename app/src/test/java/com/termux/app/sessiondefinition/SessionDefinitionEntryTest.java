package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class SessionDefinitionEntryTest {

    @Test
    public void sessionNameCombinesGroupAndEntryLabel() {
        SessionDefinitionEntry entry = new SessionDefinitionEntry(
            "groupLabel", "entryLabel", Collections.singletonList("https://example.test/x"));
        Assert.assertEquals("groupLabel/entryLabel", entry.getSessionName());
    }

    @Test
    public void sessionNameUsesEntryLabelWhenGroupLabelIsEmpty() {
        SessionDefinitionEntry entry = new SessionDefinitionEntry(
            "", "entryLabel", Collections.singletonList("https://example.test/x"));
        Assert.assertEquals("entryLabel", entry.getSessionName());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void urlsListIsImmutable() {
        SessionDefinitionEntry entry = new SessionDefinitionEntry(
            "groupLabel", "entryLabel", new java.util.ArrayList<>(Collections.singletonList("https://example.test/x")));
        entry.getUrls().add("https://example.test/y");
    }

    @Test
    public void urlsAreDefensivelyCopiedFromTheSourceList() {
        java.util.List<String> source = new java.util.ArrayList<>(Collections.singletonList("https://example.test/x"));
        SessionDefinitionEntry entry = new SessionDefinitionEntry("groupLabel", "entryLabel", source);
        source.add("https://example.test/y");
        Assert.assertEquals(1, entry.getUrls().size());
        Assert.assertEquals("https://example.test/x", entry.getUrls().get(0));
    }
}
