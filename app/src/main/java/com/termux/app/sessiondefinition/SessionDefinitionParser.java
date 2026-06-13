package com.termux.app.sessiondefinition;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public final class SessionDefinitionParser {

    public List<SessionDefinitionGroupReference> parseIndex(String json) throws JSONException {
        List<SessionDefinitionGroupReference> references = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        JSONArray projects = root.getJSONArray("projects");
        for (int i = 0; i < projects.length(); i++) {
            String name = projects.getString(i);
            references.add(new SessionDefinitionGroupReference(name, name + ".json"));
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
            for (int j = 0; j < urlArray.length(); j++) {
                urls.add(urlArray.getString(j));
            }
            entries.add(new SessionDefinitionEntry(groupLabel, entryLabel, urls));
        }
        return entries;
    }

    public String resolveUrl(String baseUrl, String reference) throws MalformedURLException {
        return new URL(new URL(baseUrl), reference).toString();
    }
}
