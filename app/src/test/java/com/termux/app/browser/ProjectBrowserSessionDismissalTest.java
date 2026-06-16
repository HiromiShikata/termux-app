package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class ProjectBrowserSessionDismissalTest {

    @Test
    public void dismissesWhenOverlayVisible() {
        Assert.assertTrue(ProjectBrowserSessionDismissal.shouldDismissOnSessionAccess(true));
    }

    @Test
    public void doesNotDismissWhenOverlayHidden() {
        Assert.assertFalse(ProjectBrowserSessionDismissal.shouldDismissOnSessionAccess(false));
    }
}
