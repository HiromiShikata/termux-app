package com.termux.app.apkupdate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.widget.Toast;

import com.termux.R;
import com.termux.shared.logger.Logger;

/**
 * Receives {@link PackageInstaller} session commit status callbacks for in-app installs. When the
 * system needs the user to confirm the install it forwards the confirmation intent; otherwise it
 * reports success or the failure reason to the user.
 */
public final class ApkInstallStatusReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "ApkInstallStatusReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            launchUserConfirmation(context, intent);
            return;
        }
        if (status == PackageInstaller.STATUS_SUCCESS) {
            showToast(context, context.getString(R.string.apk_revert_install_success));
            return;
        }
        String statusMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        Logger.logError(LOG_TAG, "APK install failed (status " + status + "): " + statusMessage);
        showToast(context, context.getString(R.string.apk_revert_install_failed,
            statusMessage != null ? statusMessage : String.valueOf(status)));
    }

    private void launchUserConfirmation(Context context, Intent intent) {
        Intent confirmationIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        if (confirmationIntent == null) {
            Logger.logError(LOG_TAG, "Install status was pending user action but no confirmation intent was provided");
            return;
        }
        confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.getApplicationContext().startActivity(confirmationIntent);
    }

    private void showToast(Context context, String message) {
        Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
