package com.termux.app.handedness;

import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.termux.app.TermuxActivity;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RunWith(AndroidJUnit4.class)
public class LeftHandedModeDrawerEdgeInstrumentedTest {

    private static final long SETTLE_MILLIS = 600L;
    private static final String BACKUP_SUFFIX = ".handedness-test-backup";

    @Test
    public void rightHandedDrawerPathsOpenAndCloseTheEndDrawer() throws Exception {
        exerciseDrawerPaths(false, Gravity.END);
    }

    @Test
    public void leftHandedDrawerPathsOpenAndCloseTheStartDrawer() throws Exception {
        exerciseDrawerPaths(true, Gravity.START);
    }

    @Test
    public void openDrawerShortcutOpensTheEndDrawerWhenLeftHandedModeIsOff() throws Exception {
        exerciseOpenDrawerShortcut(false, Gravity.END);
    }

    @Test
    public void openDrawerShortcutOpensTheStartDrawerWhenLeftHandedModeIsOn() throws Exception {
        exerciseOpenDrawerShortcut(true, Gravity.START);
    }

    private static void exerciseDrawerPaths(boolean leftHanded, int expectedEdge) throws Exception {
        TermuxAppSharedPreferences preferences =
            TermuxAppSharedPreferences.build(ApplicationProvider.getApplicationContext());
        preferences.setLeftHandedModeEnabled(leftHanded);
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        try {
            settle();

            scenario.onActivity(activity -> activity.getTermuxBrowserController().toggleTabsDrawer());
            settle();
            assertDrawerOpen(scenario, expectedEdge, true, "browser toggle must open the drawer");
            scenario.onActivity(activity -> activity.getTermuxBrowserController().toggleTabsDrawer());
            settle();
            assertDrawerOpen(scenario, expectedEdge, false, "browser toggle must close the drawer");

            scenario.onActivity(activity -> activity.getDrawer().openDrawer(expectedEdge, false));
            settle();
            scenario.onActivity(TermuxActivity::onBackPressed);
            settle();
            assertDrawerOpen(scenario, expectedEdge, false, "back must close an open drawer");

            scenario.onActivity(activity -> activity.getTermuxTerminalExtraKeys()
                .onTerminalExtraKeyButtonClick(null, "DRAWER", false, false, false, false));
            settle();
            assertDrawerOpen(scenario, expectedEdge, true, "the DRAWER extra key must open the drawer");
            scenario.onActivity(activity -> activity.getTermuxTerminalExtraKeys()
                .onTerminalExtraKeyButtonClick(null, "DRAWER", false, false, false, false));
            settle();
            assertDrawerOpen(scenario, expectedEdge, false, "the DRAWER extra key must close the drawer");
        } finally {
            preferences.setLeftHandedModeEnabled(false);
        }
    }

    private static void exerciseOpenDrawerShortcut(boolean leftHanded, int expectedEdge) throws Exception {
        TermuxAppSharedPreferences preferences =
            TermuxAppSharedPreferences.build(ApplicationProvider.getApplicationContext());
        preferences.setLeftHandedModeEnabled(leftHanded);
        File propertiesFile = new File(TermuxConstants.TERMUX_PROPERTIES_PRIMARY_FILE_PATH);
        File backupFile = new File(propertiesFile.getPath() + BACKUP_SUFFIX);
        try {
            enableHardwareKeyboardShortcuts(propertiesFile, backupFile);
            ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
            settle();

            scenario.onActivity(activity -> {
                long now = SystemClock.uptimeMillis();
                KeyEvent ctrlAltDpadRight = new KeyEvent(now, now, KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_DPAD_RIGHT, 0, KeyEvent.META_CTRL_ON | KeyEvent.META_ALT_ON);
                Assert.assertTrue("ctrl+alt+right must be consumed by the view client",
                    activity.getTermuxTerminalViewClient().onKeyDown(
                        KeyEvent.KEYCODE_DPAD_RIGHT, ctrlAltDpadRight, activity.getCurrentSession()));
                Assert.assertEquals("open-drawer shortcut must target the edge reported for handedness "
                        + leftHanded, expectedEdge, TabDrawerEdge.currentGravity(activity.getDrawer()));
            });
            settle();
            assertDrawerOpen(scenario, expectedEdge, true,
                "the open-drawer shortcut must open the drawer");
            int oppositeEdge = expectedEdge == Gravity.END ? Gravity.START : Gravity.END;
            assertDrawerOpen(scenario, oppositeEdge, false,
                "the open-drawer shortcut must not open the opposite drawer");
        } finally {
            preferences.setLeftHandedModeEnabled(false);
            restoreTermuxProperties(propertiesFile, backupFile);
        }
    }

    private static void enableHardwareKeyboardShortcuts(File propertiesFile, File backupFile) throws IOException {
        if (propertiesFile.exists() && !propertiesFile.renameTo(backupFile)) {
            throw new IOException("could not back up " + propertiesFile.getPath());
        }
        File parent = propertiesFile.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("could not create " + parent.getPath());
        }
        try (FileOutputStream out = new FileOutputStream(propertiesFile)) {
            out.write("disable-hardware-keyboard-shortcuts=false\n".getBytes(StandardCharsets.UTF_8));
        }
        TermuxAppSharedProperties.getProperties().loadTermuxPropertiesFromDisk();
    }

    private static void restoreTermuxProperties(File propertiesFile, File backupFile) {
        propertiesFile.delete();
        if (backupFile.exists()) {
            backupFile.renameTo(propertiesFile);
        }
        TermuxAppSharedProperties.getProperties().loadTermuxPropertiesFromDisk();
    }

    private static void assertDrawerOpen(@NonNull ActivityScenario<TermuxActivity> scenario,
                                         int edge, boolean expectedOpen, String message) {
        scenario.onActivity(activity -> Assert.assertEquals(message + " at edge " + edge,
            expectedOpen, activity.getDrawer().isDrawerOpen(edge)));
    }

    private static void settle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(SETTLE_MILLIS);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
