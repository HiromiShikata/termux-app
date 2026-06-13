package com.termux.app.sessiondefinition;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SessionDefinitionParser {

    private static final String VERSION_TWO_INDEX_SUFFIX = ".v2.json";
    private static final String DEFAULT_PROJECT_FILE_SUFFIX = ".json";

    public List<SessionDefinitionGroupReference> parseIndex(String json) throws JSONException {
        return parseIndex(json, null);
    }

    public List<SessionDefinitionGroupReference> parseIndex(String json, @Nullable String indexUrl) throws JSONException {
        List<SessionDefinitionGroupReference> references = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        JSONArray projects = root.getJSONArray("projects");
        String projectFileSuffix = projectFileSuffix(indexUrl);
        for (int i = 0; i < projects.length(); i++) {
            String name = projects.getString(i);
            references.add(new SessionDefinitionGroupReference(name, name + projectFileSuffix));
        }
        return references;
    }

    public List<SessionDefinitionEntry> parseGroup(String groupLabel, String json) throws JSONException {
        List<SessionDefinitionEntry> entries = new ArrayList<>();
        JSONArray entryArray = new JSONArray(json);
        for (int i = 0; i < entryArray.length(); i++) {
            JSONObject entry = entryArray.getJSONObject(i);
            String entryLabel = entry.getString("story");
            JSONArray urlArray = entry.getJSONArray("urls");
            List<String> urls = new ArrayList<>();
            Map<String, String> titlesByUrl = new LinkedHashMap<>();
            for (int j = 0; j < urlArray.length(); j++) {
                Object urlItem = urlArray.get(j);
                if (urlItem instanceof JSONObject) {
                    JSONObject urlObject = (JSONObject) urlItem;
                    String url = urlObject.getString("url");
                    urls.add(url);
                    String title = urlObject.optString("title", null);
                    if (title != null && !title.isEmpty()) {
                        titlesByUrl.put(url, title);
                    }
                } else {
                    urls.add(urlArray.getString(j));
                }
            }
            entries.add(new SessionDefinitionEntry(groupLabel, entryLabel, urls, titlesByUrl));
        }
        return entries;
    }

    public String resolveUrl(String baseUrl, String reference) throws MalformedURLException {
        return new URL(new URL(baseUrl), reference).toString();
    }

    private static String projectFileSuffix(@Nullable String indexUrl) {
        if (indexUrl != null && indexUrl.endsWith(VERSION_TWO_INDEX_SUFFIX)) {
            return VERSION_TWO_INDEX_SUFFIX;
        }
        return DEFAULT_PROJECT_FILE_SUFFIX;
    }
}
