package com.termux.app.sessiondefinition;

import android.util.Log;

import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SessionDefinitionHttpsUpgradeStarter {
    private static final AtomicBoolean STARTED = new AtomicBoolean();

    private SessionDefinitionHttpsUpgradeStarter() {
    }

    public static void startOnce(TermuxAppSharedPreferences preferences) {
        if (!STARTED.compareAndSet(false, true)) return;
        Thread thread = new Thread(() -> {
            SessionDefinitionHttpsUpgrade.UrlStore store = new SessionDefinitionHttpsUpgrade.UrlStore() {
                @Override
                public String get() {
                    return preferences.getSessionDefinitionUrl();
                }

                @Override
                public void set(String url) {
                    preferences.setSessionDefinitionUrl(url);
                }
            };
            if (SessionDefinitionHttpsUpgrade.upgradeStore(store, SessionDefinitionHttpsUpgrade.PRODUCTION_FETCHER)) {
                Log.i("SessionDefinitionHttps", "Session definition URL upgraded to HTTPS");
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
