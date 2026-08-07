package com.termux.shared.termux.settings.preferences;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.LinkedHashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class UserRemovedSessionTimeSerializerTest {

    private static final String SESSION_NAME_WITH_A_SPACE = "google logon";

    @Test
    public void serializedRemovalRecordsComeBackWithTheSameSessionNamesAndTimes() {
        Map<String, Long> removalRecords = new LinkedHashMap<>();
        removalRecords.put("https://example.test/a", 1_700_000_000_000L);
        removalRecords.put(SESSION_NAME_WITH_A_SPACE, 1_700_000_060_000L);

        Map<String, Long> restored = UserRemovedSessionTimeSerializer.parse(
            UserRemovedSessionTimeSerializer.serialize(removalRecords));

        Assert.assertEquals("a session name the owner typed can contain a space, so the stored record"
                + " must survive a round trip unchanged rather than losing part of the name",
            removalRecords, restored);
    }

    @Test
    public void parsingAnAbsentValueYieldsNoRemovalRecords() {
        Assert.assertTrue(UserRemovedSessionTimeSerializer.parse(null).isEmpty());
        Assert.assertTrue(UserRemovedSessionTimeSerializer.parse("").isEmpty());
    }

    @Test
    public void aStoredLineWhoseRemovalTimeIsNotANumberIsDiscardedAndTheRestSurvive() {
        Map<String, Long> restored = UserRemovedSessionTimeSerializer.parse(
            "not-a-time https://example.test/a\n1700000000000 https://example.test/b");

        Assert.assertEquals("a corrupt line must not take the intact records down with it",
            1, restored.size());
        Assert.assertEquals(Long.valueOf(1_700_000_000_000L), restored.get("https://example.test/b"));
    }

    @Test
    public void aStoredLineWithoutASessionNameIsDiscarded() {
        Assert.assertTrue(UserRemovedSessionTimeSerializer.parse("1700000000000").isEmpty());
        Assert.assertTrue(UserRemovedSessionTimeSerializer.parse("1700000000000 ").isEmpty());
    }

    @Test
    public void aRemovalRecordWithoutASessionNameOrATimeIsNotWrittenOut() {
        Map<String, Long> removalRecords = new LinkedHashMap<>();
        removalRecords.put("  ", 1_700_000_000_000L);
        removalRecords.put("https://example.test/a", null);

        Assert.assertEquals("", UserRemovedSessionTimeSerializer.serialize(removalRecords));
    }
}
