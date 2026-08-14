package com.termux.app.ownercall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class OwnerCallFilePath {

    private static final String CALL_DIRECTORY = "call-to-user";
    private static final String NO_PROJECT_DIRECTORY = "NA";
    private static final String FILE_EXTENSION = ".yaml";
    private static final char PATH_SEPARATOR = '/';
    private static final char PATH_SEPARATOR_REPLACEMENT = '_';

    private OwnerCallFilePath() {
    }

    @NonNull
    public static String of(@Nullable String projectCode, @NonNull String hostTmuxSessionName) {
        return CALL_DIRECTORY + PATH_SEPARATOR + projectDirectory(projectCode) + PATH_SEPARATOR
            + hostTmuxSessionName.replace(PATH_SEPARATOR, PATH_SEPARATOR_REPLACEMENT)
            + FILE_EXTENSION;
    }

    @NonNull
    private static String projectDirectory(@Nullable String projectCode) {
        if (projectCode == null || projectCode.trim().isEmpty()) {
            return NO_PROJECT_DIRECTORY;
        }
        return projectCode.trim();
    }
}
