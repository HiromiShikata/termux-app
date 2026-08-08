package com.termux.app.browser;

import androidx.annotation.Nullable;

public final class BrowserDownloadedDocumentDisplay {

    public static final String PDF_MEDIA_TYPE = "application/pdf";

    private static final String PDF_FILE_NAME_SUFFIX = ".pdf";

    private BrowserDownloadedDocumentDisplay() {
    }

    public static boolean displaysAsDocument(@Nullable String mediaType) {
        if (mediaType == null) return false;
        int parameterSeparator = mediaType.indexOf(';');
        String bareMediaType = parameterSeparator < 0 ? mediaType : mediaType.substring(0, parameterSeparator);
        return PDF_MEDIA_TYPE.equalsIgnoreCase(bareMediaType.trim());
    }

    public static boolean displaysAsDocument(@Nullable String mediaType, @Nullable String fileName) {
        return displaysAsDocument(mediaType);
    }
}
