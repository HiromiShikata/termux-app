package com.termux.app.browser;

import android.content.Context;
import android.webkit.ValueCallback;
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

    private static final String PROJECT_BOARD_URL =
        "https://github.com/orgs/example/projects/12";

    private static final class RecordingWebView extends WebView {

        final List<String> injectedScripts = new ArrayList<>();

        RecordingWebView(Context context) {
            super(context);
        }

        @Override
        public void evaluateJavascript(String script, @Nullable ValueCallback<String> resultCallback) {
            injectedScripts.add(script);
        }

        boolean injectedMobileViewport() {
            return injectedScripts.contains(BrowserMobileViewport.INJECTION_SCRIPT);
        }

        boolean injectedDesktopViewport() {
            return injectedScripts.contains(BrowserDesktopViewport.INJECTION_SCRIPT);
        }
    }

    private static final class RecordingHost implements BrowserCoreWebViewClient.Host {

        BrowserViewMode viewMode = BrowserViewMode.MOBILE;
        boolean injectMobileViewport = true;
        boolean pageFinishedHandled;
        boolean renderProcessGoneHandled = true;
        final List<String> events = new ArrayList<>();
        final List<String> externalBrowserUrls = new ArrayList<>();

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

        @Override
        public boolean onRenderProcessGone(@NonNull WebView view, boolean didCrash) {
            events.add("renderProcessGone:" + didCrash);
            return renderProcessGoneHandled;
        }

        @Override
        public void openInExternalBrowser(@NonNull String url) {
            externalBrowserUrls.add(url);
        }
    }

    private WebView newWebView() {
        return new WebView(RuntimeEnvironment.getApplication());
    }

    private RecordingWebView newRecordingWebView() {
        return new RecordingWebView(RuntimeEnvironment.getApplication());
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

    @Test
    @SuppressWarnings("deprecation")
    public void googleSignInHostIsRoutedToExternalBrowserAndLoadIsOverridden() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        boolean overridden = client.shouldOverrideUrlLoading(newWebView(),
            "https://accounts.google.com/signin");
        Assert.assertTrue(overridden);
        Assert.assertTrue(host.externalBrowserUrls.contains("https://accounts.google.com/signin"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void googleSheetsHostIsRoutedToExternalBrowser() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        boolean overridden = client.shouldOverrideUrlLoading(newWebView(),
            "https://docs.google.com/spreadsheets/d/abc/edit");
        Assert.assertTrue(overridden);
        Assert.assertTrue(host.externalBrowserUrls.contains(
            "https://docs.google.com/spreadsheets/d/abc/edit"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void nonGoogleHostStaysInWebView() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        boolean overridden = client.shouldOverrideUrlLoading(newWebView(),
            "https://example.com/page");
        Assert.assertFalse(overridden);
        Assert.assertTrue(host.externalBrowserUrls.isEmpty());
    }

    @Test
    public void renderProcessGoneDelegatesToHostAndReturnsHostHandledTrue() {
        RecordingHost host = new RecordingHost();
        host.renderProcessGoneHandled = true;
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        boolean handled = client.onRenderProcessGone(newWebView(), null);
        Assert.assertTrue(handled);
        Assert.assertTrue(host.events.contains("renderProcessGone:false"));
    }

    @Test
    public void renderProcessGoneReturnsTrueWhenHostRecoversEvenWithoutDetail() {
        RecordingHost host = new RecordingHost();
        host.renderProcessGoneHandled = true;
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        Assert.assertTrue(client.onRenderProcessGone(newWebView(), null));
    }

    @Test
    public void projectBoardUsesDeviceWidthMobileViewportEvenWhenTabIsDesktop() {
        RecordingHost host = new RecordingHost();
        host.viewMode = BrowserViewMode.DESKTOP;
        host.injectMobileViewport = false;
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        RecordingWebView webView = newRecordingWebView();
        client.onPageStarted(webView, PROJECT_BOARD_URL, null);
        client.onPageCommitVisible(webView, PROJECT_BOARD_URL);
        client.onPageFinished(webView, PROJECT_BOARD_URL);
        Assert.assertTrue(webView.injectedMobileViewport());
        Assert.assertFalse(webView.injectedDesktopViewport());
    }

    @Test
    public void projectBoardUsesDeviceWidthMobileViewportInMobileTab() {
        RecordingHost host = new RecordingHost();
        host.viewMode = BrowserViewMode.MOBILE;
        host.injectMobileViewport = true;
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        RecordingWebView webView = newRecordingWebView();
        client.onPageFinished(webView, PROJECT_BOARD_URL);
        Assert.assertTrue(webView.injectedMobileViewport());
        Assert.assertFalse(webView.injectedDesktopViewport());
    }

    @Test
    public void nonBoardDesktopTabStillUsesDesktopViewport() {
        RecordingHost host = new RecordingHost();
        host.viewMode = BrowserViewMode.DESKTOP;
        host.injectMobileViewport = false;
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        RecordingWebView webView = newRecordingWebView();
        client.onPageFinished(webView, "https://example.com/console");
        Assert.assertTrue(webView.injectedDesktopViewport());
        Assert.assertFalse(webView.injectedMobileViewport());
    }

    @Test
    public void consoleMobileTabUsesDeviceWidthMobileViewport() {
        RecordingHost host = new RecordingHost();
        host.viewMode = BrowserViewMode.MOBILE;
        host.injectMobileViewport = true;
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        RecordingWebView webView = newRecordingWebView();
        client.onPageStarted(webView, "https://example.com/console", null);
        client.onPageCommitVisible(webView, "https://example.com/console");
        client.onPageFinished(webView, "https://example.com/console");
        Assert.assertTrue(webView.injectedMobileViewport());
        Assert.assertFalse(webView.injectedDesktopViewport());
    }
}
