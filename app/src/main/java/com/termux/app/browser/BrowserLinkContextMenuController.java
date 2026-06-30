package com.termux.app.browser;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;
import com.termux.shared.interact.DialogUtils;
import com.termux.shared.interact.ShareUtils;

public final class BrowserLinkContextMenuController {

    public interface Actions {
        void openLinkInBrowser(@NonNull String linkUrl);

        void createSessionForLink(@NonNull String linkUrl);
    }

    private final Context mContext;

    private final WebView mWebView;

    private final Actions mActions;

    public BrowserLinkContextMenuController(@NonNull Context context, @NonNull WebView webView,
                                            @NonNull Actions actions) {
        this.mContext = context;
        this.mWebView = webView;
        this.mActions = actions;
    }

    @SuppressLint("ClickableViewAccessibility")
    public void attach() {
        mWebView.setOnLongClickListener(view -> onWebViewLongPress());
    }

    private boolean onWebViewLongPress() {
        WebView.HitTestResult hitTestResult = mWebView.getHitTestResult();
        if (hitTestResult == null) return false;
        int hitTestType = hitTestResult.getType();
        if (!BrowserLinkLongPress.isLinkHit(hitTestType)) return false;
        if (BrowserLinkLongPress.requiresHrefLookup(hitTestType)) {
            requestLinkHrefThenShowMenu(null);
            return true;
        }
        String linkUrl = hitTestResult.getExtra();
        if (!BrowserLinkLongPress.isCopyableLink(hitTestType, linkUrl)) return false;
        requestLinkHrefThenShowMenu(linkUrl);
        return true;
    }

    private void requestLinkHrefThenShowMenu(@Nullable String fallbackLinkUrl) {
        Handler hrefHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message message) {
                String linkUrl = message.getData().getString("url");
                String anchorText = message.getData().getString("title");
                if (!BrowserLinkLongPress.isOpenableLinkUrl(linkUrl)) {
                    linkUrl = fallbackLinkUrl;
                }
                if (BrowserLinkLongPress.isOpenableLinkUrl(linkUrl)) {
                    showLinkContextMenu(linkUrl, anchorText);
                }
            }
        };
        mWebView.requestFocusNodeHref(hrefHandler.obtainMessage());
    }

    void showLinkContextMenu(@NonNull String linkUrl, @Nullable String anchorText) {
        CharSequence[] actions = {
            mContext.getString(R.string.action_browser_copy_link_text),
            mContext.getString(R.string.action_browser_copy_link_url),
            mContext.getString(R.string.action_browser_open_link),
            mContext.getString(R.string.action_browser_open_in_chrome),
            mContext.getString(R.string.action_browser_create_session_for_link)
        };
        DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(mContext)
            .setTitle(linkUrl)
            .setItems(actions, (dialog, which) -> {
                if (which == 0) {
                    copyLinkText(linkUrl, anchorText);
                } else if (which == 1) {
                    copyLinkUrl(linkUrl);
                } else if (which == 2) {
                    mActions.openLinkInBrowser(linkUrl);
                } else if (which == 3) {
                    ShareUtils.openUrlInChrome(mContext, linkUrl);
                } else {
                    copyLinkUrl(linkUrl);
                    mActions.createSessionForLink(linkUrl);
                }
            }));
    }

    private void copyLinkText(@NonNull String linkUrl, @Nullable String anchorText) {
        ShareUtils.copyTextToClipboard(mContext,
            BrowserLinkLongPress.linkTextClipboardValue(linkUrl, anchorText),
            mContext.getString(R.string.msg_browser_link_text_copied));
    }

    private void copyLinkUrl(@NonNull String linkUrl) {
        ShareUtils.copyTextToClipboard(mContext, linkUrl,
            mContext.getString(R.string.msg_browser_url_copied));
    }
}
