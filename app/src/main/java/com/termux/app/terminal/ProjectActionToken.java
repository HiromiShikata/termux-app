package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.Objects;

public final class ProjectActionToken {

    private final String normalizedProjectName;
    private final ProjectAction action;

    public ProjectActionToken(@NonNull String normalizedProjectName, @NonNull ProjectAction action) {
        this.normalizedProjectName = normalizedProjectName;
        this.action = action;
    }

    @NonNull
    public String getNormalizedProjectName() {
        return normalizedProjectName;
    }

    @NonNull
    public ProjectAction getAction() {
        return action;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProjectActionToken)) {
            return false;
        }
        ProjectActionToken that = (ProjectActionToken) other;
        return normalizedProjectName.equals(that.normalizedProjectName) && action == that.action;
    }

    @Override
    public int hashCode() {
        return Objects.hash(normalizedProjectName, action);
    }

    @NonNull
    @Override
    public String toString() {
        return "ProjectActionToken{" + normalizedProjectName + ":" + action + "}";
    }
}
