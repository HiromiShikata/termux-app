package com.termux.app.browser;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;

import com.termux.R;
import com.termux.shared.logger.Logger;

public final class BrowserPdfDocumentActivity extends Activity {

    private static final String LOG_TAG = "BrowserPdfDocumentActivity";

    private BrowserPdfDocumentPages mPages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri document = getIntent() == null ? null : getIntent().getData();
        if (document == null) {
            finish();
            return;
        }
        try {
            mPages = new BrowserPdfDocumentPagesFromUri(getContentResolver(), document);
            setContentView(BrowserPdfDocumentPagesView.create(this, mPages,
                getResources().getDisplayMetrics().widthPixels));
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to display the downloaded document", e);
            Logger.showToast(this, getString(R.string.msg_browser_document_display_failed), true);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPages != null) {
            mPages.close();
            mPages = null;
        }
    }
}
