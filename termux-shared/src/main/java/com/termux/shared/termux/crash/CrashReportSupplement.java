package com.termux.shared.termux.crash;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface CrashReportSupplement {

    @Nullable
    String buildSupplementSection(@NonNull Context context);
}
