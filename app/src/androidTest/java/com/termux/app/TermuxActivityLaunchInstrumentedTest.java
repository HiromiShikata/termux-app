package com.termux.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;

@RunWith(AndroidJUnit4.class)
public class TermuxActivityLaunchInstrumentedTest {

    @Test
    public void launchReachesValidStateWithTerminalViewAndClientsPresent() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            assertFalse(readInvalidStateFlag(activity));
            assertNotNull(activity.getTerminalView());
            assertNotNull(activity.getTermuxTerminalSessionClient());
            assertNotNull(activity.getTermuxTerminalViewClient());
            assertNotNull(activity.getTermuxBrowserController());
            assertNotNull(activity.getPreferences());
            assertNotNull(activity.getProperties());
        });
    }

    private static boolean readInvalidStateFlag(TermuxActivity activity) {
        try {
            Field field = TermuxActivity.class.getDeclaredField("mIsInvalidState");
            field.setAccessible(true);
            return field.getBoolean(activity);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new RuntimeException(reflectiveOperationException);
        }
    }
}
