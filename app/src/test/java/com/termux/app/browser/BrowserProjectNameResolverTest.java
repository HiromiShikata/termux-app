package com.termux.app.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.annotation.NonNull;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class BrowserProjectNameResolverTest {

    private static final class FixedEntriesSupplier
            implements BrowserProjectNameResolver.SessionDefinitionEntriesSupplier {
        private final List<SessionDefinitionEntry> entries;

        FixedEntriesSupplier(List<SessionDefinitionEntry> entries) {
            this.entries = entries;
        }

        @NonNull
        @Override
        public List<SessionDefinitionEntry> getSessionDefinitionEntries() {
            return entries;
        }
    }

    private static SessionDefinitionEntry entry(String groupLabel, String url) {
        return new SessionDefinitionEntry(groupLabel, "entry", Collections.singletonList(url));
    }

    @Test
    public void resolvesProjectNameFromTheSharedEntriesSupplier() {
        BrowserProjectNameResolver resolver = new BrowserProjectNameResolver(
            new FixedEntriesSupplier(Collections.singletonList(
                entry("projectOne", "https://example.test/a"))));

        assertEquals("projectOne", resolver.resolveProjectName("https://example.test/a"));
    }

    @Test
    public void returnsNullWhenSharedEntriesAreEmpty() {
        BrowserProjectNameResolver resolver = new BrowserProjectNameResolver(
            new FixedEntriesSupplier(Collections.emptyList()));

        assertNull(resolver.resolveProjectName("https://example.test/a"));
    }

    @Test
    public void returnsNullWhenSessionNameHasNoMatchingEntry() {
        BrowserProjectNameResolver resolver = new BrowserProjectNameResolver(
            new FixedEntriesSupplier(Collections.singletonList(
                entry("projectOne", "https://example.test/a"))));

        assertNull(resolver.resolveProjectName("https://example.test/unmatched"));
    }

    @Test
    public void returnsNullForNullOrEmptySessionName() {
        BrowserProjectNameResolver resolver = new BrowserProjectNameResolver(
            new FixedEntriesSupplier(Collections.singletonList(
                entry("projectOne", "https://example.test/a"))));

        assertNull(resolver.resolveProjectName(null));
        assertNull(resolver.resolveProjectName(""));
    }
}
