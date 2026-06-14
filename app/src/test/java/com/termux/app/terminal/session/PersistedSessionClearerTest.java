package com.termux.app.terminal.session;

import org.junit.Assert;
import org.junit.Test;

public class PersistedSessionClearerTest {

    private static final class InMemoryPersistedSessionStore implements PersistedSessionStore {
        private String value;

        InMemoryPersistedSessionStore(String value) {
            this.value = value;
        }

        @Override
        public String getPersistedSessions() {
            return value;
        }

        @Override
        public void setPersistedSessions(String value) {
            this.value = value;
        }
    }

    @Test
    public void clearEmptiesTheStoreWhenSessionsWerePersisted() {
        InMemoryPersistedSessionStore store = new InMemoryPersistedSessionStore("[{\"name\":\"alpha\"}]");

        boolean hadPersistedSessions = PersistedSessionClearer.clear(store);

        Assert.assertTrue(hadPersistedSessions);
        Assert.assertNull(store.getPersistedSessions());
    }

    @Test
    public void clearReportsNoPersistedSessionsWhenStoreIsEmptyString() {
        InMemoryPersistedSessionStore store = new InMemoryPersistedSessionStore("");

        boolean hadPersistedSessions = PersistedSessionClearer.clear(store);

        Assert.assertFalse(hadPersistedSessions);
        Assert.assertNull(store.getPersistedSessions());
    }

    @Test
    public void clearReportsNoPersistedSessionsWhenStoreIsNull() {
        InMemoryPersistedSessionStore store = new InMemoryPersistedSessionStore(null);

        boolean hadPersistedSessions = PersistedSessionClearer.clear(store);

        Assert.assertFalse(hadPersistedSessions);
        Assert.assertNull(store.getPersistedSessions());
    }
}
