package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class BrowserTabHistorySerializer {

    private static final String KEY_URL = "url";
    private static final String KEY_TITLE = "title";
    private static final String KEY_BODY_SNIPPET = "bodySnippet";
    private static final String KEY_CLOSED_AT_MILLIS = "closedAtMillis";

    @NonNull
    public String serialize(@NonNull BrowserTabHistory history) throws JSONException {
        JSONArray entriesArray = new JSONArray();
        for (BrowserTabHistoryEntry entry : history.getEntries()) {
            JSONObject entryObject = new JSONObject();
            entryObject.put(KEY_URL, entry.getUrl());
            entryObject.put(KEY_TITLE, entry.getTitle());
            if (!entry.getBodySnippet().isEmpty()) {
                entryObject.put(KEY_BODY_SNIPPET, entry.getBodySnippet());
            }
            if (entry.getClosedAtMillis() != null) {
                entryObject.put(KEY_CLOSED_AT_MILLIS, entry.getClosedAtMillis().longValue());
            }
            entriesArray.put(entryObject);
        }
        return entriesArray.toString();
    }

    @NonNull
    public BrowserTabHistory deserialize(@Nullable String serialized, int maxEntries) throws JSONException {
        List<BrowserTabHistoryEntry> entries = new ArrayList<>();
        if (serialized == null || serialized.isEmpty()) return new BrowserTabHistory(entries, maxEntries);

        JSONArray entriesArray = new JSONArray(serialized);
        for (int index = 0; index < entriesArray.length(); index++) {
            JSONObject entryObject = entriesArray.getJSONObject(index);
            String url = entryObject.getString(KEY_URL);
            if (url.isEmpty()) continue;
            String title = entryObject.optString(KEY_TITLE, url);
            String bodySnippet = entryObject.optString(KEY_BODY_SNIPPET, "");
            Long closedAtMillis = entryObject.has(KEY_CLOSED_AT_MILLIS)
                ? entryObject.getLong(KEY_CLOSED_AT_MILLIS)
                : null;
            entries.add(BrowserTabHistoryEntry.fromPersisted(url, title, bodySnippet, closedAtMillis));
        }
        return new BrowserTabHistory(entries, maxEntries);
    }
}
