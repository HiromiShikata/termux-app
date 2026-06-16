package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionNameBarVisibilityTest {

    @Test
    public void visibleWhenSessionNamePresent() {
        Assert.assertTrue(SessionNameBarVisibility.isVisible("my-session"));
    }

    @Test
    public void hiddenWhenSessionNameNull() {
        Assert.assertFalse(SessionNameBarVisibility.isVisible(null));
    }

    @Test
    public void hiddenWhenSessionNameEmpty() {
        Assert.assertFalse(SessionNameBarVisibility.isVisible(""));
        Assert.assertFalse(SessionNameBarVisibility.isVisible("   "));
    }
}
