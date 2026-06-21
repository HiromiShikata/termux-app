package com.termux.app.terminal.session;

import androidx.annotation.NonNull;

public final class SessionDefinitionPrewarm {

    public interface DocumentLoadState {
        boolean isDocumentLoaded();

        boolean isDocumentLoading();
    }

    public interface BaseUrlSupplier {
        @NonNull
        String getSessionDefinitionBaseUrl();
    }

    public interface DocumentPrewarmAction {
        void prewarmDocument(@NonNull String baseUrl);
    }

    private final DocumentLoadState documentLoadState;

    private final BaseUrlSupplier baseUrlSupplier;

    private final DocumentPrewarmAction documentPrewarmAction;

    public SessionDefinitionPrewarm(
        @NonNull DocumentLoadState documentLoadState,
        @NonNull BaseUrlSupplier baseUrlSupplier,
        @NonNull DocumentPrewarmAction documentPrewarmAction) {
        this.documentLoadState = documentLoadState;
        this.baseUrlSupplier = baseUrlSupplier;
        this.documentPrewarmAction = documentPrewarmAction;
    }

    public void prewarmSessionDefinitionDocument() {
        if (documentLoadState.isDocumentLoaded()) return;
        if (documentLoadState.isDocumentLoading()) return;

        String baseUrl = baseUrlSupplier.getSessionDefinitionBaseUrl().trim();
        if (baseUrl.isEmpty()) return;

        documentPrewarmAction.prewarmDocument(baseUrl);
    }
}
