package com.termux.app.browser;

import android.util.Base64;

import androidx.annotation.NonNull;

public final class BrowserScreenshotCapture {

    private static final String HEREDOC_DELIMITER = "TERMUX_BROWSER_SCREENSHOT_B64";

    @NonNull
    public static String buildDeliveryCommand(@NonNull byte[] pngBytes,
                                              @NonNull String relativePngFileName) {
        String base64Lines = Base64.encodeToString(pngBytes, Base64.DEFAULT).trim();
        return "base64 -d > " + relativePngFileName + " <<'" + HEREDOC_DELIMITER + "'\n"
            + base64Lines + "\n"
            + HEREDOC_DELIMITER + "\n";
    }

    private BrowserScreenshotCapture() {
    }
}
