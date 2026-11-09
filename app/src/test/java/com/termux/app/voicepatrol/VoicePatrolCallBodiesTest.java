package com.termux.app.voicepatrol;

import static org.junit.Assert.assertEquals;

import com.termux.app.ownercall.OwnerCall;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoicePatrolCallBodiesTest {
    @Test
    public void returnsFirstBodyAndOmitsEmptySessions() {
        List<Object[]> cases = Arrays.asList(
            new Object[]{Collections.emptyMap(), Collections.emptyMap()},
            new Object[]{mapOf("alpha", Collections.emptyList()), Collections.emptyMap()},
            new Object[]{mapOf("alpha", Arrays.asList(call("alpha", "first"))), mapOf("alpha", "first")},
            new Object[]{mapOf("alpha", Arrays.asList(call("alpha", "first"), call("alpha", "second")),
                "beta", Arrays.asList(call("beta", "only"))), mapOf("alpha", "first", "beta", "only")}
        );
        for (Object[] testCase : cases) {
            assertEquals(testCase[1], VoicePatrolCallBodies.firstBodyBySessionName(
                (Map<String, List<OwnerCall>>) testCase[0]));
        }
    }

    private static OwnerCall call(String sessionName, String body) {
        return new OwnerCall(sessionName, "2026-11-09T00:00:00Z", body);
    }

    private static Map<String, Object> mapOf(String firstKey, Object firstValue) {
        Map<String, Object> result = new HashMap<>();
        result.put(firstKey, firstValue);
        return result;
    }

    private static Map<String, Object> mapOf(String firstKey, Object firstValue, String secondKey, Object secondValue) {
        Map<String, Object> result = mapOf(firstKey, firstValue);
        result.put(secondKey, secondValue);
        return result;
    }
}
