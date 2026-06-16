package com.termux.app.terminal;

import androidx.annotation.NonNull;

public final class SessionPickerOverlayLine {

    public enum Kind {
        PROJECT,
        STORY,
        SESSION
    }

    private final Kind kind;
    private final String text;
    private final boolean highlighted;

    public SessionPickerOverlayLine(@NonNull Kind kind, @NonNull String text, boolean highlighted) {
        this.kind = kind;
        this.text = text;
        this.highlighted = highlighted;
    }

    @NonNull
    public Kind getKind() {
        return kind;
    }

    @NonNull
    public String getText() {
        return text;
    }

    public boolean isHighlighted() {
        return highlighted;
    }
}
