package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionLastReplyLineTest {

    @Test
    public void isVisibleAndCarriesAgeLabelWhenPresent() {
        SessionLastReplyLine line = SessionLastReplyLine.of("5s ago");
        Assert.assertTrue(line.isVisible());
        Assert.assertEquals("5s ago", line.getAgeLabel());
    }

    @Test
    public void isHiddenWhenAgeLabelIsNull() {
        SessionLastReplyLine line = SessionLastReplyLine.of(null);
        Assert.assertFalse(line.isVisible());
        Assert.assertEquals("", line.getAgeLabel());
    }

    @Test
    public void isHiddenWhenAgeLabelIsBlank() {
        SessionLastReplyLine line = SessionLastReplyLine.of("   ");
        Assert.assertFalse(line.isVisible());
    }

    @Test
    public void trimsSurroundingWhitespaceFromAgeLabel() {
        SessionLastReplyLine line = SessionLastReplyLine.of("  12m ago  ");
        Assert.assertTrue(line.isVisible());
        Assert.assertEquals("12m ago", line.getAgeLabel());
    }
}
