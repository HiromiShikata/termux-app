package com.termux.app.browser;

import android.net.Uri;
import android.webkit.WebResourceRequest;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BrowserNewWindowUrlProbeWebViewClientTest {

    private static final class RecordingListener
        implements BrowserNewWindowUrlProbeWebViewClient.NewWindowUrlListener {

        final List<String> openedUrls = new ArrayList<>();

        @Override
        public boolean openNewTabForUrl(@NonNull String url) {
            openedUrls.add(url);
            return true;
        }
    }

    private static WebResourceRequest requestFor(String url) {
        return new WebResourceRequest() {
            @Override
            public Uri getUrl() {
                return Uri.parse(url);
            }

            @Override
            public boolean isForMainFrame() {
                return true;
            }

            @Override
            public boolean isRedirect() {
                return false;
            }

            @Override
            public boolean hasGesture() {
                return true;
            }

            @Override
            public String getMethod() {
                return "GET";
            }

            @Override
            public java.util.Map<String, String> getRequestHeaders() {
                return new java.util.HashMap<>();
            }
        };
    }

    @Test
    public void forwardsTheRequestedNewWindowUrlToTheListenerOnce() {
        RecordingListener listener = new RecordingListener();
        BrowserNewWindowUrlProbeWebViewClient client =
            new BrowserNewWindowUrlProbeWebViewClient(listener);

        boolean handled = client.shouldOverrideUrlLoading(null, requestFor("https://opened.example/page"));

        Assert.assertTrue(handled);
        Assert.assertEquals(1, listener.openedUrls.size());
        Assert.assertEquals("https://opened.example/page", listener.openedUrls.get(0));
    }

    @Test
    public void deliversTheNewWindowUrlOnlyOnceEvenWhenSeveralCallbacksFire() {
        RecordingListener listener = new RecordingListener();
        BrowserNewWindowUrlProbeWebViewClient client =
            new BrowserNewWindowUrlProbeWebViewClient(listener);

        client.shouldOverrideUrlLoading(null, requestFor("https://first.example/"));
        client.onPageStarted(null, "https://second.example/", null);
        client.shouldOverrideUrlLoading(null, requestFor("https://third.example/"));

        Assert.assertEquals(1, listener.openedUrls.size());
        Assert.assertEquals("https://first.example/", listener.openedUrls.get(0));
    }

    @Test
    public void doesNotForwardBlankPlaceholderUrls() {
        RecordingListener listener = new RecordingListener();
        BrowserNewWindowUrlProbeWebViewClient client =
            new BrowserNewWindowUrlProbeWebViewClient(listener);

        boolean handled = client.shouldOverrideUrlLoading(null, requestFor("about:blank"));

        Assert.assertTrue(handled);
        Assert.assertTrue(listener.openedUrls.isEmpty());
    }
}
