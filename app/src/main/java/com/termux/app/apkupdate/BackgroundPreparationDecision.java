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

    public static boolean shouldRetryRun(BackgroundDownloadRun.Status status,
                                         Optional<ApkUpdateBackgroundPreparation.Outcome> outcome,
                                         boolean interrupted) {
        if (status == BackgroundDownloadRun.Status.SLOT_LOST) {
            return true;
        }
        return shouldRetry(outcome, interrupted);
    }
}
