package com.termux.app.browser;

import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;

public final class BrowserWebViewAutofill {

    private BrowserWebViewAutofill() {
    }

    public static boolean shouldEnable(int sdkInt) {
        return sdkInt >= Build.VERSION_CODES.O;
    }

    public static void apply(@NonNull View view, int sdkInt) {
        if (shouldEnable(sdkInt)) {
            view.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES);
        }
    }
}
