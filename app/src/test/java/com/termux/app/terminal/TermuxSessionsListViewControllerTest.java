package com.termux.app.terminal;

import android.view.View;
import android.widget.ImageView;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class TermuxSessionsListViewControllerTest {

    @Test
    public void hidesProjectOverviewBrowserIconWhenNoOverviewActionIsProvided() {
        ImageView icon = new ImageView(RuntimeEnvironment.getApplication());
        icon.setVisibility(View.VISIBLE);

        TermuxSessionsListViewController.applyProjectOverviewBrowserIconVisibility(icon, null);

        Assert.assertEquals(View.GONE, icon.getVisibility());
        Assert.assertFalse(icon.hasOnClickListeners());
    }

    @Test
    public void showsProjectOverviewBrowserIconAndInvokesActionOnClickWhenOverviewActionIsProvided() {
        ImageView icon = new ImageView(RuntimeEnvironment.getApplication());
        icon.setVisibility(View.GONE);
        boolean[] opened = {false};

        TermuxSessionsListViewController.applyProjectOverviewBrowserIconVisibility(icon, () -> opened[0] = true);

        Assert.assertEquals(View.VISIBLE, icon.getVisibility());
        Assert.assertTrue(icon.hasOnClickListeners());

        icon.performClick();

        Assert.assertTrue(opened[0]);
    }
}
