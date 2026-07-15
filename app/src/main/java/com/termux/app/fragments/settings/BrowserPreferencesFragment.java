package com.termux.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.termux.R;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

@Keep
public class BrowserPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(BrowserPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.termux_browser_preferences, rootKey);
    }

}

class BrowserPreferencesDataStore extends PreferenceDataStore {

    private final TermuxAppSharedPreferences mPreferences;

    private static BrowserPreferencesDataStore mInstance;

    private BrowserPreferencesDataStore(Context context) {
        mPreferences = TermuxAppSharedPreferences.build(context, true);
    }

    public static synchronized BrowserPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new BrowserPreferencesDataStore(context);
        }
        return mInstance;
    }

    @Override
    public void putBoolean(String key, boolean value) {
        if (mPreferences == null || key == null) return;
        if ("browser_meet_low_power_video_enabled".equals(key)) {
            mPreferences.setBrowserMeetLowPowerVideoEnabled(value);
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        if (mPreferences == null || key == null) return defValue;
        if ("browser_meet_low_power_video_enabled".equals(key)) {
            return mPreferences.isBrowserMeetLowPowerVideoEnabled();
        }
        return defValue;
    }

    @Override
    public void putString(String key, String value) {
        if (mPreferences == null || key == null || value == null) return;
        int parsedValue;
        try {
            parsedValue = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return;
        }
        switch (key) {
            case "browser_meet_low_power_video_max_resolution":
                applyResolution(parsedValue);
                break;
            case "browser_meet_low_power_video_max_framerate":
                mPreferences.setBrowserMeetLowPowerVideoMaxFramerate(parsedValue);
                break;
            default:
                break;
        }
    }

    @Override
    public String getString(String key, String defValue) {
        if (mPreferences == null || key == null) return defValue;
        switch (key) {
            case "browser_meet_low_power_video_max_resolution":
                return String.valueOf(mPreferences.getBrowserMeetLowPowerVideoMaxWidth());
            case "browser_meet_low_power_video_max_framerate":
                return String.valueOf(mPreferences.getBrowserMeetLowPowerVideoMaxFramerate());
            default:
                return defValue;
        }
    }

    private void applyResolution(int maxWidth) {
        mPreferences.setBrowserMeetLowPowerVideoMaxWidth(maxWidth);
        mPreferences.setBrowserMeetLowPowerVideoMaxHeight(heightForWidth(maxWidth));
    }

    private int heightForWidth(int maxWidth) {
        switch (maxWidth) {
            case 480:
                return 270;
            case 320:
                return 180;
            case 640:
            default:
                return 360;
        }
    }

}
