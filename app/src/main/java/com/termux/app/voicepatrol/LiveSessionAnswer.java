package com.termux.app.voicepatrol;

import org.json.JSONException;
import org.json.JSONObject;

public final class LiveSessionAnswer {

    private static final String ERROR_FIELD = "error";

    private final boolean mAccepted;
    private final String mSdp;
    private final String mFailureReason;

    private LiveSessionAnswer(boolean accepted, String sdp, String failureReason) {
        mAccepted = accepted;
        mSdp = sdp;
        mFailureReason = failureReason;
    }

    public static LiveSessionAnswer fromHttpResponse(int statusCode, String body) {
        if (statusCode == 200 || statusCode == 201) {
            String sdp = body.trim();
            if (sdp.isEmpty()) {
                return new LiveSessionAnswer(false, "", "empty_sdp_answer");
            }
            return new LiveSessionAnswer(true, body, "");
        }
        return new LiveSessionAnswer(false, "", errorCodeOf(body, statusCode));
    }

    public static LiveSessionAnswer failed(String reason) {
        return new LiveSessionAnswer(false, "", reason);
    }

    public boolean isAccepted() {
        return mAccepted;
    }

    public String getSdp() {
        return mSdp;
    }

    public String getFailureReason() {
        return mFailureReason;
    }

    private static String errorCodeOf(String body, int statusCode) {
        try {
            String error = new JSONObject(body).optString(ERROR_FIELD);
            if (!error.isEmpty()) {
                return error;
            }
        } catch (JSONException notJson) {
            return "HTTP " + statusCode;
        }
        return "HTTP " + statusCode;
    }
}
