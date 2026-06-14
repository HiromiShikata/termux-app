package com.termux.app;

import android.content.Intent;

import com.termux.app.activities.SettingsActivity;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class TermuxActivitySettingsIntentTest {

    @Test
    public void settingsIntentTargetsSettingsActivity() {
        Intent intent = TermuxActivity.createSettingsActivityIntent(RuntimeEnvironment.getApplication());

        Assert.assertNotNull(intent.getComponent());
        Assert.assertEquals(SettingsActivity.class.getName(), intent.getComponent().getClassName());
    }
}
