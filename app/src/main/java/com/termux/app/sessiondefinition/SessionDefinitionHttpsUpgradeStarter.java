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
            String url = preferences.getSessionDefinitionUrl();
            String upgraded = SessionDefinitionHttpsUpgrade.resolve(url, SessionDefinitionHttpsUpgrade.PRODUCTION_FETCHER);
            if (SessionDefinitionHttpsUpgrade.shouldPersist(url, upgraded, preferences.getSessionDefinitionUrl())) {
                preferences.setSessionDefinitionUrl(upgraded);
                Log.i("SessionDefinitionHttps", "Session definition URL upgraded to HTTPS");
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
