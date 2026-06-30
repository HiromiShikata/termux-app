package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BrowserSessionSplitRatiosSerializer {

    @NonNull
    public String serialize(@NonNull Map<String, Float> ratiosBySessionName) {
        JSONObject object = new JSONObject();
        for (Map.Entry<String, Float> entry : ratiosBySessionName.entrySet()) {
            String sessionName = entry.getKey();
            Float ratio = entry.getValue();
            if (sessionName == null || sessionName.isEmpty() || ratio == null) continue;
            try {
                object.put(sessionName, (double) ratio);
            } catch (JSONException e) {
                return new JSONObject().toString();
            }
        }
        return object.toString();
    }

    @NonNull
    public Map<String, Float> deserialize(@Nullable String serialized) {
        Map<String, Float> ratiosBySessionName = new LinkedHashMap<>();
        if (serialized == null || serialized.isEmpty()) return ratiosBySessionName;

        try {
            JSONObject object = new JSONObject(serialized);
            Iterator<String> sessionNames = object.keys();
            while (sessionNames.hasNext()) {
                String sessionName = sessionNames.next();
                if (sessionName == null || sessionName.isEmpty()) continue;
                ratiosBySessionName.put(sessionName, (float) object.getDouble(sessionName));
            }
        } catch (JSONException e) {
            return new LinkedHashMap<>();
        }
        return ratiosBySessionName;
    }
}
