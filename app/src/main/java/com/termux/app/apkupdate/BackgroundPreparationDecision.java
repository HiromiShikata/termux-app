package com.termux.app.apkupdate;

import java.util.Optional;

public final class BackgroundPreparationDecision {

    private BackgroundPreparationDecision() {
    }

    public static boolean shouldRetry(Optional<ApkUpdateBackgroundPreparation.Outcome> outcome, boolean interrupted) {
        if (interrupted) {
            return true;
        }
        if (outcome.isPresent()) {
            return outcome.get().requestsRetry();
        }
        return false;
    }
}
