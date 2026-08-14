package com.termux.app.apkupdate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class AutomaticUpdatePromptPlanner {

    public enum Outcome {
        START_THE_SYSTEM_INSTALLER,
        SHOW_THE_FLOATING_BUTTON_ONLY,
        SHOW_NOTHING
    }

    private AutomaticUpdatePromptPlanner() {
    }

    @NonNull
    public static Outcome plan(@Nullable ApkUpdateAvailability availability) {
        if (availability == null || !availability.isUpdateAvailable()) {
            return Outcome.SHOW_NOTHING;
        }
        if (!availability.hasDownloadedFilePath()) {
            return Outcome.SHOW_THE_FLOATING_BUTTON_ONLY;
        }
        return Outcome.START_THE_SYSTEM_INSTALLER;
    }
}
