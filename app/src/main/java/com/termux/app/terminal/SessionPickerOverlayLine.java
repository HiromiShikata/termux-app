package com.termux.app.terminal;

import androidx.annotation.NonNull;

public final class SessionPickerOverlayLine {

    public enum Kind {
        PROJECT,
        STORY,
        SESSION,
        SPACER
    }

    private final Kind kind;
    private final String text;
    private final String secondaryText;
    private final boolean highlighted;
    private final boolean current;
    private final boolean marked;
    private final String newActivityLabel;

    public SessionPickerOverlayLine(@NonNull Kind kind, @NonNull String text, boolean highlighted) {
        this(kind, text, "", highlighted, false, false, "");
    }

    public SessionPickerOverlayLine(@NonNull Kind kind, @NonNull String text, @NonNull String secondaryText,
                                    boolean highlighted, boolean marked, @NonNull String newActivityLabel) {
        this(kind, text, secondaryText, highlighted, false, marked, newActivityLabel);
    }

    public SessionPickerOverlayLine(@NonNull Kind kind, @NonNull String text, @NonNull String secondaryText,
                                    boolean highlighted, boolean current, boolean marked,
                                    @NonNull String newActivityLabel) {
        this.kind = kind;
        this.text = text;
        this.secondaryText = secondaryText;
        this.highlighted = highlighted;
        this.current = current;
        this.marked = marked;
        this.newActivityLabel = newActivityLabel;
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

    public boolean isCurrent() {
        return current;
    }

    public boolean isMarked() {
        return marked;
    }

    @NonNull
    public String getNewActivityLabel() {
        return newActivityLabel;
    }
}
