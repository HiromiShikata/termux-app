package com.termux.shared.termux.settings.preferences;

import org.junit.Assert;
import org.junit.Test;

public class SpeakTagAutoReadDefaultTest {

    @Test
    public void speakTagAutoReadIsDisabledByDefault() {
        Assert.assertFalse(TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_SPEAK_TAG_AUTO_READ_ENABLED);
    }
}
