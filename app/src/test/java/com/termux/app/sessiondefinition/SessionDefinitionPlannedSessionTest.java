package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

public class SessionDefinitionPlannedSessionTest {

    @Test
    public void exposesNameAndCommand() {
        SessionDefinitionPlannedSession session =
            new SessionDefinitionPlannedSession("deploy", "connect 'https://example.test'");

        Assert.assertEquals("deploy", session.getName());
        Assert.assertEquals("connect 'https://example.test'", session.getCommand());
    }

    @Test
    public void hasCommandIsTrueForNonEmptyCommand() {
        SessionDefinitionPlannedSession session =
            new SessionDefinitionPlannedSession("deploy", "connect 'https://example.test'");

        Assert.assertTrue(session.hasCommand());
    }

    @Test
    public void hasCommandIsFalseForNullCommand() {
        SessionDefinitionPlannedSession session =
            new SessionDefinitionPlannedSession("deploy", null);

        Assert.assertFalse(session.hasCommand());
    }

    @Test
    public void hasCommandIsFalseForEmptyCommand() {
        SessionDefinitionPlannedSession session =
            new SessionDefinitionPlannedSession("deploy", "");

        Assert.assertFalse(session.hasCommand());
    }
}
