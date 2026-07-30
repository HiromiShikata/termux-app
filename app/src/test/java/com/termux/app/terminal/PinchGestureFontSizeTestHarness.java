package com.termux.app.terminal;

import android.content.Context;

import com.termux.app.TermuxActivity;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.view.TerminalView;

import org.robolectric.Robolectric;

import java.lang.reflect.Field;
import java.util.List;

final class PinchGestureFontSizeTestHarness {

    final TermuxAppSharedPreferences preferences;

    final TermuxTerminalViewClient client;

    PinchGestureFontSizeTestHarness(Context context) throws Exception {
        TextSizeApplicationRecordingShadowTerminalView.forgetAppliedTextSizes();
        preferences = TermuxAppSharedPreferences.build(context, true);
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        setActivityField(activity, "mPreferences", preferences);
        setActivityField(activity, "mTerminalView", new TerminalView(context, null));
        client = new TermuxTerminalViewClient(activity, null);
    }

    List<Integer> appliedTextSizes() {
        return TextSizeApplicationRecordingShadowTerminalView.appliedTextSizes();
    }

    int minimumFontSize() {
        int fontSizeBeforeProbe = preferences.getFontSize();
        preferences.setFontSize(Integer.MIN_VALUE);
        int minimumFontSize = preferences.getFontSize();
        preferences.setFontSize(fontSizeBeforeProbe);
        return minimumFontSize;
    }

    int maximumFontSize() {
        int fontSizeBeforeProbe = preferences.getFontSize();
        preferences.setFontSize(Integer.MAX_VALUE);
        int maximumFontSize = preferences.getFontSize();
        preferences.setFontSize(fontSizeBeforeProbe);
        return maximumFontSize;
    }

    void pinchApart(int steps) {
        for (int step = 0; step < steps; step++) {
            client.onScale(1.2f);
        }
    }

    void pinchTogether(int steps) {
        for (int step = 0; step < steps; step++) {
            client.onScale(0.8f);
        }
    }

    private static void setActivityField(TermuxActivity activity, String fieldName, Object value) throws Exception {
        Field field = TermuxActivity.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(activity, value);
    }
}
