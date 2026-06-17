package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SessionProjectStoryLine {

    private static final String SEPARATOR = " · ";

    private final String text;

    private SessionProjectStoryLine(@NonNull String text) {
        this.text = text;
    }

    @NonNull
    public static SessionProjectStoryLine of(@Nullable String project, @Nullable String story) {
        String normalizedProject = normalize(project);
        String normalizedStory = normalize(story);
        if (!normalizedProject.isEmpty() && !normalizedStory.isEmpty()) {
            return new SessionProjectStoryLine(normalizedProject + SEPARATOR + normalizedStory);
        }
        if (!normalizedProject.isEmpty()) {
            return new SessionProjectStoryLine(normalizedProject);
        }
        if (!normalizedStory.isEmpty()) {
            return new SessionProjectStoryLine(normalizedStory);
        }
        return new SessionProjectStoryLine("");
    }

    @NonNull
    private static String normalize(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    @NonNull
    public String getText() {
        return text;
    }

    public boolean hasContent() {
        return !text.isEmpty();
    }
}
