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
    public void roundTripPreservesSessionNameOutputActivityExplicitCallSeenAndUserInputTimes() throws JSONException {
        List<SessionNewActivityState> states = Arrays.asList(
            new SessionNewActivityState("session-one", 1_000L, 2_000L, "deploy failed", 3_000L, 4_000L),
            new SessionNewActivityState("session-two", 7_000L, null, null, null, null));

        List<SessionNewActivityState> result = serializer.deserialize(serializer.serialize(states));

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("session-one", result.get(0).getSessionName());
        Assert.assertEquals(Long.valueOf(1_000L), result.get(0).getLastOutputActivityTimeMillis());
        Assert.assertEquals(Long.valueOf(2_000L), result.get(0).getLastExplicitCallTimeMillis());
        Assert.assertEquals("deploy failed", result.get(0).getLastExplicitCallReason());
        Assert.assertEquals(Long.valueOf(3_000L), result.get(0).getLastSeenTimeMillis());
        Assert.assertEquals(Long.valueOf(4_000L), result.get(0).getLastUserInputTimeMillis());
        Assert.assertEquals("session-two", result.get(1).getSessionName());
        Assert.assertEquals(Long.valueOf(7_000L), result.get(1).getLastOutputActivityTimeMillis());
        Assert.assertNull(result.get(1).getLastExplicitCallTimeMillis());
        Assert.assertNull(result.get(1).getLastExplicitCallReason());
        Assert.assertNull(result.get(1).getLastSeenTimeMillis());
        Assert.assertNull(result.get(1).getLastUserInputTimeMillis());
    }

    @Test
    public void serializeUsesSessionNameKey() throws JSONException {
        List<SessionNewActivityState> states = Arrays.asList(
            new SessionNewActivityState("session-one", 1_000L, 2_000L, "", 3_000L, 4_000L));

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
            null,
            state.getLastUserInputTimeMillis(),
            state.getLastSeenTimeMillis()));
    }

    @Test
    public void roundTripPreservesStatuslineCallOutAndReplyTimes() throws JSONException {
        List<SessionNewActivityState> states = Arrays.asList(
            new SessionNewActivityState("session-one", 1_000L, 2_000L, "deploy failed", 3_000L, 4_000L,
                null, null, 5_000L, 6_000L, 7_000L));

        List<SessionNewActivityState> result = serializer.deserialize(serializer.serialize(states));

        Assert.assertEquals(Long.valueOf(5_000L), result.get(0).getStatuslineCallTimeMillis());
        Assert.assertEquals(Long.valueOf(6_000L), result.get(0).getStatuslineOutTimeMillis());
        Assert.assertEquals(Long.valueOf(7_000L), result.get(0).getStatuslineReplyTimeMillis());
    }

    @Test
    public void roundTripPreservesSubagentCount() throws JSONException {
        List<SessionNewActivityState> states = Arrays.asList(
            new SessionNewActivityState("session-one", 1_000L, 2_000L, "deploy failed", 3_000L, 4_000L,
                null, null, 5_000L, 6_000L, 7_000L, 8));

        List<SessionNewActivityState> result = serializer.deserialize(serializer.serialize(states));

        Assert.assertEquals(Integer.valueOf(8), result.get(0).getSubagentCount());
    }

    @Test
    public void legacyEntryWithoutSubagentCountDeserializesToNullSubagentCount() throws JSONException {
        List<SessionNewActivityState> result = serializer.deserialize(
            "[{\"sessionName\":\"legacy\",\"lastOutputActivityTimeMillis\":1000}]");

        Assert.assertEquals(1, result.size());
        Assert.assertNull(result.get(0).getSubagentCount());
    }

    @Test
    public void legacyEntryWithoutStatuslineTimesDeserializesToNullStatuslineTimes() throws JSONException {
        List<SessionNewActivityState> result = serializer.deserialize(
            "[{\"sessionName\":\"legacy\",\"lastOutputActivityTimeMillis\":1000}]");

        Assert.assertEquals(1, result.size());
        Assert.assertNull(result.get(0).getStatuslineCallTimeMillis());
        Assert.assertNull(result.get(0).getStatuslineOutTimeMillis());
        Assert.assertNull(result.get(0).getStatuslineReplyTimeMillis());
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

    @Test
    public void roundTripPreservesUnacknowledgedCallReasons() throws JSONException {
        List<SessionNewActivityState> states = Arrays.asList(
            new SessionNewActivityState("session-one", 1_000L, 2_000L, "second reason", 3_000L, 4_000L,
                Arrays.asList("first reason", "second reason")));

        List<SessionNewActivityState> result = serializer.deserialize(serializer.serialize(states));

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(
            Arrays.asList("first reason", "second reason"),
            result.get(0).getUnacknowledgedCallReasons());
    }

    @Test
    public void legacyEntryWithoutUnacknowledgedCallReasonsDeserializesToNull() throws JSONException {
        List<SessionNewActivityState> result = serializer.deserialize(
            "[{\"sessionName\":\"legacy\",\"lastExplicitCallTimeMillis\":1000,\"lastExplicitCallReason\":\"needs approval\"}]");

        Assert.assertEquals(1, result.size());
        Assert.assertNull(result.get(0).getUnacknowledgedCallReasons());
    }

    @Test
    public void roundTripPreservesCallTriggerValues() throws JSONException {
        List<SessionNewActivityState> states = Arrays.asList(
            new SessionNewActivityState("session-one", 1_000L, 2_000L, "second reason", 3_000L, 4_000L,
                Arrays.asList("pending reason"), Arrays.asList("trigger value")));

        List<SessionNewActivityState> result = serializer.deserialize(serializer.serialize(states));

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(
            Arrays.asList("trigger value"),
            result.get(0).getCallTriggerValues());
    }

    @Test
    public void legacyEntryWithoutCallTriggerValuesDeserializesToNull() throws JSONException {
        List<SessionNewActivityState> result = serializer.deserialize(
            "[{\"sessionName\":\"legacy\",\"lastExplicitCallTimeMillis\":1000,\"lastExplicitCallReason\":\"needs approval\"}]");

        Assert.assertEquals(1, result.size());
        Assert.assertNull(result.get(0).getCallTriggerValues());
    }

    @Test
    public void legacyAcknowledgedCallReasonsSeedCallTriggerValues() throws JSONException {
        List<SessionNewActivityState> result = serializer.deserialize(
            "[{\"sessionName\":\"legacy\",\"lastExplicitCallTimeMillis\":1000,"
                + "\"lastExplicitCallReason\":\"needs approval\","
                + "\"acknowledgedCallReasons\":[\"needs approval\"]}]");

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(
            Arrays.asList("needs approval"),
            result.get(0).getCallTriggerValues());
    }

    @Test
    public void legacyUnacknowledgedCallReasonsAlsoSeedCallTriggerValues() throws JSONException {
        List<SessionNewActivityState> result = serializer.deserialize(
            "[{\"sessionName\":\"legacy\",\"lastExplicitCallTimeMillis\":1000,"
                + "\"acknowledgedCallReasons\":[\"answered call\"],"
                + "\"unacknowledgedCallReasons\":[\"pending call\"]}]");

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(
            "unacknowledged first so the acknowledged values survive the trailing cap",
            Arrays.asList("pending call", "answered call"),
            result.get(0).getCallTriggerValues());
        Assert.assertEquals(
            Arrays.asList("pending call"),
            result.get(0).getUnacknowledgedCallReasons());
    }

    @Test
    public void anEmptyStoredCallTriggerValuesArrayStillSeedsFromTheLegacyReasons() throws JSONException {
        List<SessionNewActivityState> result = serializer.deserialize(
            "[{\"sessionName\":\"legacy\",\"callTriggerValues\":[],"
                + "\"acknowledgedCallReasons\":[\"answered call\"],"
                + "\"unacknowledgedCallReasons\":[\"pending call\"]}]");

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(
            Arrays.asList("pending call", "answered call"),
            result.get(0).getCallTriggerValues());
    }

    @Test
    public void anEmptyStoredCallTriggerValuesArrayWithoutLegacyReasonsStaysEmpty() throws JSONException {
        List<SessionNewActivityState> result = serializer.deserialize(
            "[{\"sessionName\":\"legacy\",\"callTriggerValues\":[]}]");

        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.get(0).getCallTriggerValues().isEmpty());
    }

    @Test
    public void storedCallTriggerValuesWinOverLegacyAcknowledgedCallReasons() throws JSONException {
        List<SessionNewActivityState> result = serializer.deserialize(
            "[{\"sessionName\":\"legacy\",\"acknowledgedCallReasons\":[\"legacy reason\"],"
                + "\"callTriggerValues\":[\"trigger value\"]}]");

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(
            Arrays.asList("trigger value"),
            result.get(0).getCallTriggerValues());
    }

    @Test
    public void legacySeededCallTriggerValuesKeepTheNewestEntriesUnderTheCap() throws JSONException {
        int legacyCount = SessionNewActivityStateCaps.MAX_CALL_TRIGGER_VALUES_PER_SESSION + 50;
        StringBuilder oversizedTail = new StringBuilder();
        for (int index = 0; index < SessionNewActivityStateCaps.MAX_REASON_LENGTH + 5_000; index++) {
            oversizedTail.append('x');
        }
        org.json.JSONArray legacyReasons = new org.json.JSONArray();
        List<String> expectedSurvivors = new ArrayList<>();
        for (int index = 0; index < legacyCount; index++) {
            String reason = "reason-" + index + "-" + oversizedTail;
            legacyReasons.put(reason);
            if (index >= legacyCount - SessionNewActivityStateCaps.MAX_CALL_TRIGGER_VALUES_PER_SESSION) {
                expectedSurvivors.add(
                    reason.substring(0, SessionNewActivityStateCaps.MAX_REASON_LENGTH));
            }
        }
        org.json.JSONObject legacyEntry = new org.json.JSONObject();
        legacyEntry.put("sessionName", "legacy");
        legacyEntry.put("acknowledgedCallReasons", legacyReasons);
        String legacyDocument = new org.json.JSONArray().put(legacyEntry).toString();

        List<SessionNewActivityState> result = serializer.deserialize(legacyDocument);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("the newest legacy reasons are the ones that survive",
            expectedSurvivors, result.get(0).getCallTriggerValues());
    }

    @Test
    public void legacySeedingKeepsAcknowledgedValuesWhenTheUnionExceedsTheCap() throws JSONException {
        int perList = SessionNewActivityStateCaps.MAX_CALL_TRIGGER_VALUES_PER_SESSION - 5;
        org.json.JSONArray acknowledged = new org.json.JSONArray();
        org.json.JSONArray unacknowledged = new org.json.JSONArray();
        for (int index = 0; index < perList; index++) {
            acknowledged.put("answered-" + index);
            unacknowledged.put("pending-" + index);
        }
        org.json.JSONObject legacyEntry = new org.json.JSONObject();
        legacyEntry.put("sessionName", "legacy");
        legacyEntry.put("acknowledgedCallReasons", acknowledged);
        legacyEntry.put("unacknowledgedCallReasons", unacknowledged);
        String legacyDocument = new org.json.JSONArray().put(legacyEntry).toString();

        List<SessionNewActivityState> result = serializer.deserialize(legacyDocument);

        List<String> seeded = result.get(0).getCallTriggerValues();
        Assert.assertEquals(SessionNewActivityStateCaps.MAX_CALL_TRIGGER_VALUES_PER_SESSION,
            seeded.size());
        for (int index = 0; index < perList; index++) {
            Assert.assertTrue("every answered call must survive the cap: answered-" + index,
                seeded.contains("answered-" + index));
        }
        Assert.assertFalse("the oldest pending reason is the one dropped",
            seeded.contains("pending-0"));
        Assert.assertTrue("the newest pending reason survives",
            seeded.contains("pending-" + (perList - 1)));
    }

    @Test
    public void deserializeShrinksOversizedStoredStateOnLoad() throws JSONException {
        StringBuilder oversizedReason = new StringBuilder();
        for (int index = 0; index < SessionNewActivityStateCaps.MAX_REASON_LENGTH + 5_000; index++) {
            oversizedReason.append('x');
        }
        List<String> manyReasons = new ArrayList<>();
        for (int index = 0; index < SessionNewActivityStateCaps.MAX_REASONS_PER_SESSION + 50; index++) {
            manyReasons.add(oversizedReason.toString());
        }
        List<SessionNewActivityState> oversizedState = Arrays.asList(
            new SessionNewActivityState("session-one", 1_000L, 2_000L, oversizedReason.toString(),
                3_000L, 4_000L, manyReasons, manyReasons, 5_000L, 6_000L, 7_000L));
        String serializedWithoutCaps = serializeWithoutCaps(oversizedState);

        List<SessionNewActivityState> result = serializer.deserialize(serializedWithoutCaps);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(SessionNewActivityStateCaps.MAX_REASON_LENGTH,
            result.get(0).getLastExplicitCallReason().length());
        Assert.assertEquals(SessionNewActivityStateCaps.MAX_REASONS_PER_SESSION,
            result.get(0).getUnacknowledgedCallReasons().size());
        Assert.assertEquals(SessionNewActivityStateCaps.MAX_CALL_TRIGGER_VALUES_PER_SESSION,
            result.get(0).getCallTriggerValues().size());
        for (String reason : result.get(0).getUnacknowledgedCallReasons()) {
            Assert.assertEquals(SessionNewActivityStateCaps.MAX_REASON_LENGTH, reason.length());
        }
    }

    @Test
    public void serializeCapsReasonsSoStoredValueCannotBloat() throws JSONException {
        StringBuilder oversizedReason = new StringBuilder();
        for (int index = 0; index < SessionNewActivityStateCaps.MAX_REASON_LENGTH + 1_000; index++) {
            oversizedReason.append('y');
        }
        List<SessionNewActivityState> states = Arrays.asList(
            new SessionNewActivityState("session-one", 1_000L, 2_000L, oversizedReason.toString(),
                3_000L, 4_000L));

        List<SessionNewActivityState> result = serializer.deserialize(serializer.serialize(states));

        Assert.assertEquals(SessionNewActivityStateCaps.MAX_REASON_LENGTH,
            result.get(0).getLastExplicitCallReason().length());
    }

    private static String serializeWithoutCaps(List<SessionNewActivityState> states) throws JSONException {
        org.json.JSONArray array = new org.json.JSONArray();
        for (SessionNewActivityState state : states) {
            org.json.JSONObject object = new org.json.JSONObject();
            object.put("sessionName", state.getSessionName());
            object.put("lastOutputActivityTimeMillis", state.getLastOutputActivityTimeMillis());
            object.put("lastExplicitCallTimeMillis", state.getLastExplicitCallTimeMillis());
            object.put("lastExplicitCallReason", state.getLastExplicitCallReason());
            object.put("unacknowledgedCallReasons", new org.json.JSONArray(state.getUnacknowledgedCallReasons()));
            object.put("callTriggerValues", new org.json.JSONArray(state.getCallTriggerValues()));
            object.put("lastSeenTimeMillis", state.getLastSeenTimeMillis());
            object.put("lastUserInputTimeMillis", state.getLastUserInputTimeMillis());
            object.put("statuslineCallTimeMillis", state.getStatuslineCallTimeMillis());
            object.put("statuslineOutTimeMillis", state.getStatuslineOutTimeMillis());
            object.put("statuslineReplyTimeMillis", state.getStatuslineReplyTimeMillis());
            array.put(object);
        }
        return array.toString();
    }
}
