package com.termux.shared.termux.settings.preferences;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class TermuxAppSharedPreferencesCollapsedProjectKeysTest {

    @Test
    public void parsesNewlineDelimitedKeysPreservingOrder() {
        Set<String> expected = new LinkedHashSet<>(Arrays.asList("umino", "xmile", "n/a"));
        Assert.assertEquals(expected,
            TermuxAppSharedPreferences.parseCollapsedProjectKeys("umino\nxmile\nn/a"));
    }

    @Test
    public void trimsWhitespaceAndDropsBlankLines() {
        Set<String> expected = new LinkedHashSet<>(Arrays.asList("umino", "xmile"));
        Assert.assertEquals(expected,
            TermuxAppSharedPreferences.parseCollapsedProjectKeys("  umino  \n\n  \nxmile\n"));
    }

    @Test
    public void removesDuplicateKeysPreservingFirstOccurrenceOrder() {
        Set<String> expected = new LinkedHashSet<>(Arrays.asList("umino", "xmile"));
        Assert.assertEquals(expected,
            TermuxAppSharedPreferences.parseCollapsedProjectKeys("umino\nxmile\numino"));
    }

    @Test
    public void parsingNullReturnsEmptySet() {
        Assert.assertTrue(TermuxAppSharedPreferences.parseCollapsedProjectKeys(null).isEmpty());
    }

    @Test
    public void parsingDefaultEmptyValueReturnsEmptySet() {
        Assert.assertTrue(TermuxAppSharedPreferences.parseCollapsedProjectKeys(
            TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_COLLAPSED_PROJECT_KEYS).isEmpty());
    }

    @Test
    public void serializesKeysAsNewlineDelimitedString() {
        Set<String> keys = new LinkedHashSet<>(Arrays.asList("umino", "xmile"));
        Assert.assertEquals("umino\nxmile",
            TermuxAppSharedPreferences.serializeCollapsedProjectKeys(keys));
    }

    @Test
    public void serializingEmptySetReturnsEmptyString() {
        Assert.assertEquals("",
            TermuxAppSharedPreferences.serializeCollapsedProjectKeys(new LinkedHashSet<>()));
    }

    @Test
    public void serializeSkipsBlankAndTrimsEntries() {
        Set<String> keys = new LinkedHashSet<>(Arrays.asList("  umino  ", "", "   ", "xmile"));
        Assert.assertEquals("umino\nxmile",
            TermuxAppSharedPreferences.serializeCollapsedProjectKeys(keys));
    }

    @Test
    public void serializeThenParseRoundTripsKeys() {
        Set<String> keys = new LinkedHashSet<>(Arrays.asList("umino", "xmile", "n/a"));
        String serialized = TermuxAppSharedPreferences.serializeCollapsedProjectKeys(keys);
        Assert.assertEquals(keys, TermuxAppSharedPreferences.parseCollapsedProjectKeys(serialized));
    }
}
