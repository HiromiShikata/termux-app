package com.termux.app.terminal.io;

import org.junit.Assert;
import org.junit.Test;

public class ToolbarTextInputEncoderTest {

    @Test
    public void sendsTextVerbatimWhenNotEmpty() {
        Assert.assertEquals("ls -la", ToolbarTextInputEncoder.textToSend("ls -la", true));
    }

    @Test
    public void sendsTextVerbatimWhenNotEmptyAndSubmitWhenEmptyDisabled() {
        Assert.assertEquals("ls -la", ToolbarTextInputEncoder.textToSend("ls -la", false));
    }

    @Test
    public void sendsCarriageReturnForEmptyTextWhenSubmitWhenEmptyEnabled() {
        Assert.assertEquals("\r", ToolbarTextInputEncoder.textToSend("", true));
    }

    @Test
    public void sendsNothingForEmptyTextWhenSubmitWhenEmptyDisabled() {
        Assert.assertEquals("", ToolbarTextInputEncoder.textToSend("", false));
    }

    @Test
    public void hasNoContentToSendForEmptyTextWhenSubmitWhenEmptyDisabled() {
        Assert.assertFalse(ToolbarTextInputEncoder.hasContentToSend("", false));
    }

    @Test
    public void hasContentToSendForEmptyTextWhenSubmitWhenEmptyEnabled() {
        Assert.assertTrue(ToolbarTextInputEncoder.hasContentToSend("", true));
    }

    @Test
    public void hasContentToSendForNonEmptyTextWhenSubmitWhenEmptyDisabled() {
        Assert.assertTrue(ToolbarTextInputEncoder.hasContentToSend("ls -la", false));
    }

    @Test
    public void hasContentToSendForNonEmptyTextWhenSubmitWhenEmptyEnabled() {
        Assert.assertTrue(ToolbarTextInputEncoder.hasContentToSend("ls -la", true));
    }
}
