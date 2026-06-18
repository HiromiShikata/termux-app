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

    private static boolean bottomSheetShowsBell(Long bellArrivalTimeMillis,
                                                SessionOutputActivityStore outputActivityStore,
                                                String sessionHandle) {
        boolean hasIndicator = TermuxSessionsListViewController.hasNewActivityIndicator(
            bellArrivalTimeMillis, outputActivityStore, sessionHandle);
        return TermuxSessionsListViewController.newActivityIndicatorDrawableRes(hasIndicator)
            == R.drawable.ic_session_bell_notification;
    }

    private static boolean pickerOverlayShowsBell(Long bellArrivalTimeMillis,
                                                  SessionOutputActivityStore outputActivityStore,
                                                  String sessionHandle) {
        boolean hasIndicator = TermuxSessionsListViewController.hasNewActivityIndicator(
            bellArrivalTimeMillis, outputActivityStore, sessionHandle);
        return SessionSwitchPickerController.isBellMarkSlotVisible(hasIndicator);
    }

    @Test
    public void bothRenderersAgreeWhenOnlyOutputActivityIsRecorded() {
        SessionOutputActivityStore outputActivityStore = new SessionOutputActivityStore();
        outputActivityStore.markOutputActivity("handle");

        Assert.assertEquals(
            bottomSheetShowsBell(null, outputActivityStore, "handle"),
            pickerOverlayShowsBell(null, outputActivityStore, "handle"));
        Assert.assertTrue(bottomSheetShowsBell(null, outputActivityStore, "handle"));
    }

    @Test
    public void bothRenderersAgreeWhenOnlyBellIsRecorded() {
        SessionOutputActivityStore outputActivityStore = new SessionOutputActivityStore();

        Assert.assertEquals(
            bottomSheetShowsBell(1_000L, outputActivityStore, "handle"),
            pickerOverlayShowsBell(1_000L, outputActivityStore, "handle"));
        Assert.assertTrue(bottomSheetShowsBell(1_000L, outputActivityStore, "handle"));
    }

    @Test
    public void bothRenderersAgreeWhenNeitherBellNorOutputActivityIsRecorded() {
        SessionOutputActivityStore outputActivityStore = new SessionOutputActivityStore();

        Assert.assertEquals(
            bottomSheetShowsBell(null, outputActivityStore, "handle"),
            pickerOverlayShowsBell(null, outputActivityStore, "handle"));
        Assert.assertFalse(bottomSheetShowsBell(null, outputActivityStore, "handle"));
    }

    @Test
    public void bothRenderersAgreeWhenBothBellAndOutputActivityAreRecorded() {
        SessionOutputActivityStore outputActivityStore = new SessionOutputActivityStore();
        outputActivityStore.markOutputActivity("handle");

        Assert.assertEquals(
            bottomSheetShowsBell(1_000L, outputActivityStore, "handle"),
            pickerOverlayShowsBell(1_000L, outputActivityStore, "handle"));
        Assert.assertTrue(bottomSheetShowsBell(1_000L, outputActivityStore, "handle"));
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
