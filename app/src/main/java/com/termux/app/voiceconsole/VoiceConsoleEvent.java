package com.termux.app.voiceconsole;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public final class VoiceConsoleEvent {

    public static final String SESSION_CLOSE_EVENT_JSON = "{\"type\":\"session.close\"}";

    static final String SESSION_CLOSED_REASON = "session_closed";
    static final String SESSION_ERROR_REASON = "session_error";

    private static final String LOG_TAG = "TermuxVoiceConsole";
    private static final String TYPE_FIELD = "type";
    private static final String SESSION_CLOSED_TYPE = "session.closed";
    private static final String ERROR_TYPE = "error";

    private VoiceConsoleEvent() {
    }

    /**
     * Returns why the session ends for an event that ends it, or null for every other event.
     * The app ignores every other server event.
     */
    public static String endingReasonOf(String eventJson) {
        String type;
        try {
            type = new JSONObject(eventJson).optString(TYPE_FIELD);
        } catch (JSONException notJson) {
            Log.w(LOG_TAG, "ignoring a live event that is not JSON");
            return null;
        }
        if (SESSION_CLOSED_TYPE.equals(type)) {
            return SESSION_CLOSED_REASON;
        }
        if (ERROR_TYPE.equals(type)) {
            return SESSION_ERROR_REASON;
        }
        return null;
    }
}
