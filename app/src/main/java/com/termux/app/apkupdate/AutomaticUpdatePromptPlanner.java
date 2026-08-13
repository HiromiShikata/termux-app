package com.termux.app.apkupdate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class AutomaticUpdatePromptPlanner {

    public enum Outcome {
        OPEN_THE_DIALOG,
        SHOW_THE_FLOATING_BUTTON_ONLY,
        SHOW_NOTHING
    }

    private AutomaticUpdatePromptPlanner() {
    }

    @NonNull
    public static Outcome plan(@Nullable ApkUpdateAvailability availability,
                               @Nullable String versionNameAlreadyDeclined) {
        if (availability == null || !availability.isUpdateAvailable()) {
            return Outcome.SHOW_NOTHING;
        }
        if (availability.getLatestVersionName().equals(versionNameAlreadyDeclined)) {
            return Outcome.SHOW_THE_FLOATING_BUTTON_ONLY;
        }
        return Outcome.OPEN_THE_DIALOG;
    }
}
