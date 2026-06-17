package com.termux.shared.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.termux.shared.R;
import com.termux.shared.models.ReportInfo;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenu;
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class ReportActivityTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    private ReportInfo newReportInfo(String reportString) {
        ReportInfo info = new ReportInfo("com.example.ACTION", "com.example.sender", "Report Title");
        info.setReportString(reportString);
        return info;
    }

    private ActivityController<ReportActivity> controllerFor(Intent intent) {
        ActivityController<ReportActivity> controller = Robolectric.buildActivity(ReportActivity.class, intent);
        controller.get().setTheme(R.style.Theme_MarkdownViewActivity_DayNight);
        return controller;
    }

    private ReportActivity launch(Intent intent) {
        return controllerFor(intent).create().start().resume().visible().get();
    }

    private Intent contentIntentFor(ReportInfo info) {
        ReportActivity.NewInstanceResult result = ReportActivity.newInstance(context(), info);
        Assert.assertNotNull(result.contentIntent);
        return result.contentIntent;
    }

    private Menu inflatedOptionsMenu(ReportActivity activity) {
        RoboMenu menu = new RoboMenu(context());
        activity.onCreateOptionsMenu(menu);
        return menu;
    }

    @Test
    public void launchesWithSerializableReportInfo() {
        ReportInfo info = newReportInfo("# Heading\n\nSome **markdown** body.");
        info.setReportStringPrefix("prefix\n");
        info.setReportStringSuffix("\nsuffix");
        ReportActivity activity = launch(contentIntentFor(info));
        Assert.assertNotNull(activity);
        Assert.assertFalse(activity.isFinishing());
    }

    @Test
    public void launchesWithNullTitleUsesDefaultActionBarTitle() {
        ReportInfo info = new ReportInfo("action", "sender", null);
        info.setReportString("body");
        ReportActivity activity = launch(contentIntentFor(info));
        Assert.assertFalse(activity.isFinishing());
    }

    @Test
    public void launchesWithReportInfoHeaderEnabled() {
        ReportInfo info = newReportInfo("body with header");
        info.setAddReportInfoHeaderToMarkdown(true);
        ReportActivity activity = launch(contentIntentFor(info));
        Assert.assertFalse(activity.isFinishing());
    }

    @Test
    public void launchesWithoutExtrasFinishes() {
        Intent intent = new Intent(context(), ReportActivity.class);
        ReportActivity activity = controllerFor(intent).create().get();
        Assert.assertTrue(activity.isFinishing());
    }

    @Test
    public void createOptionsMenuDisablesSaveWhenNoSavePath() {
        ReportActivity activity = launch(contentIntentFor(newReportInfo("body")));
        Menu menu = inflatedOptionsMenu(activity);
        MenuItem saveItem = menu.findItem(R.id.menu_item_save_report_to_file);
        Assert.assertFalse(saveItem.isEnabled());
    }

    @Test
    public void createOptionsMenuEnablesSaveWhenSavePathSet() {
        ReportInfo info = newReportInfo("body");
        info.setReportSaveFileLabelAndPath("label", context().getCacheDir().getAbsolutePath() + "/report_save.txt");
        ReportActivity activity = launch(contentIntentFor(info));
        Menu menu = inflatedOptionsMenu(activity);
        MenuItem saveItem = menu.findItem(R.id.menu_item_save_report_to_file);
        Assert.assertTrue(saveItem.isEnabled());
    }

    @Test
    public void shareReportMenuItemDoesNotThrow() {
        ReportActivity activity = launch(contentIntentFor(newReportInfo("body")));
        activity.onOptionsItemSelected(new RoboMenuItem(R.id.menu_item_share_report));
    }

    @Test
    public void copyReportMenuItemCopiesToClipboard() {
        ReportActivity activity = launch(contentIntentFor(newReportInfo("clipboard body")));
        activity.onOptionsItemSelected(new RoboMenuItem(R.id.menu_item_copy_report));
    }

    @Test
    public void saveReportMenuItemDoesNotThrow() {
        ReportInfo info = newReportInfo("save body");
        info.setReportSaveFileLabelAndPath("label", context().getCacheDir().getAbsolutePath() + "/report_menu_save.txt");
        ReportActivity activity = launch(contentIntentFor(info));
        activity.onOptionsItemSelected(new RoboMenuItem(R.id.menu_item_save_report_to_file));
    }

    @Test
    public void onSaveInstanceStateForSerializableBundle() {
        ReportActivity activity = launch(contentIntentFor(newReportInfo("body")));
        Bundle outState = new Bundle();
        activity.onSaveInstanceState(outState);
        Assert.assertFalse(outState.isEmpty());
    }

    @Test
    public void onNewIntentUpdatesReport() {
        ActivityController<ReportActivity> controller = controllerFor(contentIntentFor(newReportInfo("first")));
        ReportActivity activity = controller.create().start().resume().get();
        Intent newIntent = contentIntentFor(newReportInfo("second report body"));
        controller.newIntent(newIntent);
        Assert.assertFalse(activity.isFinishing());
    }

    @Test
    public void backPressedFinishesActivity() {
        ReportActivity activity = launch(contentIntentFor(newReportInfo("body")));
        activity.onBackPressed();
        Assert.assertTrue(activity.isFinishing());
    }

    @Test
    public void destroyDoesNotThrowForSerializableReport() {
        ActivityController<ReportActivity> controller = controllerFor(contentIntentFor(newReportInfo("destroy body")));
        controller.create().start().resume();
        controller.pause().stop().destroy();
    }

    @Test
    public void requestPermissionsResultGrantedDoesNotThrow() {
        ReportInfo info = newReportInfo("perm body");
        info.setReportSaveFileLabelAndPath("label", context().getCacheDir().getAbsolutePath() + "/report_perm_save.txt");
        ReportActivity activity = launch(contentIntentFor(info));
        activity.onRequestPermissionsResult(
            ReportActivity.REQUEST_GRANT_STORAGE_PERMISSION_FOR_SAVE_FILE,
            new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"},
            new int[]{android.content.pm.PackageManager.PERMISSION_GRANTED});
    }

    @Test
    public void requestPermissionsResultDeniedDoesNotThrow() {
        ReportActivity activity = launch(contentIntentFor(newReportInfo("perm body")));
        activity.onRequestPermissionsResult(
            ReportActivity.REQUEST_GRANT_STORAGE_PERMISSION_FOR_SAVE_FILE,
            new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"},
            new int[]{android.content.pm.PackageManager.PERMISSION_DENIED});
    }

    @Test
    public void startReportActivityStartsActivity() {
        ReportActivity.startReportActivity(context(), newReportInfo("started body"));
        ShadowApplication shadowApplication = Shadows.shadowOf(RuntimeEnvironment.getApplication());
        Intent started = shadowApplication.getNextStartedActivity();
        Assert.assertNotNull(started);
    }

    @Test
    public void deleteReportInfoFilesOlderThanXDaysAsynchronousReturnsNull() {
        Assert.assertNull(ReportActivity.deleteReportInfoFilesOlderThanXDays(context(), 1, false));
    }

    @Test
    public void deleteReportInfoFilesOlderThanXDaysSynchronousDoesNotThrow() {
        ReportActivity.deleteReportInfoFilesOlderThanXDays(context(), 0, true);
    }

    @Test
    public void broadcastReceiverWithNullIntentDoesNothing() {
        new ReportActivity.ReportActivityBroadcastReceiver().onReceive(context(), null);
    }

    @Test
    public void broadcastReceiverWithUnknownActionDoesNothing() {
        Intent intent = new Intent("com.example.UNKNOWN_ACTION");
        new ReportActivity.ReportActivityBroadcastReceiver().onReceive(context(), intent);
    }
}
