package com.termux.app.apkupdate;

public final class ApkUpdateFloatingIndicatorController {

    public interface IndicatorView {
        void showUpdateAvailable(String latestVersionName, Runnable onTapped);

        void hide();
    }

    public interface UpdateTrigger {
        void startUpdate(ApkUpdateAvailability availability);
    }

    private final IndicatorView indicatorView;
    private final UpdateTrigger updateTrigger;

    private ApkUpdateAvailability pendingUpdate;

    public ApkUpdateFloatingIndicatorController(IndicatorView indicatorView, UpdateTrigger updateTrigger) {
        this.indicatorView = indicatorView;
        this.updateTrigger = updateTrigger;
    }

    public void onUpdateAvailable(ApkUpdateAvailability availability) {
        if (!availability.isUpdateAvailable()) {
            onUpToDate();
            return;
        }
        pendingUpdate = availability;
        indicatorView.showUpdateAvailable(availability.getLatestVersionName(), this::onIndicatorTapped);
    }

    public void onUpToDate() {
        pendingUpdate = null;
        indicatorView.hide();
    }

    public boolean hasPendingUpdate() {
        return pendingUpdate != null;
    }

    public void onIndicatorTapped() {
        if (pendingUpdate == null) {
            return;
        }
        ApkUpdateAvailability availability = pendingUpdate;
        pendingUpdate = null;
        indicatorView.hide();
        updateTrigger.startUpdate(availability);
    }
}
