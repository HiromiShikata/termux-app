package com.termux.app.terminal.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.annotation.NonNull;

import org.junit.Test;

public class SessionDefinitionPrewarmTest {

    private static final class FixedLoadState implements SessionDefinitionPrewarm.DocumentLoadState {
        boolean loaded;
        boolean loading;

        @Override
        public boolean isDocumentLoaded() {
            return loaded;
        }

        @Override
        public boolean isDocumentLoading() {
            return loading;
        }
    }

    private static final class FixedBaseUrl implements SessionDefinitionPrewarm.BaseUrlSupplier {
        private final String baseUrl;

        FixedBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        @NonNull
        @Override
        public String getSessionDefinitionBaseUrl() {
            return baseUrl;
        }
    }

    private static final class RecordingPrewarmAction implements SessionDefinitionPrewarm.DocumentPrewarmAction {
        String prewarmedBaseUrl;
        int prewarmCount;

        @Override
        public void prewarmDocument(@NonNull String baseUrl) {
            prewarmedBaseUrl = baseUrl;
            prewarmCount++;
        }
    }

    @Test
    public void prewarmsTheDocumentWhenNotYetLoadedOrLoading() {
        RecordingPrewarmAction prewarmAction = new RecordingPrewarmAction();
        SessionDefinitionPrewarm prewarm = new SessionDefinitionPrewarm(
            new FixedLoadState(), new FixedBaseUrl("https://example.test/base/index.json"), prewarmAction);

        prewarm.prewarmSessionDefinitionDocument();

        assertEquals(1, prewarmAction.prewarmCount);
        assertEquals("https://example.test/base/index.json", prewarmAction.prewarmedBaseUrl);
    }

    @Test
    public void doesNotPrewarmWhenDocumentAlreadyLoaded() {
        FixedLoadState loadState = new FixedLoadState();
        loadState.loaded = true;
        RecordingPrewarmAction prewarmAction = new RecordingPrewarmAction();
        SessionDefinitionPrewarm prewarm = new SessionDefinitionPrewarm(
            loadState, new FixedBaseUrl("https://example.test/base/index.json"), prewarmAction);

        prewarm.prewarmSessionDefinitionDocument();

        assertEquals(0, prewarmAction.prewarmCount);
    }

    @Test
    public void doesNotPrewarmWhenDocumentIsAlreadyLoading() {
        FixedLoadState loadState = new FixedLoadState();
        loadState.loading = true;
        RecordingPrewarmAction prewarmAction = new RecordingPrewarmAction();
        SessionDefinitionPrewarm prewarm = new SessionDefinitionPrewarm(
            loadState, new FixedBaseUrl("https://example.test/base/index.json"), prewarmAction);

        prewarm.prewarmSessionDefinitionDocument();

        assertEquals(0, prewarmAction.prewarmCount);
    }

    @Test
    public void doesNotPrewarmWhenBaseUrlIsEmpty() {
        RecordingPrewarmAction prewarmAction = new RecordingPrewarmAction();
        SessionDefinitionPrewarm prewarm = new SessionDefinitionPrewarm(
            new FixedLoadState(), new FixedBaseUrl("   "), prewarmAction);

        prewarm.prewarmSessionDefinitionDocument();

        assertEquals(0, prewarmAction.prewarmCount);
        assertNull(prewarmAction.prewarmedBaseUrl);
    }

    private static final class LoadingStartingPrewarmAction
            implements SessionDefinitionPrewarm.DocumentPrewarmAction {
        private final FixedLoadState loadState;
        int prewarmCount;

        LoadingStartingPrewarmAction(FixedLoadState loadState) {
            this.loadState = loadState;
        }

        @Override
        public void prewarmDocument(@NonNull String baseUrl) {
            prewarmCount++;
            loadState.loading = true;
        }
    }

    @Test
    public void prewarmsExactlyOnceAcrossRepeatedStartupCallsWhenLoadingStartsImmediately() {
        FixedLoadState loadState = new FixedLoadState();
        LoadingStartingPrewarmAction prewarmAction = new LoadingStartingPrewarmAction(loadState);
        SessionDefinitionPrewarm prewarm = new SessionDefinitionPrewarm(
            loadState, new FixedBaseUrl("https://example.test/base/index.json"), prewarmAction);

        prewarm.prewarmSessionDefinitionDocument();
        prewarm.prewarmSessionDefinitionDocument();
        prewarm.prewarmSessionDefinitionDocument();

        assertEquals(1, prewarmAction.prewarmCount);
    }
}
