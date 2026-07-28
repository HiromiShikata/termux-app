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
    private static final String KEY_UNACKNOWLEDGED_CALL_REASONS = "unacknowledgedCallReasons";
    private static final String KEY_CALL_TRIGGER_VALUES = "callTriggerValues";
    private static final String KEY_LEGACY_ACKNOWLEDGED_CALL_REASONS = "acknowledgedCallReasons";
    private static final String KEY_LAST_SEEN_TIME_MILLIS = "lastSeenTimeMillis";
    private static final String KEY_LAST_USER_INPUT_TIME_MILLIS = "lastUserInputTimeMillis";
    private static final String KEY_STATUSLINE_CALL_TIME_MILLIS = "statuslineCallTimeMillis";
    private static final String KEY_STATUSLINE_OUT_TIME_MILLIS = "statuslineOutTimeMillis";
    private static final String KEY_STATUSLINE_REPLY_TIME_MILLIS = "statuslineReplyTimeMillis";
    private static final String KEY_SUBAGENT_COUNT = "subagentCount";

    public String serialize(List<SessionNewActivityState> states) throws JSONException {
        JSONArray array = new JSONArray();
        for (SessionNewActivityState state : SessionNewActivityStateCaps.capStates(states)) {
            JSONObject object = new JSONObject();
            object.put(KEY_SESSION_NAME, state.getSessionName());
            if (state.getLastOutputActivityTimeMillis() != null)
                object.put(KEY_LAST_OUTPUT_ACTIVITY_TIME_MILLIS, state.getLastOutputActivityTimeMillis().longValue());
            if (state.getLastExplicitCallTimeMillis() != null)
                object.put(KEY_LAST_EXPLICIT_CALL_TIME_MILLIS, state.getLastExplicitCallTimeMillis().longValue());
            if (state.getLastExplicitCallReason() != null)
                object.put(KEY_LAST_EXPLICIT_CALL_REASON, state.getLastExplicitCallReason());
            if (state.getUnacknowledgedCallReasons() != null)
                object.put(KEY_UNACKNOWLEDGED_CALL_REASONS,
                    new JSONArray(state.getUnacknowledgedCallReasons()));
            if (state.getCallTriggerValues() != null)
                object.put(KEY_CALL_TRIGGER_VALUES,
                    new JSONArray(state.getCallTriggerValues()));
            if (state.getLastSeenTimeMillis() != null)
                object.put(KEY_LAST_SEEN_TIME_MILLIS, state.getLastSeenTimeMillis().longValue());
            if (state.getLastUserInputTimeMillis() != null)
                object.put(KEY_LAST_USER_INPUT_TIME_MILLIS, state.getLastUserInputTimeMillis().longValue());
            if (state.getStatuslineCallTimeMillis() != null)
                object.put(KEY_STATUSLINE_CALL_TIME_MILLIS, state.getStatuslineCallTimeMillis().longValue());
            if (state.getStatuslineOutTimeMillis() != null)
                object.put(KEY_STATUSLINE_OUT_TIME_MILLIS, state.getStatuslineOutTimeMillis().longValue());
            if (state.getStatuslineReplyTimeMillis() != null)
                object.put(KEY_STATUSLINE_REPLY_TIME_MILLIS, state.getStatuslineReplyTimeMillis().longValue());
            if (state.getSubagentCount() != null)
                object.put(KEY_SUBAGENT_COUNT, state.getSubagentCount().intValue());
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
            List<String> unacknowledgedCallReasons =
                optionalStringList(object, KEY_UNACKNOWLEDGED_CALL_REASONS);
            List<String> callTriggerValues = object.isNull(KEY_CALL_TRIGGER_VALUES)
                ? legacyCallTriggerValues(object, unacknowledgedCallReasons)
                : optionalStringList(object, KEY_CALL_TRIGGER_VALUES);
            Long lastSeenTimeMillis = optionalLong(object, KEY_LAST_SEEN_TIME_MILLIS);
            Long lastUserInputTimeMillis = optionalLong(object, KEY_LAST_USER_INPUT_TIME_MILLIS);
            Long statuslineCallTimeMillis = optionalLong(object, KEY_STATUSLINE_CALL_TIME_MILLIS);
            Long statuslineOutTimeMillis = optionalLong(object, KEY_STATUSLINE_OUT_TIME_MILLIS);
            Long statuslineReplyTimeMillis = optionalLong(object, KEY_STATUSLINE_REPLY_TIME_MILLIS);
            Integer subagentCount = optionalInt(object, KEY_SUBAGENT_COUNT);

            states.add(SessionNewActivityStateCaps.capState(new SessionNewActivityState(sessionName,
                lastOutputActivityTimeMillis, lastExplicitCallTimeMillis, lastExplicitCallReason,
                lastSeenTimeMillis, lastUserInputTimeMillis, unacknowledgedCallReasons,
                callTriggerValues, statuslineCallTimeMillis, statuslineOutTimeMillis,
                statuslineReplyTimeMillis, subagentCount)));
        }
        return states;
    }

    /**
     * The call trigger values recovered from an entry a previous app version wrote, which carries no
     * {@link #KEY_CALL_TRIGGER_VALUES} entry. That version keyed call deduplication on the reason
     * text itself and treated a call as already known when the reason appeared in either its
     * acknowledged or its unacknowledged reason list, so both lists together are exactly the legacy
     * key set and both must be seeded. Seeding only the acknowledged list would leave a call that is
     * still pending unknown, and the first transcript scan after the update — which re-detects every
     * owner-call tag still in the scrollback — would append that call's reason a second time; seeding
     * neither would additionally re-arm the red pending state for every already-answered call. The
     * acknowledged reasons come first because a reason is acknowledged only after it stopped being
     * pending, so that order is oldest-first and matches the trailing size cap
     * {@link SessionNewActivityStateCaps#capReasons} applies to the returned list through
     * {@link SessionNewActivityStateCaps#capState}. An entry carrying neither legacy list recorded no
     * call at all and yields null, the unchanged result for an entry that has none of the three keys.
     */
    private static List<String> legacyCallTriggerValues(JSONObject object,
                                                        List<String> unacknowledgedCallReasons)
            throws JSONException {
        List<String> acknowledgedCallReasons =
            optionalStringList(object, KEY_LEGACY_ACKNOWLEDGED_CALL_REASONS);
        if (acknowledgedCallReasons == null && unacknowledgedCallReasons == null) {
            return null;
        }
        List<String> seeded = new ArrayList<>();
        appendMissing(seeded, acknowledgedCallReasons);
        appendMissing(seeded, unacknowledgedCallReasons);
        return seeded;
    }

    private static void appendMissing(List<String> target, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (!target.contains(value)) {
                target.add(value);
            }
        }
    }

    private static Long optionalLong(JSONObject object, String key) throws JSONException {
        return object.isNull(key) ? null : object.getLong(key);
    }

    private static Integer optionalInt(JSONObject object, String key) throws JSONException {
        return object.isNull(key) ? null : object.getInt(key);
    }

    private static String optionalString(JSONObject object, String key) throws JSONException {
        return object.isNull(key) ? null : object.getString(key);
    }

    private static List<String> optionalStringList(JSONObject object, String key) throws JSONException {
        if (object.isNull(key)) {
            return null;
        }
        JSONArray array = object.getJSONArray(key);
        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            values.add(array.getString(i));
        }
        return values;
    }
}
