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

        int shownCount;

        @Override
        public void onPasskeyCeremonyDetected() {
            shownCount++;
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
            mHandler, host, new BrowserPasskeyHintDebounce(10_000L), mClock::get);
    }

    @Test
    public void showsTheAffordanceOnTheMainThreadWhenACeremonyIsDetected() {
        CountingHost host = new CountingHost();
        BrowserPasskeyBridge bridge = bridge(host);

        bridge.onPasskeyCeremonyDetected("https://attacker.example/phishing");
        Assert.assertEquals(0, host.shownCount);

        mShadowLooper.idle();

        Assert.assertEquals(1, host.shownCount);
    }

    @Test
    public void debouncesRepeatedDetectionsSoTheSnackbarIsNotSpammed() {
        CountingHost host = new CountingHost();
        BrowserPasskeyBridge bridge = bridge(host);

        bridge.deliverOnMainThread();
        mClock.set(2_000L);
        bridge.deliverOnMainThread();
        mClock.set(9_000L);
        bridge.deliverOnMainThread();

        Assert.assertEquals(1, host.shownCount);
    }

    @Test
    public void showsAgainAfterTheDebounceIntervalElapses() {
        CountingHost host = new CountingHost();
        BrowserPasskeyBridge bridge = bridge(host);

        bridge.deliverOnMainThread();
        mClock.set(11_000L);
        bridge.deliverOnMainThread();

        Assert.assertEquals(2, host.shownCount);
    }
}
