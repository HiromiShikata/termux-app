package com.termux.app.terminal;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SessionBellIndicatorConsistencyTest {

    private static boolean bottomSheetShowsDot(SessionNewActivityIndicator indicator) {
        return TermuxSessionsListViewController.newActivityIndicatorDrawableRes(indicator.getTier())
            != R.drawable.ic_session_activity_dot_placeholder;
    }

    private static boolean pickerOverlayShowsDot(SessionNewActivityIndicator indicator) {
        return SessionSwitchPickerController.isBellMarkSlotVisible(indicator.isVisible());
    }

    @Test
    public void bothRenderersAgreeWhenAnUnseenSignalIsRecorded() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("background", 1_000L, "needs approval");
        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "background", 4_000L);

        Assert.assertEquals(bottomSheetShowsDot(indicator), pickerOverlayShowsDot(indicator));
        Assert.assertTrue(bottomSheetShowsDot(indicator));
    }

    @Test
    public void bothRenderersAgreeWhenNoSignalIsRecorded() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "background", 4_000L);

        Assert.assertEquals(bottomSheetShowsDot(indicator), pickerOverlayShowsDot(indicator));
        Assert.assertFalse(bottomSheetShowsDot(indicator));
    }

    @Test
    public void bothRenderersDeriveTheSameAgeLabelFromTheSharedHelper() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("background", 1_000L, "needs approval");
        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "background", 31_000L);

        String bottomSheetLabel = "  " + indicator.getLabel();
        String pickerLabel = SessionSwitchPickerController.newActivityLabelSlotText(indicator.getLabel());

        Assert.assertEquals("30s ago", indicator.getLabel());
        Assert.assertEquals(bottomSheetLabel, pickerLabel);
    }

    @Test
    public void sessionShowsNoIndicatorOnceTheUserHasRepliedToTheExplicitCall() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("active", 1_000L, "needs approval");
        store.recordUserInput("active", 2_000L);
        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "active", 4_000L);

        Assert.assertFalse(indicator.isVisible());
        Assert.assertEquals("", indicator.getLabel());
        Assert.assertFalse(bottomSheetShowsDot(indicator));
        Assert.assertFalse(pickerOverlayShowsDot(indicator));
    }

    @Test
    public void bottomSheetReservesAFixedIndicatorSlotRegardlessOfIndicatorPresence() {
        int redDrawable = TermuxSessionsListViewController.newActivityIndicatorDrawableRes(
            SessionNewActivityTier.RED);
        int yellowDrawable = TermuxSessionsListViewController.newActivityIndicatorDrawableRes(
            SessionNewActivityTier.YELLOW);
        int absentDrawable = TermuxSessionsListViewController.newActivityIndicatorDrawableRes(
            SessionNewActivityTier.NONE);

        Assert.assertNotEquals(0, redDrawable);
        Assert.assertNotEquals(0, yellowDrawable);
        Assert.assertNotEquals(0, absentDrawable);
        Assert.assertEquals(R.drawable.ic_session_activity_dot_red, redDrawable);
        Assert.assertEquals(R.drawable.ic_session_activity_dot_yellow, yellowDrawable);
        Assert.assertEquals(R.drawable.ic_session_activity_dot_placeholder, absentDrawable);
    }

    @Test
    public void pickerOverlayMarksTheSlotVisibleOnlyWhenAnActivityIsPresent() {
        Assert.assertFalse(SessionSwitchPickerController.isBellMarkSlotVisible(false));
        Assert.assertTrue(SessionSwitchPickerController.isBellMarkSlotVisible(true));
    }

    @Test
    public void bottomSheetRowKeepsAStartCompoundDrawableWithStableBoundsWhetherOrNotIndicatorIsPresent() {
        TextView withIndicator = new TextView(RuntimeEnvironment.getApplication());
        withIndicator.setCompoundDrawablesRelativeWithIntrinsicBounds(
            TermuxSessionsListViewController.newActivityIndicatorDrawableRes(SessionNewActivityTier.RED), 0, 0, 0);

        TextView withoutIndicator = new TextView(RuntimeEnvironment.getApplication());
        withoutIndicator.setCompoundDrawablesRelativeWithIntrinsicBounds(
            TermuxSessionsListViewController.newActivityIndicatorDrawableRes(SessionNewActivityTier.NONE), 0, 0, 0);

        Drawable presentStartDrawable = withIndicator.getCompoundDrawablesRelative()[0];
        Drawable absentStartDrawable = withoutIndicator.getCompoundDrawablesRelative()[0];

        Assert.assertNotNull(presentStartDrawable);
        Assert.assertNotNull(absentStartDrawable);
        Assert.assertEquals(presentStartDrawable.getIntrinsicWidth(), absentStartDrawable.getIntrinsicWidth());
        Assert.assertEquals(presentStartDrawable.getIntrinsicHeight(), absentStartDrawable.getIntrinsicHeight());
    }

    @Test
    public void pickerOverlayWidthIsIdenticalForOneItemAndManyItems() {
        DisplayMetrics displayMetrics = RuntimeEnvironment.getApplication().getResources().getDisplayMetrics();

        int widthForOneItem = SessionSwitchPickerController.overlayWidthPxForLineCount(1, displayMetrics);
        int widthForManyItems = SessionSwitchPickerController.overlayWidthPxForLineCount(50, displayMetrics);

        Assert.assertTrue(widthForOneItem > 0);
        Assert.assertEquals(widthForOneItem, widthForManyItems);
    }

    @Test
    public void appliedPickerOverlayWidthMatchesTheFixedDimensionRegardlessOfItemCount() {
        DisplayMetrics displayMetrics = RuntimeEnvironment.getApplication().getResources().getDisplayMetrics();

        int fixedWidth = SessionSwitchPickerController.overlayWidthPx(displayMetrics);

        Assert.assertEquals(fixedWidth, SessionSwitchPickerController.overlayWidthPxForLineCount(1, displayMetrics));
        Assert.assertEquals(fixedWidth, SessionSwitchPickerController.overlayWidthPxForLineCount(100, displayMetrics));
    }

    @Test
    public void pickerOverlayStructureViewDeclaresAFixedWidthAndContentSizedHeightWithoutMargins() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        View root = LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
        TextView structureView = root.findViewById(R.id.session_switch_picker_structure);
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        ViewGroup.MarginLayoutParams layoutParams =
            (ViewGroup.MarginLayoutParams) structureView.getLayoutParams();

        Assert.assertNotEquals(ViewGroup.LayoutParams.WRAP_CONTENT, layoutParams.width);
        Assert.assertEquals(SessionSwitchPickerController.overlayWidthPx(displayMetrics), layoutParams.width);
        Assert.assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, layoutParams.height);
        Assert.assertEquals(0, layoutParams.topMargin);
        Assert.assertEquals(0, layoutParams.bottomMargin);
        Assert.assertEquals(0, layoutParams.getMarginStart());
        Assert.assertEquals(0, layoutParams.leftMargin);
    }

    @Test
    public void pickerOverlayPanelSitsFlushAgainstTheTopLeftCornerWithoutLeadingPadding() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        View root = LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
        TextView structureView = root.findViewById(R.id.session_switch_picker_structure);

        Assert.assertEquals(0, structureView.getPaddingStart());
        Assert.assertEquals(0, structureView.getPaddingTop());
        Assert.assertEquals(0, structureView.getPaddingLeft());
    }

    @Test
    public void pickerOverlayContainerHasNoFullScreenScrimSoTheTerminalStaysVisible() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        View root = LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);

        View overlayContainer = root.findViewById(R.id.session_switch_picker_overlay);

        Assert.assertNull("overlay container must not paint a full-screen scrim",
            overlayContainer.getBackground());
    }

    @Test
    public void pickerOverlayMaxHeightIsCappedBelowFullScreenSoTallListsScrollInsteadOfFillingTheScreen() {
        DisplayMetrics displayMetrics = RuntimeEnvironment.getApplication().getResources().getDisplayMetrics();

        int maxHeight = SessionSwitchPickerController.overlayMaxHeightPx(displayMetrics);

        Assert.assertTrue(maxHeight > 0);
        Assert.assertTrue(maxHeight < displayMetrics.heightPixels);
    }
}
