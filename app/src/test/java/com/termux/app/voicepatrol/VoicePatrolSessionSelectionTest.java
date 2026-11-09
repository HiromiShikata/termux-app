package com.termux.app.voicepatrol;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VoicePatrolSessionSelectionTest {
    @Test
    public void callsFollowBellOrderAndRequireAnUnansweredBody() {
        List<VoicePatrolCall> calls = VoicePatrolSessionSelection.callsInBellOrder(
            Arrays.asList(2, 0, 1),
            Arrays.asList("alpha", "beta", "gamma"),
            new HashSet<>(Arrays.asList("alpha", "gamma")),
            mapOf("alpha", "first", "gamma", "third"));
        assertEquals(Arrays.asList(new VoicePatrolCall("gamma", "third"), new VoicePatrolCall("alpha", "first")), calls);
    }

    @Test
    public void callingSessionWithoutUnansweredBodyIsExcluded() {
        assertEquals(Collections.emptyList(), VoicePatrolSessionSelection.callsInBellOrder(
            Arrays.asList(0), Arrays.asList("alpha"), new HashSet<>(Collections.singletonList("alpha")),
            Collections.emptyMap()));
    }

    @Test
    public void absentSessionNameHasIndexMinusOne() {
        assertEquals(-1, VoicePatrolSessionSelection.indexOfSessionName(Arrays.asList("alpha"), "beta"));
    }

    private static Map<String, String> mapOf(String firstName, String firstBody, String secondName, String secondBody) {
        Map<String, String> result = new HashMap<>();
        result.put(firstName, firstBody);
        result.put(secondName, secondBody);
        return result;
    }
}
