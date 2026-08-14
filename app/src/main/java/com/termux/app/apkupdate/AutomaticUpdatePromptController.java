package com.termux.app.apkupdate;

import androidx.annotation.NonNull;

public final class AutomaticUpdatePromptController {

    private final ApkUpdateFloatingIndicatorController indicatorController;
    private final ApkUpdateFloatingIndicatorController.UpdateTrigger updateTrigger;

    public AutomaticUpdatePromptController(
        @NonNull ApkUpdateFloatingIndicatorController indicatorController,
        @NonNull ApkUpdateFloatingIndicatorController.UpdateTrigger updateTrigger) {
        this.indicatorController = indicatorController;
        this.updateTrigger = updateTrigger;
    }

    public void onUpdateAvailable(@NonNull ApkUpdateAvailability availability) {
        AutomaticUpdatePromptPlanner.Outcome outcome = AutomaticUpdatePromptPlanner.plan(availability);
        if (outcome == AutomaticUpdatePromptPlanner.Outcome.SHOW_NOTHING) {
            indicatorController.onUpToDate();
            return;
        }
        indicatorController.onUpdateAvailable(availability);
        if (outcome == AutomaticUpdatePromptPlanner.Outcome.SHOW_THE_FLOATING_BUTTON_ONLY) {
            return;
        }
        updateTrigger.startUpdate(availability);
    }

    public void onUpToDate() {
        indicatorController.onUpToDate();
    }
}
