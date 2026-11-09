package com.termux.app.voicepatrol;

import java.util.List;

public interface VoicePatrolCallSource {

    List<VoicePatrolCall> callingSessionsInBellOrder();
}
