package com.termux.app.sessiondefinition;

import com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants.TERMUX_APP;

import org.junit.Assert;
import org.junit.Test;

public class SessionCapAgainstProcessCeilingTest {

    private static final int PROCESS_COUNT_ANDROID_ALLOWS_THE_APP = 32;

    @Test
    public void theDefaultSessionCapStaysWithinTheProcessCountAndroidAllowsTheApp() {
        Assert.assertTrue("Android kills the app's forked processes once their count passes the ceiling"
                + " it enforces, whose documented default is " + PROCESS_COUNT_ANDROID_ALLOWS_THE_APP
                + ", and every session that counts toward this cap holds at least one such process. A"
                + " cap above that ceiling therefore lets the app reach a state Android answers by"
                + " killing shells, which is what the owner sees as sessions going dark. Actual cap: "
                + TERMUX_APP.DEFAULT_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS,
            TERMUX_APP.DEFAULT_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS
                <= PROCESS_COUNT_ANDROID_ALLOWS_THE_APP);
    }

    @Test
    public void theDefaultSessionCapStaysAboveTheMinimumTheSettingAccepts() {
        Assert.assertTrue("a default below the minimum the setting accepts would be silently raised on"
                + " read, so the value stated as the default would not be the value in force. Actual"
                + " cap: " + TERMUX_APP.DEFAULT_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS
                + ", minimum: " + TERMUX_APP.MINIMUM_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS,
            TERMUX_APP.DEFAULT_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS
                >= TERMUX_APP.MINIMUM_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS);
    }
}
