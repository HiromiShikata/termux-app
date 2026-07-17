package com.termux.app.terminal.io;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public final class PinnedTextInputHistorySerializer {

    @NonNull
    public String serialize(@NonNull List<String> pinnedEntries) {
        JSONArray array = new JSONArray();
        for (String pinnedEntry : pinnedEntries) {
            if (pinnedEntry != null && !pinnedEntry.isEmpty()) array.put(pinnedEntry);
        }
        return array.toString();
    }

    @NonNull
    public List<String> deserialize(@Nullable String serialized) {
        List<String> pinnedEntries = new ArrayList<>();
        if (serialized == null || serialized.isEmpty()) return pinnedEntries;

        try {
            JSONArray array = new JSONArray(serialized);
            for (int index = 0; index < array.length(); index++) {
                String pinnedEntry = array.isNull(index) ? null : array.getString(index);
                if (pinnedEntry != null && !pinnedEntry.isEmpty() && !pinnedEntries.contains(pinnedEntry)) {
                    pinnedEntries.add(pinnedEntry);
                }
            }
        } catch (JSONException exception) {
            return new ArrayList<>();
        }
        return pinnedEntries;
    }
}
