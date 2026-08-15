package com.termux.app.ownercall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants;

public final class OwnerCallDialogStoredPlacement {

    private OwnerCallDialogStoredPlacement() {
    }

    @Nullable
    public static OwnerCallDialogPlacement of(@NonNull TermuxAppSharedPreferences preferences) {
        int leftMargin = preferences.getOwnerCallDialogLeftMargin();
        int bottomMargin = preferences.getOwnerCallDialogBottomMargin();
        int width = preferences.getOwnerCallDialogWidth();
        int height = preferences.getOwnerCallDialogHeight();
        if (isUnset(leftMargin) || isUnset(bottomMargin) || isUnset(width) || isUnset(height)) {
            return null;
        }
        return new OwnerCallDialogPlacement(leftMargin, bottomMargin, width, height);
    }

    private static boolean isUnset(int storedValue) {
        return storedValue
            == TermuxPreferenceConstants.TERMUX_APP.VALUE_OWNER_CALL_DIALOG_PLACEMENT_UNSET;
    }
}
