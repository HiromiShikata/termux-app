package com.termux.app.apkupdate;

import android.app.Activity;
import android.app.AlertDialog;

import androidx.annotation.NonNull;

import com.termux.R;
import com.termux.shared.interact.DialogUtils;

public final class AlertDialogAutomaticUpdatePrompt implements AutomaticUpdatePromptController.Dialog {

    private final Activity activity;

    public AlertDialogAutomaticUpdatePrompt(@NonNull Activity activity) {
        this.activity = activity;
    }

    @Override
    public void openUpdateDialog(@NonNull String latestVersionName, @NonNull Runnable onInstallChosen,
                                 @NonNull Runnable onCancelled) {
        if (activity.isFinishing()) {
            return;
        }
        DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(activity)
            .setTitle(R.string.apk_update_dialog_title)
            .setMessage(activity.getString(R.string.apk_update_dialog_message, latestVersionName))
            .setPositiveButton(R.string.apk_update_dialog_install, (dialog, which) -> onInstallChosen.run())
            .setNegativeButton(R.string.apk_update_dialog_later, (dialog, which) -> onCancelled.run())
            .setOnCancelListener(dialog -> onCancelled.run()));
    }
}
