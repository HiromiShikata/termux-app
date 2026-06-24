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
}
