package com.termux.app.terminal.session;

import com.termux.app.terminal.SessionNewActivityState;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SessionNewActivityStateSerializerTest {

    private final SessionNewActivityStateSerializer serializer = new SessionNewActivityStateSerializer();

    @Test
    public void roundTripPreservesHandleBellAndSeenTimes() throws JSONException {
        List<SessionNewActivityState> states = Arrays.asList(
            new SessionNewActivityState("handle-one", 1_000L, 2_000L),
            new SessionNewActivityState("handle-two", 7_000L, null));

        List<SessionNewActivityState> result = serializer.deserialize(serializer.serialize(states));

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("handle-one", result.get(0).getHandle());
        Assert.assertEquals(Long.valueOf(1_000L), result.get(0).getLastBellTimeMillis());
        Assert.assertEquals(Long.valueOf(2_000L), result.get(0).getLastSeenTimeMillis());
        Assert.assertEquals("handle-two", result.get(1).getHandle());
        Assert.assertEquals(Long.valueOf(7_000L), result.get(1).getLastBellTimeMillis());
        Assert.assertNull(result.get(1).getLastSeenTimeMillis());
    }

    @Test
    public void deserializeNullReturnsEmptyList() throws JSONException {
        Assert.assertTrue(serializer.deserialize(null).isEmpty());
    }

    @Test
    public void deserializeEmptyStringReturnsEmptyList() throws JSONException {
        Assert.assertTrue(serializer.deserialize("").isEmpty());
    }

    @Test
    public void serializeEmptyListProducesEmptyJsonArray() throws JSONException {
        Assert.assertEquals("[]", serializer.serialize(new ArrayList<>()));
    }

    @Test
    public void deserializeSkipsEntriesWithoutHandle() throws JSONException {
        List<SessionNewActivityState> result =
            serializer.deserialize("[{\"lastBellTimeMillis\":1000}]");

        Assert.assertTrue(result.isEmpty());
    }
}
