package com.termux.app.apkupdate;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ApkUpdateFloatingIndicatorViewTest {

    private static final class ButtonBackedIndicatorView
        implements ApkUpdateFloatingIndicatorController.IndicatorView {

        private final FloatingActionButton indicator;

        ButtonBackedIndicatorView(FloatingActionButton indicator) {
            this.indicator = indicator;
        }

        @Override
        public void showUpdateAvailable(String latestVersionName, Runnable onTapped) {
            indicator.setContentDescription("Update available, version " + latestVersionName);
            indicator.setOnClickListener(view -> onTapped.run());
            indicator.setVisibility(View.VISIBLE);
        }

        @Override
        public void hide() {
            indicator.setOnClickListener(null);
            indicator.setVisibility(View.GONE);
        }
    }

    private FloatingActionButton newIndicatorButton() {
        Context themedContext = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            com.google.android.material.R.style.Theme_MaterialComponents_DayNight);
        FloatingActionButton indicator = new FloatingActionButton(themedContext);
        indicator.setVisibility(View.GONE);
        return indicator;
    }

    @Test
    public void showsButtonWithContentDescriptionWhenUpdateAvailableAndTapStartsUpdate() {
        FloatingActionButton indicator = newIndicatorButton();
        List<ApkUpdateAvailability> startedUpdates = new ArrayList<>();
        ApkUpdateFloatingIndicatorController controller = new ApkUpdateFloatingIndicatorController(
            new ButtonBackedIndicatorView(indicator), startedUpdates::add);
        ApkUpdateAvailability availability =
            ApkUpdateAvailability.available("0.121.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk");

        controller.onUpdateAvailable(availability);

        Assert.assertEquals(View.VISIBLE, indicator.getVisibility());
        Assert.assertTrue(indicator.hasOnClickListeners());
        Assert.assertEquals("Update available, version 0.121.0", indicator.getContentDescription());

        indicator.performClick();

        Assert.assertEquals(1, startedUpdates.size());
        Assert.assertSame(availability, startedUpdates.get(0));
        Assert.assertEquals(View.VISIBLE, indicator.getVisibility());
        Assert.assertTrue(indicator.hasOnClickListeners());
    }

    @Test
    public void buttonStaysVisibleWhileUpdateRemainsAvailable() {
        FloatingActionButton indicator = newIndicatorButton();
        ApkUpdateFloatingIndicatorController controller = new ApkUpdateFloatingIndicatorController(
            new ButtonBackedIndicatorView(indicator), availability -> { });
        ApkUpdateAvailability availability =
            ApkUpdateAvailability.available("0.121.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk");

        controller.onUpdateAvailable(availability);
        controller.onUpdateAvailable(availability);

        Assert.assertEquals(View.VISIBLE, indicator.getVisibility());
        Assert.assertTrue(indicator.hasOnClickListeners());
    }

    @Test
    public void hidesButtonWhenUpToDate() {
        FloatingActionButton indicator = newIndicatorButton();
        indicator.setVisibility(View.VISIBLE);
        ApkUpdateFloatingIndicatorController controller = new ApkUpdateFloatingIndicatorController(
            new ButtonBackedIndicatorView(indicator), availability -> { });

        controller.onUpToDate();

        Assert.assertEquals(View.GONE, indicator.getVisibility());
        Assert.assertFalse(indicator.hasOnClickListeners());
    }
}
