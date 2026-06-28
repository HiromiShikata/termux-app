package com.termux.app.apkupdate;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

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

        private final ExtendedFloatingActionButton indicator;

        ButtonBackedIndicatorView(ExtendedFloatingActionButton indicator) {
            this.indicator = indicator;
        }

        @Override
        public void showUpdateAvailable(String latestVersionName, Runnable onTapped) {
            indicator.setOnClickListener(view -> onTapped.run());
            indicator.setVisibility(View.VISIBLE);
        }

        @Override
        public void hide() {
            indicator.setOnClickListener(null);
            indicator.setVisibility(View.GONE);
        }
    }

    private ExtendedFloatingActionButton newIndicatorButton() {
        Context themedContext = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            com.google.android.material.R.style.Theme_MaterialComponents_DayNight);
        ExtendedFloatingActionButton indicator = new ExtendedFloatingActionButton(themedContext);
        indicator.setVisibility(View.GONE);
        return indicator;
    }

    @Test
    public void showsButtonWhenUpdateAvailableAndTapStartsUpdate() {
        ExtendedFloatingActionButton indicator = newIndicatorButton();
        List<ApkUpdateAvailability> startedUpdates = new ArrayList<>();
        ApkUpdateFloatingIndicatorController controller = new ApkUpdateFloatingIndicatorController(
            new ButtonBackedIndicatorView(indicator), startedUpdates::add);
        ApkUpdateAvailability availability =
            ApkUpdateAvailability.available("0.121.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk");

        controller.onUpdateAvailable(availability);

        Assert.assertEquals(View.VISIBLE, indicator.getVisibility());
        Assert.assertTrue(indicator.hasOnClickListeners());

        indicator.performClick();

        Assert.assertEquals(1, startedUpdates.size());
        Assert.assertSame(availability, startedUpdates.get(0));
        Assert.assertEquals(View.GONE, indicator.getVisibility());
    }

    @Test
    public void hidesButtonWhenUpToDate() {
        ExtendedFloatingActionButton indicator = newIndicatorButton();
        indicator.setVisibility(View.VISIBLE);
        ApkUpdateFloatingIndicatorController controller = new ApkUpdateFloatingIndicatorController(
            new ButtonBackedIndicatorView(indicator), availability -> { });

        controller.onUpToDate();

        Assert.assertEquals(View.GONE, indicator.getVisibility());
        Assert.assertFalse(indicator.hasOnClickListeners());
    }
}
