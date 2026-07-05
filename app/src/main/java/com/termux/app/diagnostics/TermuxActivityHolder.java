package com.termux.app.diagnostics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.TermuxActivity;

import java.lang.ref.WeakReference;

public final class TermuxActivityHolder {

    @NonNull
    private static WeakReference<TermuxActivity> sActivityRef = new WeakReference<>(null);

    private TermuxActivityHolder() {
    }

    public static void set(@NonNull TermuxActivity activity) {
        sActivityRef = new WeakReference<>(activity);
    }

    public static void clear(@NonNull TermuxActivity activity) {
        if (sActivityRef.get() == activity) {
            sActivityRef = new WeakReference<>(null);
        }
    }

    @Nullable
    public static TermuxActivity get() {
        return sActivityRef.get();
    }
}
