package com.termux.app.sessiondefinition;

import java.io.IOException;

public interface SessionDefinitionDocumentFetcher {

    String fetch(String url) throws IOException;
}
