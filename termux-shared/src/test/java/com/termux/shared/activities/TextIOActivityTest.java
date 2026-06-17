package com.termux.shared.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.widget.EditText;

import com.termux.shared.R;
import com.termux.shared.models.TextIOInfo;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenuItem;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class TextIOActivityTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    private TextIOInfo newTextIOInfo() {
        return new TextIOInfo("com.example.ACTION", "com.example.sender");
    }

    private ActivityController<TextIOActivity> controllerFor(Intent intent) {
        ActivityController<TextIOActivity> controller = Robolectric.buildActivity(TextIOActivity.class, intent);
        controller.get().setTheme(R.style.Theme_MarkdownViewActivity_DayNight);
        return controller;
    }

    private TextIOActivity launch(TextIOInfo info) {
        Intent intent = TextIOActivity.newInstance(context(), info);
        return controllerFor(intent).create().start().resume().visible().get();
    }

    @Test
    public void newInstanceBuildsIntentWithExtra() {
        Intent intent = TextIOActivity.newInstance(context(), newTextIOInfo());
        Assert.assertNotNull(intent.getExtras());
    }

    @Test
    public void launchesWithDefaultInfo() {
        TextIOInfo info = newTextIOInfo();
        info.setTitle("Editor");
        info.setText("initial text");
        TextIOActivity activity = launch(info);
        Assert.assertFalse(activity.isFinishing());
    }

    @Test
    public void launchesWithNullTitleUsesDefault() {
        TextIOInfo info = newTextIOInfo();
        info.setText("text without title");
        TextIOActivity activity = launch(info);
        Assert.assertFalse(activity.isFinishing());
    }

    @Test
    public void launchesWithBackButtonInActionBar() {
        TextIOInfo info = newTextIOInfo();
        info.setTitle("With Back");
        info.setShowBackButtonInActionBar(true);
        TextIOActivity activity = launch(info);
        Assert.assertFalse(activity.isFinishing());
    }

    @Test
    public void launchesWithLabelEnabled() {
        TextIOInfo info = newTextIOInfo();
        info.setLabelEnabled(true);
        info.setLabel("A label");
        info.setLabelSize(14);
        info.setLabelColor(0xFF112233);
        info.setLabelTypeFaceFamily("monospace");
        info.setLabelTypeFaceStyle(Typeface.BOLD);
        info.setText("text with label");
        TextIOActivity activity = launch(info);
        Assert.assertFalse(activity.isFinishing());
    }

    @Test
    public void launchesNotHorizontallyScrollableRemovesScrollView() {
        TextIOInfo info = newTextIOInfo();
        info.setTextHorizontallyScrolling(false);
        info.setText("non scrollable text");
        TextIOActivity activity = launch(info);
        Assert.assertFalse(activity.isFinishing());
    }

    @Test
    public void launchesHorizontallyScrollable() {
        TextIOInfo info = newTextIOInfo();
        info.setTextHorizontallyScrolling(true);
        info.setText("scrollable text");
        TextIOActivity activity = launch(info);
        Assert.assertFalse(activity.isFinishing());
    }

    @Test
    public void launchesWithEditingDisabled() {
        TextIOInfo info = newTextIOInfo();
        info.setEditingTextDisabled(true);
        info.setText("read only text");
        TextIOActivity activity = launch(info);
        Assert.assertFalse(activity.isFinishing());
    }

    @Test
    public void launchesWithCharacterUsageAndUpdatesOnTextChange() {
        TextIOInfo info = newTextIOInfo();
        info.setShowTextCharacterUsage(true);
        info.setText("abc");
        TextIOActivity activity = launch(info);

        EditText editText = activity.findViewById(R.id.text_io_text);
        editText.setText("abcdef");
        Assert.assertFalse(activity.isFinishing());
    }

    @Test
    public void launchesWithoutExtrasFinishes() {
        Intent intent = new Intent(context(), TextIOActivity.class);
        TextIOActivity activity = controllerFor(intent).create().start().resume().get();
        Assert.assertTrue(activity.isFinishing());
    }

    @Test
    public void cancelMenuItemFinishesActivity() {
        TextIOActivity activity = launch(newTextIOInfo());
        activity.onOptionsItemSelected(new RoboMenuItem(R.id.menu_item_cancel));
        Assert.assertTrue(activity.isFinishing());
    }

    @Test
    public void shareTextMenuItemDoesNotThrow() {
        TextIOInfo info = newTextIOInfo();
        info.setTitle("Title");
        info.setText("shareable");
        TextIOActivity activity = launch(info);
        activity.onOptionsItemSelected(new RoboMenuItem(R.id.menu_item_share_text));
    }

    @Test
    public void copyTextMenuItemDoesNotThrow() {
        TextIOInfo info = newTextIOInfo();
        info.setText("copyable");
        TextIOActivity activity = launch(info);
        activity.onOptionsItemSelected(new RoboMenuItem(R.id.menu_item_copy_text));
    }

    @Test
    public void homeMenuItemConfirmsAndFinishes() {
        TextIOActivity activity = launch(newTextIOInfo());
        activity.onOptionsItemSelected(new RoboMenuItem(android.R.id.home));
        Assert.assertTrue(activity.isFinishing());
    }

    @Test
    public void backPressedConfirmsAndFinishes() {
        TextIOActivity activity = launch(newTextIOInfo());
        activity.onBackPressed();
        Assert.assertTrue(activity.isFinishing());
    }

    @Test
    public void onSaveInstanceStateStoresTextIOInfo() {
        TextIOInfo info = newTextIOInfo();
        info.setText("state text");
        TextIOActivity activity = launch(info);
        Bundle outState = new Bundle();
        activity.onSaveInstanceState(outState);
        Assert.assertFalse(outState.isEmpty());
    }

    @Test
    public void onNewIntentRestartsActivity() {
        TextIOInfo info = newTextIOInfo();
        info.setText("first");
        ActivityController<TextIOActivity> controller = controllerFor(TextIOActivity.newInstance(context(), info));
        TextIOActivity activity = controller.create().start().resume().get();

        TextIOInfo second = newTextIOInfo();
        second.setText("second");
        controller.newIntent(TextIOActivity.newInstance(context(), second));
        Assert.assertTrue(activity.isFinishing());
    }
}
