package com.termux.app.voicepatrol;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class VoicePatrolCallOrder {

    private VoicePatrolCallOrder() {
    }

    public static List<Integer> callingSessionIndexesInBellOrder(List<Integer> orderedSessionIndexes,
                                                                 List<String> sessionNamesByIndex,
                                                                 Set<String> callingSessionNames) {
        List<Integer> indexes = new ArrayList<>();
        for (int sessionIndex : orderedSessionIndexes) {
            if (sessionIndex < 0 || sessionIndex >= sessionNamesByIndex.size()) {
                continue;
            }
            if (callingSessionNames.contains(sessionNamesByIndex.get(sessionIndex))) {
                indexes.add(sessionIndex);
            }
        }
        return indexes;
    }
}
