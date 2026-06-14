package com.termux.shared.interact;

import android.app.AlertDialog;

public final class DialogUtils {

    private DialogUtils() {}

    public static AlertDialog showDismissibleOnTouchOutside(AlertDialog.Builder builder) {
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        return dialog;
    }

}
