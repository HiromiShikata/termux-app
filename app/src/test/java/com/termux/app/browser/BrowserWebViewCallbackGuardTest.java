package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(RobolectricTestRunner.class)
public class BrowserWebViewCallbackGuardTest {

    @Test
    public void runExecutesActionOnNormalPath() {
        BrowserWebViewCallbackGuard guard = new BrowserWebViewCallbackGuard("test");
        AtomicBoolean executed = new AtomicBoolean(false);
        guard.run("callback", () -> executed.set(true));
        Assert.assertTrue(executed.get());
    }

    @Test
    public void runSwallowsThrownRuntimeException() {
        BrowserWebViewCallbackGuard guard = new BrowserWebViewCallbackGuard("test");
        guard.run("callback", () -> {
            throw new IllegalStateException("boom");
        });
    }

    @Test
    public void runSwallowsThrownError() {
        BrowserWebViewCallbackGuard guard = new BrowserWebViewCallbackGuard("test");
        guard.run("callback", () -> {
            throw new AssertionError("boom");
        });
    }

    @Test
    public void runReturningReturnsActionResultOnNormalPath() {
        BrowserWebViewCallbackGuard guard = new BrowserWebViewCallbackGuard("test");
        Assert.assertTrue(guard.runReturning("callback", false, () -> true));
        Assert.assertFalse(guard.runReturning("callback", true, () -> false));
    }

    @Test
    public void runReturningReturnsFallbackWhenActionThrows() {
        BrowserWebViewCallbackGuard guard = new BrowserWebViewCallbackGuard("test");
        boolean result = guard.runReturning("callback", true, () -> {
            throw new IllegalStateException("boom");
        });
        Assert.assertTrue(result);
    }

    @Test
    public void runReturningReturnsFallbackWhenActionThrowsError() {
        BrowserWebViewCallbackGuard guard = new BrowserWebViewCallbackGuard("test");
        boolean result = guard.runReturning("callback", false, () -> {
            throw new AssertionError("boom");
        });
        Assert.assertFalse(result);
    }
}
