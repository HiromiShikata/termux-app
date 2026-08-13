package com.termux.app.diagnostics;

import android.view.View;

import androidx.annotation.NonNull;

import com.termux.app.TermuxActivity;

public final class ScrollbarViewCensusSnapshot {

    private ScrollbarViewCensusSnapshot() {
    }

    @NonNull
    public static ScrollbarViewCensus take(@NonNull TermuxActivity activity) {
        View decorView = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
        if (decorView == null) return ScrollbarViewCensus.empty();
        return ScrollbarViewCensus.take(new AndroidScrollbarViewNode(decorView));
    }
}
