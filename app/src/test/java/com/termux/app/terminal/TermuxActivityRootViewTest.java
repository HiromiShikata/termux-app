package com.termux.app.terminal;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.InputQueue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.termux.app.TermuxActivity;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.function.Consumer;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.R)
public class TermuxActivityRootViewTest {

    private static final Rect BASELINE_WINDOW_AVAILABLE_RECT = new Rect(0, 0, 1080, 2000);
    private static final int BASELINE_BOTTOM_SPACE_VIEW_LOCATION_X = 0;
    private static final int BASELINE_BOTTOM_SPACE_VIEW_LOCATION_Y = 1900;
    private static final int BASELINE_BOTTOM_SPACE_VIEW_WIDTH = 1080;
    private static final int BASELINE_BOTTOM_SPACE_VIEW_HEIGHT = 100;

    private void setFieldByReflection(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class FakeDecorView extends View {
        private WindowInsets currentRootWindowInsets;

        FakeDecorView(Context context) {
            super(context);
        }

        void setCurrentRootWindowInsets(WindowInsets insets) {
            this.currentRootWindowInsets = insets;
        }

        @Override
        public WindowInsets getRootWindowInsets() {
            return currentRootWindowInsets;
        }
    }

    private static final class FakeWindow extends Window {
        private final FakeDecorView decorView;

        FakeWindow(Context context) {
            super(context);
            decorView = new FakeDecorView(context);
        }

        @Override public View getDecorView() { return decorView; }
        @Override public View peekDecorView() { return decorView; }
        @Override public void addContentView(View view, ViewGroup.LayoutParams params) { }
        @Override public void closeAllPanels() { }
        @Override public void closePanel(int featureId) { }
        @Override public View getCurrentFocus() { return null; }
        @Override public LayoutInflater getLayoutInflater() { return null; }
        @Override public int getNavigationBarColor() { return 0; }
        @Override public int getStatusBarColor() { return 0; }
        @Override public int getVolumeControlStream() { return 0; }
        @Override public void invalidatePanelMenu(int featureId) { }
        @Override public boolean isFloating() { return false; }
        @Override public boolean isShortcutKey(int keyCode, KeyEvent event) { return false; }
        @Override protected void onActive() { }
        @Override public void onConfigurationChanged(android.content.res.Configuration newConfig) { }
        @Override public void openPanel(int featureId, KeyEvent event) { }
        @Override public boolean performContextMenuIdentifierAction(int id, int flags) { return false; }
        @Override public boolean performPanelIdentifierAction(int featureId, int id, int flags) { return false; }
        @Override public boolean performPanelShortcut(int featureId, int keyCode, KeyEvent event, int flags) { return false; }
        @Override public void restoreHierarchyState(Bundle savedInstanceState) { }
        @Override public Bundle saveHierarchyState() { return null; }
        @Override public void setBackgroundDrawable(Drawable drawable) { }
        @Override public void setChildDrawable(int featureId, Drawable drawable) { }
        @Override public void setChildInt(int featureId, int value) { }
        @Override public void setContentView(View view) { }
        @Override public void setContentView(View view, ViewGroup.LayoutParams params) { }
        @Override public void setContentView(int layoutResID) { }
        @Override public void setDecorCaptionShade(int decorCaptionShade) { }
        @Override public void setFeatureDrawable(int featureId, Drawable drawable) { }
        @Override public void setFeatureDrawableAlpha(int featureId, int alpha) { }
        @Override public void setFeatureDrawableResource(int featureId, int resId) { }
        @Override public void setFeatureDrawableUri(int featureId, Uri uri) { }
        @Override public void setFeatureInt(int featureId, int value) { }
        @Override public void setNavigationBarColor(int color) { }
        @Override public void setResizingCaptionDrawable(Drawable drawable) { }
        @Override public void setStatusBarColor(int color) { }
        @Override public void setTitle(CharSequence title) { }
        @Override public void setTitleColor(int textColor) { }
        @Override public void setVolumeControlStream(int streamType) { }
        @Override public boolean superDispatchGenericMotionEvent(MotionEvent event) { return false; }
        @Override public boolean superDispatchKeyEvent(KeyEvent event) { return false; }
        @Override public boolean superDispatchKeyShortcutEvent(KeyEvent event) { return false; }
        @Override public boolean superDispatchTouchEvent(MotionEvent event) { return false; }
        @Override public boolean superDispatchTrackballEvent(MotionEvent event) { return false; }
        @Override public void takeInputQueue(InputQueue.Callback callback) { }
        @Override public void takeKeyEvents(boolean get) { }
        @Override public void takeSurface(SurfaceHolder.Callback2 callback) { }
        @Override public void togglePanel(int featureId, KeyEvent event) { }
    }

    private static final class ControllableBottomSpaceView extends View {
        private final Rect windowVisibleDisplayFrame;
        private final int locationInWindowX;
        private final int locationInWindowY;

        ControllableBottomSpaceView(Context context, Rect windowVisibleDisplayFrame, int locationInWindowX, int locationInWindowY, int width, int height) {
            super(context);
            this.windowVisibleDisplayFrame = windowVisibleDisplayFrame;
            this.locationInWindowX = locationInWindowX;
            this.locationInWindowY = locationInWindowY;
            layout(0, 0, width, height);
        }

        @Override
        public boolean isShown() {
            return true;
        }

        @Override
        public void getWindowVisibleDisplayFrame(Rect outRect) {
            outRect.set(windowVisibleDisplayFrame);
        }

        @Override
        public void getLocationInWindow(int[] outLocation) {
            outLocation[0] = locationInWindowX;
            outLocation[1] = locationInWindowY;
        }
    }

    /**
     * A {@link TermuxActivityRootView} whose {@link #getRootWindowInsets()} is overridden to return
     * a caller-controlled {@link WindowInsets} rather than whatever the simulated window reports.
     * Used only by the API-29 genuinely-attached-view tests below, since the existing {@code FakeWindow}
     * / {@code FakeDecorView} fixtures never become {@link View#isAttachedToWindow() attached} and so
     * cannot reach the view-aware branch of the two-argument {@code toWindowInsetsCompat} overload.
     */
    private static final class WindowInsetsReturningTermuxActivityRootView extends TermuxActivityRootView {
        private WindowInsets rootWindowInsetsToReturn;

        WindowInsetsReturningTermuxActivityRootView(Context context) {
            super(context);
        }

        void setRootWindowInsetsToReturn(WindowInsets insets) {
            this.rootWindowInsetsToReturn = insets;
        }

        @Override
        public WindowInsets getRootWindowInsets() {
            return rootWindowInsetsToReturn;
        }
    }

    /**
     * A minimal {@link Window} whose {@link #getDecorView()} / {@link #peekDecorView()} return a
     * caller-supplied {@link View} directly, so that view can be reused as both the decor view
     * {@link TermuxActivityRootView#onGlobalLayout()} queries and the production
     * {@link TermuxActivityRootView} under test.
     */
    private static final class SingleDecorViewFakeWindow extends Window {
        private final View decorView;

        SingleDecorViewFakeWindow(Context context, View decorView) {
            super(context);
            this.decorView = decorView;
        }

        @Override public View getDecorView() { return decorView; }
        @Override public View peekDecorView() { return decorView; }
        @Override public void addContentView(View view, ViewGroup.LayoutParams params) { }
        @Override public void closeAllPanels() { }
        @Override public void closePanel(int featureId) { }
        @Override public View getCurrentFocus() { return null; }
        @Override public LayoutInflater getLayoutInflater() { return null; }
        @Override public int getNavigationBarColor() { return 0; }
        @Override public int getStatusBarColor() { return 0; }
        @Override public int getVolumeControlStream() { return 0; }
        @Override public void invalidatePanelMenu(int featureId) { }
        @Override public boolean isFloating() { return false; }
        @Override public boolean isShortcutKey(int keyCode, KeyEvent event) { return false; }
        @Override protected void onActive() { }
        @Override public void onConfigurationChanged(android.content.res.Configuration newConfig) { }
        @Override public void openPanel(int featureId, KeyEvent event) { }
        @Override public boolean performContextMenuIdentifierAction(int id, int flags) { return false; }
        @Override public boolean performPanelIdentifierAction(int featureId, int id, int flags) { return false; }
        @Override public boolean performPanelShortcut(int featureId, int keyCode, KeyEvent event, int flags) { return false; }
        @Override public void restoreHierarchyState(Bundle savedInstanceState) { }
        @Override public Bundle saveHierarchyState() { return null; }
        @Override public void setBackgroundDrawable(Drawable drawable) { }
        @Override public void setChildDrawable(int featureId, Drawable drawable) { }
        @Override public void setChildInt(int featureId, int value) { }
        @Override public void setContentView(View view) { }
        @Override public void setContentView(View view, ViewGroup.LayoutParams params) { }
        @Override public void setContentView(int layoutResID) { }
        @Override public void setDecorCaptionShade(int decorCaptionShade) { }
        @Override public void setFeatureDrawable(int featureId, Drawable drawable) { }
        @Override public void setFeatureDrawableAlpha(int featureId, int alpha) { }
        @Override public void setFeatureDrawableResource(int featureId, int resId) { }
        @Override public void setFeatureDrawableUri(int featureId, Uri uri) { }
        @Override public void setFeatureInt(int featureId, int value) { }
        @Override public void setNavigationBarColor(int color) { }
        @Override public void setResizingCaptionDrawable(Drawable drawable) { }
        @Override public void setStatusBarColor(int color) { }
        @Override public void setTitle(CharSequence title) { }
        @Override public void setTitleColor(int textColor) { }
        @Override public void setVolumeControlStream(int streamType) { }
        @Override public boolean superDispatchGenericMotionEvent(MotionEvent event) { return false; }
        @Override public boolean superDispatchKeyEvent(KeyEvent event) { return false; }
        @Override public boolean superDispatchKeyShortcutEvent(KeyEvent event) { return false; }
        @Override public boolean superDispatchTrackballEvent(MotionEvent event) { return false; }
        @Override public void takeInputQueue(InputQueue.Callback callback) { }
        @Override public void takeKeyEvents(boolean get) { }
        @Override public void takeSurface(SurfaceHolder.Callback2 callback) { }
        @Override public void togglePanel(int featureId, KeyEvent event) { }
        @Override public boolean superDispatchTouchEvent(MotionEvent event) { return false; }
    }

    /**
     * Builds a {@link WindowInsetsReturningTermuxActivityRootView} that is genuinely attached to a
     * real (Robolectric-simulated) window — {@link View#isAttachedToWindow()} is {@code true} — unlike
     * the {@code FakeWindow}/{@code FakeDecorView} fixtures used by the rest of this suite, so that the
     * view-aware branch of the two-argument {@code toWindowInsetsCompat} overload actually activates.
     *
     * <p>A bare {@link WindowManager#addView(View, ViewGroup.LayoutParams)} call on the application
     * {@link WindowManager} does not reach Robolectric's real {@code ViewRootImpl} attachment path, so
     * {@link View#isAttachedToWindow()} stays {@code false} afterwards. Driving a throwaway carrier
     * {@link Activity} through the real create/start/resume/visible lifecycle and installing this view
     * as its content view does reach that path, because that is the same mechanism
     * {@link org.robolectric.Robolectric#setupActivity(Class)}-style tests rely on throughout this
     * codebase to obtain a genuinely attached view.
     */
    private WindowInsetsReturningTermuxActivityRootView buildGenuinelyAttachedRootViewReturning(WindowInsets rootWindowInsetsToReturn) {
        Context context = RuntimeEnvironment.getApplication();
        WindowInsetsReturningTermuxActivityRootView rootView = new WindowInsetsReturningTermuxActivityRootView(context);
        rootView.setRootWindowInsetsToReturn(rootWindowInsetsToReturn);
        rootView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Activity carrierActivity = Robolectric.buildActivity(Activity.class)
            .create()
            .start()
            .resume()
            .visible()
            .get();
        carrierActivity.setContentView(rootView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        return rootView;
    }

    private TermuxActivity buildVisibleActivityWithControllableWindowInsets() throws Exception {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        FakeWindow fakeWindow = new FakeWindow(activity);
        fakeWindow.setCallback(activity);
        setFieldByReflection(activity, Activity.class, "mWindow", fakeWindow);
        setFieldByReflection(activity, TermuxActivity.class, "mIsVisible", true);
        return activity;
    }

    private void setKeyboardVisible(TermuxActivity activity, boolean keyboardVisible) {
        WindowInsets insets = new WindowInsets.Builder()
            .setVisible(WindowInsets.Type.ime(), keyboardVisible)
            .build();
        ((FakeDecorView) activity.getWindow().getDecorView()).setCurrentRootWindowInsets(insets);
    }

    private void setKeyboardVisibleWithInsetHeight(TermuxActivity activity, boolean keyboardVisible, int imeInsetHeightPx) {
        WindowInsets.Builder builder = new WindowInsets.Builder()
            .setVisible(WindowInsets.Type.ime(), keyboardVisible);
        if (keyboardVisible) {
            builder.setInsets(WindowInsets.Type.ime(), android.graphics.Insets.of(0, 0, 0, imeInsetHeightPx));
        }
        WindowInsets insets = builder.build();
        ((FakeDecorView) activity.getWindow().getDecorView()).setCurrentRootWindowInsets(insets);
    }

    private WindowInsets buildWindowInsetsCarryingKeyboardState(boolean keyboardVisible, int imeInsetHeightPx) {
        WindowInsets.Builder builder = new WindowInsets.Builder()
            .setVisible(WindowInsets.Type.ime(), keyboardVisible);
        if (keyboardVisible) {
            builder.setInsets(WindowInsets.Type.ime(), android.graphics.Insets.of(0, 0, 0, imeInsetHeightPx));
        }
        return builder.build();
    }

    private void setBottomSpaceView(TermuxActivity activity, View bottomSpaceView) throws Exception {
        setFieldByReflection(activity, TermuxActivity.class, "mTermuxActivityBottomSpaceView", bottomSpaceView);
    }

    private TermuxActivityRootView buildRootView(TermuxActivity activity) {
        TermuxActivityRootView rootView = new TermuxActivityRootView(activity);
        rootView.setActivity(activity);
        rootView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return rootView;
    }

    private int bottomMarginOf(TermuxActivityRootView rootView) {
        return ((FrameLayout.LayoutParams) rootView.getLayoutParams()).bottomMargin;
    }

    private void setBottomMarginOf(TermuxActivityRootView rootView, int bottomMargin) {
        ((FrameLayout.LayoutParams) rootView.getLayoutParams()).bottomMargin = bottomMargin;
    }

    private ControllableBottomSpaceView newBaselineBottomSpaceView(Context context) {
        return new ControllableBottomSpaceView(
            context,
            BASELINE_WINDOW_AVAILABLE_RECT,
            BASELINE_BOTTOM_SPACE_VIEW_LOCATION_X,
            BASELINE_BOTTOM_SPACE_VIEW_LOCATION_Y,
            BASELINE_BOTTOM_SPACE_VIEW_WIDTH,
            BASELINE_BOTTOM_SPACE_VIEW_HEIGHT
        );
    }

    private void runBaselineCallEstablishingInertState(TermuxActivityRootView rootView, TermuxActivity activity) throws Exception {
        setBottomSpaceView(activity, newBaselineBottomSpaceView(RuntimeEnvironment.getApplication()));
        rootView.onGlobalLayout();
    }

    private static final class GateIntegrationCase {
        final String name;
        final boolean secondCallKeyboardVisible;
        final int expectedBottomMarginAfterSecondCall;

        GateIntegrationCase(String name, boolean secondCallKeyboardVisible, int expectedBottomMarginAfterSecondCall) {
            this.name = name;
            this.secondCallKeyboardVisible = secondCallKeyboardVisible;
            this.expectedBottomMarginAfterSecondCall = expectedBottomMarginAfterSecondCall;
        }
    }

    @Test
    public void onGlobalLayoutGateIntegrationControlsWhetherMarginRecomputeRuns() throws Exception {
        int sentinelBottomMarginProvingWhetherRecomputeRan = 9999;
        GateIntegrationCase[] cases = new GateIntegrationCase[]{
            new GateIntegrationCase(
                "unchanged keyboard-visibility between calls leaves the sentinel margin untouched",
                false,
                sentinelBottomMarginProvingWhetherRecomputeRan
            ),
            new GateIntegrationCase(
                "changed keyboard-visibility between calls overwrites the sentinel margin",
                true,
                0
            ),
        };

        for (GateIntegrationCase c : cases) {
            TermuxActivity activity = buildVisibleActivityWithControllableWindowInsets();
            setKeyboardVisible(activity, false);
            TermuxActivityRootView rootView = buildRootView(activity);

            Rect hiddenByTwoHundredPixelsWindowAvailableRect = new Rect(0, 0, 1080, 1700);
            ControllableBottomSpaceView partiallyHiddenBottomSpaceView = new ControllableBottomSpaceView(
                RuntimeEnvironment.getApplication(), hiddenByTwoHundredPixelsWindowAvailableRect, 0, 1800, 1080, 100
            );
            setBottomSpaceView(activity, partiallyHiddenBottomSpaceView);

            rootView.onGlobalLayout();
            Assert.assertEquals(c.name + ": first call must establish a 200px margin", 200, bottomMarginOf(rootView));

            setBottomMarginOf(rootView, sentinelBottomMarginProvingWhetherRecomputeRan);
            setKeyboardVisible(activity, c.secondCallKeyboardVisible);
            rootView.onGlobalLayout();

            Assert.assertEquals(c.name, c.expectedBottomMarginAfterSecondCall, bottomMarginOf(rootView));
        }
    }

    private static final class RegressionPinningCase {
        final String name;
        final Rect secondCallWindowAvailableRect;
        final int secondCallBottomSpaceViewLocationX;
        final int secondCallBottomSpaceViewLocationY;
        final int secondCallBottomSpaceViewWidth;
        final int secondCallBottomSpaceViewHeight;
        final Consumer<TermuxActivityRootView> injectStaleStatePreSecondCall;
        final Consumer<TermuxActivityRootView> assertCorrectionAppliedAfterSecondCall;

        RegressionPinningCase(
            String name,
            Rect secondCallWindowAvailableRect,
            int secondCallBottomSpaceViewLocationX,
            int secondCallBottomSpaceViewLocationY,
            int secondCallBottomSpaceViewWidth,
            int secondCallBottomSpaceViewHeight,
            Consumer<TermuxActivityRootView> injectStaleStatePreSecondCall,
            Consumer<TermuxActivityRootView> assertCorrectionAppliedAfterSecondCall
        ) {
            this.name = name;
            this.secondCallWindowAvailableRect = secondCallWindowAvailableRect;
            this.secondCallBottomSpaceViewLocationX = secondCallBottomSpaceViewLocationX;
            this.secondCallBottomSpaceViewLocationY = secondCallBottomSpaceViewLocationY;
            this.secondCallBottomSpaceViewWidth = secondCallBottomSpaceViewWidth;
            this.secondCallBottomSpaceViewHeight = secondCallBottomSpaceViewHeight;
            this.injectStaleStatePreSecondCall = injectStaleStatePreSecondCall;
            this.assertCorrectionAppliedAfterSecondCall = assertCorrectionAppliedAfterSecondCall;
        }
    }

    private void runRegressionPinningCase(RegressionPinningCase c) throws Exception {
        TermuxActivity activity = buildVisibleActivityWithControllableWindowInsets();
        setKeyboardVisible(activity, false);
        TermuxActivityRootView rootView = buildRootView(activity);

        runBaselineCallEstablishingInertState(rootView, activity);
        Assert.assertEquals(c.name + ": baseline call must leave bottomMargin at 0", 0, bottomMarginOf(rootView));
        Assert.assertNull(c.name + ": baseline call must leave marginBottom unset", rootView.marginBottom);
        Assert.assertNull(c.name + ": baseline call must leave lastMarginBottom unset", rootView.lastMarginBottom);

        c.injectStaleStatePreSecondCall.accept(rootView);

        ControllableBottomSpaceView secondCallBottomSpaceView = new ControllableBottomSpaceView(
            RuntimeEnvironment.getApplication(),
            c.secondCallWindowAvailableRect,
            c.secondCallBottomSpaceViewLocationX,
            c.secondCallBottomSpaceViewLocationY,
            c.secondCallBottomSpaceViewWidth,
            c.secondCallBottomSpaceViewHeight
        );
        setBottomSpaceView(activity, secondCallBottomSpaceView);
        setKeyboardVisible(activity, false);

        rootView.onGlobalLayout();

        c.assertCorrectionAppliedAfterSecondCall.accept(rootView);
    }

    @Test
    public void onGlobalLayoutMustStillClearStaleLastMarginBottomOnFreeformTaskbarExtraMarginGeometryWhenKeyboardVisibilityIsUnchanged() throws Exception {
        runRegressionPinningCase(new RegressionPinningCase(
            "isVisibleBecauseExtraMargin freeform/landscape-taskbar geometry correction clears the stale lastMarginBottom",
            new Rect(0, 0, 1080, 2000),
            0, 1700, 1080, 200,
            rootView -> rootView.lastMarginBottom = 777,
            rootView -> Assert.assertNull(
                "isVisibleBecauseExtraMargin correction must clear the stale lastMarginBottom sentinel",
                rootView.lastMarginBottom
            )
        ));
    }

    @Test
    public void onGlobalLayoutMustStillForceCorrectWrongMarginOnExtraKeysToggleGeometryWhenKeyboardVisibilityIsUnchanged() throws Exception {
        runRegressionPinningCase(new RegressionPinningCase(
            "pxHidden>0 && bottomMargin>0 wrong-margin force-correction resets the stale margin to zero",
            new Rect(0, 0, 1080, 1700),
            0, 1600, 1080, 400,
            rootView -> setBottomMarginOf(rootView, 500),
            rootView -> Assert.assertEquals(
                "wrong-margin force-correction must reset the stale 500px margin to 0",
                0, bottomMarginOf(rootView)
            )
        ));
    }

    @Test
    public void onGlobalLayoutMustStillScheduleZeroPendingMarginOnReturnToFullScreenGeometryWhenKeyboardVisibilityIsUnchanged() throws Exception {
        runRegressionPinningCase(new RegressionPinningCase(
            "return-to-full-screen resize while visible-because-of-margin schedules a pending margin of zero",
            new Rect(0, 0, 1080, 2000),
            0, 1600, 1080, 400,
            rootView -> setBottomMarginOf(rootView, 400),
            rootView -> Assert.assertEquals(
                "return-to-full-screen resize must schedule marginBottom 0 to be applied on next measure",
                Integer.valueOf(0), rootView.marginBottom
            )
        ));
    }

    private void applyPendingMeasuredMarginOntoRealLayoutParamsAsADevicesMeasurePassWould(TermuxActivityRootView rootView, int windowWidth, int windowHeight) {
        rootView.onMeasure(
            View.MeasureSpec.makeMeasureSpec(windowWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(windowHeight, View.MeasureSpec.EXACTLY)
        );
    }

    @Test
    public void onGlobalLayoutMustConvergeAndStopChangingMarginAcrossAContinuousResizeSequenceWithConstantKeyboardVisibility() throws Exception {
        TermuxActivity activity = buildVisibleActivityWithControllableWindowInsets();
        setKeyboardVisible(activity, false);
        TermuxActivityRootView rootView = buildRootView(activity);

        int windowWidth = 1080;
        int bottomSpaceViewHeight = 100;
        int fixedStructuralOffsetPxUnrelatedToMargin = 60;
        int[] resizeSequenceWindowHeights = {2000, 1800, 1600, 1400, 1200, 1200, 1200};
        int firstSettledWindowHeightIndex = 4;

        int[] bottomMarginAfterEachPass = new int[resizeSequenceWindowHeights.length];

        for (int i = 0; i < resizeSequenceWindowHeights.length; i++) {
            int windowHeight = resizeSequenceWindowHeights[i];
            int marginLeftInEffectByPreviousPass = bottomMarginOf(rootView);

            int bottomSpaceViewBottom = windowHeight + fixedStructuralOffsetPxUnrelatedToMargin - marginLeftInEffectByPreviousPass;
            int bottomSpaceViewLocationY = bottomSpaceViewBottom - bottomSpaceViewHeight;

            Rect windowAvailableRect = new Rect(0, 0, windowWidth, windowHeight);
            ControllableBottomSpaceView bottomSpaceView = new ControllableBottomSpaceView(
                RuntimeEnvironment.getApplication(), windowAvailableRect, 0, bottomSpaceViewLocationY, windowWidth, bottomSpaceViewHeight
            );
            setBottomSpaceView(activity, bottomSpaceView);
            setKeyboardVisible(activity, false);

            rootView.onGlobalLayout();
            applyPendingMeasuredMarginOntoRealLayoutParamsAsADevicesMeasurePassWould(rootView, windowWidth, windowHeight);

            bottomMarginAfterEachPass[i] = bottomMarginOf(rootView);

            Thread.sleep(50);
        }

        for (int i = firstSettledWindowHeightIndex + 1; i < bottomMarginAfterEachPass.length; i++) {
            Assert.assertEquals(
                "bottom margin must not keep changing on pass #" + i + " versus pass #" + firstSettledWindowHeightIndex
                    + " once the window height and keyboard visibility both stopped changing (all margins recorded: "
                    + java.util.Arrays.toString(bottomMarginAfterEachPass) + ")",
                bottomMarginAfterEachPass[firstSettledWindowHeightIndex],
                bottomMarginAfterEachPass[i]
            );
        }
    }

    @Test
    public void onGlobalLayoutMustSettleOnTheCorrectMarginWhenTheFinalRealResizeStepLandsOnASuppressedPass() throws Exception {
        TermuxActivity activity = buildVisibleActivityWithControllableWindowInsets();
        setKeyboardVisible(activity, false);
        TermuxActivityRootView rootView = buildRootView(activity);

        int windowWidth = 1080;
        int bottomSpaceViewHeight = 100;
        int fixedStructuralOffsetPxUnrelatedToMargin = 60;
        int[] resizeSequenceWindowHeights = {2000, 1800, 1600, 1600, 1600};
        int firstSettledWindowHeightIndex = 2;

        int[] bottomMarginAfterEachPass = new int[resizeSequenceWindowHeights.length];

        for (int i = 0; i < resizeSequenceWindowHeights.length; i++) {
            int windowHeight = resizeSequenceWindowHeights[i];
            int marginLeftInEffectByPreviousPass = bottomMarginOf(rootView);

            int bottomSpaceViewBottom = windowHeight + fixedStructuralOffsetPxUnrelatedToMargin - marginLeftInEffectByPreviousPass;
            int bottomSpaceViewLocationY = bottomSpaceViewBottom - bottomSpaceViewHeight;

            Rect windowAvailableRect = new Rect(0, 0, windowWidth, windowHeight);
            ControllableBottomSpaceView bottomSpaceView = new ControllableBottomSpaceView(
                RuntimeEnvironment.getApplication(), windowAvailableRect, 0, bottomSpaceViewLocationY, windowWidth, bottomSpaceViewHeight
            );
            setBottomSpaceView(activity, bottomSpaceView);
            setKeyboardVisible(activity, false);

            rootView.onGlobalLayout();
            applyPendingMeasuredMarginOntoRealLayoutParamsAsADevicesMeasurePassWould(rootView, windowWidth, windowHeight);

            bottomMarginAfterEachPass[i] = bottomMarginOf(rootView);

            Thread.sleep(50);
        }

        int finalSettledMargin = bottomMarginAfterEachPass[bottomMarginAfterEachPass.length - 1];
        Assert.assertEquals(
            "final settled bottom margin at window height "
                + resizeSequenceWindowHeights[resizeSequenceWindowHeights.length - 1]
                + " must equal the fixed structural offset (" + fixedStructuralOffsetPxUnrelatedToMargin
                + "px) to avoid leaving the bottom toolbar clipped, since that offset does not depend on"
                + " window height (all margins recorded: " + java.util.Arrays.toString(bottomMarginAfterEachPass) + ")",
            fixedStructuralOffsetPxUnrelatedToMargin,
            finalSettledMargin
        );
    }

    @Test
    public void onGlobalLayoutMustNotSuppressGenuineBottomSpaceViewChangeFromExtraKeysRowToggleAsASelfInducedMarginEcho() throws Exception {
        TermuxActivity activity = buildVisibleActivityWithControllableWindowInsets();
        setKeyboardVisible(activity, false);
        TermuxActivityRootView rootView = buildRootView(activity);

        Rect windowAvailableRectBeforeKeyboardOpens = new Rect(0, 0, 1080, 2000);
        ControllableBottomSpaceView bottomSpaceViewBeforeKeyboardOpens = new ControllableBottomSpaceView(
            RuntimeEnvironment.getApplication(), windowAvailableRectBeforeKeyboardOpens, 0, 1900, 1080, 100
        );
        setBottomSpaceView(activity, bottomSpaceViewBeforeKeyboardOpens);
        rootView.onGlobalLayout();
        Assert.assertEquals(
            "baseline call before keyboard opens must leave bottomMargin at 0",
            0, bottomMarginOf(rootView)
        );

        Rect windowAvailableRectWhileKeyboardVisible = new Rect(0, 0, 1080, 1700);
        int bottomSpaceViewHeight = 100;
        int bottomSpaceViewLocationYHiddenByKeyboard = 1800;
        int pxHiddenByKeyboardOnFirstPass = (bottomSpaceViewLocationYHiddenByKeyboard + bottomSpaceViewHeight)
            - windowAvailableRectWhileKeyboardVisible.bottom;

        setKeyboardVisible(activity, true);
        ControllableBottomSpaceView bottomSpaceViewHiddenByKeyboard = new ControllableBottomSpaceView(
            RuntimeEnvironment.getApplication(), windowAvailableRectWhileKeyboardVisible,
            0, bottomSpaceViewLocationYHiddenByKeyboard, 1080, bottomSpaceViewHeight
        );
        setBottomSpaceView(activity, bottomSpaceViewHiddenByKeyboard);
        rootView.onGlobalLayout();
        Assert.assertEquals(
            "first pass with keyboard newly visible must set the margin to the pixels hidden behind the keyboard",
            pxHiddenByKeyboardOnFirstPass, bottomMarginOf(rootView)
        );

        int extraKeysRowToggleShiftPxUnrelatedToFirstPassMarginDelta = 50;
        int bottomSpaceViewLocationYAfterExtraKeysRowToggle = bottomSpaceViewLocationYHiddenByKeyboard
            - extraKeysRowToggleShiftPxUnrelatedToFirstPassMarginDelta;
        ControllableBottomSpaceView bottomSpaceViewAfterExtraKeysRowToggle = new ControllableBottomSpaceView(
            RuntimeEnvironment.getApplication(), windowAvailableRectWhileKeyboardVisible,
            0, bottomSpaceViewLocationYAfterExtraKeysRowToggle, 1080, bottomSpaceViewHeight
        );
        setBottomSpaceView(activity, bottomSpaceViewAfterExtraKeysRowToggle);
        setKeyboardVisible(activity, true);
        rootView.onGlobalLayout();

        Assert.assertEquals(
            "extra keys row toggle moving the bottom space view by an amount unrelated to the first"
                + " pass margin delta, with window rect and keyboard visibility both unchanged, must not"
                + " be classified as a self-induced echo of the first pass margin mutation, so the stale "
                + pxHiddenByKeyboardOnFirstPass + "px margin from the first pass must be force-corrected"
                + " to 0 rather than left unchanged",
            0, bottomMarginOf(rootView)
        );
    }

    @Test
    public void onGlobalLayoutMustApplyKeyboardInsetHeightMarginWhenGeometryShowsNoOverlapAtSmallWindowHeight() throws Exception {
        TermuxActivity activity = buildVisibleActivityWithControllableWindowInsets();
        setKeyboardVisible(activity, false);
        TermuxActivityRootView rootView = buildRootView(activity);

        int smallWindowWidth = 1080;
        int smallWindowHeight = 700;
        Rect smallWindowAvailableRect = new Rect(0, 0, smallWindowWidth, smallWindowHeight);
        int bottomSpaceViewWidth = 1080;
        int bottomSpaceViewHeight = 100;
        int bottomSpaceViewLocationYFlushWithSmallWindowBottom = 600;

        ControllableBottomSpaceView settledBottomSpaceView = new ControllableBottomSpaceView(
            RuntimeEnvironment.getApplication(), smallWindowAvailableRect, 0,
            bottomSpaceViewLocationYFlushWithSmallWindowBottom, bottomSpaceViewWidth, bottomSpaceViewHeight
        );
        setBottomSpaceView(activity, settledBottomSpaceView);
        rootView.onGlobalLayout();
        applyPendingMeasuredMarginOntoRealLayoutParamsAsADevicesMeasurePassWould(rootView, smallWindowWidth, smallWindowHeight);
        Assert.assertEquals(
            "settled small-window baseline (AC-1) must start with bottomMargin at 0",
            0, bottomMarginOf(rootView)
        );

        int realImeInsetHeightPxWhenKeyboardGenuinelyVisible = 823;
        ControllableBottomSpaceView bottomSpaceViewWithIdenticalGeometryWhileKeyboardVisible = new ControllableBottomSpaceView(
            RuntimeEnvironment.getApplication(), smallWindowAvailableRect, 0,
            bottomSpaceViewLocationYFlushWithSmallWindowBottom, bottomSpaceViewWidth, bottomSpaceViewHeight
        );
        setBottomSpaceView(activity, bottomSpaceViewWithIdenticalGeometryWhileKeyboardVisible);
        setKeyboardVisibleWithInsetHeight(activity, true, realImeInsetHeightPxWhenKeyboardGenuinelyVisible);
        rootView.onGlobalLayout();
        applyPendingMeasuredMarginOntoRealLayoutParamsAsADevicesMeasurePassWould(rootView, smallWindowWidth, smallWindowHeight);
        Assert.assertEquals(
            "AC-4: tapping the text field to show the real keyboard must move the bottom toolbar by the"
                + " real IME inset height, the same adjustment made at full window size, even though"
                + " window geometry shows no overlap at this small settled window height",
            realImeInsetHeightPxWhenKeyboardGenuinelyVisible, bottomMarginOf(rootView)
        );

        ControllableBottomSpaceView bottomSpaceViewWithIdenticalGeometryAfterKeyboardDismissed = new ControllableBottomSpaceView(
            RuntimeEnvironment.getApplication(), smallWindowAvailableRect, 0,
            bottomSpaceViewLocationYFlushWithSmallWindowBottom, bottomSpaceViewWidth, bottomSpaceViewHeight
        );
        setBottomSpaceView(activity, bottomSpaceViewWithIdenticalGeometryAfterKeyboardDismissed);
        setKeyboardVisible(activity, false);
        rootView.onGlobalLayout();
        applyPendingMeasuredMarginOntoRealLayoutParamsAsADevicesMeasurePassWould(rootView, smallWindowWidth, smallWindowHeight);
        Assert.assertEquals(
            "AC-4: dismissing the keyboard again with geometry still unchanged must return the bottom"
                + " toolbar to the settled small-window-height position AC-1 describes",
            0, bottomMarginOf(rootView)
        );
    }

    @Test
    public void onApplyWindowInsetsAloneMustApplyKeyboardInsetHeightMarginWhenGlobalLayoutNeverFiresAtSmallWindowHeight() throws Exception {
        TermuxActivity activity = buildVisibleActivityWithControllableWindowInsets();
        setKeyboardVisible(activity, false);
        TermuxActivityRootView rootView = buildRootView(activity);

        int smallWindowWidth = 1080;
        int smallWindowHeight = 700;
        Rect smallWindowAvailableRect = new Rect(0, 0, smallWindowWidth, smallWindowHeight);
        int bottomSpaceViewWidth = 1080;
        int bottomSpaceViewHeight = 100;
        int bottomSpaceViewLocationYFlushWithSmallWindowBottom = 600;

        ControllableBottomSpaceView settledBottomSpaceView = new ControllableBottomSpaceView(
            RuntimeEnvironment.getApplication(), smallWindowAvailableRect, 0,
            bottomSpaceViewLocationYFlushWithSmallWindowBottom, bottomSpaceViewWidth, bottomSpaceViewHeight
        );
        setBottomSpaceView(activity, settledBottomSpaceView);
        rootView.onGlobalLayout();
        applyPendingMeasuredMarginOntoRealLayoutParamsAsADevicesMeasurePassWould(rootView, smallWindowWidth, smallWindowHeight);
        Assert.assertEquals(
            "settled small-window baseline (AC-1) must start with bottomMargin at 0",
            0, bottomMarginOf(rootView)
        );

        TermuxActivityRootView.WindowInsetsListener windowInsetsListener = new TermuxActivityRootView.WindowInsetsListener();

        int realImeInsetHeightPxWhenKeyboardGenuinelyVisible = 823;
        WindowInsets keyboardVisibleInsets = buildWindowInsetsCarryingKeyboardState(true, realImeInsetHeightPxWhenKeyboardGenuinelyVisible);
        windowInsetsListener.onApplyWindowInsets(rootView, keyboardVisibleInsets);
        applyPendingMeasuredMarginOntoRealLayoutParamsAsADevicesMeasurePassWould(rootView, smallWindowWidth, smallWindowHeight);
        Assert.assertEquals(
            "AC-4 regression on the onApplyWindowInsets path: tapping the text field to show the real"
                + " keyboard must move the bottom toolbar by the real IME inset height via the"
                + " onApplyWindowInsets callback alone, since onGlobalLayout() never fires during this"
                + " transition in genuine split-screen at this small settled window height",
            realImeInsetHeightPxWhenKeyboardGenuinelyVisible, bottomMarginOf(rootView)
        );

        WindowInsets keyboardHiddenInsets = buildWindowInsetsCarryingKeyboardState(false, 0);
        windowInsetsListener.onApplyWindowInsets(rootView, keyboardHiddenInsets);
        applyPendingMeasuredMarginOntoRealLayoutParamsAsADevicesMeasurePassWould(rootView, smallWindowWidth, smallWindowHeight);
        Assert.assertEquals(
            "AC-4 regression on the onApplyWindowInsets path: dismissing the keyboard again via the"
                + " onApplyWindowInsets callback alone, with geometry still unchanged, must return the"
                + " bottom toolbar to the settled small-window-height position AC-1 describes",
            0, bottomMarginOf(rootView)
        );
    }

    @Test
    public void onApplyWindowInsetsAndOnGlobalLayoutTogetherMustNotDoubleApplyOrResetMarginForOneKeyboardTransitionOnOrdinaryDevice() throws Exception {
        TermuxActivity activity = buildVisibleActivityWithControllableWindowInsets();
        setKeyboardVisible(activity, false);
        TermuxActivityRootView rootView = buildRootView(activity);

        int ordinaryWindowWidth = BASELINE_WINDOW_AVAILABLE_RECT.width();
        int ordinaryWindowHeight = BASELINE_WINDOW_AVAILABLE_RECT.height();

        ControllableBottomSpaceView bottomSpaceView = newBaselineBottomSpaceView(RuntimeEnvironment.getApplication());
        setBottomSpaceView(activity, bottomSpaceView);
        rootView.onGlobalLayout();
        applyPendingMeasuredMarginOntoRealLayoutParamsAsADevicesMeasurePassWould(rootView, ordinaryWindowWidth, ordinaryWindowHeight);
        Assert.assertEquals(
            "ordinary-device baseline must start with bottomMargin at 0",
            0, bottomMarginOf(rootView)
        );

        TermuxActivityRootView.WindowInsetsListener windowInsetsListener = new TermuxActivityRootView.WindowInsetsListener();
        int realImeInsetHeightPxWhenKeyboardGenuinelyVisible = 823;

        WindowInsets keyboardVisibleInsets = buildWindowInsetsCarryingKeyboardState(true, realImeInsetHeightPxWhenKeyboardGenuinelyVisible);
        windowInsetsListener.onApplyWindowInsets(rootView, keyboardVisibleInsets);
        applyPendingMeasuredMarginOntoRealLayoutParamsAsADevicesMeasurePassWould(rootView, ordinaryWindowWidth, ordinaryWindowHeight);
        Assert.assertEquals(
            "the onApplyWindowInsets call site must move the bottom toolbar by the real IME inset height",
            realImeInsetHeightPxWhenKeyboardGenuinelyVisible, bottomMarginOf(rootView)
        );

        setKeyboardVisibleWithInsetHeight(activity, true, realImeInsetHeightPxWhenKeyboardGenuinelyVisible);
        rootView.onGlobalLayout();
        applyPendingMeasuredMarginOntoRealLayoutParamsAsADevicesMeasurePassWould(rootView, ordinaryWindowWidth, ordinaryWindowHeight);
        Assert.assertEquals(
            "a second call, via onGlobalLayout(), for the same keyboard-visible transition the"
                + " onApplyWindowInsets call site already handled must leave the bottom toolbar margin"
                + " exactly at the real IME inset height, neither doubling it nor resetting it to 0 due to"
                + " marginRecomputeGate treating the unchanged keyboard-visible state as nothing new",
            realImeInsetHeightPxWhenKeyboardGenuinelyVisible, bottomMarginOf(rootView)
        );

        WindowInsets keyboardHiddenInsets = buildWindowInsetsCarryingKeyboardState(false, 0);
        windowInsetsListener.onApplyWindowInsets(rootView, keyboardHiddenInsets);
        setKeyboardVisible(activity, false);
        rootView.onGlobalLayout();
        applyPendingMeasuredMarginOntoRealLayoutParamsAsADevicesMeasurePassWould(rootView, ordinaryWindowWidth, ordinaryWindowHeight);
        Assert.assertEquals(
            "dismissing the keyboard again, driven through both the onApplyWindowInsets call site and a"
                + " subsequent onGlobalLayout() call for that same dismissal, must return the bottom"
                + " toolbar margin to 0",
            0, bottomMarginOf(rootView)
        );
    }

    @Test
    public void onGlobalLayoutMustStillScheduleZeroPendingMarginOnReturnToFullScreenGeometryWhenKeyboardIsGenuinelyVisibleWithRealInset() throws Exception {
        TermuxActivity activity = buildVisibleActivityWithControllableWindowInsets();
        setKeyboardVisible(activity, false);
        TermuxActivityRootView rootView = buildRootView(activity);

        runBaselineCallEstablishingInertState(rootView, activity);
        Assert.assertEquals("baseline call must leave bottomMargin at 0", 0, bottomMarginOf(rootView));
        Assert.assertNull("baseline call must leave marginBottom unset", rootView.marginBottom);
        Assert.assertNull("baseline call must leave lastMarginBottom unset", rootView.lastMarginBottom);

        setBottomMarginOf(rootView, 400);

        Rect secondCallWindowAvailableRect = new Rect(0, 0, 1080, 2000);
        ControllableBottomSpaceView secondCallBottomSpaceView = new ControllableBottomSpaceView(
            RuntimeEnvironment.getApplication(), secondCallWindowAvailableRect, 0, 1600, 1080, 400
        );
        setBottomSpaceView(activity, secondCallBottomSpaceView);
        int realImeInsetHeightPxDuringMarginEchoPass = 823;
        setKeyboardVisibleWithInsetHeight(activity, true, realImeInsetHeightPxDuringMarginEchoPass);

        rootView.onGlobalLayout();

        Assert.assertEquals(
            "return-to-full-screen resize while visible-because-of-margin must still schedule"
                + " marginBottom 0 to be applied on next measure even though the keyboard is"
                + " genuinely visible with a nonzero real IME inset during this same echo pass",
            Integer.valueOf(0), rootView.marginBottom
        );
    }

    /**
     * Builds the single raw {@link WindowInsets} both API-29 genuinely-attached-view tests below share:
     * a legacy system-window bottom inset and a legacy stable bottom inset, both {@code 100px}, with no
     * transient IME component (no {@code setVisible(Type.ime(), ...)} and no {@code setInsets(Type.ime(), ...)}
     * call), matching a persistent non-IME bottom inset such as a legacy 3-button navigation bar.
     */
    private WindowInsets buildPersistentNonImeBottomInsetWindowInsets(int persistentBottomInsetPx) {
        return new WindowInsets.Builder()
            .setSystemWindowInsets(android.graphics.Insets.of(0, 0, 0, persistentBottomInsetPx))
            .setStableInsets(android.graphics.Insets.of(0, 0, 0, persistentBottomInsetPx))
            .build();
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    public void onGlobalLayoutMustNotFalselyReportKeyboardVisibleFromPersistentNonImeBottomInsetWhenDecorViewIsGenuinelyAttached() throws Exception {
        int persistentBottomInsetPx = 100;
        WindowInsets rawWindowInsets = buildPersistentNonImeBottomInsetWindowInsets(persistentBottomInsetPx);

        WindowInsetsReturningTermuxActivityRootView rootView = buildGenuinelyAttachedRootViewReturning(rawWindowInsets);
        Assert.assertTrue(
            "precondition: the decor view onGlobalLayout() passes to toWindowInsetsCompat must be"
                + " genuinely attached to a window, otherwise the two-argument overload's view-aware"
                + " logic never activates and this test would not exercise the scenario Completion"
                + " Criterion 3 requires",
            rootView.isAttachedToWindow()
        );

        TermuxActivity activity = buildVisibleActivityWithControllableWindowInsets();
        setFieldByReflection(activity, Activity.class, "mWindow", new SingleDecorViewFakeWindow(activity, rootView));
        rootView.setActivity(activity);
        setBottomSpaceView(activity, newBaselineBottomSpaceView(RuntimeEnvironment.getApplication()));

        Assert.assertEquals(
            "precondition: root view must start with bottomMargin at 0 before onGlobalLayout() runs",
            0, bottomMarginOf(rootView)
        );

        rootView.onGlobalLayout();

        Assert.assertEquals(
            "fixed behaviour target: a persistent " + persistentBottomInsetPx + "px non-IME bottom inset"
                + " (e.g. a legacy 3-button navigation bar) with no IME visibility signal must not be"
                + " misreported as a visible keyboard once the view-aware two-argument"
                + " toWindowInsetsCompat overload correctly populates the root stable inset from the"
                + " genuinely attached decor view, so bottomMargin must stay at 0 rather than the"
                + " false-positive " + persistentBottomInsetPx + "px the single-argument overload"
                + " produces by forcing the root stable inset to 0",
            0, bottomMarginOf(rootView)
        );
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    public void onApplyWindowInsetsMustNotFalselyReportKeyboardVisibleFromPersistentNonImeBottomInsetWhenViewArgumentIsGenuinelyAttached() throws Exception {
        int persistentBottomInsetPx = 100;
        WindowInsets rawWindowInsets = buildPersistentNonImeBottomInsetWindowInsets(persistentBottomInsetPx);

        WindowInsetsReturningTermuxActivityRootView rootView = buildGenuinelyAttachedRootViewReturning(rawWindowInsets);
        Assert.assertTrue(
            "precondition: the v parameter onApplyWindowInsets() receives must be genuinely attached to"
                + " a window, otherwise the two-argument overload's view-aware logic never activates and"
                + " this test would not exercise the scenario Completion Criterion 4 requires",
            rootView.isAttachedToWindow()
        );

        TermuxActivity activity = buildVisibleActivityWithControllableWindowInsets();
        rootView.setActivity(activity);
        setBottomSpaceView(activity, newBaselineBottomSpaceView(RuntimeEnvironment.getApplication()));

        Assert.assertEquals(
            "precondition: root view must start with bottomMargin at 0 before onApplyWindowInsets() runs",
            0, bottomMarginOf(rootView)
        );

        TermuxActivityRootView.WindowInsetsListener windowInsetsListener = new TermuxActivityRootView.WindowInsetsListener();
        windowInsetsListener.onApplyWindowInsets(rootView, rawWindowInsets);

        Assert.assertEquals(
            "fixed behaviour target: a persistent " + persistentBottomInsetPx + "px non-IME bottom inset"
                + " with no IME visibility signal must not be misreported as a visible keyboard via the"
                + " onApplyWindowInsets call site once the view-aware two-argument toWindowInsetsCompat"
                + " overload correctly populates the root stable inset from the genuinely attached v"
                + " parameter, so bottomMargin must stay at 0 rather than the false-positive "
                + persistentBottomInsetPx + "px the single-argument overload produces by forcing the"
                + " root stable inset to 0",
            0, bottomMarginOf(rootView)
        );
    }
}
