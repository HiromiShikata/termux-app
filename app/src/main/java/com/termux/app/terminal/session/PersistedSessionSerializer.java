package com.termux.app.terminal.session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class PersistedSessionSerializer {

    private static final String KEY_NAME = "name";
    private static final String KEY_EXECUTABLE_PATH = "executablePath";
    private static final String KEY_ARGUMENTS = "arguments";
    private static final String KEY_IS_FAIL_SAFE = "isFailSafe";
    private static final String KEY_WORKING_DIRECTORY = "workingDirectory";

    public String serialize(List<PersistedSession> sessions) throws JSONException {
        JSONArray array = new JSONArray();
        for (PersistedSession session : sessions) {
            JSONObject object = new JSONObject();
            if (session.getName() != null)
                object.put(KEY_NAME, session.getName());
            if (session.getExecutablePath() != null)
                object.put(KEY_EXECUTABLE_PATH, session.getExecutablePath());
            if (session.getArguments() != null) {
                JSONArray arguments = new JSONArray();
                for (String argument : session.getArguments())
                    arguments.put(argument);
                object.put(KEY_ARGUMENTS, arguments);
            }
            object.put(KEY_IS_FAIL_SAFE, session.isFailSafe());
            if (session.getWorkingDirectory() != null)
                object.put(KEY_WORKING_DIRECTORY, session.getWorkingDirectory());
            array.put(object);
        }
        return array.toString();
    }

    public List<PersistedSession> deserialize(String serialized) throws JSONException {
        List<PersistedSession> sessions = new ArrayList<>();
        if (serialized == null || serialized.isEmpty())
            return sessions;

        JSONArray array = new JSONArray(serialized);
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);

            String name = object.has(KEY_NAME) ? object.getString(KEY_NAME) : null;
            String executablePath = object.has(KEY_EXECUTABLE_PATH) ? object.getString(KEY_EXECUTABLE_PATH) : null;

            String[] arguments = null;
            if (object.has(KEY_ARGUMENTS)) {
                JSONArray argumentArray = object.getJSONArray(KEY_ARGUMENTS);
                arguments = new String[argumentArray.length()];
                for (int j = 0; j < argumentArray.length(); j++)
                    arguments[j] = argumentArray.getString(j);
            }

            boolean isFailSafe = object.optBoolean(KEY_IS_FAIL_SAFE, false);
            String workingDirectory = object.has(KEY_WORKING_DIRECTORY) ? object.getString(KEY_WORKING_DIRECTORY) : null;

            sessions.add(new PersistedSession(name, executablePath, arguments, isFailSafe, workingDirectory));
        }
        return sessions;
    }
}
