package com.termux.app.browser;

import android.content.Context;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        final List<String> nativeAppUrls = new ArrayList<>();
        boolean matchingNativeAppInstalled;

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

        @Override
        public boolean openInMatchingNativeApp(@NonNull String url) {
            nativeAppUrls.add(url);
            return matchingNativeAppInstalled;
        }
    }

    private static final class ThrowingHost implements BrowserCoreWebViewClient.Host {

        @NonNull
        @Override
        public BrowserViewMode getViewMode() {
            throw new IllegalStateException("boom");
        }

        @Override
        public boolean shouldInjectMobileViewport() {
            throw new IllegalStateException("boom");
        }

        @Override
        public void onPageStarted(@NonNull WebView view, @Nullable String url) {
            throw new IllegalStateException("boom");
        }

        @Override
        public void onPageCommitVisible(@NonNull WebView view, @Nullable String url) {
            throw new IllegalStateException("boom");
        }

        @Override
        public boolean onPageFinished(@NonNull WebView view, @Nullable String url) {
            throw new IllegalStateException("boom");
        }

        @Override
        public void onVisitedHistoryUpdated(@NonNull WebView view, @Nullable String url, boolean isReload) {
            throw new IllegalStateException("boom");
        }

        @Override
        public void onMainFrameError(@NonNull WebView view) {
            throw new IllegalStateException("boom");
        }

        @Override
        public boolean onRenderProcessGone(@NonNull WebView view, boolean didCrash) {
            throw new IllegalStateException("boom");
        }

        @Override
        public void openInExternalBrowser(@NonNull String url) {
            throw new IllegalStateException("boom");
        }

        @Override
        public boolean openInMatchingNativeApp(@NonNull String url) {
            throw new IllegalStateException("boom");
        }
    }

    private static final class FakeWebResourceRequest implements WebResourceRequest {

        private final Uri mUrl;
        private final boolean mHasGesture;
        private final boolean mIsForMainFrame;

        FakeWebResourceRequest(String url, boolean hasGesture, boolean isForMainFrame) {
            this.mUrl = Uri.parse(url);
            this.mHasGesture = hasGesture;
            this.mIsForMainFrame = isForMainFrame;
        }

        @Override
        public Uri getUrl() {
            return mUrl;
        }

        @Override
        public boolean isForMainFrame() {
            return mIsForMainFrame;
        }

        @Override
        public boolean isRedirect() {
            return false;
        }

        @Override
        public boolean hasGesture() {
            return mHasGesture;
        }

        @Override
        public String getMethod() {
            return "GET";
        }

        @Override
        public Map<String, String> getRequestHeaders() {
            return Collections.emptyMap();
        }
    }

    private static FakeWebResourceRequest tappedLink(String url) {
        return new FakeWebResourceRequest(url, true, true);
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
    public void googleSignInHostStaysInWebView() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        boolean overridden = client.shouldOverrideUrlLoading(newWebView(),
            "https://accounts.google.com/signin");
        Assert.assertFalse(overridden);
        Assert.assertTrue(host.externalBrowserUrls.isEmpty());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void googleSheetsContentStaysInWebView() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        boolean overridden = client.shouldOverrideUrlLoading(newWebView(),
            "https://docs.google.com/spreadsheets/d/abc/edit");
        Assert.assertFalse(overridden);
        Assert.assertTrue(host.externalBrowserUrls.isEmpty());
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
    public void aDriveLinkTheUserTappedOpensInTheMatchingNativeApp() {
        RecordingHost host = new RecordingHost();
        host.matchingNativeAppInstalled = true;
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        boolean overridden = client.shouldOverrideUrlLoading(newWebView(),
            tappedLink("https://drive.google.com/file/d/abc/view?usp=drivesdk"));
        Assert.assertTrue("a tapped Drive link must leave the in-app browser", overridden);
        Assert.assertTrue(host.nativeAppUrls.contains(
            "https://drive.google.com/file/d/abc/view?usp=drivesdk"));
    }

    @Test
    public void aTappedLinkStaysInTheBrowserWhenNoNativeAppTakesIt() {
        RecordingHost host = new RecordingHost();
        host.matchingNativeAppInstalled = false;
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        boolean overridden = client.shouldOverrideUrlLoading(newWebView(),
            tappedLink("https://drive.google.com/file/d/abc/view"));
        Assert.assertFalse("without an application to take it the link must load in the browser",
            overridden);
    }

    @Test
    public void aMainFrameNavigationReportingNoGestureStillReachesTheMatchingNativeApp() {
        RecordingHost host = new RecordingHost();
        host.matchingNativeAppInstalled = true;
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        boolean overridden = client.shouldOverrideUrlLoading(newWebView(),
            new FakeWebResourceRequest("https://drive.google.com/file/d/abc/view", false, true));
        Assert.assertTrue("Google web properties navigate by script after a tap and WebView reports no"
                + " gesture for that, so gating on one kept exactly the links the owner wants in his"
                + " applications inside the in-app browser, where he is not signed in",
            overridden);
        Assert.assertEquals(Collections.singletonList("https://drive.google.com/file/d/abc/view"),
            host.nativeAppUrls);
    }

    @Test
    public void aSubframeNavigationStaysInTheBrowser() {
        RecordingHost host = new RecordingHost();
        host.matchingNativeAppInstalled = true;
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        boolean overridden = client.shouldOverrideUrlLoading(newWebView(),
            new FakeWebResourceRequest("https://drive.google.com/file/d/abc/view", true, false));
        Assert.assertFalse("an embedded frame must not take over the whole screen", overridden);
        Assert.assertTrue(host.nativeAppUrls.isEmpty());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void aNavigationWithoutGestureInformationStaysInTheBrowser() {
        RecordingHost host = new RecordingHost();
        host.matchingNativeAppInstalled = true;
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        boolean overridden = client.shouldOverrideUrlLoading(newWebView(),
            "https://drive.google.com/file/d/abc/view");
        Assert.assertFalse("a navigation that cannot be shown to be user initiated must stay in the browser",
            overridden);
        Assert.assertTrue(host.nativeAppUrls.isEmpty());
    }

    @Test
    public void aTappedLinkDoesNotPropagateHostException() {
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(new ThrowingHost());
        Assert.assertFalse(client.shouldOverrideUrlLoading(newWebView(),
            tappedLink("https://drive.google.com/file/d/abc/view")));
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
    public void projectBoardDesktopTabUsesDesktopViewport() {
        RecordingHost host = new RecordingHost();
        host.viewMode = BrowserViewMode.DESKTOP;
        host.injectMobileViewport = false;
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(host);
        RecordingWebView webView = newRecordingWebView();
        client.onPageStarted(webView, PROJECT_BOARD_URL, null);
        client.onPageCommitVisible(webView, PROJECT_BOARD_URL);
        client.onPageFinished(webView, PROJECT_BOARD_URL);
        Assert.assertTrue(webView.injectedDesktopViewport());
        Assert.assertFalse(webView.injectedMobileViewport());
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

    @Test
    public void pageStartedDoesNotPropagateHostException() {
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(new ThrowingHost());
        client.onPageStarted(newWebView(), "https://example.com/", null);
    }

    @Test
    public void pageCommitVisibleDoesNotPropagateHostException() {
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(new ThrowingHost());
        client.onPageCommitVisible(newWebView(), "https://example.com/");
    }

    @Test
    public void pageFinishedDoesNotPropagateHostException() {
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(new ThrowingHost());
        client.onPageFinished(newWebView(), "https://example.com/");
    }

    @Test
    public void visitedHistoryUpdateDoesNotPropagateHostException() {
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(new ThrowingHost());
        client.doUpdateVisitedHistory(newWebView(), "https://example.com/page", true);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void mainFrameErrorDoesNotPropagateHostException() {
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(new ThrowingHost());
        client.onReceivedError(newWebView(), -2, "net error", "https://example.com/");
    }

    @Test
    public void renderProcessGoneReturnsHandledTrueWhenHostThrows() {
        BrowserCoreWebViewClient client = new BrowserCoreWebViewClient(new ThrowingHost());
        Assert.assertTrue(client.onRenderProcessGone(newWebView(), null));
    }
}
