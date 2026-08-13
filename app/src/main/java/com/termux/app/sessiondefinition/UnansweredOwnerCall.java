package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;

public final class UnansweredOwnerCall {

    private final String calledAt;
    private final String body;

    public UnansweredOwnerCall(@NonNull String calledAt, @NonNull String body) {
        this.calledAt = calledAt;
        this.body = body;
    }

    @NonNull
    public String getCalledAt() {
        return calledAt;
    }

    @NonNull
    public String getBody() {
        return body;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UnansweredOwnerCall)) {
            return false;
        }
        UnansweredOwnerCall otherCall = (UnansweredOwnerCall) other;
        return calledAt.equals(otherCall.calledAt) && body.equals(otherCall.body);
    }

    @Override
    public int hashCode() {
        return 31 * calledAt.hashCode() + body.hashCode();
    }

    @Override
    public String toString() {
        return "UnansweredOwnerCall{calledAt=" + calledAt + ", body=" + body + "}";
    }
}
