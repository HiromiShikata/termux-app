package com.termux.app.browser;

import android.os.Handler;
import android.os.Looper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.atomic.AtomicLong;

@RunWith(RobolectricTestRunner.class)
public class BrowserPasskeyBridgeTest {

    private static final class CountingHost implements BrowserPasskeyBridge.Host {

        int passkeyShownCount;

        int loginFormShownCount;

        @Override
        public void onPasskeyCeremonyDetected() {
            passkeyShownCount++;
        }

        @Override
        public void onLoginFormDetected() {
            loginFormShownCount++;
        }
    }

    private Handler mHandler;
    private ShadowLooper mShadowLooper;
    private AtomicLong mClock;

    @Before
    public void setUp() {
        mHandler = new Handler(Looper.getMainLooper());
        mShadowLooper = Shadows.shadowOf(Looper.getMainLooper());
        mClock = new AtomicLong(0L);
    }

    private BrowserPasskeyBridge bridge(CountingHost host) {
        return new BrowserPasskeyBridge(
            mHandler, host,
            new BrowserPasskeyHintDebounce(10_000L),
            new BrowserPasskeyHintDebounce(10_000L),
            mClock::get);
    }

    @Test
    public void showsTheAffordanceOnTheMainThreadWhenACeremonyIsDetected() {
        CountingHost host = new CountingHost();
        BrowserPasskeyBridge bridge = bridge(host);

        bridge.onPasskeyCeremonyDetected("https://attacker.example/phishing");
        Assert.assertEquals(0, host.passkeyShownCount);

        mShadowLooper.idle();

        Assert.assertEquals(1, host.passkeyShownCount);
    }

    @Test
    public void showsTheLoginFormAffordanceOnTheMainThreadWhenALoginFormIsDetected() {
        CountingHost host = new CountingHost();
        BrowserPasskeyBridge bridge = bridge(host);

        bridge.onLoginFormDetected("https://attacker.example/phishing");
        Assert.assertEquals(0, host.loginFormShownCount);

        mShadowLooper.idle();

        Assert.assertEquals(1, host.loginFormShownCount);
        Assert.assertEquals(0, host.passkeyShownCount);
    }

    @Test
    public void debouncesRepeatedDetectionsSoTheSnackbarIsNotSpammed() {
        CountingHost host = new CountingHost();
        BrowserPasskeyBridge bridge = bridge(host);

        bridge.deliverPasskeyOnMainThread();
        mClock.set(2_000L);
        bridge.deliverPasskeyOnMainThread();
        mClock.set(9_000L);
        bridge.deliverPasskeyOnMainThread();

        Assert.assertEquals(1, host.passkeyShownCount);
    }

    @Test
    public void debouncesRepeatedLoginFormDetectionsIndependentlyFromPasskey() {
        CountingHost host = new CountingHost();
        BrowserPasskeyBridge bridge = bridge(host);

        bridge.deliverLoginFormOnMainThread();
        mClock.set(2_000L);
        bridge.deliverLoginFormOnMainThread();

        Assert.assertEquals(1, host.loginFormShownCount);

        bridge.deliverPasskeyOnMainThread();

        Assert.assertEquals(1, host.passkeyShownCount);
    }

    @Test
    public void showsAgainAfterTheDebounceIntervalElapses() {
        CountingHost host = new CountingHost();
        BrowserPasskeyBridge bridge = bridge(host);

        bridge.deliverPasskeyOnMainThread();
        mClock.set(11_000L);
        bridge.deliverPasskeyOnMainThread();

        Assert.assertEquals(2, host.passkeyShownCount);
    }
}
