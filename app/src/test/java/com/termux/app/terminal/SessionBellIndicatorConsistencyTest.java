package com.termux.app.terminal;

import android.graphics.drawable.Drawable;
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
}
