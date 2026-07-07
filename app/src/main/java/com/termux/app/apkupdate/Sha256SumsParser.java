package com.termux.app.apkupdate;

import androidx.annotation.Nullable;

import java.util.Locale;

public final class Sha256SumsParser {

    @Nullable
    public String findExpectedSha256(String sumsContent, String assetFileName) {
        if (sumsContent == null || assetFileName == null) {
            return null;
        }
        String abiSuffix = abiSuffixOf(assetFileName);
        if (abiSuffix == null) {
            return null;
        }
        for (String line : sumsContent.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int separatorIndex = firstWhitespaceIndex(trimmed);
            if (separatorIndex <= 0) {
                continue;
            }
            String hash = trimmed.substring(0, separatorIndex);
            String fileName = trimmed.substring(separatorIndex).trim();
            if (fileName.startsWith("*")) {
                fileName = fileName.substring(1);
            }
            if (fileName.endsWith(abiSuffix) && isHexSha256(hash)) {
                return hash.toLowerCase(Locale.ROOT);
            }
        }
        return null;
    }

    @Nullable
    private String abiSuffixOf(String assetFileName) {
        int underscoreIndex = assetFileName.lastIndexOf('_');
        if (underscoreIndex < 0) {
            return null;
        }
        return assetFileName.substring(underscoreIndex);
    }

    private int firstWhitespaceIndex(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) {
                return index;
            }
        }
        return -1;
    }

    private boolean isHexSha256(String value) {
        if (value.length() != 64) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            boolean isHexDigit = (character >= '0' && character <= '9')
                || (character >= 'a' && character <= 'f')
                || (character >= 'A' && character <= 'F');
            if (!isHexDigit) {
                return false;
            }
        }
        return true;
    }
}
