package com.termux.app.apkupdate;

import androidx.annotation.NonNull;

public final class AutomaticUpdatePromptController {

    public interface Dialog {
        void openUpdateDialog(@NonNull String latestVersionName, @NonNull Runnable onInstallChosen,
                              @NonNull Runnable onCancelled);
    }

    private final Dialog dialog;
    private final ApkUpdateFloatingIndicatorController indicatorController;
    private final DeclinedUpdateVersion declinedUpdateVersion;

    public AutomaticUpdatePromptController(@NonNull Dialog dialog,
                                           @NonNull ApkUpdateFloatingIndicatorController indicatorController,
                                           @NonNull DeclinedUpdateVersion declinedUpdateVersion) {
        this.dialog = dialog;
        this.indicatorController = indicatorController;
        this.declinedUpdateVersion = declinedUpdateVersion;
    }

    public void onUpdateAvailable(@NonNull ApkUpdateAvailability availability) {
        AutomaticUpdatePromptPlanner.Outcome outcome =
            AutomaticUpdatePromptPlanner.plan(availability, declinedUpdateVersion.recall());
        if (outcome == AutomaticUpdatePromptPlanner.Outcome.SHOW_NOTHING) {
            indicatorController.onUpToDate();
            return;
        }
        indicatorController.onUpdateAvailable(availability);
        if (outcome == AutomaticUpdatePromptPlanner.Outcome.SHOW_THE_FLOATING_BUTTON_ONLY) {
            return;
        }
        dialog.openUpdateDialog(availability.getLatestVersionName(),
            indicatorController::onIndicatorTapped,
            () -> declinedUpdateVersion.remember(availability.getLatestVersionName()));
    }

    public void onUpToDate() {
        indicatorController.onUpToDate();
    }
}
