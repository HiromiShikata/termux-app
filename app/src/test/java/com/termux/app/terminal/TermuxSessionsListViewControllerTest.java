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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

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
    public void projectHeaderLayoutHostsADistinctNewIssueIconNextToTheOverviewAndTdpmConsoleIcons() {
        Context context = themedContext();
        View projectHeader = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_project_header, new FrameLayout(context), false);

        View overviewIcon = projectHeader.findViewById(R.id.session_project_header_overview_browser_icon);
        View tdpmConsoleIcon = projectHeader.findViewById(R.id.session_project_header_tdpm_console_icon);
        View newIssueIcon = projectHeader.findViewById(R.id.session_project_header_new_issue_icon);

        Assert.assertNotNull(newIssueIcon);
        Assert.assertNotEquals(overviewIcon.getId(), newIssueIcon.getId());
        Assert.assertNotEquals(tdpmConsoleIcon.getId(), newIssueIcon.getId());
        Assert.assertEquals(View.GONE, newIssueIcon.getVisibility());
    }

    @Test
    public void projectHeaderLayoutHostsACountBadgeDistinctFromTheTitle() {
        Context context = themedContext();
        View projectHeader = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_project_header, new FrameLayout(context), false);

        View titleView = projectHeader.findViewById(R.id.session_project_header_title);
        View countBadgeView = projectHeader.findViewById(R.id.session_project_header_count_badge);

        Assert.assertNotNull(countBadgeView);
        Assert.assertNotEquals(titleView.getId(), countBadgeView.getId());
    }

    @Test
    public void sessionCountBadgeTextRendersTheRawCount() {
        Assert.assertEquals("5", TermuxSessionsListViewController.sessionCountBadgeText(5));
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
        Context context = themedContext();
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

    @Test
    public void activeSessionIndicatorUsesTheNeutralGrayLightAccentInDayMode() {
        RuntimeEnvironment.setQualifiers("notnight");
        Context context = RuntimeEnvironment.getApplication();

        Assert.assertEquals(0xFF3A6F8F,
            ContextCompat.getColor(context, R.color.session_active_indicator));
    }

    @Test
    public void activeSessionIndicatorUsesTheNeutralGrayDarkAccentInNightMode() {
        RuntimeEnvironment.setQualifiers("night");
        Context context = RuntimeEnvironment.getApplication();

        Assert.assertEquals(0xFF8AB4C8,
            ContextCompat.getColor(context, R.color.session_active_indicator));
    }

    @Test
    public void darkChromeSurfacesUseTheMaterialDarkGrayFamilyRatherThanPureBlackInNightMode() {
        RuntimeEnvironment.setQualifiers("night");
        Context context = RuntimeEnvironment.getApplication();

        Assert.assertEquals(0xFF121212,
            ContextCompat.getColor(context, com.termux.shared.R.color.schema_background));
        Assert.assertEquals(0xFF1E1E1E,
            ContextCompat.getColor(context, com.termux.shared.R.color.schema_surface));
        Assert.assertEquals(0xFF242424,
            ContextCompat.getColor(context, com.termux.shared.R.color.schema_surface_elevated));
        Assert.assertEquals(0xFF2C2C2C,
            ContextCompat.getColor(context, com.termux.shared.R.color.schema_divider));
        Assert.assertNotEquals(Color.BLACK,
            ContextCompat.getColor(context, com.termux.shared.R.color.schema_background));
    }

    @Test
    public void lightChromeSurfacesUseTheNeutralGrayLightFamilyInDayMode() {
        RuntimeEnvironment.setQualifiers("notnight");
        Context context = RuntimeEnvironment.getApplication();

        Assert.assertEquals(0xFFFAFAFA,
            ContextCompat.getColor(context, com.termux.shared.R.color.schema_background));
        Assert.assertEquals(0xFFFFFFFF,
            ContextCompat.getColor(context, com.termux.shared.R.color.schema_surface));
        Assert.assertEquals(0xFFE0E0E0,
            ContextCompat.getColor(context, com.termux.shared.R.color.schema_divider));
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
    public void navigableSessionIndexesExcludeDisabledSessionsSoVolumeKeyNavigationSkipsThem() {
        List<Integer> visibleSessionIndexes = Arrays.asList(0, 1, 2);
        List<String> sessionNamesByIndex = Arrays.asList("alpha", "beta", "gamma");

        List<Integer> navigable = TermuxSessionsListViewController.navigableSessionIndexes(
            visibleSessionIndexes, sessionNamesByIndex, new LinkedHashSet<>(Collections.singletonList("beta")));

        Assert.assertEquals(Arrays.asList(0, 2), navigable);
    }

    @Test
    public void navigableSessionIndexesReturnAllVisibleIndexesWhenNoSessionIsDisabled() {
        List<Integer> visibleSessionIndexes = Arrays.asList(0, 1, 2);
        List<String> sessionNamesByIndex = Arrays.asList("alpha", "beta", "gamma");

        List<Integer> navigable = TermuxSessionsListViewController.navigableSessionIndexes(
            visibleSessionIndexes, sessionNamesByIndex, Collections.emptySet());

        Assert.assertEquals(visibleSessionIndexes, navigable);
    }

    @Test
    public void reEnablingADisabledSessionRestoresItToTheNavigableSet() {
        List<Integer> visibleSessionIndexes = Arrays.asList(0, 1, 2);
        List<String> sessionNamesByIndex = Arrays.asList("alpha", "beta", "gamma");

        List<Integer> whileDisabled = TermuxSessionsListViewController.navigableSessionIndexes(
            visibleSessionIndexes, sessionNamesByIndex, new LinkedHashSet<>(Collections.singletonList("beta")));
        List<Integer> afterReEnable = TermuxSessionsListViewController.navigableSessionIndexes(
            visibleSessionIndexes, sessionNamesByIndex, Collections.emptySet());

        Assert.assertEquals(Arrays.asList(0, 2), whileDisabled);
        Assert.assertEquals(Arrays.asList(0, 1, 2), afterReEnable);
    }

    @Test
    public void navigableSessionIndexesIgnoreDisabledNamesThatHaveNoMatchingSession() {
        List<Integer> visibleSessionIndexes = Arrays.asList(0, 1);
        List<String> sessionNamesByIndex = Arrays.asList("alpha", "beta");

        List<Integer> navigable = TermuxSessionsListViewController.navigableSessionIndexes(
            visibleSessionIndexes, sessionNamesByIndex, new LinkedHashSet<>(Collections.singletonList("missing")));

        Assert.assertEquals(Arrays.asList(0, 1), navigable);
    }

    @Test
    public void disabledSessionRowShowsTheDisabledNavigationIconAndEnabledRowShowsTheEnabledIcon() {
        Assert.assertEquals(R.drawable.ic_session_navigation_disabled,
            TermuxSessionsListViewController.sessionDisableToggleIconRes(true));
        Assert.assertEquals(R.drawable.ic_session_navigation_enabled,
            TermuxSessionsListViewController.sessionDisableToggleIconRes(false));
    }

    @Test
    public void sessionRowLayoutHostsTheRightEdgeDisableToggle() {
        Context context = themedContext();
        View row = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_list, new FrameLayout(context), false);

        View disableToggle = row.findViewById(R.id.session_disable_toggle);

        Assert.assertNotNull(disableToggle);
        Assert.assertTrue(disableToggle instanceof ImageView);
    }

    @Test
    public void sessionRowDoesNotReserveAForcedMinimumHeightSoUntitledRowsStayCompact() {
        Context context = themedContext();
        View row = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_list, new FrameLayout(context), false);

        Assert.assertEquals(0, row.getMinimumHeight());
    }
}
