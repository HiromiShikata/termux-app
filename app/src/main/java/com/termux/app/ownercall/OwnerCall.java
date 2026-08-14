package com.termux.app.ownercall;

import androidx.annotation.NonNull;

import java.util.Objects;

public final class OwnerCall {

    @NonNull
    private final String mSessionName;

    @NonNull
    private final String mCalledAt;

    @NonNull
    private final String mBody;

    public OwnerCall(@NonNull String sessionName, @NonNull String calledAt, @NonNull String body) {
        mSessionName = sessionName;
        mCalledAt = calledAt;
        mBody = body;
    }

    @NonNull
    public String getSessionName() {
        return mSessionName;
    }

    @NonNull
    public String getCalledAt() {
        return mCalledAt;
    }

    @NonNull
    public String getBody() {
        return mBody;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof OwnerCall)) {
            return false;
        }
        OwnerCall otherCall = (OwnerCall) other;
        return mSessionName.equals(otherCall.mSessionName)
            && mCalledAt.equals(otherCall.mCalledAt)
            && mBody.equals(otherCall.mBody);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSessionName, mCalledAt, mBody);
    }

    @Override
    public String toString() {
        return "OwnerCall{" + mSessionName + " at " + mCalledAt + "}";
    }
}
