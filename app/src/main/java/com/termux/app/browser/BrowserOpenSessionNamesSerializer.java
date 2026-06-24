package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedHashSet;
import java.util.Set;

public final class BrowserOpenSessionNamesSerializer {

    @NonNull
    public String serialize(@NonNull Set<String> sessionNames) {
        JSONArray array = new JSONArray();
        for (String sessionName : sessionNames) {
            if (sessionName != null) array.put(sessionName);
        }
        return array.toString();
    }

    @NonNull
    public Set<String> deserialize(@Nullable String serialized) {
        Set<String> sessionNames = new LinkedHashSet<>();
        if (serialized == null || serialized.isEmpty()) return sessionNames;

        try {
            JSONArray array = new JSONArray(serialized);
            for (int i = 0; i < array.length(); i++) {
                String sessionName = array.isNull(i) ? null : array.getString(i);
                if (sessionName != null && !sessionName.isEmpty()) sessionNames.add(sessionName);
            }
        } catch (JSONException e) {
            return new LinkedHashSet<>();
        }
        return sessionNames;
    }
}
