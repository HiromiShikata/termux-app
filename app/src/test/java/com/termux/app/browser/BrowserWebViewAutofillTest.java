package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserWebViewAutofillTest {

    @Test
    public void leavesAutofillDisabledBelowApi26() {
        Assert.assertFalse(BrowserWebViewAutofill.shouldEnable(21));
        Assert.assertFalse(BrowserWebViewAutofill.shouldEnable(25));
    }

    @Test
    public void enablesAutofillAtApi26() {
        Assert.assertTrue(BrowserWebViewAutofill.shouldEnable(26));
    }

    @Test
    public void enablesAutofillAboveApi26() {
        Assert.assertTrue(BrowserWebViewAutofill.shouldEnable(34));
    }
}
