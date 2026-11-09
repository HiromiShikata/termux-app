package com.termux.app.voicepatrol;

import com.termux.app.ownercall.OwnerCall;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VoicePatrolCallBodies {

    private VoicePatrolCallBodies() {
    }

    public static Map<String, String> firstBodyBySessionName(Map<String, List<OwnerCall>> unansweredCallsBySessionName) {
        Map<String, String> bodies = new HashMap<>();
        for (Map.Entry<String, List<OwnerCall>> entry : unansweredCallsBySessionName.entrySet()) {
            List<OwnerCall> unansweredCalls = entry.getValue();
            if (!unansweredCalls.isEmpty()) {
                bodies.put(entry.getKey(), unansweredCalls.get(0).getBody());
            }
        }
        return bodies;
    }
}
