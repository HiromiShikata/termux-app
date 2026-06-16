package com.termux.shared.shell.command.environment;

import org.junit.Assert;
import org.junit.Test;

public class ShellEnvironmentVariableTest {

    @Test
    public void twoArgConstructorDefaultsEscapedToFalse() {
        ShellEnvironmentVariable variable = new ShellEnvironmentVariable("NAME", "value");
        Assert.assertEquals("NAME", variable.name);
        Assert.assertEquals("value", variable.value);
        Assert.assertFalse(variable.escaped);
    }

    @Test
    public void threeArgConstructorRetainsEscapedFlag() {
        ShellEnvironmentVariable variable = new ShellEnvironmentVariable("NAME", "value", true);
        Assert.assertTrue(variable.escaped);
    }

    @Test
    public void compareToOrdersByNameAscending() {
        ShellEnvironmentVariable alpha = new ShellEnvironmentVariable("ALPHA", "x");
        ShellEnvironmentVariable beta = new ShellEnvironmentVariable("BETA", "y");
        Assert.assertTrue(alpha.compareTo(beta) < 0);
        Assert.assertTrue(beta.compareTo(alpha) > 0);
    }

    @Test
    public void compareToReturnsZeroForEqualNames() {
        ShellEnvironmentVariable first = new ShellEnvironmentVariable("SAME", "1");
        ShellEnvironmentVariable second = new ShellEnvironmentVariable("SAME", "2");
        Assert.assertEquals(0, first.compareTo(second));
    }
}
