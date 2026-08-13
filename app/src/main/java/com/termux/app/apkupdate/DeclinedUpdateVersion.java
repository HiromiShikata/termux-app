package com.termux.app.apkupdate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class DeclinedUpdateVersion {

    static final String KEY_DECLINED_VERSION_NAME = "apk_update_declined_version_name";

    private final ApkUpdatePendingState.Store store;

    public DeclinedUpdateVersion(@NonNull ApkUpdatePendingState.Store store) {
        this.store = store;
    }

    public void remember(@NonNull String versionName) {
        store.putString(KEY_DECLINED_VERSION_NAME, versionName);
    }

    @Nullable
    public String recall() {
        return store.getString(KEY_DECLINED_VERSION_NAME);
    }
}
