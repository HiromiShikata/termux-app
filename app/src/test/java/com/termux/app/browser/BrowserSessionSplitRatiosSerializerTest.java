package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.LinkedHashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class BrowserSessionSplitRatiosSerializerTest {

    private static final float DELTA = 1e-6f;

    private final BrowserSessionSplitRatiosSerializer mSerializer = new BrowserSessionSplitRatiosSerializer();

    @Test
    public void deserializesAnEmptyStringToAnEmptyMap() {
        Assert.assertTrue(mSerializer.deserialize("").isEmpty());
    }

    @Test
    public void deserializesNullToAnEmptyMap() {
        Assert.assertTrue(mSerializer.deserialize(null).isEmpty());
    }

    @Test
    public void deserializesMalformedJsonToAnEmptyMap() {
        Assert.assertTrue(mSerializer.deserialize("{not valid json").isEmpty());
    }

    @Test
    public void serializesAnEmptyMapToAnEmptyJsonObject() {
        Assert.assertEquals("{}", mSerializer.serialize(new LinkedHashMap<>()));
    }

    @Test
    public void roundTripsRatiosForMultipleSessions() {
        Map<String, Float> ratios = new LinkedHashMap<>();
        ratios.put("sessionA", 0.4f);
        ratios.put("sessionB", 0.8f);

        Map<String, Float> restored = mSerializer.deserialize(mSerializer.serialize(ratios));

        Assert.assertEquals(2, restored.size());
        Assert.assertEquals(0.4f, restored.get("sessionA"), DELTA);
        Assert.assertEquals(0.8f, restored.get("sessionB"), DELTA);
    }

    @Test
    public void skipsEntriesWithAnEmptySessionNameWhenSerializing() {
        Map<String, Float> ratios = new LinkedHashMap<>();
        ratios.put("", 0.4f);
        ratios.put("sessionA", 0.5f);

        Map<String, Float> restored = mSerializer.deserialize(mSerializer.serialize(ratios));

        Assert.assertEquals(1, restored.size());
        Assert.assertEquals(0.5f, restored.get("sessionA"), DELTA);
    }

    @Test
    public void skipsEntriesWithANullRatioWhenSerializing() {
        Map<String, Float> ratios = new LinkedHashMap<>();
        ratios.put("sessionA", null);
        ratios.put("sessionB", 0.5f);

        Map<String, Float> restored = mSerializer.deserialize(mSerializer.serialize(ratios));

        Assert.assertEquals(1, restored.size());
        Assert.assertEquals(0.5f, restored.get("sessionB"), DELTA);
    }
}
