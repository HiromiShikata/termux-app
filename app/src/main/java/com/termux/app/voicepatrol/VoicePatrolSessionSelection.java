package com.termux.app.voicepatrol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VoicePatrolSessionSelection {

    private VoicePatrolSessionSelection() {
    }

    public static List<VoicePatrolCall> callsInBellOrder(List<Integer> orderedSessionIndexes,
                                                         List<String> sessionNames,
                                                         Set<String> callingSessionNames,
                                                         Map<String, String> unansweredBodiesBySessionName) {
        List<VoicePatrolCall> calls = new ArrayList<>();
        for (int sessionIndex : VoicePatrolCallOrder.callingSessionIndexesInBellOrder(
                orderedSessionIndexes, sessionNames, callingSessionNames)) {
            String sessionName = sessionNames.get(sessionIndex);
            if (unansweredBodiesBySessionName.containsKey(sessionName)) {
                calls.add(new VoicePatrolCall(sessionName, unansweredBodiesBySessionName.get(sessionName)));
            }
        }
        return calls;
    }

    public static int indexOfSessionName(List<String> sessionNames, String sessionName) {
        return sessionNames.indexOf(sessionName);
    }
}
