package com.termux.app.sessiondefinition;

import com.termux.shared.logger.Logger;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class SessionDefinitionLoader {

    private static final String LOG_TAG = "SessionDefinitionLoader";

    private final SessionDefinitionDocumentFetcher fetcher;
    private final SessionDefinitionParser parser;

    public SessionDefinitionLoader(SessionDefinitionDocumentFetcher fetcher, SessionDefinitionParser parser) {
        this.fetcher = fetcher;
        this.parser = parser;
    }

    public SessionDefinitionLoadResult load(String baseUrl) throws IOException, JSONException {
        String indexJson = fetcher.fetch(baseUrl);
        List<SessionDefinitionGroupReference> groupReferences = parser.parseIndex(indexJson, baseUrl);

        List<SessionDefinitionEntry> entries = new ArrayList<>();
        List<String> failedGroupLabels = new ArrayList<>();
        for (SessionDefinitionGroupReference groupReference : groupReferences) {
            try {
                String groupUrl = parser.resolveUrl(baseUrl, groupReference.getUrl());
                String groupJson = fetcher.fetch(groupUrl);
                entries.addAll(parser.parseGroup(groupReference.getLabel(), groupJson));
            } catch (IOException | JSONException groupException) {
                failedGroupLabels.add(groupReference.getLabel());
                Logger.logStackTraceWithMessage(LOG_TAG,
                    "Skipping session definition group that failed to load: " + groupReference.getLabel(),
                    groupException);
            }
        }
        return new SessionDefinitionLoadResult(entries, groupReferences.size(), failedGroupLabels);
    }
}
