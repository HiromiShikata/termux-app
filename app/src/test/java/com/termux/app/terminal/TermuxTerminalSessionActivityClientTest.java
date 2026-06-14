package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class TermuxTerminalSessionActivityClientTest {

    @Test
    public void copiesSessionNameWhenNameIsPresent() {
        Assert.assertEquals("workSession",
            TermuxTerminalSessionActivityClient.resolveSessionNameForCopy("workSession", "bash"));
    }

    @Test
    public void fallsBackToTitleWhenNameIsNull() {
        Assert.assertEquals("bash",
            TermuxTerminalSessionActivityClient.resolveSessionNameForCopy(null, "bash"));
    }

    @Test
    public void fallsBackToTitleWhenNameIsEmpty() {
        Assert.assertEquals("bash",
            TermuxTerminalSessionActivityClient.resolveSessionNameForCopy("", "bash"));
    }

    @Test
    public void returnsNullWhenBothNameAndTitleAreNull() {
        Assert.assertNull(
            TermuxTerminalSessionActivityClient.resolveSessionNameForCopy(null, null));
    }

    @Test
    public void returnsNullWhenBothNameAndTitleAreEmpty() {
        Assert.assertNull(
            TermuxTerminalSessionActivityClient.resolveSessionNameForCopy("", ""));
    }
}
