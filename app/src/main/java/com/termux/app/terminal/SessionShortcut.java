package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.Objects;

public final class SessionShortcut {

    private final String label;
    private final String targetSessionName;

    public SessionShortcut(@NonNull String label, @NonNull String targetSessionName) {
        this.label = label;
        this.targetSessionName = targetSessionName;
    }

    @NonNull
    public String getLabel() {
        return label;
    }

    @NonNull
    public String getTargetSessionName() {
        return targetSessionName;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SessionShortcut)) {
            return false;
        }
        SessionShortcut otherShortcut = (SessionShortcut) other;
        return label.equals(otherShortcut.label)
            && targetSessionName.equals(otherShortcut.targetSessionName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, targetSessionName);
    }

    @NonNull
    @Override
    public String toString() {
        return "SessionShortcut{label=" + label + ", targetSessionName=" + targetSessionName + "}";
    }
}
