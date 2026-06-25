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
    public void projectHeaderActionIconsAreInteractiveChildrenSoTheTapDoesNotFallThroughToTheRowCollapseToggle() {
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
            Assert.assertTrue("action icon must be focusable so it is treated as an interactive child",
                actionIcon.isFocusable());
        }
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
    public void timestampLineShowsDashesForCallOutAndSeenWhenNoActivityIsRecorded() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        String line = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 61_000L);

        Assert.assertEquals("call: -         out: -         seen: -", line);
    }

    @Test
    public void outRendersADashRatherThanZeroSecondsWhenNoOutputIsRecorded() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 60_000L);
        store.recordSeen("worker", 60_000L);

        String line = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 60_000L);

        Assert.assertTrue("out must render a dash, never 0s, when unrecorded: " + line,
            line.contains("out: -"));
        Assert.assertFalse("out must not render 0s for unrecorded output: " + line,
            line.contains("out: 0s"));
    }

    @Test
    public void timestampLineIsEmptyForNullSessionName() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        String line = TermuxSessionsListViewController.buildTimestampLine(store, null, 61_000L);

        Assert.assertEquals("", line);
    }

    @Test
    public void timestampLineShowsDashesForUnsetCallAndSeenWhenOnlyOutputActivityIsRecorded() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("worker", 1_000L);

        String line = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 61_000L);

        Assert.assertEquals("call: -         out: 1m ago    seen: -", line);
    }

    @Test
    public void timestampLineShowsDashForUnsetSeenWhenCallAndOutAreRecorded() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L);
        store.recordOutputActivity("worker", 2_000L);

        String line = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 62_000L);

        Assert.assertEquals("call: 1m ago    out: 1m ago    seen: -", line);
    }

    @Test
    public void timestampLineKeepsSeenForTheCurrentlyActiveSession() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("worker", 1_000L);
        store.recordSeen("worker", 2_000L);

        String line = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 62_000L);

        Assert.assertEquals("call: -         out: 1m ago    seen: 1m ago", line);
    }

    @Test
    public void timestampLineBuildsAllThreePartsInCallOutSeenOrderWhenAllAreRecorded() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L);
        store.recordOutputActivity("worker", 1_000L);
        store.recordSeen("worker", 1_000L);

        String line = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 61_000L);

        Assert.assertEquals("call: 1m ago    out: 1m ago    seen: 1m ago", line);
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
    public void outValueDiffersBetweenSessionsWithDifferentLastOutputTimes() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("alpha", 10_000L);
        store.recordOutputActivity("beta", 130_000L);
        long nowMillis = 190_000L;

        String alphaLine = TermuxSessionsListViewController.buildTimestampLine(store, "alpha", nowMillis);
        String betaLine = TermuxSessionsListViewController.buildTimestampLine(store, "beta", nowMillis);

        Assert.assertEquals("call: -         out: 3m ago    seen: -", alphaLine);
        Assert.assertEquals("call: -         out: 1m ago    seen: -", betaLine);
        Assert.assertNotEquals(alphaLine, betaLine);
    }

    @Test
    public void outValueKeepsGrowingPastOneMinuteWithoutResetting() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("worker", 1_000L);

        String at30Seconds = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 31_000L);
        String at90Seconds = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 91_000L);
        String at150Seconds = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 151_000L);
        String at10Minutes = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 601_000L);

        Assert.assertEquals("call: -         out: 30s ago   seen: -", at30Seconds);
        Assert.assertEquals("call: -         out: 1m ago    seen: -", at90Seconds);
        Assert.assertEquals("call: -         out: 2m ago    seen: -", at150Seconds);
        Assert.assertEquals("call: -         out: 10m ago   seen: -", at10Minutes);
    }

    @Test
    public void outValueIsUnchangedByARefreshTickThatRecordsNoNewOutput() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("worker", 1_000L);

        String firstRender = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 121_000L);
        String secondRenderSameClock = TermuxSessionsListViewController.buildTimestampLine(store, "worker", 121_000L);

        Assert.assertEquals("call: -         out: 2m ago    seen: -", firstRender);
        Assert.assertEquals(firstRender, secondRenderSameClock);
        Assert.assertEquals(Long.valueOf(1_000L), store.getLastOutputActivityTimeMillis("worker"));
    }
}
