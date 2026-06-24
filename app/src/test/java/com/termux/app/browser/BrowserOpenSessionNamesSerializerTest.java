package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.LinkedHashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class BrowserOpenSessionNamesSerializerTest {

    private final BrowserOpenSessionNamesSerializer mSerializer = new BrowserOpenSessionNamesSerializer();

    @Test
    public void serializeThenDeserializeRoundTripsTheSessionNames() {
        Set<String> names = new LinkedHashSet<>();
        names.add("session-a");
        names.add("session-b");

        Set<String> restored = mSerializer.deserialize(mSerializer.serialize(names));

        Assert.assertEquals(names, restored);
    }

    @Test
    public void deserializeNullYieldsAnEmptySet() {
        Assert.assertTrue(mSerializer.deserialize(null).isEmpty());
    }

    @Test
    public void deserializeEmptyStringYieldsAnEmptySet() {
        Assert.assertTrue(mSerializer.deserialize("").isEmpty());
    }

    @Test
    public void deserializeMalformedJsonYieldsAnEmptySet() {
        Assert.assertTrue(mSerializer.deserialize("not-json").isEmpty());
    }

    @Test
    public void serializeEmptySetYieldsAnEmptyJsonArray() {
        Assert.assertEquals("[]", mSerializer.serialize(new LinkedHashSet<>()));
    }

    @Test
    public void deserializeSkipsEmptySessionNames() {
        Set<String> restored = mSerializer.deserialize("[\"session-a\",\"\"]");
        Assert.assertEquals(1, restored.size());
        Assert.assertTrue(restored.contains("session-a"));
    }
}
