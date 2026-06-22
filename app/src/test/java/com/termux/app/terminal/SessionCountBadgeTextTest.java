package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionCountBadgeTextTest {

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

    @Test
    public void sessionCountBadgeTextIsTheCount() {
        Assert.assertEquals("2", TermuxSessionsListViewController.sessionCountBadgeText(2));
        Assert.assertEquals("0", TermuxSessionsListViewController.sessionCountBadgeText(0));
    }
}
