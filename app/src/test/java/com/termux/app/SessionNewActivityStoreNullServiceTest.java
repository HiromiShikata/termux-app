package com.termux.app;

import com.termux.app.terminal.SessionNewActivityStore;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SessionNewActivityStoreNullServiceTest {

    @Test
    public void returnsNullInsteadOfThrowingWhenServiceIsNotBound() {
        SessionNewActivityStore store = TermuxActivity.sessionNewActivityStoreOrNull(null);

        Assert.assertNull(store);
    }

    @Test
    public void relativeTimeRefreshTickRebuildPathDoesNotThrowWhenServiceIsNull() {
        SessionNewActivityStore store = null;
        try {
            store = TermuxActivity.sessionNewActivityStoreOrNull(null);
        } catch (NullPointerException e) {
            Assert.fail("sessionNewActivityStoreOrNull must not throw when the service is null: " + e);
        }

        Assert.assertNull(store);
    }
}
