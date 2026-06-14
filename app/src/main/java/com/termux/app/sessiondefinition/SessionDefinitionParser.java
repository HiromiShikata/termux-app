package com.termux.app.sessiondefinition;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SessionDefinitionParser {

    private static final String VERSION_TWO_INDEX_SUFFIX = ".v2.json";
    private static final String VERSION_THREE_INDEX_SUFFIX = ".v3.json";
    private static final String DEFAULT_PROJECT_FILE_SUFFIX = ".json";
    private static final int VERSION_THREE = 3;

    public List<SessionDefinitionGroupReference> parseIndex(String json) throws JSONException {
        return parseIndex(json, null);
    }

    public List<SessionDefinitionGroupReference> parseIndex(String json, @Nullable String indexUrl) throws JSONException {
        List<SessionDefinitionGroupReference> references = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        JSONArray projects = root.getJSONArray("projects");
        int version = root.optInt("version", 0);
        String projectFileSuffix = projectFileSuffix(indexUrl, version);
        for (int i = 0; i < projects.length(); i++) {
            String name = projects.getString(i);
            references.add(new SessionDefinitionGroupReference(name, name + projectFileSuffix));
        }
        return references;
    }

    public List<SessionDefinitionEntry> parseGroup(String groupLabel, String json) throws JSONException {
        Object root = new JSONTokener(json).nextValue();
        if (root instanceof JSONObject) {
            JSONObject projectObject = (JSONObject) root;
            String overviewUrl = nonEmptyOrNull(projectObject.optString("overviewUrl", null));
            JSONArray groupArray = projectObject.getJSONArray("groups");
            return parseEntryArray(groupLabel, groupArray, overviewUrl);
        }
        if (root instanceof JSONArray) {
            return parseEntryArray(groupLabel, (JSONArray) root, null);
        }
        throw new JSONException("Unexpected session definition group document");
    }

    private List<SessionDefinitionEntry> parseEntryArray(String groupLabel, JSONArray entryArray,
                                                         @Nullable String overviewUrl) throws JSONException {
        List<SessionDefinitionEntry> entries = new ArrayList<>();
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
            entries.add(new SessionDefinitionEntry(groupLabel, entryLabel, urls, titlesByUrl, overviewUrl));
        }
        return entries;
    }

    public String resolveUrl(String baseUrl, String reference) throws MalformedURLException {
        return new URL(new URL(baseUrl), reference).toString();
    }

    private static String projectFileSuffix(@Nullable String indexUrl, int version) {
        if ((indexUrl != null && indexUrl.endsWith(VERSION_THREE_INDEX_SUFFIX)) || version == VERSION_THREE) {
            return VERSION_THREE_INDEX_SUFFIX;
        }
        if (indexUrl != null && indexUrl.endsWith(VERSION_TWO_INDEX_SUFFIX)) {
            return VERSION_TWO_INDEX_SUFFIX;
        }
        return DEFAULT_PROJECT_FILE_SUFFIX;
    }

    @Nullable
    private static String nonEmptyOrNull(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value;
    }
}
