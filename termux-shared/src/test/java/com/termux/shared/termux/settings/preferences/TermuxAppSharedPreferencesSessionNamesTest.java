package com.termux.shared.termux.settings.preferences;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class TermuxAppSharedPreferencesSessionNamesTest {

    @Test
    public void parseAlwaysNaSessionNamesPreservesOrder() {
        Set<String> expected = new LinkedHashSet<>(Arrays.asList("build", "deploy", "logs"));
        Assert.assertEquals(expected,
            TermuxAppSharedPreferences.parseAlwaysNaSessionNames("build\ndeploy\nlogs"));
    }

    @Test
    public void parseAlwaysNaSessionNamesTrimsWhitespaceAndDropsBlankLines() {
        Set<String> expected = new LinkedHashSet<>(Arrays.asList("build", "deploy"));
        Assert.assertEquals(expected,
            TermuxAppSharedPreferences.parseAlwaysNaSessionNames("  build  \n\n  \ndeploy\n"));
    }

    @Test
    public void parseAlwaysNaSessionNamesRemovesDuplicatesPreservingFirstOccurrence() {
        Set<String> expected = new LinkedHashSet<>(Arrays.asList("build", "deploy"));
        Assert.assertEquals(expected,
            TermuxAppSharedPreferences.parseAlwaysNaSessionNames("build\ndeploy\nbuild"));
    }

    @Test
    public void parseAlwaysNaSessionNamesWithNullReturnsEmptySet() {
        Assert.assertTrue(TermuxAppSharedPreferences.parseAlwaysNaSessionNames(null).isEmpty());
    }

    @Test
    public void parseAlwaysNaSessionNamesWithDefaultValueReturnsEmptySet() {
        Assert.assertTrue(TermuxAppSharedPreferences.parseAlwaysNaSessionNames(
            TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_ALWAYS_NA_SESSION_NAMES).isEmpty());
    }

    @Test
    public void parseDisabledSessionNamesPreservesOrder() {
        Set<String> expected = new LinkedHashSet<>(Arrays.asList("build", "deploy", "logs"));
        Assert.assertEquals(expected,
            TermuxAppSharedPreferences.parseDisabledSessionNames("build\ndeploy\nlogs"));
    }

    @Test
    public void parseDisabledSessionNamesTrimsWhitespaceAndDropsBlankLines() {
        Set<String> expected = new LinkedHashSet<>(Arrays.asList("build", "deploy"));
        Assert.assertEquals(expected,
            TermuxAppSharedPreferences.parseDisabledSessionNames("  build  \n\n  \ndeploy\n"));
    }

    @Test
    public void parseDisabledSessionNamesRemovesDuplicatesPreservingFirstOccurrence() {
        Set<String> expected = new LinkedHashSet<>(Arrays.asList("build", "deploy"));
        Assert.assertEquals(expected,
            TermuxAppSharedPreferences.parseDisabledSessionNames("build\ndeploy\nbuild"));
    }

    @Test
    public void parseDisabledSessionNamesWithNullReturnsEmptySet() {
        Assert.assertTrue(TermuxAppSharedPreferences.parseDisabledSessionNames(null).isEmpty());
    }

    @Test
    public void parseDisabledSessionNamesWithDefaultValueReturnsEmptySet() {
        Assert.assertTrue(TermuxAppSharedPreferences.parseDisabledSessionNames(
            TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_DISABLED_SESSION_NAMES).isEmpty());
    }

    @Test
    public void serializeDisabledSessionNamesJoinsWithNewline() {
        Set<String> names = new LinkedHashSet<>(Arrays.asList("build", "deploy"));
        Assert.assertEquals("build\ndeploy",
            TermuxAppSharedPreferences.serializeDisabledSessionNames(names));
    }

    @Test
    public void serializeDisabledSessionNamesSkipsBlankAndTrimsEntries() {
        Set<String> names = new LinkedHashSet<>(Arrays.asList("  build  ", "", "   ", "deploy"));
        Assert.assertEquals("build\ndeploy",
            TermuxAppSharedPreferences.serializeDisabledSessionNames(names));
    }

    @Test
    public void serializeEmptyDisabledSessionNamesReturnsEmptyString() {
        Assert.assertEquals("",
            TermuxAppSharedPreferences.serializeDisabledSessionNames(new LinkedHashSet<>()));
    }

    @Test
    public void serializeThenParseDisabledSessionNamesRoundTrips() {
        Set<String> names = new LinkedHashSet<>(Arrays.asList("build", "deploy", "logs"));
        String serialized = TermuxAppSharedPreferences.serializeDisabledSessionNames(names);
        Assert.assertEquals(names, TermuxAppSharedPreferences.parseDisabledSessionNames(serialized));
    }
}
