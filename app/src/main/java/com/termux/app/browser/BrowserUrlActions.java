package com.termux.app.browser;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Selection;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;
import com.termux.shared.interact.ShareUtils;

public final class BrowserUrlActions {

    public interface Host {
        @Nullable
        String currentPageUrl();

        void showNoCurrentUrlMessage();

        void navigateToUrl(@NonNull String editedUrl);

        void createSessionForUrl(@NonNull String editedUrl);

        void addCurrentPageBookmark();

        void showBookmarksList();
    }

    private final Context mContext;

    private final Host mHost;

    public BrowserUrlActions(@NonNull Context context, @NonNull Host host) {
        this.mContext = context;
        this.mHost = host;
    }

    public void promptEditCurrentPageUrl() {
        String currentUrl = mHost.currentPageUrl();
        if (currentUrl == null) {
            mHost.showNoCurrentUrlMessage();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
            .setTitle(R.string.title_browser_edit_url);
        View dialogView = LayoutInflater.from(builder.getContext())
            .inflate(R.layout.dialog_browser_edit_url, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);

        EditText urlInput = dialogView.findViewById(R.id.browser_edit_url_input);
        urlInput.setText(currentUrl);
        Selection.setSelection(urlInput.getText(), currentUrl.length());

        Button goButton = dialogView.findViewById(R.id.browser_edit_url_go);
        Button copyButton = dialogView.findViewById(R.id.browser_edit_url_copy);
        Button createSessionButton = dialogView.findViewById(R.id.browser_edit_url_create_session);
        Button cancelButton = dialogView.findViewById(R.id.browser_edit_url_cancel);
        Button addBookmarkButton = dialogView.findViewById(R.id.browser_edit_url_add_bookmark);
        Button bookmarksButton = dialogView.findViewById(R.id.browser_edit_url_bookmarks);
        Button openInChromeButton = dialogView.findViewById(R.id.browser_edit_url_open_in_chrome);

        goButton.setText(R.string.action_browser_edit_url_confirm);
        copyButton.setText(R.string.action_browser_edit_url_copy);
        createSessionButton.setText(R.string.action_browser_edit_url_create_session);
        cancelButton.setText(android.R.string.cancel);
        addBookmarkButton.setText(R.string.action_browser_edit_url_add_bookmark);
        bookmarksButton.setText(R.string.action_browser_edit_url_bookmarks);
        openInChromeButton.setText(R.string.action_browser_open_in_chrome);

        goButton.setOnClickListener(view -> {
            mHost.navigateToUrl(urlInput.getText().toString());
            dialog.dismiss();
        });
        copyButton.setOnClickListener(view -> copyEditedUrlToClipboard(urlInput.getText().toString()));
        createSessionButton.setOnClickListener(view -> {
            mHost.createSessionForUrl(urlInput.getText().toString());
            dialog.dismiss();
        });
        addBookmarkButton.setOnClickListener(view -> mHost.addCurrentPageBookmark());
        bookmarksButton.setOnClickListener(view -> {
            dialog.dismiss();
            mHost.showBookmarksList();
        });
        openInChromeButton.setOnClickListener(view -> {
            openEditedUrlInChrome(urlInput.getText().toString());
            dialog.dismiss();
        });
        cancelButton.setOnClickListener(view -> dialog.dismiss());

        urlInput.setOnEditorActionListener((view, actionId, event) -> {
            mHost.navigateToUrl(urlInput.getText().toString());
            dialog.dismiss();
            return true;
        });

        dialog.show();
    }

    public void copyEditedUrlToClipboard(@Nullable String editedUrl) {
        String url = BrowserEditedUrl.trimmedOrNull(editedUrl);
        if (url == null) {
            mHost.showNoCurrentUrlMessage();
            return;
        }
        ShareUtils.copyTextToClipboard(mContext, url,
            mContext.getString(R.string.msg_browser_url_copied));
    }

    public void openEditedUrlInChrome(@Nullable String editedUrl) {
        String url = BrowserEditedUrl.trimmedOrNull(editedUrl);
        if (url == null) {
            mHost.showNoCurrentUrlMessage();
            return;
        }
        ShareUtils.openUrlInChrome(mContext, url);
    }
}
