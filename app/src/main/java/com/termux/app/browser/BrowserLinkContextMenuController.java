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
import com.termux.app.link.GoogleAppLink;
import com.termux.shared.interact.DialogUtils;
import com.termux.shared.interact.ShareUtils;

import java.util.ArrayList;
import java.util.List;

public final class BrowserLinkContextMenuController {

    public interface Actions {
        void openLinkInBrowser(@NonNull String linkUrl);

        void createSessionForLink(@NonNull String linkUrl);
    }

    private final Context mContext;

    private final WebView mWebView;

    private final Actions mActions;

    private final BrowserWebViewCallbackGuard mCallbackGuard =
        new BrowserWebViewCallbackGuard("BrowserLinkContextMenuController");

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
                mCallbackGuard.run("requestFocusNodeHref", () -> {
                    String linkUrl = message.getData().getString("url");
                    String anchorText = message.getData().getString("title");
                    if (!BrowserLinkLongPress.isOpenableLinkUrl(linkUrl)) {
                        linkUrl = fallbackLinkUrl;
                    }
                    if (BrowserLinkLongPress.isOpenableLinkUrl(linkUrl)) {
                        showLinkContextMenu(linkUrl, anchorText);
                    }
                });
            }
        };
        mWebView.requestFocusNodeHref(hrefHandler.obtainMessage());
    }

    void showLinkContextMenu(@NonNull String linkUrl, @Nullable String anchorText) {
        GoogleAppLink.GoogleAppTarget googleAppTarget =
            GoogleAppLink.resolveTarget(linkUrl);

        List<CharSequence> actions = new ArrayList<>();
        actions.add(mContext.getString(R.string.action_browser_copy_link_text));
        actions.add(mContext.getString(R.string.action_browser_copy_link_url));
        actions.add(mContext.getString(R.string.action_browser_open_link));
        if (googleAppTarget != null) {
            actions.add(mContext.getString(R.string.action_browser_open_in_google_app,
                googleAppTarget.getAppDisplayName()));
        }
        actions.add(mContext.getString(R.string.action_browser_open_in_chrome));
        actions.add(mContext.getString(R.string.action_browser_create_session_for_link));

        CharSequence[] actionItems = actions.toArray(new CharSequence[0]);
        DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(mContext)
            .setTitle(linkUrl)
            .setItems(actionItems, (dialog, which) -> {
                CharSequence selected = actionItems[which];
                if (selected.equals(mContext.getString(R.string.action_browser_copy_link_text))) {
                    copyLinkText(linkUrl, anchorText);
                } else if (selected.equals(mContext.getString(R.string.action_browser_copy_link_url))) {
                    copyLinkUrl(linkUrl);
                } else if (selected.equals(mContext.getString(R.string.action_browser_open_link))) {
                    mActions.openLinkInBrowser(linkUrl);
                } else if (googleAppTarget != null && selected.equals(
                    mContext.getString(R.string.action_browser_open_in_google_app,
                        googleAppTarget.getAppDisplayName()))) {
                    openLinkInGoogleApp(linkUrl, googleAppTarget);
                } else if (selected.equals(mContext.getString(R.string.action_browser_open_in_chrome))) {
                    ShareUtils.openUrlInChrome(mContext, linkUrl);
                } else {
                    copyLinkUrl(linkUrl);
                    mActions.createSessionForLink(linkUrl);
                }
            }));
    }

    private void openLinkInGoogleApp(@NonNull String linkUrl,
                                     @NonNull GoogleAppLink.GoogleAppTarget googleAppTarget) {
        if (!GoogleAppLink.openInGoogleApp(mContext, linkUrl, googleAppTarget)) {
            mActions.openLinkInBrowser(linkUrl);
        }
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
