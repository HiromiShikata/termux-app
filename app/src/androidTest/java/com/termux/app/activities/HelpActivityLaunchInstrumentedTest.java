package com.termux.app.activities;

import static org.junit.Assert.assertNotNull;

import android.webkit.WebView;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class HelpActivityLaunchInstrumentedTest {

    @Test
    public void launchInflatesWebViewWithoutCrashing() {
        ActivityScenario<HelpActivity> scenario = ActivityScenario.launch(HelpActivity.class);
        scenario.moveToState(Lifecycle.State.RESUMED);
        scenario.onActivity(activity -> {
            assertNotNull(activity);
            assertNotNull(activity.mWebView);
        });
    }

    @Test
    public void webViewIsInstantiatedAsAndroidWebView() {
        ActivityScenario<HelpActivity> scenario = ActivityScenario.launch(HelpActivity.class);
        scenario.onActivity(activity -> {
            WebView webView = activity.mWebView;
            assertNotNull(webView);
        });
    }
}
