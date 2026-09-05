package com.termux.app.browser;

import android.app.Activity;
import android.os.Build;
import android.webkit.WebView;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.M)
public class BrowserWebViewScrollTrackerTest {

    private Activity mActivity;
    private BrowserWebViewScrollTracker mScrollTracker;

    @Before
    public void setUp() {
        mActivity = Robolectric.buildActivity(Activity.class).create().get();
        mScrollTracker = new BrowserWebViewScrollTracker();
    }

    @Test
    public void reportsAtTopForAFreshlyAttachedWebView() {
        WebView webView = new WebView(mActivity);
        mScrollTracker.attach(webView);

        Assert.assertTrue(mScrollTracker.isAtTop(webView));
    }

    @Test
    public void reportsNotAtTopAfterTheWebViewScrollsDown() {
        WebView webView = new WebView(mActivity);
        mScrollTracker.attach(webView);

        mScrollTracker.recordScrollY(webView, 480);

        Assert.assertFalse(mScrollTracker.isAtTop(webView));
    }

    @Test
    public void reportsAtTopAgainAfterTheWebViewScrollsBackToTheTop() {
        WebView webView = new WebView(mActivity);
        mScrollTracker.attach(webView);

        mScrollTracker.recordScrollY(webView, 480);
        mScrollTracker.recordScrollY(webView, 0);

        Assert.assertTrue(mScrollTracker.isAtTop(webView));
    }

    @Test
    public void resetToTopTreatsAReloadedPageAsAtTopEvenAfterPriorScrolling() {
        WebView webView = new WebView(mActivity);
        mScrollTracker.attach(webView);

        mScrollTracker.recordScrollY(webView, 900);
        mScrollTracker.resetToTop(webView);

        Assert.assertTrue(mScrollTracker.isAtTop(webView));
    }

    @Test
    public void tracksEachWebViewIndependentlySoTabSwitchesStayCorrect() {
        WebView firstTabWebView = new WebView(mActivity);
        WebView secondTabWebView = new WebView(mActivity);
        mScrollTracker.attach(firstTabWebView);
        mScrollTracker.attach(secondTabWebView);

        mScrollTracker.recordScrollY(firstTabWebView, 600);

        Assert.assertFalse(mScrollTracker.isAtTop(firstTabWebView));
        Assert.assertTrue(mScrollTracker.isAtTop(secondTabWebView));
    }

    @Test
    public void reportsNotAtTopForAWebViewItDoesNotTrack() {
        WebView untrackedWebView = new WebView(mActivity);

        Assert.assertFalse(mScrollTracker.isAtTop(untrackedWebView));
    }

    @Test
    public void reportsNotAtTopForANullWebView() {
        Assert.assertFalse(mScrollTracker.isAtTop(null));
    }

    @Test
    public void forgetsAWebViewSoItIsNoLongerTreatedAsAtTop() {
        WebView webView = new WebView(mActivity);
        mScrollTracker.attach(webView);

        mScrollTracker.forget(webView);

        Assert.assertFalse(mScrollTracker.isAtTop(webView));
    }

    @Test
    public void reportsNotAtTopWhenInnerScrollHasContentAbove() {
        WebView webView = new WebView(mActivity);
        mScrollTracker.attach(webView);

        mScrollTracker.recordInnerScrollHasContentAbove(webView, true);

        Assert.assertFalse(mScrollTracker.isAtTop(webView));
    }

    @Test
    public void reportsAtTopAgainWhenInnerScrollReturnsToTopOfContent() {
        WebView webView = new WebView(mActivity);
        mScrollTracker.attach(webView);

        mScrollTracker.recordInnerScrollHasContentAbove(webView, true);
        mScrollTracker.recordInnerScrollHasContentAbove(webView, false);

        Assert.assertTrue(mScrollTracker.isAtTop(webView));
    }

    @Test
    public void resetToTopClearsInnerScrollContentAboveFlag() {
        WebView webView = new WebView(mActivity);
        mScrollTracker.attach(webView);

        mScrollTracker.recordInnerScrollHasContentAbove(webView, true);
        mScrollTracker.resetToTop(webView);

        Assert.assertTrue(mScrollTracker.isAtTop(webView));
    }
}
