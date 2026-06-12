package com.termux.app.sessiondefinition;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class SessionDefinitionLoader {

    private final SessionDefinitionDocumentFetcher fetcher;
    private final SessionDefinitionParser parser;

    public SessionDefinitionLoader(SessionDefinitionDocumentFetcher fetcher, SessionDefinitionParser parser) {
        this.fetcher = fetcher;
        this.parser = parser;
    }

    public List<SessionDefinitionEntry> load(String baseUrl) throws IOException, JSONException {
        String indexJson = fetcher.fetch(baseUrl);
        List<SessionDefinitionGroupReference> groupReferences = parser.parseIndex(indexJson);

        List<SessionDefinitionEntry> entries = new ArrayList<>();
        for (SessionDefinitionGroupReference groupReference : groupReferences) {
            String groupUrl = parser.resolveUrl(baseUrl, groupReference.getUrl());
            String groupJson = fetcher.fetch(groupUrl);
            entries.addAll(parser.parseGroup(groupReference.getLabel(), groupJson));
        }
        return entries;
    }
}
