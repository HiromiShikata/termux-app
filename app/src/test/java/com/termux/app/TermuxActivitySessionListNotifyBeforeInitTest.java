package com.termux.app;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TermuxActivitySessionListNotifyBeforeInitTest {

    @Test
    public void sessionListNotifyDoesNotThrowBeforeViewControllerIsInitialized() {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();

        try {
            activity.termuxSessionListNotifyUpdated();
        } catch (NullPointerException e) {
            Assert.fail("termuxSessionListNotifyUpdated must not throw before the session list view "
                + "controller is initialized: " + e);
        }
    }
}
