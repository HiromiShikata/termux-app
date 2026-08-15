package com.termux.app.diagnostics;

import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class ScrollbarViewCensusSnapshot {

    private ScrollbarViewCensusSnapshot() {
    }

    @NonNull
    public static ScrollbarViewCensus take() {
        return take(ActivityWindowRecorderHolder.getInstance().snapshotWindowRoots());
    }

    @NonNull
    public static ScrollbarViewCensus take(@NonNull ActivityWindowRoots windowRoots) {
        List<ScrollbarViewCensus.ViewNode> nodes = new ArrayList<>();
        for (View windowRoot : windowRoots.getReachableWindowRoots()) {
            nodes.add(new AndroidScrollbarViewNode(windowRoot));
            break;
        }
        return ScrollbarViewCensus.take(nodes, windowRoots.getNoLongerReachableCount());
    }
}
