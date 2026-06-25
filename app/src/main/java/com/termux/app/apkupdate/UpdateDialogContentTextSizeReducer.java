package com.termux.app.apkupdate;

import android.app.AlertDialog;
import android.util.TypedValue;
import android.widget.TextView;

public final class UpdateDialogContentTextSizeReducer {

    static final float HALF_SCALE = 0.5f;

    private UpdateDialogContentTextSizeReducer() {}

    public static void reduceContentTextToHalf(AlertDialog dialog) {
        if (dialog == null) return;
        TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView == null) return;
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_PX, messageView.getTextSize() * HALF_SCALE);
    }
}
