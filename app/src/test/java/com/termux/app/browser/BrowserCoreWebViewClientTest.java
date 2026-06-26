package com.termux.app.browser;

import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BrowserCoreWebViewClientTest {

    private static final class RecordingHost implements BrowserCoreWebViewClient.Host {

        BrowserViewMode viewMode = BrowserViewMode.MOBILE;
        boolean injectMobileViewport = true;
        boolean pageFinishedHandled;
        final List<String> events = new ArrayList<>();

        @NonNull
        @Override
        public BrowserViewMode getViewMode() {
            return viewMode;
        }

        @Override
        public boolean shouldInjectMobileViewport() {
            return injectMobileViewport;
        }

        @Override
        public void onPageStarted(@NonNull WebView view, @Nullable String url) {
            events.add("started:" + url);
        }

        @Override
        public void onPageCommitVisible(@NonNull WebView view, @Nullable String url) {
            events.add("commit:" + url);
        }

        @Override
        public boolean onPageFinished(@NonNull WebView view, @Nullable String url) {
            events.add("finished:" + url);
            return pageFinishedHandled;
        }

        @Override
        public void onVisitedHistoryUpdated(@NonNull WebView view, @Nullable String url, boolean isReload) {
            events.add("history:" + url + ":" + isReload);
        }

        @Override
        public void onMainFrameError(@NonNull WebView view) {
            events.add("error");
        }
    }

    private WebView newWebView() {
        return new WebView(RuntimeEnvironment.getApplication());
    }

    @Test
    public void inheritsHttpAuthHandling() {
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(new RecordingHost());
        Assert.assertTrue(client instanceof BrowserHttpAuthWebViewClient);
    }

    @Test
    public void pageStartedDelegatesToHost() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        client.onPageStarted(newWebView(), "https://example.com/", null);
        Assert.assertTrue(host.events.contains("started:https://example.com/"));
    }

    @Test
    public void pageCommitVisibleDelegatesToHost() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        client.onPageCommitVisible(newWebView(), "https://example.com/");
        Assert.assertTrue(host.events.contains("commit:https://example.com/"));
    }

    @Test
    public void pageFinishedDelegatesToHost() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        client.onPageFinished(newWebView(), "https://example.com/");
        Assert.assertTrue(host.events.contains("finished:https://example.com/"));
    }

    @Test
    public void visitedHistoryUpdateDelegatesToHost() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        client.doUpdateVisitedHistory(newWebView(), "https://example.com/page", true);
        Assert.assertTrue(host.events.contains("history:https://example.com/page:true"));
    }

    @Test
    public void mainFrameErrorIsForwardedForDeprecatedCallback() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        client.onReceivedError(newWebView(), -2, "net error", "https://example.com/");
        Assert.assertTrue(host.events.contains("error"));
    }
}
