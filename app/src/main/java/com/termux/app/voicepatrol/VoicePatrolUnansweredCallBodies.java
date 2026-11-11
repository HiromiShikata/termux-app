package com.termux.app.voicepatrol;

import com.termux.app.ownercall.OwnerCall;
import com.termux.app.ownercall.OwnerCallUnansweredSelection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VoicePatrolUnansweredCallBodies {

    public interface FileUrlOf {
        String fileUrlOf(String sessionName);
    }

    public interface AnsweredThroughMillisOf {
        Long answeredThroughMillisOf(String sessionName);
    }

    private VoicePatrolUnansweredCallBodies() {
    }

    public static Map<String, String> bodiesBySessionName(Set<String> callingSessionNames,
                                                          VoicePatrolOwnerCallCache cache,
                                                          FileUrlOf fileUrlOf,
                                                          AnsweredThroughMillisOf answeredThroughMillisOf,
                                                          long nowMillis) {
        Map<String, List<OwnerCall>> unansweredCallsBySessionName = new HashMap<>();
        for (String sessionName : callingSessionNames) {
            List<OwnerCall> calls = cache.callsOf(sessionName, fileUrlOf.fileUrlOf(sessionName), nowMillis);
            unansweredCallsBySessionName.put(sessionName, OwnerCallUnansweredSelection.of(
                calls, answeredThroughMillisOf.answeredThroughMillisOf(sessionName), nowMillis));
        }
        cache.retainOnly(callingSessionNames);
        return VoicePatrolCallBodies.firstBodyBySessionName(unansweredCallsBySessionName);
    }
}
