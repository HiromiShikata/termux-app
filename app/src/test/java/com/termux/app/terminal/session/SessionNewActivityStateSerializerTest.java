package com.termux.app.terminal.session;

import com.termux.app.terminal.SessionNewActivityState;
import com.termux.app.terminal.SessionNewActivityTier;

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
    public void roundTripPreservesSessionNameOutputActivityExplicitCallAndSeenTimes() throws JSONException {
        List<SessionNewActivityState> states = Arrays.asList(
            new SessionNewActivityState("session-one", 1_000L, 2_000L, "deploy failed", 3_000L),
            new SessionNewActivityState("session-two", 7_000L, null, null, null));

        List<SessionNewActivityState> result = serializer.deserialize(serializer.serialize(states));

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("session-one", result.get(0).getSessionName());
        Assert.assertEquals(Long.valueOf(1_000L), result.get(0).getLastOutputActivityTimeMillis());
        Assert.assertEquals(Long.valueOf(2_000L), result.get(0).getLastExplicitCallTimeMillis());
        Assert.assertEquals("deploy failed", result.get(0).getLastExplicitCallReason());
        Assert.assertEquals(Long.valueOf(3_000L), result.get(0).getLastSeenTimeMillis());
        Assert.assertEquals("session-two", result.get(1).getSessionName());
        Assert.assertEquals(Long.valueOf(7_000L), result.get(1).getLastOutputActivityTimeMillis());
        Assert.assertNull(result.get(1).getLastExplicitCallTimeMillis());
        Assert.assertNull(result.get(1).getLastExplicitCallReason());
        Assert.assertNull(result.get(1).getLastSeenTimeMillis());
    }

    @Test
    public void serializeUsesSessionNameKey() throws JSONException {
        List<SessionNewActivityState> states = Arrays.asList(
            new SessionNewActivityState("session-one", 1_000L, 2_000L, "", 3_000L));

        String serialized = serializer.serialize(states);

        Assert.assertTrue(serialized.contains("\"sessionName\":\"session-one\""));
    }

    @Test
    public void legacyEntryWithoutExplicitCallReasonDeserializesToNullReason() throws JSONException {
        List<SessionNewActivityState> result = serializer.deserialize(
            "[{\"sessionName\":\"legacy\",\"lastExplicitCallTimeMillis\":1000}]");

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(Long.valueOf(1_000L), result.get(0).getLastExplicitCallTimeMillis());
        Assert.assertNull(result.get(0).getLastExplicitCallReason());
    }

    @Test
    public void legacyLastBellTimeMillisIsDroppedAndDoesNotBecomeExplicitCall() throws JSONException {
        List<SessionNewActivityState> result = serializer.deserialize(
            "[{\"sessionName\":\"legacy\",\"lastBellTimeMillis\":1234,\"lastSeenTimeMillis\":500}]");

        Assert.assertEquals(1, result.size());
        Assert.assertNull(result.get(0).getLastExplicitCallTimeMillis());
        Assert.assertNull(result.get(0).getLastOutputActivityTimeMillis());
        Assert.assertEquals(Long.valueOf(500L), result.get(0).getLastSeenTimeMillis());
    }

    @Test
    public void legacyLastBellTimeMillisDoesNotProduceRedTierAfterLoad() throws JSONException {
        List<SessionNewActivityState> result = serializer.deserialize(
            "[{\"sessionName\":\"legacy\",\"lastBellTimeMillis\":1234}]");

        Assert.assertEquals(1, result.size());
        SessionNewActivityState state = result.get(0);
        Assert.assertEquals(SessionNewActivityTier.NONE, SessionNewActivityTier.resolve(
            state.getLastOutputActivityTimeMillis(),
            state.getLastExplicitCallTimeMillis(),
            state.getLastSeenTimeMillis()));
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
    public void deserializeSkipsEntriesWithoutSessionName() throws JSONException {
        List<SessionNewActivityState> result =
            serializer.deserialize("[{\"lastExplicitCallTimeMillis\":1000}]");

        Assert.assertTrue(result.isEmpty());
    }
}
