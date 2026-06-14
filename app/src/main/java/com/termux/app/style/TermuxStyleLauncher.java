package com.termux.app.style;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.termux.R;
import com.termux.shared.activity.ActivityUtils;
import com.termux.shared.interact.DialogUtils;
import com.termux.shared.termux.TermuxConstants;

public final class TermuxStyleLauncher {

    private TermuxStyleLauncher() {}

    public static void showStylingDialog(@NonNull Activity activity) {
        Intent stylingIntent = new Intent();
        stylingIntent.setClassName(TermuxConstants.TERMUX_STYLING_PACKAGE_NAME, TermuxConstants.TERMUX_STYLING_APP.TERMUX_STYLING_ACTIVITY_NAME);
        try {
            activity.startActivity(stylingIntent);
        } catch (ActivityNotFoundException | IllegalArgumentException e) {
            DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(activity).setMessage(activity.getString(R.string.error_styling_not_installed))
                .setPositiveButton(R.string.action_styling_install,
                    (dialog, which) -> ActivityUtils.startActivity(activity, new Intent(Intent.ACTION_VIEW, Uri.parse(TermuxConstants.TERMUX_STYLING_FDROID_PACKAGE_URL))))
                .setNegativeButton(android.R.string.cancel, null));
        }
    }

}
