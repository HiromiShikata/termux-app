package com.termux.app.browser;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
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
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
            .setTitle(R.string.title_browser_new_tab)
            .setView(dialogView)
            .setPositiveButton(R.string.action_browser_new_tab_open, (d, which) -> {
                String typed = urlInput.getText().toString().trim();
                if (!typed.isEmpty()) urlOpener.openUrlInNewTab(typed);
            })
            .setNegativeButton(android.R.string.cancel, null);

        if (clipboardUrlOffer.isOffered()) {
            builder.setNeutralButton(
                context.getString(R.string.action_browser_new_tab_open_clipboard_url,
                    clipboardUrlOffer.getHost()),
                (d, which) -> urlOpener.openUrlInNewTab(clipboardUrlOffer.getUrl()));
        }

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }
}
