package com.termux.app.terminal.io;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public final class SubmittedTextInputHistorySerializer {

    @NonNull
    public String serialize(@NonNull List<String> pinnedEntries) throws JSONException {
        JSONArray entriesArray = new JSONArray();
        for (String pinnedEntry : pinnedEntries) {
            if (pinnedEntry == null || pinnedEntry.isEmpty()) continue;
            entriesArray.put(pinnedEntry);
        }
        return entriesArray.toString();
    }

    @NonNull
    public List<String> deserialize(@Nullable String serialized) throws JSONException {
        List<String> pinnedEntries = new ArrayList<>();
        if (serialized == null || serialized.isEmpty()) return pinnedEntries;
        JSONArray entriesArray = new JSONArray(serialized);
        for (int index = 0; index < entriesArray.length(); index++) {
            String pinnedEntry = entriesArray.optString(index, "");
            if (pinnedEntry.isEmpty()) continue;
            if (pinnedEntries.contains(pinnedEntry)) continue;
            pinnedEntries.add(pinnedEntry);
        }
        return pinnedEntries;
    }
}
