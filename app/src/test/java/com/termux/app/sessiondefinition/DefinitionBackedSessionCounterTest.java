package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class DefinitionBackedSessionCounterTest {

    private static final String AUTOSSH_TEMPLATE = "autossh -M 0 ssh {name}";

    @Test
    public void countsEverySessionWithNonBlankNameWhenTemplatePresent() {
        DefinitionBackedSessionCounter counter = new DefinitionBackedSessionCounter();

        int count = counter.countDefinitionBackedSessions(
            Arrays.asList("host-a", "host-b", "host-c"), AUTOSSH_TEMPLATE);

        Assert.assertEquals(3, count);
    }

    @Test
    public void countsZeroWhenTemplateIsEmpty() {
        DefinitionBackedSessionCounter counter = new DefinitionBackedSessionCounter();

        int count = counter.countDefinitionBackedSessions(
            Arrays.asList("host-a", "host-b"), "");

        Assert.assertEquals(0, count);
    }

    @Test
    public void countsZeroWhenTemplateIsNull() {
        DefinitionBackedSessionCounter counter = new DefinitionBackedSessionCounter();

        int count = counter.countDefinitionBackedSessions(
            Arrays.asList("host-a", "host-b"), null);

        Assert.assertEquals(0, count);
    }

    @Test
    public void skipsBlankAndNullSessionNames() {
        DefinitionBackedSessionCounter counter = new DefinitionBackedSessionCounter();

        int count = counter.countDefinitionBackedSessions(
            Arrays.asList("host-a", "   ", null, ""), AUTOSSH_TEMPLATE);

        Assert.assertEquals(1, count);
    }

    @Test
    public void emptySessionListCountsZero() {
        DefinitionBackedSessionCounter counter = new DefinitionBackedSessionCounter();

        Assert.assertEquals(0,
            counter.countDefinitionBackedSessions(Collections.emptyList(), AUTOSSH_TEMPLATE));
    }

    @Test
    public void isDefinitionBackedTrueForNamedSessionWithTemplate() {
        DefinitionBackedSessionCounter counter = new DefinitionBackedSessionCounter();

        Assert.assertTrue(counter.isDefinitionBacked("host-a", AUTOSSH_TEMPLATE));
    }

    @Test
    public void isDefinitionBackedFalseForBlankNameOrAbsentTemplate() {
        DefinitionBackedSessionCounter counter = new DefinitionBackedSessionCounter();

        Assert.assertFalse(counter.isDefinitionBacked("   ", AUTOSSH_TEMPLATE));
        Assert.assertFalse(counter.isDefinitionBacked(null, AUTOSSH_TEMPLATE));
        Assert.assertFalse(counter.isDefinitionBacked("host-a", ""));
        Assert.assertFalse(counter.isDefinitionBacked("host-a", null));
    }
}
