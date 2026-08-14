package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SessionShortcutRows {

    @NonNull
    private final List<SessionShortcut> mAlwaysSessionShortcuts;

    @NonNull
    private final List<SessionShortcut> mProjectManagerSessionShortcuts;

    public SessionShortcutRows(@NonNull List<SessionShortcut> alwaysSessionShortcuts,
                               @NonNull List<SessionShortcut> projectManagerSessionShortcuts) {
        mAlwaysSessionShortcuts =
            Collections.unmodifiableList(new ArrayList<>(alwaysSessionShortcuts));
        mProjectManagerSessionShortcuts =
            Collections.unmodifiableList(new ArrayList<>(projectManagerSessionShortcuts));
    }

    @NonNull
    public List<SessionShortcut> getAlwaysSessionShortcuts() {
        return mAlwaysSessionShortcuts;
    }

    @NonNull
    public List<SessionShortcut> getProjectManagerSessionShortcuts() {
        return mProjectManagerSessionShortcuts;
    }
}
