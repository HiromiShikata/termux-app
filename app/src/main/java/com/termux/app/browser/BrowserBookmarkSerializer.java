package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class BrowserBookmarkSerializer {

    private static final String KEY_URL = "url";
    private static final String KEY_TITLE = "title";

    @NonNull
    public String serialize(@NonNull List<BrowserBookmark> bookmarks) throws JSONException {
        JSONArray array = new JSONArray();
        for (BrowserBookmark bookmark : bookmarks) {
            JSONObject object = new JSONObject();
            object.put(KEY_URL, bookmark.getUrl());
            object.put(KEY_TITLE, bookmark.getTitle());
            array.put(object);
        }
        return array.toString();
    }

    @NonNull
    public List<BrowserBookmark> deserialize(@Nullable String serialized) throws JSONException {
        List<BrowserBookmark> bookmarks = new ArrayList<>();
        if (serialized == null || serialized.isEmpty()) return bookmarks;

        JSONArray array = new JSONArray(serialized);
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            String url = object.getString(KEY_URL);
            String title = object.getString(KEY_TITLE);
            bookmarks.add(new BrowserBookmark(url, title));
        }
        return bookmarks;
    }
}
