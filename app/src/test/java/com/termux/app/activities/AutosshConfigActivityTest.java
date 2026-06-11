package com.termux.app.activities;

import com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants;

import org.junit.Assert;
import org.junit.Test;

public class AutosshConfigActivityTest {

    @Test
    public void autosshCommandKeyIsStable() {
        Assert.assertEquals("autossh_command", TermuxPreferenceConstants.TERMUX_APP.KEY_AUTOSSH_COMMAND);
    }

    @Test
    public void autosshCommandDefaultValueIsEmpty() {
        Assert.assertEquals("", TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_AUTOSSH_COMMAND);
    }

}
