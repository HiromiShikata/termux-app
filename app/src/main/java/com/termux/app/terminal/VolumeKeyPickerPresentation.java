package com.termux.app.terminal;

import androidx.annotation.NonNull;

public final class VolumeKeyPickerPresentation {

    public interface SessionSwitcher {
        void switchToHighlightedSession();
    }

    public interface OverlayRenderer {
        void renderStructure();
    }

    public interface OverlayPresenter {
        void showOverlay();
    }

    public interface HideScheduler {
        void scheduleHide();
    }

    public interface CommitScheduler {
        void scheduleCommit();
    }

    private VolumeKeyPickerPresentation() {
    }

    public static void present(@NonNull VolumeKeyPickerMoveDecision decision,
                               @NonNull SessionSwitcher sessionSwitcher,
                               @NonNull OverlayRenderer overlayRenderer,
                               @NonNull OverlayPresenter overlayPresenter,
                               @NonNull HideScheduler hideScheduler,
                               @NonNull CommitScheduler commitScheduler) {
        if (decision.shouldSwitchImmediately()) {
            sessionSwitcher.switchToHighlightedSession();
            overlayRenderer.renderStructure();
            overlayPresenter.showOverlay();
            hideScheduler.scheduleHide();
        } else {
            overlayRenderer.renderStructure();
            overlayPresenter.showOverlay();
            commitScheduler.scheduleCommit();
        }
    }
}
