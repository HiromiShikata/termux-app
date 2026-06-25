package com.termux.app.browser;

import android.app.AlertDialog;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;
import com.termux.shared.interact.DialogUtils;
import com.termux.shared.interact.ShareUtils;

public final class BrowserHeaderUrlMenuController {

    public interface Actions {
        void createSessionForUrl(@NonNull String url);
    }

    private final Context mContext;

    private final Actions mActions;

    public BrowserHeaderUrlMenuController(@NonNull Context context, @NonNull Actions actions) {
        this.mContext = context;
        this.mActions = actions;
    }

    public boolean showHeaderUrlMenu(@Nullable String url) {
        if (!BrowserHeaderUrlMenuEligibility.canShowMenuFor(url)) return false;
        showMenu(url);
        return true;
    }

    private void showMenu(@NonNull String url) {
        CharSequence[] actions = {
            mContext.getString(R.string.action_browser_copy_url),
            mContext.getString(R.string.action_browser_new_session),
            mContext.getString(R.string.action_browser_open_in_chrome)
        };
        DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(mContext)
            .setTitle(url)
            .setItems(actions, (dialog, which) -> {
                if (which == 0) {
                    copyUrl(url);
                } else if (which == 1) {
                    mActions.createSessionForUrl(url);
                } else {
                    ShareUtils.openUrlInChrome(mContext, url);
                }
            }));
    }

    private void copyUrl(@NonNull String url) {
        ShareUtils.copyTextToClipboard(mContext, url,
            mContext.getString(R.string.msg_browser_url_copied));
    }
}
