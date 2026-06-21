package com.termux.shared.termux.settings.preferences;

import org.junit.Assert;
import org.junit.Test;

public class OpenTagAutoOpenDefaultTest {

    @Test
    public void openTagAutoOpenIsEnabledByDefault() {
        Assert.assertTrue(TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_OPEN_TAG_AUTO_OPEN_ENABLED);
    }
}
