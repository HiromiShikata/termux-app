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

    private static boolean bottomSheetShowsBell(SessionNewActivityIndicator indicator) {
        return TermuxSessionsListViewController.newActivityIndicatorDrawableRes(indicator.isVisible())
            == R.drawable.ic_session_bell_notification;
    }

    private static boolean pickerOverlayShowsBell(SessionNewActivityIndicator indicator) {
        return SessionSwitchPickerController.isBellMarkSlotVisible(indicator.isVisible());
    }

    @Test
    public void bothRenderersAgreeWhenAnUnseenBellIsRecorded() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("background-handle", 1_000L);
        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "background-handle", 4_000L);

        Assert.assertEquals(bottomSheetShowsBell(indicator), pickerOverlayShowsBell(indicator));
        Assert.assertTrue(bottomSheetShowsBell(indicator));
    }

    @Test
    public void bothRenderersAgreeWhenNoBellIsRecorded() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "background-handle", 4_000L);

        Assert.assertEquals(bottomSheetShowsBell(indicator), pickerOverlayShowsBell(indicator));
        Assert.assertFalse(bottomSheetShowsBell(indicator));
    }

    @Test
    public void bothRenderersDeriveTheSameAgeLabelFromTheSharedHelper() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("background-handle", 1_000L);
        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "background-handle", 31_000L);

        String bottomSheetLabel = "  " + indicator.getLabel();
        String pickerLabel = SessionSwitchPickerController.newActivityLabelSlotText(indicator.getLabel());

        Assert.assertEquals("30s ago", indicator.getLabel());
        Assert.assertEquals(bottomSheetLabel, pickerLabel);
    }

    @Test
    public void activeSessionShowsNoIndicatorPurelyBecauseLastSeenCaughtUpToTheBell() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("active-handle", 1_000L);
        store.recordSeen("active-handle", 2_000L);
        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "active-handle", 4_000L);

        Assert.assertFalse(indicator.isVisible());
        Assert.assertEquals("", indicator.getLabel());
        Assert.assertFalse(bottomSheetShowsBell(indicator));
        Assert.assertFalse(pickerOverlayShowsBell(indicator));
    }

    @Test
    public void bottomSheetReservesAFixedIndicatorSlotRegardlessOfIndicatorPresence() {
        int presentDrawable = TermuxSessionsListViewController.newActivityIndicatorDrawableRes(true);
        int absentDrawable = TermuxSessionsListViewController.newActivityIndicatorDrawableRes(false);

        Assert.assertNotEquals(0, presentDrawable);
        Assert.assertNotEquals(0, absentDrawable);
        Assert.assertEquals(R.drawable.ic_session_bell_notification, presentDrawable);
        Assert.assertEquals(R.drawable.ic_session_bell_notification_placeholder, absentDrawable);
    }

    @Test
    public void pickerOverlayEmitsTheSameBellMarkSlotTextWhetherOrNotMarked() {
        Assert.assertFalse(SessionSwitchPickerController.bellMarkSlotText().isEmpty());
        Assert.assertFalse(SessionSwitchPickerController.isBellMarkSlotVisible(false));
        Assert.assertTrue(SessionSwitchPickerController.isBellMarkSlotVisible(true));
    }

    @Test
    public void bottomSheetRowKeepsAStartCompoundDrawableWithStableBoundsWhetherOrNotIndicatorIsPresent() {
        TextView withIndicator = new TextView(RuntimeEnvironment.getApplication());
        withIndicator.setCompoundDrawablesRelativeWithIntrinsicBounds(
            TermuxSessionsListViewController.newActivityIndicatorDrawableRes(true), 0, 0, 0);

        TextView withoutIndicator = new TextView(RuntimeEnvironment.getApplication());
        withoutIndicator.setCompoundDrawablesRelativeWithIntrinsicBounds(
            TermuxSessionsListViewController.newActivityIndicatorDrawableRes(false), 0, 0, 0);

        Drawable presentStartDrawable = withIndicator.getCompoundDrawablesRelative()[0];
        Drawable absentStartDrawable = withoutIndicator.getCompoundDrawablesRelative()[0];

        Assert.assertNotNull(presentStartDrawable);
        Assert.assertNotNull(absentStartDrawable);
        Assert.assertEquals(presentStartDrawable.getIntrinsicWidth(), absentStartDrawable.getIntrinsicWidth());
        Assert.assertEquals(presentStartDrawable.getIntrinsicHeight(), absentStartDrawable.getIntrinsicHeight());
    }

    @Test
    public void pickerOverlayHeightIsIdenticalForOneItemAndManyItems() {
        DisplayMetrics displayMetrics = RuntimeEnvironment.getApplication().getResources().getDisplayMetrics();

        int heightForOneItem = SessionSwitchPickerController.overlayHeightPxForLineCount(1, displayMetrics);
        int heightForManyItems = SessionSwitchPickerController.overlayHeightPxForLineCount(50, displayMetrics);

        Assert.assertTrue(heightForOneItem > 0);
        Assert.assertEquals(heightForOneItem, heightForManyItems);
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
    public void appliedPickerOverlaySizeMatchesTheFixedDimensionRegardlessOfItemCount() {
        DisplayMetrics displayMetrics = RuntimeEnvironment.getApplication().getResources().getDisplayMetrics();

        int fixedHeight = SessionSwitchPickerController.overlayHeightPx(displayMetrics);
        int fixedWidth = SessionSwitchPickerController.overlayWidthPx(displayMetrics);

        Assert.assertEquals(fixedHeight, SessionSwitchPickerController.overlayHeightPxForLineCount(1, displayMetrics));
        Assert.assertEquals(fixedHeight, SessionSwitchPickerController.overlayHeightPxForLineCount(100, displayMetrics));
        Assert.assertEquals(fixedWidth, SessionSwitchPickerController.overlayWidthPxForLineCount(1, displayMetrics));
        Assert.assertEquals(fixedWidth, SessionSwitchPickerController.overlayWidthPxForLineCount(100, displayMetrics));
    }

    @Test
    public void pickerOverlayStructureViewDeclaresAFixedSizeInTheLayoutMatchingTheControllerConstants() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        View root = LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
        TextView structureView = root.findViewById(R.id.session_switch_picker_structure);
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        ViewGroup.LayoutParams layoutParams = structureView.getLayoutParams();

        Assert.assertNotEquals(ViewGroup.LayoutParams.WRAP_CONTENT, layoutParams.width);
        Assert.assertNotEquals(ViewGroup.LayoutParams.WRAP_CONTENT, layoutParams.height);
        Assert.assertEquals(SessionSwitchPickerController.overlayWidthPx(displayMetrics), layoutParams.width);
        Assert.assertEquals(SessionSwitchPickerController.overlayHeightPx(displayMetrics), layoutParams.height);
    }
}
