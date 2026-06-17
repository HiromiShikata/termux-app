package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class HeaderTapCopyTargetTest {

    @Test
    public void copiesProjectBrowserUrlWhenOverlayVisible() {
        HeaderTapCopyTarget target = HeaderTapCopyTarget.resolve(
            true, "https://github.com/owner/repo/issues/1", "workSession");

        Assert.assertTrue(target.isProjectBrowserUrl());
        Assert.assertEquals(HeaderTapCopyTarget.Kind.PROJECT_BROWSER_URL, target.getKind());
        Assert.assertEquals("https://github.com/owner/repo/issues/1", target.getText());
        Assert.assertFalse(target.isEmpty());
    }

    @Test
    public void copiesSessionNameWhenOverlayHidden() {
        HeaderTapCopyTarget target = HeaderTapCopyTarget.resolve(
            false, "https://github.com/owner/repo/issues/1", "workSession");

        Assert.assertFalse(target.isProjectBrowserUrl());
        Assert.assertEquals(HeaderTapCopyTarget.Kind.SESSION_NAME, target.getKind());
        Assert.assertEquals("workSession", target.getText());
        Assert.assertFalse(target.isEmpty());
    }

    @Test
    public void copiesSessionNameWhenOverlayVisibleButUrlNull() {
        HeaderTapCopyTarget target = HeaderTapCopyTarget.resolve(true, null, "workSession");

        Assert.assertFalse(target.isProjectBrowserUrl());
        Assert.assertEquals("workSession", target.getText());
    }

    @Test
    public void copiesSessionNameWhenOverlayVisibleButUrlEmpty() {
        HeaderTapCopyTarget target = HeaderTapCopyTarget.resolve(true, "", "workSession");

        Assert.assertFalse(target.isProjectBrowserUrl());
        Assert.assertEquals("workSession", target.getText());
    }

    @Test
    public void isEmptyWhenOverlayHiddenAndNoSessionName() {
        HeaderTapCopyTarget target = HeaderTapCopyTarget.resolve(false, null, null);

        Assert.assertFalse(target.isProjectBrowserUrl());
        Assert.assertTrue(target.isEmpty());
        Assert.assertNull(target.getText());
    }
}
