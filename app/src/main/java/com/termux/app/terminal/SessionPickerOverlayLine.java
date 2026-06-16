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
    private final String secondaryText;
    private final boolean highlighted;

    public SessionPickerOverlayLine(@NonNull Kind kind, @NonNull String text, boolean highlighted) {
        this(kind, text, "", highlighted);
    }

    public SessionPickerOverlayLine(@NonNull Kind kind, @NonNull String text, @NonNull String secondaryText,
                                    boolean highlighted) {
        this.kind = kind;
        this.text = text;
        this.secondaryText = secondaryText;
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

    @NonNull
    public String getSecondaryText() {
        return secondaryText;
    }

    public boolean isHighlighted() {
        return highlighted;
    }
}
