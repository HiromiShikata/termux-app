package com.termux.app.terminal.session;

import com.termux.app.terminal.SessionNewActivityState;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class SessionNewActivityStateSerializer {

    private static final String KEY_HANDLE = "handle";
    private static final String KEY_LAST_BELL_TIME_MILLIS = "lastBellTimeMillis";
    private static final String KEY_LAST_SEEN_TIME_MILLIS = "lastSeenTimeMillis";

    public String serialize(List<SessionNewActivityState> states) throws JSONException {
        JSONArray array = new JSONArray();
        for (SessionNewActivityState state : states) {
            JSONObject object = new JSONObject();
            object.put(KEY_HANDLE, state.getHandle());
            if (state.getLastBellTimeMillis() != null)
                object.put(KEY_LAST_BELL_TIME_MILLIS, state.getLastBellTimeMillis().longValue());
            if (state.getLastSeenTimeMillis() != null)
                object.put(KEY_LAST_SEEN_TIME_MILLIS, state.getLastSeenTimeMillis().longValue());
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
            if (object.isNull(KEY_HANDLE))
                continue;

            String handle = object.getString(KEY_HANDLE);
            Long lastBellTimeMillis = object.isNull(KEY_LAST_BELL_TIME_MILLIS)
                ? null : object.getLong(KEY_LAST_BELL_TIME_MILLIS);
            Long lastSeenTimeMillis = object.isNull(KEY_LAST_SEEN_TIME_MILLIS)
                ? null : object.getLong(KEY_LAST_SEEN_TIME_MILLIS);

            states.add(new SessionNewActivityState(handle, lastBellTimeMillis, lastSeenTimeMillis));
        }
        return states;
    }
}
