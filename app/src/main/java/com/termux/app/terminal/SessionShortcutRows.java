package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SessionShortcutRows {

    @NonNull
    private final List<SessionShortcut> mAlwaysSessionShortcuts;

    public SessionShortcutRows(@NonNull List<SessionShortcut> alwaysSessionShortcuts) {
        mAlwaysSessionShortcuts =
            Collections.unmodifiableList(new ArrayList<>(alwaysSessionShortcuts));
    }

    @NonNull
    public List<SessionShortcut> getAlwaysSessionShortcuts() {
        return mAlwaysSessionShortcuts;
    }
}
