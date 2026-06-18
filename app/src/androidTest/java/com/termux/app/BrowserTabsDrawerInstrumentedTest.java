package com.termux.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.Gravity;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BrowserTabsDrawerInstrumentedTest {

    @Test
    public void openingAndClosingTheRightDrawerTransitionsTheDrawerState() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            DrawerLayout drawer = activity.getDrawer();
            assertNotNull(drawer);
            assertFalse(drawer.isDrawerOpen(Gravity.RIGHT));

            drawer.openDrawer(Gravity.RIGHT, false);
            assertTrue(drawer.isDrawerOpen(Gravity.RIGHT));

            drawer.closeDrawer(Gravity.RIGHT, false);
            assertFalse(drawer.isDrawerOpen(Gravity.RIGHT));
        });
    }
}
