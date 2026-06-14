package com.termux.app.terminal;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

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
    public void hidesProjectHeaderIconWhenNoActionIsProvided() {
        ImageView icon = new ImageView(RuntimeEnvironment.getApplication());
        icon.setVisibility(View.VISIBLE);

        TermuxSessionsListViewController.applyProjectHeaderIconVisibility(icon, null);

        Assert.assertEquals(View.GONE, icon.getVisibility());
        Assert.assertFalse(icon.hasOnClickListeners());
    }

    @Test
    public void showsProjectHeaderIconAndInvokesActionOnClickWhenActionIsProvided() {
        ImageView icon = new ImageView(RuntimeEnvironment.getApplication());
        icon.setVisibility(View.GONE);
        boolean[] opened = {false};

        TermuxSessionsListViewController.applyProjectHeaderIconVisibility(icon, () -> opened[0] = true);

        Assert.assertEquals(View.VISIBLE, icon.getVisibility());
        Assert.assertTrue(icon.hasOnClickListeners());

        icon.performClick();

        Assert.assertTrue(opened[0]);
    }

    @Test
    public void projectHeaderLayoutHostsADistinctTdpmConsoleIconNextToTheOverviewIcon() {
        Context context = themedContext();
        View projectHeader = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_project_header, new FrameLayout(context), false);

        View overviewIcon = projectHeader.findViewById(R.id.session_project_header_overview_browser_icon);
        View tdpmConsoleIcon = projectHeader.findViewById(R.id.session_project_header_tdpm_console_icon);

        Assert.assertNotNull(overviewIcon);
        Assert.assertNotNull(tdpmConsoleIcon);
        Assert.assertNotEquals(overviewIcon.getId(), tdpmConsoleIcon.getId());
        Assert.assertEquals(View.GONE, tdpmConsoleIcon.getVisibility());
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
    public void sessionRowLayoutHostsTheActiveIndicatorBarReservingItsGutterByDefaultAlongsideTheTitle() {
        Context context = RuntimeEnvironment.getApplication();
        View row = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_list, new FrameLayout(context), false);

        View activeIndicatorBar = row.findViewById(R.id.session_active_indicator_bar);
        View sessionTitle = row.findViewById(R.id.session_title);

        Assert.assertNotNull(activeIndicatorBar);
        Assert.assertNotNull(sessionTitle);
        Assert.assertEquals(View.INVISIBLE, activeIndicatorBar.getVisibility());
    }

    @Test
    public void currentSessionRowShowsTheAccentColoredIndicatorBar() {
        View activeIndicatorBar = new View(RuntimeEnvironment.getApplication());
        int accentColor = 0xFF8AB4C8;

        TermuxSessionsListViewController.applyActiveIndicatorBarVisibility(activeIndicatorBar, true, accentColor);

        Assert.assertEquals(View.VISIBLE, activeIndicatorBar.getVisibility());
        Assert.assertTrue(activeIndicatorBar.getBackground() instanceof ColorDrawable);
        Assert.assertEquals(accentColor, ((ColorDrawable) activeIndicatorBar.getBackground()).getColor());
    }

    @Test
    public void nonCurrentSessionRowKeepsTheIndicatorBarInvisibleSoItStillReservesTheGutter() {
        View activeIndicatorBar = new View(RuntimeEnvironment.getApplication());
        int accentColor = 0xFF8AB4C8;

        TermuxSessionsListViewController.applyActiveIndicatorBarVisibility(activeIndicatorBar, false, accentColor);

        Assert.assertEquals(View.INVISIBLE, activeIndicatorBar.getVisibility());
        Assert.assertTrue(activeIndicatorBar.getBackground() instanceof ColorDrawable);
        Assert.assertEquals(Color.TRANSPARENT, ((ColorDrawable) activeIndicatorBar.getBackground()).getColor());
    }

    @Test
    public void currentSessionRowUsesTheCurrentSessionHighlightBackground() {
        Assert.assertEquals(R.drawable.current_session,
            TermuxSessionsListViewController.sessionRowBackgroundRes(true));
    }

    @Test
    public void nonCurrentSessionRowUsesTheNormalRippleBackground() {
        Assert.assertEquals(R.drawable.session_ripple,
            TermuxSessionsListViewController.sessionRowBackgroundRes(false));
    }

    @Test
    public void projectHeaderBackgroundIsDistinctFromTheListSurface() {
        Context context = themedContext();
        View projectHeader = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_project_header, new FrameLayout(context), false);

        Assert.assertTrue(projectHeader.getBackground() instanceof ColorDrawable);
        int headerColor = ((ColorDrawable) projectHeader.getBackground()).getColor();
        Assert.assertEquals(ContextCompat.getColor(context, com.termux.shared.R.color.schema_surface_elevated), headerColor);
        Assert.assertNotEquals(ContextCompat.getColor(context, com.termux.shared.R.color.schema_surface), headerColor);
    }

    @Test
    public void projectHeaderHasSymmetricVerticalPadding() {
        Context context = themedContext();
        View projectHeader = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_project_header, new FrameLayout(context), false);

        Assert.assertEquals(projectHeader.getPaddingTop(), projectHeader.getPaddingBottom());
    }

    private static Context themedContext() {
        return new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
    }

    @Test
    public void storyHeaderHasSymmetricVerticalPadding() {
        Context context = RuntimeEnvironment.getApplication();
        View storyHeader = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_story_header, new FrameLayout(context), false);

        Assert.assertEquals(storyHeader.getPaddingTop(), storyHeader.getPaddingBottom());
    }

    @Test
    public void sessionRowDoesNotReserveAForcedMinimumHeightSoUntitledRowsStayCompact() {
        Context context = RuntimeEnvironment.getApplication();
        View row = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_list, new FrameLayout(context), false);

        Assert.assertEquals(0, row.getMinimumHeight());
    }
}
