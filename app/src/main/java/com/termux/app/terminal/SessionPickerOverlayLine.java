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
    private final SessionNewActivityTier tier;
    private final String newActivityLabel;

    public SessionPickerOverlayLine(@NonNull Kind kind, @NonNull String text, boolean highlighted) {
        this(kind, text, "", highlighted, false, SessionNewActivityTier.NONE, "");
    }

    public SessionPickerOverlayLine(@NonNull Kind kind, @NonNull String text, @NonNull String secondaryText,
                                    boolean highlighted, @NonNull SessionNewActivityTier tier,
                                    @NonNull String newActivityLabel) {
        this(kind, text, secondaryText, highlighted, false, tier, newActivityLabel);
    }

    public SessionPickerOverlayLine(@NonNull Kind kind, @NonNull String text, @NonNull String secondaryText,
                                    boolean highlighted, boolean current, @NonNull SessionNewActivityTier tier,
                                    @NonNull String newActivityLabel) {
        this.kind = kind;
        this.text = text;
        this.secondaryText = secondaryText;
        this.highlighted = highlighted;
        this.current = current;
        this.tier = tier;
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

    @NonNull
    public SessionNewActivityTier getTier() {
        return tier;
    }

    public boolean isMarked() {
        return tier != SessionNewActivityTier.NONE;
    }

    @NonNull
    public String getNewActivityLabel() {
        return newActivityLabel;
    }
}
