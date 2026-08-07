package com.termux.app.terminal.io;

import org.junit.Assert;
import org.junit.Test;

public class ToolbarTextInputEncoderTest {

    @Test
    public void sendsTextVerbatimWithoutASubmitTerminatorBecauseTheEnterSequenceIsWrittenSeparately() {
        Assert.assertEquals("ls -la", ToolbarTextInputEncoder.textToSend("ls -la"));
    }

    @Test
    public void sendsNothingForEmptyTextBecauseTheEnterSequenceAloneSubmitsAnEmptyInput() {
        Assert.assertEquals("", ToolbarTextInputEncoder.textToSend(""));
    }

    @Test
    public void hasNoContentToSendForEmptyText() {
        Assert.assertFalse(ToolbarTextInputEncoder.hasContentToSend(""));
    }

    @Test
    public void hasContentToSendForNonEmptyText() {
        Assert.assertTrue(ToolbarTextInputEncoder.hasContentToSend("ls -la"));
    }
}
