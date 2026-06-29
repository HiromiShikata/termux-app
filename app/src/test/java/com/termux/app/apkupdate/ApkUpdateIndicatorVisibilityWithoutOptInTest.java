package com.termux.app.apkupdate;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ApkUpdateIndicatorVisibilityWithoutOptInTest {

    private Context context;

    private boolean shown;
    private boolean hidden;

    private final ApkUpdateFloatingIndicatorController.IndicatorView indicatorView =
        new ApkUpdateFloatingIndicatorController.IndicatorView() {
            @Override
            public void showUpdateAvailable(String latestVersionName, Runnable onTapped) {
                shown = true;
            }

            @Override
            public void hide() {
                hidden = true;
            }
        };

    private ApkUpdateFloatingIndicatorController newController() {
        return new ApkUpdateFloatingIndicatorController(indicatorView, availability -> { });
    }

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().clear()
            .putBoolean(ApkUpdateManager.PREFERENCE_KEY_AUTO_CHECK, false)
            .commit();
        shown = false;
        hidden = false;
    }

    @Test
    public void showsButtonWhenUpdateAvailableWithNoPersistedStateAndOptInDisabled() {
        ApkUpdatePendingState pendingState =
            new ApkUpdatePendingState(new SharedPreferencesApkUpdatePendingStore(context));
        Assert.assertFalse(pendingState.hasPending());
        Assert.assertFalse(ApkUpdateManager.isAutoCheckEnabled(context));

        ApkUpdateAvailability availability = ApkUpdateAvailability.available(
            "0.121.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk");
        newController().onUpdateAvailable(availability);

        Assert.assertTrue(shown);
        Assert.assertFalse(hidden);
    }

    @Test
    public void hidesButtonWhenUpToDateWithNoPersistedStateAndOptInDisabled() {
        ApkUpdatePendingState pendingState =
            new ApkUpdatePendingState(new SharedPreferencesApkUpdatePendingStore(context));
        Assert.assertFalse(pendingState.hasPending());
        Assert.assertFalse(ApkUpdateManager.isAutoCheckEnabled(context));

        newController().onUpdateAvailable(ApkUpdateAvailability.upToDate("0.121.0"));

        Assert.assertFalse(shown);
        Assert.assertTrue(hidden);
    }
}
