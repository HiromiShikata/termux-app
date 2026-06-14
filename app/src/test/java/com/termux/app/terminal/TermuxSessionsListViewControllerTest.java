package com.termux.app.terminal;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.termux.R;
import com.termux.app.terminal.TermuxSessionsListViewController.SessionRowActiveIndicator;

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

    @Test
    public void currentRunningSessionShowsAccentBarAndAccentName() {
        SessionRowActiveIndicator indicator =
            TermuxSessionsListViewController.computeActiveIndicator(true, true);

        Assert.assertTrue(indicator.showAccentBar);
        Assert.assertTrue(indicator.useAccentNameColor);
    }

    @Test
    public void currentFinishedSessionShowsAccentBarButKeepsFinishedNameColor() {
        SessionRowActiveIndicator indicator =
            TermuxSessionsListViewController.computeActiveIndicator(true, false);

        Assert.assertTrue(indicator.showAccentBar);
        Assert.assertFalse(indicator.useAccentNameColor);
    }

    @Test
    public void nonCurrentRunningSessionHasNoActiveIndicator() {
        SessionRowActiveIndicator indicator =
            TermuxSessionsListViewController.computeActiveIndicator(false, true);

        Assert.assertFalse(indicator.showAccentBar);
        Assert.assertFalse(indicator.useAccentNameColor);
    }

    @Test
    public void nonCurrentFinishedSessionHasNoActiveIndicator() {
        SessionRowActiveIndicator indicator =
            TermuxSessionsListViewController.computeActiveIndicator(false, false);

        Assert.assertFalse(indicator.showAccentBar);
        Assert.assertFalse(indicator.useAccentNameColor);
    }

    @Test
    public void staleRowIndexBeyondShrunkenSessionListIsTreatedAsOutOfRange() {
        int sessionCountAfterDelete = 1;
        int staleRowSessionIndex = 1;

        Assert.assertFalse(
            TermuxSessionsListViewController.isSessionIndexInRange(staleRowSessionIndex, sessionCountAfterDelete));
    }

    @Test
    public void negativeSessionIndexIsTreatedAsOutOfRange() {
        Assert.assertFalse(TermuxSessionsListViewController.isSessionIndexInRange(-1, 3));
    }

    @Test
    public void anySessionIndexIsOutOfRangeWhenSessionListIsEmpty() {
        Assert.assertFalse(TermuxSessionsListViewController.isSessionIndexInRange(0, 0));
    }

    @Test
    public void sessionIndexWithinTheLiveSessionListIsInRange() {
        Assert.assertTrue(TermuxSessionsListViewController.isSessionIndexInRange(0, 2));
        Assert.assertTrue(TermuxSessionsListViewController.isSessionIndexInRange(1, 2));
    }

    @Test
    public void sessionIndexEqualToSessionCountIsOutOfRange() {
        Assert.assertFalse(TermuxSessionsListViewController.isSessionIndexInRange(2, 2));
    }

    @Test
    public void sessionRowLayoutHostsTheActiveIndicatorBarHiddenByDefaultAlongsideTheTitle() {
        Context context = RuntimeEnvironment.getApplication();
        View row = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_list, new FrameLayout(context), false);

        View activeIndicatorBar = row.findViewById(R.id.session_active_indicator_bar);
        View sessionTitle = row.findViewById(R.id.session_title);

        Assert.assertNotNull(activeIndicatorBar);
        Assert.assertNotNull(sessionTitle);
        Assert.assertEquals(View.GONE, activeIndicatorBar.getVisibility());
    }
}
