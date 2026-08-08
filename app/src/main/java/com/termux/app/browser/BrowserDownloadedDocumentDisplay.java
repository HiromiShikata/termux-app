package com.termux.app.browser;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class BrowserDownloadedDocumentDisplay {

    public static final String PDF_MEDIA_TYPE = "application/pdf";

    private static final String PDF_FILE_NAME_SUFFIX = ".pdf";

    private static final List<String> MEDIA_TYPES_THAT_NAME_NO_FORMAT = Arrays.asList(
        "", "*/*", "application/octet-stream", "application/binary", "binary/octet-stream",
        "application/download", "application/force-download", "application/x-download");

    private BrowserDownloadedDocumentDisplay() {
    }

    public static boolean displaysAsDocument(@Nullable String mediaType) {
        return PDF_MEDIA_TYPE.equals(bareMediaType(mediaType));
    }

    public static boolean displaysAsDocument(@Nullable String mediaType, @Nullable String fileName) {
        if (displaysAsDocument(mediaType)) return true;
        if (!MEDIA_TYPES_THAT_NAME_NO_FORMAT.contains(bareMediaType(mediaType))) return false;
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(PDF_FILE_NAME_SUFFIX);
    }

    private static String bareMediaType(@Nullable String mediaType) {
        if (mediaType == null) return "";
        int parameterSeparator = mediaType.indexOf(';');
        String withoutParameters = parameterSeparator < 0 ? mediaType : mediaType.substring(0, parameterSeparator);
        return withoutParameters.trim().toLowerCase(Locale.ROOT);
    }
}
