package com.termux.app.browser;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.webkit.WebView;

import androidx.annotation.NonNull;

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
            requestLinkHrefThenShowMenu();
            return true;
        }
        String linkUrl = hitTestResult.getExtra();
        if (!BrowserLinkLongPress.isCopyableLink(hitTestType, linkUrl)) return false;
        showLinkContextMenu(linkUrl);
        return true;
    }

    private void requestLinkHrefThenShowMenu() {
        Handler hrefHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message message) {
                String linkUrl = message.getData().getString("url");
                if (BrowserLinkLongPress.isOpenableLinkUrl(linkUrl)) {
                    showLinkContextMenu(linkUrl);
                }
            }
        };
        mWebView.requestFocusNodeHref(hrefHandler.obtainMessage());
    }

    void showLinkContextMenu(@NonNull String linkUrl) {
        CharSequence[] actions = {
            mContext.getString(R.string.action_browser_copy_link_url),
            mContext.getString(R.string.action_browser_open_link),
            mContext.getString(R.string.action_browser_open_in_chrome),
            mContext.getString(R.string.action_browser_create_session_for_link)
        };
        DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(mContext)
            .setTitle(linkUrl)
            .setItems(actions, (dialog, which) -> {
                if (which == 0) {
                    copyLinkUrl(linkUrl);
                } else if (which == 1) {
                    mActions.openLinkInBrowser(linkUrl);
                } else if (which == 2) {
                    ShareUtils.openUrlInChrome(mContext, linkUrl);
                } else {
                    copyLinkUrl(linkUrl);
                    mActions.createSessionForLink(linkUrl);
                }
            }));
    }

    private void copyLinkUrl(@NonNull String linkUrl) {
        ShareUtils.copyTextToClipboard(mContext, linkUrl,
            mContext.getString(R.string.msg_browser_url_copied));
    }
}
