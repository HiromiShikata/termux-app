package com.termux.shared.termux.models;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class UserActionTest {

    @Test
    public void crashReportExposesHumanReadableName() {
        Assert.assertEquals("crash report", UserAction.CRASH_REPORT.getName());
    }

    @Test
    public void pluginExecutionCommandExposesHumanReadableName() {
        Assert.assertEquals("plugin execution command", UserAction.PLUGIN_EXECUTION_COMMAND.getName());
    }

    @Test
    public void valuesContainAllDeclaredConstants() {
        List<UserAction> values = Arrays.asList(UserAction.values());
        Assert.assertTrue(values.contains(UserAction.CRASH_REPORT));
        Assert.assertTrue(values.contains(UserAction.PLUGIN_EXECUTION_COMMAND));
        Assert.assertEquals(2, values.size());
    }

    @Test
    public void valueOfRoundTripsConstantName() {
        Assert.assertSame(UserAction.CRASH_REPORT, UserAction.valueOf("CRASH_REPORT"));
        Assert.assertSame(UserAction.PLUGIN_EXECUTION_COMMAND, UserAction.valueOf("PLUGIN_EXECUTION_COMMAND"));
    }
}
