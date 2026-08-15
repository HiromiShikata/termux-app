package com.termux.app.browser;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.termux.R;

public final class BrowserNewTabDialog {

    public interface UrlOpener {
        void openUrlInNewTab(@NonNull String url);
    }

    private BrowserNewTabDialog() {
    }

    @NonNull
    public static AlertDialog create(@NonNull Context context,
                                     @NonNull View dialogView,
                                     @NonNull BrowserClipboardUrlOffer clipboardUrlOffer,
                                     @NonNull UrlOpener urlOpener) {
        EditText urlInput = dialogView.findViewById(R.id.browser_new_tab_url_input);
        AlertDialog dialog = new AlertDialog.Builder(context)
            .setTitle(R.string.title_browser_new_tab)
            .setView(dialogView)
            .setPositiveButton(R.string.action_browser_new_tab_open, (d, which) -> {
                String typed = urlInput.getText().toString().trim();
                if (!typed.isEmpty()) urlOpener.openUrlInNewTab(typed);
            })
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        dialog.setCanceledOnTouchOutside(true);

        if (clipboardUrlOffer.isOffered()) {
            Button clipboardUrlButton = dialogView.findViewById(R.id.browser_new_tab_clipboard_url_button);
            clipboardUrlButton.setText(context.getString(
                R.string.action_browser_new_tab_open_clipboard_url, clipboardUrlOffer.getUrl()));
            clipboardUrlButton.setVisibility(View.VISIBLE);
            clipboardUrlButton.setOnClickListener(v -> {
                dialog.dismiss();
                urlOpener.openUrlInNewTab(clipboardUrlOffer.getUrl());
            });
        }
        return dialog;
    }
}
