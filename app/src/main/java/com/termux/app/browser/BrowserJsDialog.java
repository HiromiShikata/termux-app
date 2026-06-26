package com.termux.app.browser;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;

public final class BrowserJsDialog {

    private BrowserJsDialog() {
    }

    public static void showAlert(@NonNull Context context,
                                 @Nullable String message,
                                 @NonNull JsResult result) {
        JsResultGuard guard = new JsResultGuard(result);
        AlertDialog dialog = new AlertDialog.Builder(context)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, (d, which) -> guard.confirm())
            .setOnCancelListener(d -> guard.cancel())
            .create();
        dialog.show();
    }

    public static void showConfirm(@NonNull Context context,
                                   @Nullable String message,
                                   @NonNull JsResult result) {
        JsResultGuard guard = new JsResultGuard(result);
        AlertDialog dialog = new AlertDialog.Builder(context)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, (d, which) -> guard.confirm())
            .setNegativeButton(android.R.string.cancel, (d, which) -> guard.cancel())
            .setOnCancelListener(d -> guard.cancel())
            .create();
        dialog.show();
    }

    public static void showPrompt(@NonNull Context context,
                                  @Nullable String message,
                                  @Nullable String defaultValue,
                                  @NonNull JsPromptResult result) {
        JsPromptResultGuard guard = new JsPromptResultGuard(result);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_browser_js_prompt, null);
        TextView messageView = dialogView.findViewById(R.id.browser_js_prompt_message);
        messageView.setText(message);
        EditText valueInput = dialogView.findViewById(R.id.browser_js_prompt_value);
        valueInput.setText(defaultValue);
        AlertDialog dialog = new AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok, (d, which) ->
                guard.confirm(valueInput.getText().toString()))
            .setNegativeButton(android.R.string.cancel, (d, which) -> guard.cancel())
            .setOnCancelListener(d -> guard.cancel())
            .create();
        dialog.show();
    }

    private static final class JsResultGuard {

        private final JsResult mResult;

        private boolean mResolved;

        JsResultGuard(@NonNull JsResult result) {
            this.mResult = result;
        }

        void confirm() {
            if (mResolved) return;
            mResolved = true;
            mResult.confirm();
        }

        void cancel() {
            if (mResolved) return;
            mResolved = true;
            mResult.cancel();
        }
    }

    private static final class JsPromptResultGuard {

        private final JsPromptResult mResult;

        private boolean mResolved;

        JsPromptResultGuard(@NonNull JsPromptResult result) {
            this.mResult = result;
        }

        void confirm(@NonNull String value) {
            if (mResolved) return;
            mResolved = true;
            mResult.confirm(value);
        }

        void cancel() {
            if (mResolved) return;
            mResolved = true;
            mResult.cancel();
        }
    }
}
