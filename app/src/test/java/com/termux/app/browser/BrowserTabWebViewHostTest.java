package com.termux.app.browser;

import android.app.Activity;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BrowserTabWebViewHostTest {

    private static final String SESSION_A = "session-a";
    private static final String SESSION_B = "session-b";

    private Activity mActivity;
    private FrameLayout mContainer;
    private List<WebView> mCreatedWebViews;
    private BrowserTabWebViewHost mHost;

    @Before
    public void setUp() {
        mActivity = Robolectric.buildActivity(Activity.class).create().get();
        mContainer = new FrameLayout(mActivity);
        mCreatedWebViews = new ArrayList<>();
        mHost = new BrowserTabWebViewHost(mContainer, tab -> {
            WebView webView = new WebView(mActivity);
            mCreatedWebViews.add(webView);
            return webView;
        });
    }

    private BrowserTab tab(String sessionHandle, String url) {
        return new BrowserTab(sessionHandle, url);
    }

    @Test
    public void eachTabGetsItsOwnDistinctWebViewInstance() {
        BrowserTab tabOne = tab(SESSION_A, "https://a.example/1");
        BrowserTab tabTwo = tab(SESSION_A, "https://a.example/2");

        WebView webViewOne = mHost.showTab(tabOne);
        WebView webViewTwo = mHost.showTab(tabTwo);

        Assert.assertNotSame(webViewOne, webViewTwo);
        Assert.assertEquals(2, mCreatedWebViews.size());
    }

    @Test
    public void switchingBackToATabReusesItsLiveWebViewWithoutRecreating() {
        BrowserTab tabOne = tab(SESSION_A, "https://a.example/1");
        BrowserTab tabTwo = tab(SESSION_A, "https://a.example/2");

        WebView firstShowOfTabOne = mHost.showTab(tabOne);
        mHost.showTab(tabTwo);
        WebView secondShowOfTabOne = mHost.showTab(tabOne);

        Assert.assertSame(firstShowOfTabOne, secondShowOfTabOne);
        Assert.assertEquals(2, mCreatedWebViews.size());
    }

    @Test
    public void onlyTheDisplayedTabsWebViewIsVisible() {
        BrowserTab tabOne = tab(SESSION_A, "https://a.example/1");
        BrowserTab tabTwo = tab(SESSION_A, "https://a.example/2");

        WebView webViewOne = mHost.showTab(tabOne);
        WebView webViewTwo = mHost.showTab(tabTwo);

        Assert.assertEquals(View.GONE, webViewOne.getVisibility());
        Assert.assertEquals(View.VISIBLE, webViewTwo.getVisibility());
        Assert.assertSame(tabTwo, mHost.getDisplayedTab());
        Assert.assertSame(webViewTwo, mHost.getDisplayedWebView());
    }

    @Test
    public void removingASessionDestroysOnlyThatSessionsWebViews() {
        BrowserTab tabInSessionA = tab(SESSION_A, "https://a.example/");
        BrowserTab tabInSessionB = tab(SESSION_B, "https://b.example/");
        mHost.showTab(tabInSessionA);
        mHost.showTab(tabInSessionB);

        mHost.removeSession(SESSION_A);

        Assert.assertFalse(mHost.hasWebViewForTab(tabInSessionA));
        Assert.assertTrue(mHost.hasWebViewForTab(tabInSessionB));
    }

    @Test
    public void removingATabDestroysItsWebViewAndClearsDisplayWhenItWasDisplayed() {
        BrowserTab tabOne = tab(SESSION_A, "https://a.example/1");
        mHost.showTab(tabOne);

        mHost.removeTab(tabOne);

        Assert.assertFalse(mHost.hasWebViewForTab(tabOne));
        Assert.assertNull(mHost.getDisplayedTab());
        Assert.assertNull(mHost.getDisplayedWebView());
    }

    @Test
    public void tabsFromDifferentSessionsNeverShareAWebView() {
        BrowserTab tabInSessionA = tab(SESSION_A, "https://shared.example/");
        BrowserTab tabInSessionB = tab(SESSION_B, "https://shared.example/");

        WebView webViewA = mHost.showTab(tabInSessionA);
        WebView webViewB = mHost.showTab(tabInSessionB);

        Assert.assertNotSame(webViewA, webViewB);
    }

    @Test
    public void findTabForWebViewReturnsTheOwningTab() {
        BrowserTab tabOne = tab(SESSION_A, "https://a.example/1");
        BrowserTab tabTwo = tab(SESSION_A, "https://a.example/2");
        WebView webViewOne = mHost.showTab(tabOne);
        WebView webViewTwo = mHost.showTab(tabTwo);

        Assert.assertSame(tabOne, mHost.findTabForWebView(webViewOne));
        Assert.assertSame(tabTwo, mHost.findTabForWebView(webViewTwo));
    }

    @Test
    public void recreateWebViewForTabReplacesOnlyThatTabsWebViewInstance() {
        BrowserTab tabOne = tab(SESSION_A, "https://a.example/1");
        WebView original = mHost.showTab(tabOne);

        WebView recreated = mHost.recreateWebViewForTab(tabOne);

        Assert.assertNotSame(original, recreated);
        Assert.assertSame(recreated, mHost.getDisplayedWebView());
        Assert.assertTrue(mHost.hasWebViewForTab(tabOne));
        Assert.assertEquals(2, mCreatedWebViews.size());
    }

    @Test
    public void recreateWebViewForTabDoesNotTouchOtherTabsWebViews() {
        BrowserTab tabToRecover = tab(SESSION_A, "https://a.example/recover");
        BrowserTab otherTab = tab(SESSION_B, "https://b.example/keep");
        WebView recoverWebView = mHost.showTab(tabToRecover);
        WebView otherWebView = mHost.showTab(otherTab);

        WebView recreated = mHost.recreateWebViewForTab(tabToRecover);

        Assert.assertNotSame(recoverWebView, recreated);
        Assert.assertSame(otherTab, mHost.findTabForWebView(otherWebView));
        Assert.assertTrue(mHost.hasWebViewForTab(otherTab));
    }

    @Test
    public void recreateWebViewForTabKeepsHiddenVisibilityForNonDisplayedTab() {
        BrowserTab backgroundTab = tab(SESSION_A, "https://a.example/bg");
        BrowserTab displayedTab = tab(SESSION_A, "https://a.example/fg");
        mHost.showTab(backgroundTab);
        mHost.showTab(displayedTab);

        WebView recreated = mHost.recreateWebViewForTab(backgroundTab);

        Assert.assertEquals(View.GONE, recreated.getVisibility());
        Assert.assertSame(displayedTab, mHost.getDisplayedTab());
    }

    @Test
    public void recreateWebViewForTabLoadsTheTabUrl() {
        List<String> loadedUrls = new ArrayList<>();
        BrowserTabWebViewHost host = new BrowserTabWebViewHost(new FrameLayout(mActivity),
            tab -> new UrlRecordingWebView(mActivity, loadedUrls));
        BrowserTab tabOne = tab(SESSION_A, "https://a.example/page");
        host.showTab(tabOne);
        loadedUrls.clear();

        host.recreateWebViewForTab(tabOne);

        Assert.assertEquals(List.of("https://a.example/page"), loadedUrls);
    }

    @Test
    public void recreateWebViewForTabWithBlankPageLoadsAboutBlankInsteadOfTheCrashedUrl() {
        List<String> loadedUrls = new ArrayList<>();
        BrowserTabWebViewHost host = new BrowserTabWebViewHost(new FrameLayout(mActivity),
            tab -> new UrlRecordingWebView(mActivity, loadedUrls));
        BrowserTab tabOne = tab(SESSION_A, "https://a.example/crashing");
        host.showTab(tabOne);
        loadedUrls.clear();

        WebView recreated = host.recreateWebViewForTabWithBlankPage(tabOne);

        Assert.assertTrue(host.hasWebViewForTab(tabOne));
        Assert.assertEquals(List.of("about:blank"), loadedUrls);
        Assert.assertSame(recreated, host.getDisplayedWebView());
    }

    private static final class UrlRecordingWebView extends WebView {
        private final List<String> mLoadedUrls;

        UrlRecordingWebView(Activity activity, List<String> loadedUrls) {
            super(activity);
            mLoadedUrls = loadedUrls;
        }

        @Override
        public void loadUrl(String url) {
            mLoadedUrls.add(url);
            super.loadUrl(url);
        }
    }
}
