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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RunWith(RobolectricTestRunner.class)
public class BrowserPasskeyBridgeTest {

    private static final class RecordingHost implements BrowserPasskeyBridge.Host {

        final List<String> shownUrls = new ArrayList<>();

        @Override
        public void onPasskeyCeremonyDetected(String pageUrl) {
            shownUrls.add(pageUrl);
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

    private BrowserPasskeyBridge bridge(RecordingHost host) {
        return new BrowserPasskeyBridge(
            mHandler, host, new BrowserPasskeyHintDebounce(10_000L), mClock::get);
    }

    @Test
    public void showsTheAffordanceForTheDetectedPageUrlOnTheMainThread() {
        RecordingHost host = new RecordingHost();
        BrowserPasskeyBridge bridge = bridge(host);

        bridge.onPasskeyCeremonyDetected("https://example.com/login");
        Assert.assertTrue(host.shownUrls.isEmpty());

        mShadowLooper.idle();

        Assert.assertEquals(1, host.shownUrls.size());
        Assert.assertEquals("https://example.com/login", host.shownUrls.get(0));
    }

    @Test
    public void debouncesRepeatedDetectionsSoTheSnackbarIsNotSpammed() {
        RecordingHost host = new RecordingHost();
        BrowserPasskeyBridge bridge = bridge(host);

        bridge.deliverOnMainThread("https://example.com/login");
        mClock.set(2_000L);
        bridge.deliverOnMainThread("https://example.com/login");
        mClock.set(9_000L);
        bridge.deliverOnMainThread("https://example.com/login");

        Assert.assertEquals(1, host.shownUrls.size());
    }

    @Test
    public void showsAgainAfterTheDebounceIntervalElapses() {
        RecordingHost host = new RecordingHost();
        BrowserPasskeyBridge bridge = bridge(host);

        bridge.deliverOnMainThread("https://example.com/login");
        mClock.set(11_000L);
        bridge.deliverOnMainThread("https://example.com/login");

        Assert.assertEquals(2, host.shownUrls.size());
    }

    @Test
    public void ignoresNullAndEmptyPageUrls() {
        RecordingHost host = new RecordingHost();
        BrowserPasskeyBridge bridge = bridge(host);

        bridge.onPasskeyCeremonyDetected(null);
        bridge.onPasskeyCeremonyDetected("");
        mShadowLooper.idle();

        Assert.assertTrue(host.shownUrls.isEmpty());
    }
}
