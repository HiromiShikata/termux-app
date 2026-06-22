package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionCountTitleTest {

    @Test
    public void sessionCountTitleAppendsTotalCountInParentheses() {
        Assert.assertEquals("Sessions (3)",
            SessionListBottomSheetController.sessionCountTitle("Sessions", 3));
    }

    @Test
    public void sessionCountTitleShowsZeroWhenNoSessions() {
        Assert.assertEquals("Sessions (0)",
            SessionListBottomSheetController.sessionCountTitle("Sessions", 0));
    }

}
