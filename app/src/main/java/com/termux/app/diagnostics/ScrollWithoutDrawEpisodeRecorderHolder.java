package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class ScrollWithoutDrawEpisodeRecorderHolder {

    private static final ScrollWithoutDrawEpisodeRecorder INSTANCE =
        new ScrollWithoutDrawEpisodeRecorder();

    private ScrollWithoutDrawEpisodeRecorderHolder() {
    }

    @NonNull
    public static ScrollWithoutDrawEpisodeRecorder getInstance() {
        return INSTANCE;
    }
}
