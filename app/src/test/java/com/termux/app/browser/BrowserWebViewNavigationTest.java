package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserWebViewNavigationTest {

    private static final String SESSION_A = "session-a";
    private static final String SESSION_B = "session-b";
    private static final String URL_A = "https://a.example/page";
    private static final String URL_B = "https://b.example/page";

    @Test
    public void urlCallbackMutatesTheOriginatingTabWhenTheNavigationStillOwnsTheWebView() {
        BrowserTab tabA = new BrowserTab(SESSION_A, URL_A);
        BrowserWebViewNavigation navigation = BrowserWebViewNavigation.startedBy(tabA);

        Assert.assertSame(tabA, navigation.tabForUrlCallback(SESSION_A, SESSION_A, URL_A, URL_A));
    }

    @Test
    public void urlCallbackIsRejectedWhenTheActiveSessionDiffersFromTheOriginatingSession() {
        BrowserTab tabA = new BrowserTab(SESSION_A, URL_A);
        BrowserWebViewNavigation navigation = BrowserWebViewNavigation.startedBy(tabA);

        Assert.assertNull(navigation.tabForUrlCallback(SESSION_B, SESSION_B, URL_A, URL_A));
    }

    @Test
    public void urlCallbackIsRejectedWhenTheWebViewIsNotOwnedByTheCurrentSession() {
        BrowserTab tabA = new BrowserTab(SESSION_A, URL_A);
        BrowserWebViewNavigation navigation = BrowserWebViewNavigation.startedBy(tabA);

        Assert.assertNull(navigation.tabForUrlCallback(SESSION_A, SESSION_B, URL_A, URL_A));
    }

    @Test
    public void aLatePageLoadCallbackForSessionADoesNotMutateAnyTabAfterSwitchingToSessionB() {
        BrowserTab tabA = new BrowserTab(SESSION_A, URL_A);
        BrowserWebViewNavigation navigationForSessionA = BrowserWebViewNavigation.startedBy(tabA);

        BrowserTab tabB = new BrowserTab(SESSION_B, URL_B);
        BrowserWebViewNavigation navigationForSessionB = BrowserWebViewNavigation.startedBy(tabB);

        Assert.assertNull(navigationForSessionA.tabForUrlCallback(SESSION_B, SESSION_B, URL_A, URL_B));
        Assert.assertSame(tabB, navigationForSessionB.tabForUrlCallback(SESSION_B, SESSION_B, URL_B, URL_B));
        Assert.assertEquals(URL_B, tabB.getUrl());
    }

    @Test
    public void urlCallbackIsRejectedWhenTheReportedUrlDoesNotMatchTheWebViewCurrentUrl() {
        BrowserTab tabA = new BrowserTab(SESSION_A, URL_A);
        BrowserWebViewNavigation navigation = BrowserWebViewNavigation.startedBy(tabA);

        Assert.assertNull(navigation.tabForUrlCallback(SESSION_A, SESSION_A, URL_A, URL_B));
    }

    @Test
    public void urlCallbackIsRejectedWhenThereIsNoCurrentSession() {
        BrowserTab tabA = new BrowserTab(SESSION_A, URL_A);
        BrowserWebViewNavigation navigation = BrowserWebViewNavigation.startedBy(tabA);

        Assert.assertNull(navigation.tabForUrlCallback(null, null, URL_A, URL_A));
    }

    @Test
    public void titleCallbackMutatesTheOriginatingTabWhenTheWebViewShowsThatTabsUrl() {
        BrowserTab tabA = new BrowserTab(SESSION_A, URL_A);
        BrowserWebViewNavigation navigation = BrowserWebViewNavigation.startedBy(tabA);

        Assert.assertSame(tabA, navigation.tabForTitleCallback(SESSION_A, SESSION_A, URL_A));
    }

    @Test
    public void aLateTitleCallbackForSessionADoesNotMutateSessionBTabAfterSwitch() {
        BrowserTab tabA = new BrowserTab(SESSION_A, URL_A);
        BrowserWebViewNavigation navigationForSessionA = BrowserWebViewNavigation.startedBy(tabA);

        Assert.assertNull(navigationForSessionA.tabForTitleCallback(SESSION_B, SESSION_B, URL_B));
    }

    @Test
    public void titleCallbackIsRejectedWhenTheWebViewShowsADifferentUrlThanTheTab() {
        BrowserTab tabA = new BrowserTab(SESSION_A, URL_A);
        BrowserWebViewNavigation navigation = BrowserWebViewNavigation.startedBy(tabA);

        Assert.assertNull(navigation.tabForTitleCallback(SESSION_A, SESSION_A, URL_B));
    }

    @Test
    public void theOriginatingSessionHandleIsCapturedAtNavigationTime() {
        BrowserTab tabA = new BrowserTab(SESSION_A, URL_A);
        BrowserWebViewNavigation navigation = BrowserWebViewNavigation.startedBy(tabA);

        Assert.assertEquals(SESSION_A, navigation.getSessionHandle());
        Assert.assertSame(tabA, navigation.getTab());
    }
}
