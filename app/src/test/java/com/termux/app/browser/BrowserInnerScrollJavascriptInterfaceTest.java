package com.termux.app.browser;

import android.app.Activity;
import android.os.Looper;
import android.webkit.WebView;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 26)
public class BrowserInnerScrollJavascriptInterfaceTest {

    private Activity mActivity;
    private BrowserWebViewScrollTracker mScrollTracker;

    @Before
    public void setUp() {
        mActivity = Robolectric.buildActivity(Activity.class).create().get();
        mScrollTracker = new BrowserWebViewScrollTracker();
    }

    @Test
    public void onScrolledTrueReportsNotAtTopAfterPostedRunnableRuns() {
        WebView webView = new WebView(mActivity);
        mScrollTracker.attach(webView);
        BrowserInnerScrollJavascriptInterface jsInterface =
            new BrowserInnerScrollJavascriptInterface(webView, mScrollTracker);

        jsInterface.onScrolled(true);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        Assert.assertFalse(mScrollTracker.isAtTop(webView));
    }

    @Test
    public void onScrolledFalseReportsAtTopAfterPostedRunnableRuns() {
        WebView webView = new WebView(mActivity);
        mScrollTracker.attach(webView);
        mScrollTracker.recordInnerScrollHasContentAbove(webView, true);
        BrowserInnerScrollJavascriptInterface jsInterface =
            new BrowserInnerScrollJavascriptInterface(webView, mScrollTracker);

        jsInterface.onScrolled(false);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        Assert.assertTrue(mScrollTracker.isAtTop(webView));
    }
}
