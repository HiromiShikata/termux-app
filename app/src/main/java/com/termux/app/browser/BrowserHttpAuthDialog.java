package com.termux.app.browser;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;

public final class BrowserHttpAuthDialog {

    private BrowserHttpAuthDialog() {
    }

    public static void show(@NonNull Context context,
                            @NonNull HttpAuthHandler handler,
                            @Nullable String host,
                            @Nullable String realm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
            .setTitle(R.string.title_browser_http_auth);
        View dialogView = LayoutInflater.from(builder.getContext())
            .inflate(R.layout.dialog_browser_http_auth, null);
        builder.setView(dialogView);

        TextView promptView = dialogView.findViewById(R.id.browser_http_auth_prompt);
        promptView.setText(context.getString(R.string.msg_browser_http_auth_prompt,
            BrowserHttpAuthPrompt.describe(host, realm)));

        EditText usernameInput = dialogView.findViewById(R.id.browser_http_auth_username);
        EditText passwordInput = dialogView.findViewById(R.id.browser_http_auth_password);
        Button signInButton = dialogView.findViewById(R.id.browser_http_auth_sign_in);
        Button cancelButton = dialogView.findViewById(R.id.browser_http_auth_cancel);

        signInButton.setText(R.string.action_browser_http_auth_sign_in);
        cancelButton.setText(android.R.string.cancel);

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);

        DismissalGuard guard = new DismissalGuard(handler);
        dialog.setOnCancelListener(d -> guard.cancel());

        signInButton.setOnClickListener(view -> {
            BrowserHttpAuthRequest request = new BrowserHttpAuthRequest(
                usernameInput.getText().toString(),
                passwordInput.getText().toString());
            guard.proceed(request);
            dialog.dismiss();
        });
        cancelButton.setOnClickListener(view -> dialog.cancel());

        dialog.show();
    }

    private static final class DismissalGuard {

        private final HttpAuthHandler mHandler;

        private boolean mResolved;

        DismissalGuard(@NonNull HttpAuthHandler handler) {
            this.mHandler = handler;
        }

        void proceed(@NonNull BrowserHttpAuthRequest request) {
            if (mResolved) return;
            mResolved = true;
            mHandler.proceed(request.getUsername(), request.getPassword());
        }

        void cancel() {
            if (mResolved) return;
            mResolved = true;
            mHandler.cancel();
        }
    }
}
