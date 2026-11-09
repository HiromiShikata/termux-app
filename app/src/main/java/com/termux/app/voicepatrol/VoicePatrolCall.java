package com.termux.app.voicepatrol;

import java.util.Objects;

public final class VoicePatrolCall {

    private final String mSessionName;
    private final String mBody;

    public VoicePatrolCall(String sessionName, String body) {
        mSessionName = sessionName;
        mBody = body;
    }

    public String getSessionName() {
        return mSessionName;
    }

    public String getBody() {
        return mBody;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof VoicePatrolCall)) {
            return false;
        }
        VoicePatrolCall otherCall = (VoicePatrolCall) other;
        return mSessionName.equals(otherCall.mSessionName) && mBody.equals(otherCall.mBody);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSessionName, mBody);
    }
}
