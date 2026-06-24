package com.termux.app.browser;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.logger.Logger;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public final class BrowserBulkOpenController {

    private static final String LOG_TAG = "BrowserBulkOpenController";

    private final TermuxActivity mActivity;

    public BrowserBulkOpenController(@NonNull TermuxActivity activity) {
        this.mActivity = activity;
    }

    public void openDisplayedTaskUrls(@NonNull WebView webView, int limit) {
        webView.evaluateJavascript(BrowserGithubTaskUrls.COLLECT_SCRIPT, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String collectedUrlsJson) {
                List<String> displayedUrls;
                try {
                    displayedUrls = BrowserGithubTaskUrls.parseCollectedUrls(collectedUrlsJson);
                } catch (JSONException e) {
                    Logger.logStackTraceWithMessage(LOG_TAG, "Failed to parse collected task URLs", e);
                    return;
                }
                openUrlsInExternalBrowser(BrowserGithubTaskUrls.selectForBulkOpen(displayedUrls, limit));
            }
        });
    }

    private void openUrlsInExternalBrowser(@NonNull List<String> urls) {
        if (urls.isEmpty()) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_no_task_urls_found), false);
            return;
        }
        for (Intent viewIntent : buildViewIntents(urls)) {
            try {
                mActivity.startActivity(viewIntent);
            } catch (ActivityNotFoundException e) {
                Logger.logStackTraceWithMessage(LOG_TAG,
                    "No external browser found to open " + viewIntent.getDataString(), e);
            }
        }
    }

    @NonNull
    public static List<Intent> buildViewIntents(@NonNull List<String> urls) {
        List<Intent> intents = new ArrayList<>();
        for (String url : urls) {
            Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            intents.add(viewIntent);
        }
        return intents;
    }
}
