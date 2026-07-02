package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BrowserWebViewPausePlanTest {

    @Test
    public void displayedWebViewResumesWhileHiddenTabsPauseWhenBrowserForegrounded() {
        String displayed = "displayed";
        String hiddenA = "hiddenA";
        String hiddenB = "hiddenB";
        List<String> all = Arrays.asList(hiddenA, displayed, hiddenB);

        BrowserWebViewPausePlan<String> plan =
            BrowserWebViewPausePlan.resolve(all, displayed, true, true);

        Assert.assertEquals(Collections.singletonList(displayed), plan.getWebViewsToResume());
        Assert.assertEquals(Arrays.asList(hiddenA, hiddenB), plan.getWebViewsToPause());
        Assert.assertTrue(plan.shouldTimersBeActive());
    }

    @Test
    public void allWebViewsPauseWhenBrowserHidden() {
        String displayed = "displayed";
        String other = "other";
        List<String> all = Arrays.asList(displayed, other);

        BrowserWebViewPausePlan<String> plan =
            BrowserWebViewPausePlan.resolve(all, displayed, false, true);

        Assert.assertTrue(plan.getWebViewsToResume().isEmpty());
        Assert.assertEquals(Arrays.asList(displayed, other), plan.getWebViewsToPause());
        Assert.assertFalse(plan.shouldTimersBeActive());
    }

    @Test
    public void allWebViewsPauseWhenAppBackgroundedEvenIfBrowserVisible() {
        String displayed = "displayed";
        List<String> all = Collections.singletonList(displayed);

        BrowserWebViewPausePlan<String> plan =
            BrowserWebViewPausePlan.resolve(all, displayed, true, false);

        Assert.assertTrue(plan.getWebViewsToResume().isEmpty());
        Assert.assertEquals(Collections.singletonList(displayed), plan.getWebViewsToPause());
        Assert.assertFalse(plan.shouldTimersBeActive());
    }

    @Test
    public void timersInactiveWhenNoDisplayedWebViewExists() {
        String orphan = "orphan";
        List<String> all = Collections.singletonList(orphan);

        BrowserWebViewPausePlan<String> plan =
            BrowserWebViewPausePlan.resolve(all, null, true, true);

        Assert.assertTrue(plan.getWebViewsToResume().isEmpty());
        Assert.assertEquals(Collections.singletonList(orphan), plan.getWebViewsToPause());
        Assert.assertFalse(plan.shouldTimersBeActive());
    }

    @Test
    public void emptyWebViewSetPausesTimers() {
        BrowserWebViewPausePlan<String> plan =
            BrowserWebViewPausePlan.resolve(Collections.emptyList(), null, true, true);

        Assert.assertTrue(plan.getWebViewsToResume().isEmpty());
        Assert.assertTrue(plan.getWebViewsToPause().isEmpty());
        Assert.assertFalse(plan.shouldTimersBeActive());
    }
}
