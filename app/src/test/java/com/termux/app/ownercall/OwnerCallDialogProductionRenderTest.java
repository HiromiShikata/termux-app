package com.termux.app.ownercall;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.termux.R;
import com.termux.app.sessiondefinition.UnansweredOwnerCall;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class OwnerCallDialogProductionRenderTest {

    private static final long CALLED_AT_EPOCH_MILLIS = 1_800_000_000_000L;
    private static final long NOW = CALLED_AT_EPOCH_MILLIS + 6 * 60 * 1000L;
    private static final UnansweredOwnerCall FIRST_CALL = new UnansweredOwnerCall(
        "2027-01-15T08:00:00.000Z", "クロネコ住所録の旧住所の一括削除を実行してよいかご判断ください");
    private static final UnansweredOwnerCall SECOND_CALL = new UnansweredOwnerCall(
        "2027-01-15T08:03:00.000Z", "請求書の送付先を変更してよいかご判断ください");
    private static final UnansweredOwnerCall THIRD_CALL = new UnansweredOwnerCall(
        "2027-01-15T08:05:00.000Z", "クロネコ住所録の旧住所の一括削除を実行してよいかご判断ください");
    private static final List<UnansweredOwnerCall> THREE_CALLS =
        Arrays.asList(FIRST_CALL, SECOND_CALL, THIRD_CALL);

    private static final int SCREEN_HEIGHT_PIXELS = 2400;
    private static final int TERMINAL_AREA_WIDTH_PIXELS = 1080;
    private static final int TERMINAL_ROW_HEIGHT_PIXELS = 36;
    private static final OwnerCallDialogGeometry PORTRAIT_GEOMETRY =
        OwnerCallDialogGeometry.resolve(SCREEN_HEIGHT_PIXELS, 0, TERMINAL_AREA_WIDTH_PIXELS, 120,
            TERMINAL_ROW_HEIGHT_PIXELS);

    @Test
    public void showsNothingWhileNoOwnerCallIsWaiting() {
        View root = inflateActivityLayout();

        OwnerCallDialogBinder.bind(root, Collections.emptyList(), 0, NOW, PORTRAIT_GEOMETRY, null);

        Assert.assertEquals(View.GONE, root.findViewById(R.id.owner_call_dialog).getVisibility());
    }

    @Test
    public void placesTheDialogOverTheTerminalLeavingFiveTerminalRowsVisibleBelowIt() {
        View root = inflateActivityLayout();

        OwnerCallDialogBinder.bind(root, THREE_CALLS, 0, NOW, PORTRAIT_GEOMETRY, null);

        View dialog = root.findViewById(R.id.owner_call_dialog);
        ViewGroup.MarginLayoutParams layoutParams =
            (ViewGroup.MarginLayoutParams) dialog.getLayoutParams();

        Assert.assertEquals(View.VISIBLE, dialog.getVisibility());
        Assert.assertEquals(TERMINAL_AREA_WIDTH_PIXELS, layoutParams.width);
        Assert.assertEquals(SCREEN_HEIGHT_PIXELS / 4, layoutParams.height);
        Assert.assertEquals(0, layoutParams.leftMargin);
        Assert.assertEquals(120 + 5 * TERMINAL_ROW_HEIGHT_PIXELS, layoutParams.bottomMargin);
    }

    @Test
    public void movesTheDialogOntoTheTerminalPaneInLandscape() {
        View root = inflateActivityLayout();
        OwnerCallDialogGeometry landscapeGeometry =
            OwnerCallDialogGeometry.resolve(1080, 1035, 1389, 120, TERMINAL_ROW_HEIGHT_PIXELS);

        OwnerCallDialogBinder.bind(root, THREE_CALLS, 0, NOW, landscapeGeometry, null);

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)
            root.findViewById(R.id.owner_call_dialog).getLayoutParams();

        Assert.assertEquals(1389, layoutParams.width);
        Assert.assertEquals(1080 / 4, layoutParams.height);
        Assert.assertEquals(1035, layoutParams.leftMargin);
        Assert.assertEquals(120 + 5 * TERMINAL_ROW_HEIGHT_PIXELS, layoutParams.bottomMargin);
    }

    @Test
    public void putsTheElapsedTimeOnTheLeftAndEveryControlOnTheRightOfOneHeaderLine() {
        View root = inflateActivityLayout();

        OwnerCallDialogBinder.bind(root, THREE_CALLS, 0, NOW, PORTRAIT_GEOMETRY, null);

        LinearLayout header = root.findViewById(R.id.owner_call_dialog_header);
        TextView relativeTime = root.findViewById(R.id.owner_call_dialog_relative_time);
        TextView position = root.findViewById(R.id.owner_call_dialog_position);

        Assert.assertEquals(LinearLayout.HORIZONTAL, header.getOrientation());
        Assert.assertEquals("6分前", relativeTime.getText().toString());
        Assert.assertEquals("1 / 3", position.getText().toString());
        Assert.assertEquals(1, relativeTime.getMaxLines());
        Assert.assertEquals(1, position.getMaxLines());
        Assert.assertEquals(1.0f,
            ((LinearLayout.LayoutParams) relativeTime.getLayoutParams()).weight, 0.0f);
        Assert.assertEquals(
            Arrays.asList(R.id.owner_call_dialog_relative_time, R.id.owner_call_dialog_position,
                R.id.owner_call_dialog_previous_button, R.id.owner_call_dialog_next_button,
                R.id.owner_call_dialog_close_button),
            childIdsOf(header));
    }

    @Test
    public void pagesThroughEverySimultaneousCallByItsOwnCallTime() {
        View root = inflateActivityLayout();
        TextView body = root.findViewById(R.id.owner_call_dialog_body);
        TextView position = root.findViewById(R.id.owner_call_dialog_position);
        ImageButton previousButton = root.findViewById(R.id.owner_call_dialog_previous_button);
        ImageButton nextButton = root.findViewById(R.id.owner_call_dialog_next_button);

        OwnerCallDialogBinder.bind(root, THREE_CALLS, 0, NOW, PORTRAIT_GEOMETRY, null);
        Assert.assertEquals(FIRST_CALL.getBody(), body.getText().toString());
        Assert.assertFalse(previousButton.isEnabled());
        Assert.assertTrue(nextButton.isEnabled());

        OwnerCallDialogBinder.bind(root, THREE_CALLS, 2, NOW, PORTRAIT_GEOMETRY, null);
        Assert.assertEquals("3 / 3", position.getText().toString());
        Assert.assertEquals(THIRD_CALL.getBody(), body.getText().toString());
        Assert.assertTrue(previousButton.isEnabled());
        Assert.assertFalse(nextButton.isEnabled());
    }

    @Test
    public void closingTheDialogReportsExactlyTheCallThatWasOnScreen() {
        View root = inflateActivityLayout();
        List<UnansweredOwnerCall> dismissed = new ArrayList<>();

        OwnerCallDialogBinder.bind(root, THREE_CALLS, 2, NOW, PORTRAIT_GEOMETRY,
            new OwnerCallDialogBinder.OwnerCallDialogActions() {
                @Override
                public void onPreviousCallRequested() {
                }

                @Override
                public void onNextCallRequested() {
                }

                @Override
                public void onCallDismissed(UnansweredOwnerCall call) {
                    dismissed.add(call);
                }
            });
        root.findViewById(R.id.owner_call_dialog_close_button).performClick();

        Assert.assertEquals(Collections.singletonList(THIRD_CALL), dismissed);
        Assert.assertEquals(THIRD_CALL.getCalledAt(), dismissed.get(0).getCalledAt());
    }

    @Test
    public void keepsALongCallBodyFullyReadableByScrollingInsideTheDialog() {
        View root = inflateActivityLayout();
        UnansweredOwnerCall longCall = new UnansweredOwnerCall("2027-01-15T08:00:00.000Z",
            repeat("クロネコ住所録の旧住所の一括削除を実行してよいかご判断ください\n", 40));

        OwnerCallDialogBinder.bind(root, Collections.singletonList(longCall), 0, NOW,
            PORTRAIT_GEOMETRY, null);
        View dialog = layoutDialog(root);

        ScrollView bodyScroll = root.findViewById(R.id.owner_call_dialog_body_scroll);
        TextView body = root.findViewById(R.id.owner_call_dialog_body);

        Assert.assertEquals(PORTRAIT_GEOMETRY.getHeightPixels(), dialog.getHeight());
        Assert.assertTrue("the whole call body must be reachable inside the dialog",
            body.getHeight() > bodyScroll.getHeight());
        Assert.assertTrue("the call body area must scroll", bodyScroll.canScrollVertically(1));
    }

    @Test
    public void leavesTheTerminalAloneWhenTheDisplayedCallDidNotChange() {
        View root = inflateActivityLayout();

        OwnerCallDialogBinder.bind(root, THREE_CALLS, 0, NOW, PORTRAIT_GEOMETRY, null);
        View dialog = layoutDialog(root);
        OwnerCallDialogBinder.bind(root, THREE_CALLS, 0, NOW, PORTRAIT_GEOMETRY, null);

        Assert.assertFalse("re-binding an unchanged call must not force a layout pass",
            dialog.isLayoutRequested());
    }

    @Test
    public void keepsTheReadingPositionOfALongCallBodyAcrossRefreshes() {
        View root = inflateActivityLayout();
        UnansweredOwnerCall longCall = new UnansweredOwnerCall("2027-01-15T08:00:00.000Z",
            repeat("クロネコ住所録の旧住所の一括削除を実行してよいかご判断ください\n", 40));
        List<UnansweredOwnerCall> oneCall = Collections.singletonList(longCall);

        OwnerCallDialogBinder.bind(root, oneCall, 0, NOW, PORTRAIT_GEOMETRY, null);
        layoutDialog(root);
        ScrollView bodyScroll = root.findViewById(R.id.owner_call_dialog_body_scroll);
        bodyScroll.scrollTo(0, 120);

        OwnerCallDialogBinder.bind(root, oneCall, 0, NOW + 60_000L, PORTRAIT_GEOMETRY, null);

        Assert.assertEquals(120, bodyScroll.getScrollY());
    }

    @Test
    public void returnsToTheTopOfTheBodyWhenAnotherCallIsShown() {
        View root = inflateActivityLayout();

        OwnerCallDialogBinder.bind(root, THREE_CALLS, 0, NOW, PORTRAIT_GEOMETRY, null);
        layoutDialog(root);
        ScrollView bodyScroll = root.findViewById(R.id.owner_call_dialog_body_scroll);
        bodyScroll.scrollTo(0, 90);

        OwnerCallDialogBinder.bind(root, THREE_CALLS, 1, NOW, PORTRAIT_GEOMETRY, null);

        Assert.assertEquals(0, bodyScroll.getScrollY());
    }

    @Test
    public void rendersOnlyTheElapsedTimeThePositionAndTheCallBody() {
        View root = inflateActivityLayout();

        OwnerCallDialogBinder.bind(root, THREE_CALLS, 0, NOW, PORTRAIT_GEOMETRY, null);
        View dialog = layoutDialog(root);

        Assert.assertEquals("6分前" + "1 / 3" + FIRST_CALL.getBody(), renderedTextUnder(dialog));
    }

    @Test
    public void renderedDialogScreenshotShowsTheWaitingCall() throws IOException {
        View root = inflateActivityLayout();

        OwnerCallDialogBinder.bind(root, THREE_CALLS, 0, NOW, PORTRAIT_GEOMETRY, null);
        View dialog = layoutDialog(root);
        Bitmap bitmap = Bitmap.createBitmap(dialog.getWidth(), dialog.getHeight(),
            Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(0xFF101010);
        dialog.draw(canvas);

        File output = new File(System.getProperty("java.io.tmpdir"), "owner-call-dialog-render.png");
        try (FileOutputStream stream = new FileOutputStream(output)) {
            Assert.assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        }
        Assert.assertTrue("rendered dialog screenshot must be written for device-equivalent proof",
            output.exists() && output.length() > 0);
        Assert.assertEquals(TERMINAL_AREA_WIDTH_PIXELS, bitmap.getWidth());
        Assert.assertEquals(SCREEN_HEIGHT_PIXELS / 4, bitmap.getHeight());
    }

    private static String repeat(String text, int times) {
        StringBuilder builder = new StringBuilder();
        for (int repetition = 0; repetition < times; repetition++) {
            builder.append(text);
        }
        return builder.toString();
    }

    private static List<Integer> childIdsOf(ViewGroup group) {
        List<Integer> childIds = new ArrayList<>();
        for (int childIndex = 0; childIndex < group.getChildCount(); childIndex++) {
            childIds.add(group.getChildAt(childIndex).getId());
        }
        return childIds;
    }

    private static String renderedTextUnder(View view) {
        StringBuilder collected = new StringBuilder();
        if (view instanceof TextView) {
            collected.append(((TextView) view).getText());
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int childIndex = 0; childIndex < group.getChildCount(); childIndex++) {
                collected.append(renderedTextUnder(group.getChildAt(childIndex)));
            }
        }
        return collected.toString();
    }

    private static View layoutDialog(View root) {
        View dialog = root.findViewById(R.id.owner_call_dialog);
        dialog.measure(
            View.MeasureSpec.makeMeasureSpec(PORTRAIT_GEOMETRY.getWidthPixels(),
                View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(PORTRAIT_GEOMETRY.getHeightPixels(),
                View.MeasureSpec.EXACTLY));
        dialog.layout(0, 0, dialog.getMeasuredWidth(), dialog.getMeasuredHeight());
        return dialog;
    }

    private static View inflateActivityLayout() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        return LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
    }
}
