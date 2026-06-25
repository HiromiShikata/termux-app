package com.termux.app.terminal;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

public final class PendingCallToUserFooterBinder {

    private PendingCallToUserFooterBinder() {
    }

    public static void bind(@NonNull View barView,
                            @NonNull TextView reportTextView,
                            @NonNull ImageButton scrollButton,
                            @NonNull PendingCallToUserFooterDecision decision,
                            @NonNull Runnable scrollAction) {
        if (!decision.isVisible()) {
            barView.setVisibility(View.GONE);
            reportTextView.setText("");
            scrollButton.setOnClickListener(null);
            scrollButton.setClickable(false);
            return;
        }
        reportTextView.setText(decision.getReportText());
        barView.setVisibility(View.VISIBLE);
        scrollButton.setOnClickListener(view -> scrollAction.run());
    }
}
