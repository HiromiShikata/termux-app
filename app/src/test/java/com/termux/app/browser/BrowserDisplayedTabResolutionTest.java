package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserDisplayedTabResolutionTest {

    private static final String SESSION_A = "session-a";
    private static final String SESSION_B = "session-b";

    @Test
    public void withoutAnActiveTabNothingMayBeDisplayed() {
        BrowserDisplayedTabResolution resolution = BrowserDisplayedTabResolution.resolve(
            SESSION_A, null, SESSION_A, SESSION_A, false);

        Assert.assertEquals(BrowserDisplayedTabResolution.Action.CLEAR, resolution.getAction());
        Assert.assertFalse(resolution.shouldDisplay());
        Assert.assertFalse(resolution.shouldLoadWebView());
    }

    @Test
    public void withoutACurrentSessionNothingMayBeDisplayed() {
        BrowserDisplayedTabResolution resolution = BrowserDisplayedTabResolution.resolve(
            null, SESSION_A, SESSION_A, null, false);

        Assert.assertEquals(BrowserDisplayedTabResolution.Action.CLEAR, resolution.getAction());
        Assert.assertFalse(resolution.shouldDisplay());
    }

    @Test
    public void aTabOwnedByADifferentSessionCanNeverBeDisplayed() {
        BrowserDisplayedTabResolution resolution = BrowserDisplayedTabResolution.resolve(
            SESSION_A, SESSION_B, SESSION_B, SESSION_B, false);

        Assert.assertEquals(BrowserDisplayedTabResolution.Action.CLEAR, resolution.getAction());
        Assert.assertFalse(resolution.shouldDisplay());
        Assert.assertFalse(resolution.shouldLoadWebView());
    }

    @Test
    public void displayingTheCurrentSessionsTabIntoAFreshWebViewLoadsIt() {
        BrowserDisplayedTabResolution resolution = BrowserDisplayedTabResolution.resolve(
            SESSION_A, SESSION_A, null, null, false);

        Assert.assertEquals(BrowserDisplayedTabResolution.Action.LOAD, resolution.getAction());
        Assert.assertTrue(resolution.shouldDisplay());
        Assert.assertTrue(resolution.shouldLoadWebView());
    }

    @Test
    public void switchingFromAnotherSessionForcesAReloadEvenWhenTabReferenceMatches() {
        BrowserDisplayedTabResolution resolution = BrowserDisplayedTabResolution.resolve(
            SESSION_A, SESSION_A, SESSION_B, SESSION_A, false);

        Assert.assertEquals(BrowserDisplayedTabResolution.Action.LOAD, resolution.getAction());
        Assert.assertTrue(resolution.shouldDisplay());
        Assert.assertTrue(resolution.shouldLoadWebView());
    }

    @Test
    public void redisplayingTheAlreadyShownTabOfTheCurrentSessionSkipsTheReload() {
        BrowserDisplayedTabResolution resolution = BrowserDisplayedTabResolution.resolve(
            SESSION_A, SESSION_A, SESSION_A, SESSION_A, false);

        Assert.assertEquals(BrowserDisplayedTabResolution.Action.KEEP, resolution.getAction());
        Assert.assertTrue(resolution.shouldDisplay());
        Assert.assertFalse(resolution.shouldLoadWebView());
    }

    @Test
    public void forceReloadAlwaysLoadsEvenWhenTabIsAlreadyDisplayedAndOwned() {
        BrowserDisplayedTabResolution resolution = BrowserDisplayedTabResolution.resolve(
            SESSION_A, SESSION_A, SESSION_A, SESSION_A, true);

        Assert.assertEquals(BrowserDisplayedTabResolution.Action.LOAD, resolution.getAction());
        Assert.assertTrue(resolution.shouldDisplay());
        Assert.assertTrue(resolution.shouldLoadWebView());
    }

    @Test
    public void displayingIntoAWebViewThatOwnsTheCurrentSessionButShowsNoTabLoadsTheTab() {
        BrowserDisplayedTabResolution resolution = BrowserDisplayedTabResolution.resolve(
            SESSION_A, SESSION_A, SESSION_A, null, false);

        Assert.assertEquals(BrowserDisplayedTabResolution.Action.LOAD, resolution.getAction());
        Assert.assertTrue(resolution.shouldDisplay());
        Assert.assertTrue(resolution.shouldLoadWebView());
    }

    @Test
    public void aLateCallbackForThePreviousSessionMustNotBeAppliedAfterASwitch() {
        Assert.assertFalse(BrowserDisplayedTabResolution.callbackMayMutateDisplayedTab(
            SESSION_A, SESSION_B, SESSION_B));
    }

    @Test
    public void aCallbackForTheCurrentSessionsOwnedWebViewMayBeApplied() {
        Assert.assertTrue(BrowserDisplayedTabResolution.callbackMayMutateDisplayedTab(
            SESSION_A, SESSION_A, SESSION_A));
    }

    @Test
    public void aCallbackIsRejectedWhenTheWebViewIsNotYetOwnedByTheCurrentSession() {
        Assert.assertFalse(BrowserDisplayedTabResolution.callbackMayMutateDisplayedTab(
            SESSION_A, SESSION_A, SESSION_B));
    }

    @Test
    public void aCallbackIsRejectedWhenThereIsNoCurrentSession() {
        Assert.assertFalse(BrowserDisplayedTabResolution.callbackMayMutateDisplayedTab(
            null, null, null));
    }

    @Test
    public void aCallbackIsRejectedWhenTheDisplayedTabHasNoOwnerHandle() {
        Assert.assertFalse(BrowserDisplayedTabResolution.callbackMayMutateDisplayedTab(
            SESSION_A, null, SESSION_A));
    }
}
