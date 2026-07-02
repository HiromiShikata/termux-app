package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class OnScreenSessionRowSelector {

    public static final int NO_POSITION = -1;

    @NonNull
    public List<String> selectOnScreenSessionNames(@NonNull List<SessionHierarchyRow> rows,
                                                    int firstOnScreenPosition,
                                                    int lastOnScreenPosition) {
        List<String> onScreenSessionNames = new ArrayList<>();
        if (firstOnScreenPosition == NO_POSITION || lastOnScreenPosition == NO_POSITION) {
            return onScreenSessionNames;
        }
        int firstPosition = Math.max(0, firstOnScreenPosition);
        int lastPosition = Math.min(rows.size() - 1, lastOnScreenPosition);
        for (int position = firstPosition; position <= lastPosition; position++) {
            SessionHierarchyRow row = rows.get(position);
            if (row.isHeader()) {
                continue;
            }
            String sessionName = row.getSessionName();
            if (sessionName != null) {
                onScreenSessionNames.add(sessionName);
            }
        }
        return onScreenSessionNames;
    }
}
