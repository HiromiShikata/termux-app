package com.termux.app.ownercall;

import androidx.annotation.NonNull;

import com.termux.app.sessiondefinition.UnansweredOwnerCall;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class OwnerCallDialogState {

    private final Set<String> mDismissedCalledAt = new HashSet<>();

    private int mIndex;

    @NonNull
    public List<UnansweredOwnerCall> visibleCalls(@NonNull List<UnansweredOwnerCall> calls) {
        List<UnansweredOwnerCall> visible = new ArrayList<>();
        for (UnansweredOwnerCall call : calls) {
            if (!mDismissedCalledAt.contains(call.getCalledAt())) {
                visible.add(call);
            }
        }
        return visible;
    }

    public int getIndex() {
        return mIndex;
    }

    public void showPreviousCall() {
        mIndex = mIndex - 1;
    }

    public void showNextCall() {
        mIndex = mIndex + 1;
    }

    public void applyResolvedIndex(int resolvedIndex) {
        mIndex = resolvedIndex;
    }

    public void dismiss(@NonNull UnansweredOwnerCall call) {
        mDismissedCalledAt.add(call.getCalledAt());
        mIndex = 0;
    }
}
