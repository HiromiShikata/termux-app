package com.termux.app.terminal.session;

import com.termux.app.terminal.SessionNewActivityState;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class SessionNewActivityStateSerializer {

    private static final String KEY_SESSION_NAME = "sessionName";
    private static final String KEY_LAST_OUTPUT_ACTIVITY_TIME_MILLIS = "lastOutputActivityTimeMillis";
    private static final String KEY_LAST_EXPLICIT_CALL_TIME_MILLIS = "lastExplicitCallTimeMillis";
    private static final String KEY_LAST_EXPLICIT_CALL_REASON = "lastExplicitCallReason";
    private static final String KEY_LAST_SEEN_TIME_MILLIS = "lastSeenTimeMillis";
    private static final String KEY_LAST_USER_INPUT_TIME_MILLIS = "lastUserInputTimeMillis";

    public String serialize(List<SessionNewActivityState> states) throws JSONException {
        JSONArray array = new JSONArray();
        for (SessionNewActivityState state : states) {
            JSONObject object = new JSONObject();
            object.put(KEY_SESSION_NAME, state.getSessionName());
            if (state.getLastOutputActivityTimeMillis() != null)
                object.put(KEY_LAST_OUTPUT_ACTIVITY_TIME_MILLIS, state.getLastOutputActivityTimeMillis().longValue());
            if (state.getLastExplicitCallTimeMillis() != null)
                object.put(KEY_LAST_EXPLICIT_CALL_TIME_MILLIS, state.getLastExplicitCallTimeMillis().longValue());
            if (state.getLastExplicitCallReason() != null)
                object.put(KEY_LAST_EXPLICIT_CALL_REASON, state.getLastExplicitCallReason());
            if (state.getLastSeenTimeMillis() != null)
                object.put(KEY_LAST_SEEN_TIME_MILLIS, state.getLastSeenTimeMillis().longValue());
            if (state.getLastUserInputTimeMillis() != null)
                object.put(KEY_LAST_USER_INPUT_TIME_MILLIS, state.getLastUserInputTimeMillis().longValue());
            array.put(object);
        }
        return array.toString();
    }

    public List<SessionNewActivityState> deserialize(String serialized) throws JSONException {
        List<SessionNewActivityState> states = new ArrayList<>();
        if (serialized == null || serialized.isEmpty())
            return states;

        JSONArray array = new JSONArray(serialized);
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            if (object.isNull(KEY_SESSION_NAME))
                continue;

            String sessionName = object.getString(KEY_SESSION_NAME);
            Long lastOutputActivityTimeMillis = optionalLong(object, KEY_LAST_OUTPUT_ACTIVITY_TIME_MILLIS);
            Long lastExplicitCallTimeMillis = optionalLong(object, KEY_LAST_EXPLICIT_CALL_TIME_MILLIS);
            String lastExplicitCallReason = optionalString(object, KEY_LAST_EXPLICIT_CALL_REASON);
            Long lastSeenTimeMillis = optionalLong(object, KEY_LAST_SEEN_TIME_MILLIS);
            Long lastUserInputTimeMillis = optionalLong(object, KEY_LAST_USER_INPUT_TIME_MILLIS);

            states.add(new SessionNewActivityState(sessionName, lastOutputActivityTimeMillis,
                lastExplicitCallTimeMillis, lastExplicitCallReason, lastSeenTimeMillis,
                lastUserInputTimeMillis));
        }
        return states;
    }

    private static Long optionalLong(JSONObject object, String key) throws JSONException {
        return object.isNull(key) ? null : object.getLong(key);
    }

    private static String optionalString(JSONObject object, String key) throws JSONException {
        return object.isNull(key) ? null : object.getString(key);
    }
}
