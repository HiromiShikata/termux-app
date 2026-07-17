package com.termux.app.apkupdate;

import android.app.Activity;

import androidx.annotation.NonNull;

public final class UpdateTagUpdateRunner implements UpdateTagUpdateController.ReasonTrigger {

    private final ApkUpdateUiController updateUiController;
    private final ApkUpdateFloatingIndicatorController.IndicatorView indicatorView;

    public UpdateTagUpdateRunner(@NonNull Activity activity,
                                 @NonNull ApkUpdateFloatingIndicatorController.IndicatorView indicatorView) {
        this(new ApkUpdateUiController(activity), indicatorView);
    }

    UpdateTagUpdateRunner(@NonNull ApkUpdateUiController updateUiController,
                          @NonNull ApkUpdateFloatingIndicatorController.IndicatorView indicatorView) {
        this.updateUiController = updateUiController;
        this.indicatorView = indicatorView;
    }

    @Override
    public void onUpdateRequested(String reason) {
        updateUiController.checkAndShowFloatingIndicator(indicatorView);
    }
}
