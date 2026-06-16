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
    private static final String VERSION_FOUR_INDEX_SUFFIX = ".v4.json";
    private static final String DEFAULT_PROJECT_FILE_SUFFIX = ".json";
    private static final int VERSION_THREE = 3;
    private static final int VERSION_FOUR = 4;

    public List<SessionDefinitionGroupReference> parseIndex(String json) throws JSONException {
        return parseIndex(json, null);
    }

    public List<SessionDefinitionGroupReference> parseIndex(String json, @Nullable String indexUrl) throws JSONException {
        List<SessionDefinitionGroupReference> references = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        JSONArray projects = root.getJSONArray("projects");
        int version = root.optInt("version", 0);
        if (isVersionFourIndex(indexUrl, version)) {
            for (int i = 0; i < projects.length(); i++) {
                JSONObject project = projects.getJSONObject(i);
                String name = project.getString("name");
                String path = project.getString("path");
                references.add(new SessionDefinitionGroupReference(name, path));
            }
            return references;
        }
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
            String tdpmConsoleUrl = nonEmptyOrNull(projectObject.optString("tdpmConsoleUrl", null));
            String newIssueUrl = nonEmptyOrNull(projectObject.optString("newIssueUrl", null));
            JSONArray groupArray = projectObject.getJSONArray("groups");
            return parseEntryArray(groupLabel, groupArray, overviewUrl, tdpmConsoleUrl, newIssueUrl);
        }
        if (root instanceof JSONArray) {
            return parseEntryArray(groupLabel, (JSONArray) root, null, null, null);
        }
        throw new JSONException("Unexpected session definition group document");
    }

    private List<SessionDefinitionEntry> parseEntryArray(String groupLabel, JSONArray entryArray,
                                                         @Nullable String overviewUrl,
                                                         @Nullable String tdpmConsoleUrl,
                                                         @Nullable String newIssueUrl) throws JSONException {
        List<SessionDefinitionEntry> entries = new ArrayList<>();
        for (int i = 0; i < entryArray.length(); i++) {
            JSONObject entry = entryArray.getJSONObject(i);
            String entryLabel = entry.getString("story");
            JSONArray sessionArray = entry.optJSONArray("sessions");
            if (sessionArray == null) {
                sessionArray = entry.optJSONArray("urls");
            }
            List<String> urls = new ArrayList<>();
            Map<String, String> titlesByUrl = new LinkedHashMap<>();
            int sessionCount = sessionArray == null ? 0 : sessionArray.length();
            for (int j = 0; j < sessionCount; j++) {
                Object sessionItem = sessionArray.get(j);
                if (sessionItem instanceof JSONObject) {
                    JSONObject sessionObject = (JSONObject) sessionItem;
                    String url = sessionObject.optString("name", null);
                    if (url == null) {
                        url = sessionObject.getString("url");
                    }
                    urls.add(url);
                    String description = sessionObject.optString("description", null);
                    if (description == null || description.isEmpty()) {
                        description = sessionObject.optString("title", null);
                    }
                    if (description != null && !description.isEmpty()) {
                        titlesByUrl.put(url, description);
                    }
                } else {
                    urls.add(sessionArray.getString(j));
                }
            }
            entries.add(new SessionDefinitionEntry(groupLabel, entryLabel, urls, titlesByUrl, overviewUrl,
                tdpmConsoleUrl, newIssueUrl));
        }
        return entries;
    }

    public String resolveUrl(String baseUrl, String reference) throws MalformedURLException {
        return new URL(new URL(baseUrl), reference).toString();
    }

    private static boolean isVersionFourIndex(@Nullable String indexUrl, int version) {
        return version == VERSION_FOUR || (indexUrl != null && hasVersionFourSuffix(indexUrl));
    }

    private static boolean hasVersionFourSuffix(String indexUrl) {
        int queryStart = indexUrl.indexOf('?');
        String path = queryStart >= 0 ? indexUrl.substring(0, queryStart) : indexUrl;
        return path.endsWith(VERSION_FOUR_INDEX_SUFFIX);
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
