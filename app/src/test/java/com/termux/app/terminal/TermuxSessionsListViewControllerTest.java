package com.termux.app.terminal;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

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
    public void projectHeaderActionIconsExposeAtLeastTheAccessibilityMinimumTouchTarget() {
        Context context = themedContext();
        View projectHeader = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_project_header, new FrameLayout(context), false);

        int minimumTouchTargetPx = Math.round(48f
            * context.getResources().getDisplayMetrics().density);

        int[] actionIconIds = {
            R.id.session_project_header_overview_browser_icon,
            R.id.session_project_header_tdpm_console_icon,
            R.id.session_project_header_new_issue_icon
        };

        for (int actionIconId : actionIconIds) {
            View actionIcon = projectHeader.findViewById(actionIconId);
            ViewGroup.LayoutParams layoutParams = actionIcon.getLayoutParams();
            Assert.assertTrue("touch target width below the accessibility minimum",
                layoutParams.width >= minimumTouchTargetPx);
            Assert.assertTrue("touch target height below the accessibility minimum",
                layoutParams.height >= minimumTouchTargetPx);
        }
    }

    @Test
    public void projectHeaderActionIconsAreClickableButNotFocusableSoTheRowItemClickDoesNotConsumeTheirTap() {
        Context context = themedContext();
        View projectHeader = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_project_header, new FrameLayout(context), false);

        int[] actionIconIds = {
            R.id.session_project_header_overview_browser_icon,
            R.id.session_project_header_tdpm_console_icon,
            R.id.session_project_header_new_issue_icon
        };

        for (int actionIconId : actionIconIds) {
            View actionIcon = projectHeader.findViewById(actionIconId);
            Assert.assertTrue("action icon must be clickable so it wins the touch over the row click",
                actionIcon.isClickable());
            Assert.assertFalse("a focusable action icon makes AbsListView treat the row as a single clickable unit and swallow the tap",
                actionIcon.isFocusable());
            Assert.assertFalse(actionIcon.isFocusableInTouchMode());
        }
    }

    @Test
    public void projectHeaderRowDoesNotReportFocusableSoTheListDeliversTheTapToTheClickableActionIcons() {
        Context context = themedContext();
        ViewGroup projectHeader = (ViewGroup) LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_project_header, new FrameLayout(context), false);

        Assert.assertFalse("a row reporting focusable descendants makes AbsListView consume the action-icon tap as a row item-click",
            projectHeader.hasFocusable());
    }

    @Test
    public void projectHeaderRowBlocksDescendantsSoTheListDeliversEveryRowTapToOnItemClick() {
        Context context = themedContext();
        ViewGroup projectHeader = (ViewGroup) LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_project_header, new FrameLayout(context), false);

        Assert.assertEquals("the row must block descendant focusability so AbsListView passes its hasFocusable() gate and fires onItemClick on the first row tap",
            ViewGroup.FOCUS_BLOCK_DESCENDANTS, projectHeader.getDescendantFocusability());
    }

    @Test
    public void projectHeaderRowIsNotClickableItselfSoItDoesNotStealTheFirstTapFromTheActionIcons() {
        Context context = themedContext();
        ViewGroup projectHeader = (ViewGroup) LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_project_header, new FrameLayout(context), false);

        Assert.assertFalse("a self-clickable row routes ACTION_DOWN through AbsListView row pressed-state handling and consumes the first tap that lands on an action icon",
            projectHeader.isClickable());
        Assert.assertFalse(projectHeader.hasOnClickListeners());
    }

    @Test
    public void tappingAProjectHeaderActionIconRunsItsActionOnTheFirstTapWithoutTheRowConsumingIt() {
        Context context = themedContext();
        ViewGroup projectHeader = (ViewGroup) LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_project_header, new FrameLayout(context), false);
        boolean[] overviewOpened = {false};

        View overviewIcon = projectHeader.findViewById(R.id.session_project_header_overview_browser_icon);
        TermuxSessionsListViewController.applyProjectHeaderIconVisibility(
            overviewIcon, () -> overviewOpened[0] = true);

        overviewIcon.performClick();

        Assert.assertTrue("the action icon must perform its own action on a single tap", overviewOpened[0]);
        Assert.assertFalse("the row must not be clickable, so the action-icon tap cannot also toggle the accordion",
            projectHeader.isClickable());
    }

    @Test
    public void everyProjectHeaderActionIconRunsOnlyItsOwnActionWithoutTogglingTheAccordion() {
        Context context = themedContext();
        ViewGroup projectHeader = (ViewGroup) LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_project_header, new FrameLayout(context), false);

        int[] actionIconIds = {
            R.id.session_project_header_overview_browser_icon,
            R.id.session_project_header_tdpm_console_icon,
            R.id.session_project_header_new_issue_icon
        };

        for (int actionIconId : actionIconIds) {
            boolean[] actionRan = {false};
            View actionIcon = projectHeader.findViewById(actionIconId);
            TermuxSessionsListViewController.applyProjectHeaderIconVisibility(
                actionIcon, () -> actionRan[0] = true);

            boolean handled = actionIcon.performClick();

            Assert.assertTrue("the right-end action icon must consume its own tap", handled);
            Assert.assertTrue("tapping the right-end action icon must run its action", actionRan[0]);
        }

        Assert.assertFalse("the project header row must not be clickable, so a right-end action tap cannot also toggle the accordion",
            projectHeader.isClickable());
        Assert.assertFalse("the project header row must report no focusable descendants, so AbsListView delivers the tap to the action icon rather than firing onItemClick (accordion toggle)",
            projectHeader.hasFocusable());
    }

    @Test
    public void projectHeaderTitleAppendsSessionCountInParenthesesAfterTheName() {
        Assert.assertEquals("ProjectName (3)",
            TermuxSessionsListViewController.projectHeaderTitle("ProjectName", 3));
        Assert.assertEquals("ProjectName (0)",
            TermuxSessionsListViewController.projectHeaderTitle("ProjectName", 0));
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
    public void sessionRowLayoutHostsADedicatedTimesViewThatWrapsAndIsHiddenUntilBound() {
        Context context = themedContext();
        View row = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_list, new FrameLayout(context), false);

        android.widget.TextView timesView = row.findViewById(R.id.session_row_times);

        Assert.assertNotNull(timesView);
        Assert.assertEquals(View.GONE, timesView.getVisibility());
        Assert.assertNull(timesView.getEllipsize());
        Assert.assertTrue(timesView.getMaxLines() > 1);
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
    public void disabledSessionIsExcludedFromActivityIndicatorsSoItNeverGetsATier() {
        Assert.assertTrue(TermuxSessionsListViewController.isExcludedFromActivityIndicators(
            "beta", new LinkedHashSet<>(Collections.singletonList("beta"))));
    }

    @Test
    public void enabledSessionIsNotExcludedFromActivityIndicators() {
        Assert.assertFalse(TermuxSessionsListViewController.isExcludedFromActivityIndicators(
            "alpha", new LinkedHashSet<>(Collections.singletonList("beta"))));
    }

    @Test
    public void unnamedSessionIsNotExcludedFromActivityIndicators() {
        Assert.assertFalse(TermuxSessionsListViewController.isExcludedFromActivityIndicators(
            null, new LinkedHashSet<>(Collections.singletonList("beta"))));
    }

    @Test
    public void noSessionIsExcludedFromActivityIndicatorsWhenNothingIsDisabled() {
        Assert.assertFalse(TermuxSessionsListViewController.isExcludedFromActivityIndicators(
            "alpha", Collections.emptySet()));
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
    public void sessionRowBlocksDescendantFocusSoTheListDoesNotStealTheDisableToggleTap() {
        Context context = themedContext();
        ViewGroup row = (ViewGroup) LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_list, new FrameLayout(context), false);

        Assert.assertEquals(ViewGroup.FOCUS_BLOCK_DESCENDANTS, row.getDescendantFocusability());
    }

    @Test
    public void disableToggleIsClickableButNotFocusableSoTheRowItemClickDoesNotConsumeItsTap() {
        Context context = themedContext();
        View row = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_list, new FrameLayout(context), false);

        View disableToggle = row.findViewById(R.id.session_disable_toggle);

        Assert.assertTrue(disableToggle.isClickable());
        Assert.assertFalse(disableToggle.isFocusable());
        Assert.assertFalse(disableToggle.isFocusableInTouchMode());
    }

    @Test
    public void rowDoesNotReportFocusableSoTheListDeliversTheTapToTheClickableDisableToggle() {
        Context context = themedContext();
        ViewGroup row = (ViewGroup) LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_list, new FrameLayout(context), false);

        Assert.assertFalse("a row reporting focusable descendants makes AbsListView consume the toggle tap as a row item-click",
            row.hasFocusable());
    }

    @Test
    public void tappingTheDisableToggleInvokesItsClickHandlerRatherThanSelectingTheRow() {
        Context context = themedContext();
        View row = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_list, new FrameLayout(context), false);

        View disableToggle = row.findViewById(R.id.session_disable_toggle);
        boolean[] toggled = {false};
        disableToggle.setOnClickListener(v -> toggled[0] = true);

        boolean handled = disableToggle.performClick();

        Assert.assertTrue(handled);
        Assert.assertTrue(toggled[0]);
    }

    @Test
    public void disableToggleExposesAtLeastTheAccessibilityMinimumTouchTargetSoTapsDoNotFallThroughToTheRow() {
        Context context = themedContext();
        View row = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_list, new FrameLayout(context), false);

        View disableToggle = row.findViewById(R.id.session_disable_toggle);
        ViewGroup.LayoutParams layoutParams = disableToggle.getLayoutParams();

        int minimumTouchTargetPx = Math.round(48f
            * context.getResources().getDisplayMetrics().density);

        Assert.assertTrue("disable toggle touch target width below the accessibility minimum",
            layoutParams.width >= minimumTouchTargetPx);
        Assert.assertTrue("disable toggle touch target height below the accessibility minimum",
            layoutParams.height >= minimumTouchTargetPx);
        Assert.assertTrue("disable toggle minWidth below the accessibility minimum",
            disableToggle.getMinimumWidth() >= minimumTouchTargetPx);
        Assert.assertTrue("disable toggle minHeight below the accessibility minimum",
            disableToggle.getMinimumHeight() >= minimumTouchTargetPx);
    }

    @Test
    public void timestampLineShowsMoreThanOneDayForCallOutAndReplyWhenNoStatuslineIsRecorded() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        String line = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 61_000L);

        Assert.assertEquals("call: >1d  out: >1d  reply: >1d  sub: 0", line);
    }

    @Test
    public void timestampLineNeverDerivesCallOrOutFromGenuineActivityFallbacks() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 60_000L);
        store.recordOutputActivity("worker", 60_000L);
        store.recordSeen("worker", 60_000L);

        String line = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 61_000L);

        Assert.assertEquals("call: >1d  out: >1d  reply: >1d  sub: 0", line);
    }

    @Test
    public void timestampLineShowsTheAppCapturedOwnerInputTimeAsTheReplyEvenBeforeAnyStatuslineReply() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordUserInput("worker", 60_000L);

        String line = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 61_000L);

        Assert.assertEquals("call: >1d  out: >1d  reply: 1s  sub: 0", line);
    }

    @Test
    public void timestampLineShowsTheAppInputTimeAsTheReplyWhenItIsNewerThanTheStatuslineReply() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("worker", null, null, 2_000L);
        store.recordUserInput("worker", 60_000L);

        String line = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 61_000L);

        Assert.assertEquals("call: >1d  out: >1d  reply: 1s  sub: 0", line);
    }

    @Test
    public void timestampLineFallsBackToTheStatuslineReplyWhenNoAppInputIsHeld() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("worker", null, null, 2_000L);

        String line = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 62_000L);

        Assert.assertEquals("call: >1d  out: >1d  reply: 1m  sub: 0", line);
    }

    @Test
    public void timestampLineIsEmptyForNullSessionName() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        String line = TermuxSessionsListViewController.buildTimestampLine(store, null, 61_000L);

        Assert.assertEquals("", line);
    }

    @Test
    public void timestampLineShowsMoreThanOneDayForUnsetCallAndReplyWhenOnlyStatuslineOutIsRecorded() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("worker", null, 1_000L, null);

        String line = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 61_000L);

        Assert.assertEquals("call: >1d  out: 1m  reply: >1d  sub: 0", line);
    }

    @Test
    public void timestampLineShowsMoreThanOneDayForUnsetReplyWhenStatuslineCallAndOutAreRecorded() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("worker", 1_000L, 2_000L, null);

        String line = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 62_000L);

        Assert.assertEquals("call: 1m  out: 1m  reply: >1d  sub: 0", line);
    }

    @Test
    public void timestampLineShowsReplyWhenStatuslineReplyIsRecorded() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("worker", null, null, 2_000L);

        String line = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 62_000L);

        Assert.assertEquals("call: >1d  out: >1d  reply: 1m  sub: 0", line);
    }

    @Test
    public void timestampLineBuildsAllThreePartsInCallOutReplyOrderWhenAllAreRecorded() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("worker", 1_000L, 1_000L, 1_000L);

        String line = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 61_000L);

        Assert.assertEquals("call: 1m  out: 1m  reply: 1m  sub: 0", line);
    }

    @Test
    public void timestampLineMatchesTheCurrentSessionInfoAreaLineFromTheSameEffectiveReplySource() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("worker", 1_000L, 2_000L, null);
        store.recordUserInput("worker", 30_000L);
        long nowMillis = 62_000L;

        String rowLine = TermuxSessionsListViewController.buildTimestampLine(store, "worker", nowMillis);
        String infoAreaLine = SessionTimesLine.of(
            store.getStatuslineCallTimeMillis("worker"),
            store.getStatuslineOutTimeMillis("worker"),
            store.effectiveReplyTimeMillis("worker"),
            store.getSubagentCount("worker"),
            nowMillis).getText();

        Assert.assertEquals(infoAreaLine, rowLine);
    }

    @Test
    public void sessionRowHostsAGroupDividerSoStackedSessionBlocksAreVisuallySeparated() {
        Context context = themedContext();
        View row = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_list, new FrameLayout(context), false);

        View groupDivider = row.findViewById(R.id.session_row_group_divider);

        Assert.assertNotNull(groupDivider);
    }

    @Test
    public void groupDividerIsShownBetweenTwoConsecutiveSessionRowsSoEachSessionInfoBlockIsDistinct() {
        Assert.assertTrue(TermuxSessionsListViewController.shouldShowSessionRowGroupDivider(
            SessionHierarchyRow.Type.SESSION));
    }

    @Test
    public void groupDividerIsHiddenForTheFirstSessionUnderAStoryHeaderSoNoStrayLineSitsBelowTheHeader() {
        Assert.assertFalse(TermuxSessionsListViewController.shouldShowSessionRowGroupDivider(
            SessionHierarchyRow.Type.STORY_HEADER));
    }

    @Test
    public void groupDividerIsHiddenForTheFirstSessionUnderAProjectHeaderSoNoStrayLineSitsBelowTheHeader() {
        Assert.assertFalse(TermuxSessionsListViewController.shouldShowSessionRowGroupDivider(
            SessionHierarchyRow.Type.PROJECT_HEADER));
    }

    @Test
    public void groupDividerIsHiddenForTheVeryFirstRowWhichHasNoPrecedingRow() {
        Assert.assertFalse(TermuxSessionsListViewController.shouldShowSessionRowGroupDivider(null));
    }

    @Test
    public void disableToggleIsVerticallyCenteredSoItAlignsWithTheRowContentRatherThanSittingAtTheTop() {
        Context context = themedContext();
        View row = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_list, new FrameLayout(context), false);

        View disableToggle = row.findViewById(R.id.session_disable_toggle);
        android.widget.LinearLayout.LayoutParams layoutParams =
            (android.widget.LinearLayout.LayoutParams) disableToggle.getLayoutParams();

        Assert.assertEquals(android.view.Gravity.CENTER_VERTICAL,
            layoutParams.gravity & android.view.Gravity.VERTICAL_GRAVITY_MASK);
        Assert.assertNotEquals(android.view.Gravity.TOP,
            layoutParams.gravity & android.view.Gravity.VERTICAL_GRAVITY_MASK);
    }

    @Test
    public void sessionRowDoesNotReserveAForcedMinimumHeightSoUntitledRowsStayCompact() {
        Context context = themedContext();
        View row = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_list, new FrameLayout(context), false);

        Assert.assertEquals(0, row.getMinimumHeight());
    }

    @Test
    public void outValueDiffersBetweenSessionsWithDifferentStatuslineOutTimes() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("alpha", null, 10_000L, null);
        store.recordStatuslineTimes("beta", null, 130_000L, null);
        long nowMillis = 190_000L;

        String alphaLine = TermuxSessionsListViewController.buildTimestampLine(store, "alpha", nowMillis);
        String betaLine = TermuxSessionsListViewController.buildTimestampLine(store, "beta", nowMillis);

        Assert.assertEquals("call: >1d  out: 3m  reply: >1d  sub: 0", alphaLine);
        Assert.assertEquals("call: >1d  out: 1m  reply: >1d  sub: 0", betaLine);
        Assert.assertNotEquals(alphaLine, betaLine);
    }

    @Test
    public void outValueKeepsGrowingPastOneMinuteWithoutResetting() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("worker", null, 1_000L, null);

        String at30Seconds = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 31_000L);
        String at90Seconds = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 91_000L);
        String at150Seconds = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 151_000L);
        String at10Minutes = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 601_000L);

        Assert.assertEquals("call: >1d  out: 30s  reply: >1d  sub: 0", at30Seconds);
        Assert.assertEquals("call: >1d  out: 1m  reply: >1d  sub: 0", at90Seconds);
        Assert.assertEquals("call: >1d  out: 2m  reply: >1d  sub: 0", at150Seconds);
        Assert.assertEquals("call: >1d  out: 10m  reply: >1d  sub: 0", at10Minutes);
    }

    @Test
    public void outValueIsUnchangedByARefreshTickThatRecordsNoNewStatuslineOutput() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("worker", null, 1_000L, null);

        String firstRender = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 121_000L);
        String secondRenderSameClock = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 121_000L);

        Assert.assertEquals("call: >1d  out: 2m  reply: >1d  sub: 0", firstRender);
        Assert.assertEquals(firstRender, secondRenderSameClock);
        Assert.assertEquals(Long.valueOf(1_000L), store.getStatuslineOutTimeMillis("worker"));
    }

    @Test
    public void sessionRowStableIdReusesTheSessionIndexSoTheSameSessionKeepsItsViewHolderAcrossUpdates() {
        long firstSessionId = TermuxSessionsListViewController.rowItemId(SessionHierarchyRow.session(0));
        long sameSessionIdAfterRebuild = TermuxSessionsListViewController.rowItemId(SessionHierarchyRow.session(0));
        long otherSessionId = TermuxSessionsListViewController.rowItemId(SessionHierarchyRow.session(1));

        Assert.assertEquals(0L, firstSessionId);
        Assert.assertEquals(firstSessionId, sameSessionIdAfterRebuild);
        Assert.assertNotEquals(firstSessionId, otherSessionId);
    }

    @Test
    public void headerAndSessionRowsCarryDistinctStableIdsSoMixedItemTypesNeverCollide() {
        long projectHeaderId = TermuxSessionsListViewController.rowItemId(
            SessionHierarchyRow.projectHeader("alpha"));
        long storyHeaderId = TermuxSessionsListViewController.rowItemId(
            SessionHierarchyRow.storyHeader("alpha"));
        long sessionRowId = TermuxSessionsListViewController.rowItemId(SessionHierarchyRow.session(0));

        Assert.assertNotEquals(projectHeaderId, storyHeaderId);
        Assert.assertNotEquals(projectHeaderId, sessionRowId);
        Assert.assertNotEquals(storyHeaderId, sessionRowId);
    }

    @Test
    public void projectHeaderStableIdFollowsItsLabelSoTheSameProjectKeepsItsIdentity() {
        long alphaHeaderId = TermuxSessionsListViewController.rowItemId(
            SessionHierarchyRow.projectHeader("alpha"));
        long alphaHeaderIdAgain = TermuxSessionsListViewController.rowItemId(
            SessionHierarchyRow.projectHeader("alpha"));
        long betaHeaderId = TermuxSessionsListViewController.rowItemId(
            SessionHierarchyRow.projectHeader("beta"));

        Assert.assertEquals(alphaHeaderId, alphaHeaderIdAgain);
        Assert.assertNotEquals(alphaHeaderId, betaHeaderId);
    }

    @Test
    public void diffTreatsTheSameSessionIndexAsTheSameRowSoUnchangedRowsAreNotTornDown() {
        Assert.assertTrue(TermuxSessionsListViewController.sameRowIdentity(
            SessionHierarchyRow.session(2), SessionHierarchyRow.session(2)));
        Assert.assertFalse(TermuxSessionsListViewController.sameRowIdentity(
            SessionHierarchyRow.session(2), SessionHierarchyRow.session(3)));
    }

    @Test
    public void diffTreatsTheSameProjectHeaderLabelAsTheSameRowAcrossRebuilds() {
        Assert.assertTrue(TermuxSessionsListViewController.sameRowIdentity(
            SessionHierarchyRow.projectHeader("alpha"), SessionHierarchyRow.projectHeader("alpha")));
        Assert.assertFalse(TermuxSessionsListViewController.sameRowIdentity(
            SessionHierarchyRow.projectHeader("alpha"), SessionHierarchyRow.projectHeader("beta")));
    }

    @Test
    public void diffNeverMatchesAcrossDifferentRowTypesEvenWhenTheirLabelsCoincide() {
        Assert.assertFalse(TermuxSessionsListViewController.sameRowIdentity(
            SessionHierarchyRow.projectHeader("alpha"), SessionHierarchyRow.storyHeader("alpha")));
        Assert.assertFalse(TermuxSessionsListViewController.sameRowIdentity(
            SessionHierarchyRow.storyHeader("alpha"), SessionHierarchyRow.session(0)));
    }

    @Test
    public void relativeTimePayloadIsASingleSharedTokenSoTickUpdatesAreRecognizedAsTimeOnly() {
        Assert.assertNotNull(TermuxSessionsListViewController.RELATIVE_TIME_PAYLOAD);
        Assert.assertSame(TermuxSessionsListViewController.RELATIVE_TIME_PAYLOAD,
            TermuxSessionsListViewController.RELATIVE_TIME_PAYLOAD);
    }

    @Test
    public void rowTimesStartPaddingOffsetsTheTitleIndentByTheActivityDotIconWidthAndPadding() {
        Assert.assertEquals(24 + 16 + 4,
            TermuxSessionsListViewController.sessionRowTimesStartPaddingPx(24, 16, 4));
        Assert.assertEquals(6 + 16 + 4,
            TermuxSessionsListViewController.sessionRowTimesStartPaddingPx(6, 16, 4));
    }

    @Test
    public void alignSessionRowTimesStartSetsStartPaddingSoTimesLeftEdgeMatchesTitleText() {
        TextView times = new TextView(RuntimeEnvironment.getApplication());
        times.setPaddingRelative(6, 3, 9, 12);

        TermuxSessionsListViewController.alignSessionRowTimesStartWithTitleText(times, 44);

        Assert.assertEquals(44, times.getPaddingStart());
        Assert.assertEquals("vertical and end padding must be preserved while only the start padding is aligned",
            3, times.getPaddingTop());
        Assert.assertEquals(9, times.getPaddingEnd());
        Assert.assertEquals(12, times.getPaddingBottom());
    }
}
