package com.termux.app.sessiondefinition;

import android.util.Log;

import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

public final class SessionDefinitionPlainHttpRestoreStarter {
    private SessionDefinitionPlainHttpRestoreStarter() {
    }

    public static void restoreStoredUrl(TermuxAppSharedPreferences preferences) {
        boolean restored = SessionDefinitionPlainHttpRestore.restoreStore(new SessionDefinitionPlainHttpRestore.UrlStore() {
            @Override
            public String get() {
                return preferences.getSessionDefinitionUrl();
            }

            @Override
            public void set(String url) {
                preferences.setSessionDefinitionUrl(url);
            }
        });
        if (restored) {
            Log.i("SessionDefinitionPlainHttp", "Session definition URL restored to plain HTTP");
        }
    }
}
