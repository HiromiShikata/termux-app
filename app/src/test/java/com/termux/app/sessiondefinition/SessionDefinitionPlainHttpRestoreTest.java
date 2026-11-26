package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SessionDefinitionPlainHttpRestoreTest {

    private static final class ScriptedUrlStore implements SessionDefinitionPlainHttpRestore.UrlStore {
        private final List<String> reads;
        private int readIndex;
        final List<String> writes = new ArrayList<>();

        ScriptedUrlStore(String... reads) {
            this.reads = new ArrayList<>(Arrays.asList(reads));
        }

        @Override
        public String get() {
            String value = reads.get(Math.min(readIndex, reads.size() - 1));
            readIndex++;
            return value;
        }

        @Override
        public void set(String url) {
            writes.add(url);
        }
    }

    @Test
    public void restoreRewritesOnlyHttpsOnPort9443AndKeepsPathQueryFragment() {
        String[][] cases = {
            {"https://50.30.32.53:9443/projects/a?k=x", "http://50.30.32.53:9980/projects/a?k=x"},
            {"https://console.example.test:9443/", "http://console.example.test:9980/"},
            {"https://console.example.test:9443", "http://console.example.test:9980"},
            {"https://console.example.test:9443/p?q=1#frag", "http://console.example.test:9980/p?q=1#frag"},
            {"https://user@console.example.test:9443/p", "http://user@console.example.test:9980/p"},
            {"https://[2001:db8::1]:9443/p", "http://[2001:db8::1]:9980/p"},
            {"http://50.30.32.53:9980/projects/a?k=x", "http://50.30.32.53:9980/projects/a?k=x"},
            {"http://50.30.32.53:9443/projects/a", "http://50.30.32.53:9443/projects/a"},
            {"https://50.30.32.53:8443/projects/a", "https://50.30.32.53:8443/projects/a"},
            {"https://50.30.32.53/projects/a", "https://50.30.32.53/projects/a"},
            {"", ""},
            {null, null},
            {"not a url", "not a url"},
            {"https://[bad/", "https://[bad/"},
            {"mailto:someone@example.test", "mailto:someone@example.test"},
        };
        for (String[] row : cases) {
            assertEquals("input: " + row[0], row[1], SessionDefinitionPlainHttpRestore.restore(row[0]));
        }
    }

    @Test
    public void restoreStoreWritesRestoredUrlWhenStoredValueIsUnchanged() {
        String stored = "https://50.30.32.53:9443/projects/a?k=x";
        ScriptedUrlStore store = new ScriptedUrlStore(stored, stored);

        assertTrue(SessionDefinitionPlainHttpRestore.restoreStore(store));
        assertEquals(Collections.singletonList("http://50.30.32.53:9980/projects/a?k=x"), store.writes);
    }

    @Test
    public void restoreStoreDoesNotOverwriteAValueChangedDuringRestore() {
        ScriptedUrlStore store = new ScriptedUrlStore(
            "https://50.30.32.53:9443/projects/a?k=x",
            "http://other.example.test:9980/changed");

        assertFalse(SessionDefinitionPlainHttpRestore.restoreStore(store));
        assertEquals(Collections.emptyList(), store.writes);
    }

    @Test
    public void restoreStoreDoesNotWriteWhenNothingNeedsRestoring() {
        String[] storedValues = {"http://50.30.32.53:9980/projects/a", "https://50.30.32.53:8443/x", "", null};
        for (String stored : storedValues) {
            ScriptedUrlStore store = new ScriptedUrlStore(stored, stored);

            assertFalse("stored: " + stored, SessionDefinitionPlainHttpRestore.restoreStore(store));
            assertEquals(Collections.emptyList(), store.writes);
        }
    }
}
