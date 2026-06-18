package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.viewpager.widget.ViewPager;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TerminalToolbarToggleInstrumentedTest {

    @Test
    public void toggleTerminalToolbarFlipsTheToolbarViewPagerVisibility() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            ViewPager terminalToolbarViewPager = activity.getTerminalToolbarViewPager();
            assertNotNull(terminalToolbarViewPager);

            int initialVisibility = terminalToolbarViewPager.getVisibility();
            assertTrue(initialVisibility == View.VISIBLE || initialVisibility == View.GONE);
            int toggledVisibility = initialVisibility == View.VISIBLE ? View.GONE : View.VISIBLE;

            activity.toggleTerminalToolbar();
            assertEquals(toggledVisibility, terminalToolbarViewPager.getVisibility());
            assertNotEquals(initialVisibility, terminalToolbarViewPager.getVisibility());

            activity.toggleTerminalToolbar();
            assertEquals(initialVisibility, terminalToolbarViewPager.getVisibility());
        });
    }
}
