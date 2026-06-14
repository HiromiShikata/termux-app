package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionNameBarVisibilityTest {

    @Test
    public void hiddenWhenBrowserVisibleEvenWithSessionName() {
        Assert.assertFalse(SessionNameBarVisibility.isVisible("my-session", true));
    }

    @Test
    public void visibleWhenBrowserHiddenAndSessionNamePresent() {
        Assert.assertTrue(SessionNameBarVisibility.isVisible("my-session", false));
    }

    @Test
    public void hiddenWhenBrowserHiddenAndSessionNameNull() {
        Assert.assertFalse(SessionNameBarVisibility.isVisible(null, false));
    }

    @Test
    public void hiddenWhenBrowserHiddenAndSessionNameEmpty() {
        Assert.assertFalse(SessionNameBarVisibility.isVisible("", false));
        Assert.assertFalse(SessionNameBarVisibility.isVisible("   ", false));
    }

    @Test
    public void hiddenWhenBrowserVisibleAndSessionNameNull() {
        Assert.assertFalse(SessionNameBarVisibility.isVisible(null, true));
    }
}
