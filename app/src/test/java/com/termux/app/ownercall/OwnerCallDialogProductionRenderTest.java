package com.termux.app.ownercall;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.termux.R;

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

    private static final String SESSION_NAME =
        "https_//github_com/HiromiShikata/termux-app/issues/1884";
    private static final long CALLED_AT_EPOCH_MILLIS = 1786681348000L;
    private static final long NOW = CALLED_AT_EPOCH_MILLIS + 6 * 60 * 1000L;
    private static final String REPEATED_BODY =
        "Decide whether the invoice recipient may be changed.";
    private static final OwnerCall FIRST_CALL = new OwnerCall(SESSION_NAME, "2026-08-14T04:22:28Z",
        "Decide whether the previous addresses may be deleted in bulk.");
    private static final OwnerCall SECOND_CALL =
        new OwnerCall(SESSION_NAME, "2026-08-14T04:25:28Z", REPEATED_BODY);
    private static final OwnerCall THIRD_CALL =
        new OwnerCall(SESSION_NAME, "2026-08-14T04:27:28Z", REPEATED_BODY);
    private static final List<OwnerCall> THREE_CALLS =
        Arrays.asList(FIRST_CALL, SECOND_CALL, THIRD_CALL);

    private static final String TABLE_BODY = "Intro line\n\n| Fruit | Price |\n"
        + "|:---|---:|\n| Apple | 100 |\n| Peach | 250 |\nOutro line";
    private static final String TAPPABLE_TABLE_BODY = "| Step | Target |\n|---|---|\n"
        + "| 1 | <copy>termux-reload-settings</copy> |\n"
        + "| 2 | https://github.com/HiromiShikata/termux-app/pull/1925 |";

    private static final int SCREEN_HEIGHT_PIXELS = 2400;
    private static final int TERMINAL_AREA_WIDTH_PIXELS = 1080;
    private static final int TERMINAL_ROW_HEIGHT_PIXELS = 36;
    private static final int TOOLBAR_HEIGHT_PIXELS = 120;
    private static final OwnerCallDialogGeometry PORTRAIT_GEOMETRY =
        OwnerCallDialogGeometry.resolve(SCREEN_HEIGHT_PIXELS, 0, TERMINAL_AREA_WIDTH_PIXELS,
            TOOLBAR_HEIGHT_PIXELS, TERMINAL_ROW_HEIGHT_PIXELS);

    @Test
    public void showsNothingWhileNoOwnerCallIsWaiting() {
        View root = inflateActivityLayout();

        OwnerCallDialogBinder.bind(root, Collections.emptyList(), 0, NOW, PORTRAIT_GEOMETRY, null);

        Assert.assertEquals(View.GONE, root.findViewById(R.id.owner_call_dialog).getVisibility());
    }

    @Test
    public void staysBehindTheSessionListSoTheOwnerCanStillReachIt() {
        View root = inflateActivityLayout();

        OwnerCallDialogBinder.bind(root, THREE_CALLS, 0, NOW, PORTRAIT_GEOMETRY, null);

        float dialogElevation = root.findViewById(R.id.owner_call_dialog).getElevation();
        Assert.assertTrue("the dialog must not cover the session list bottom sheet",
            dialogElevation < root.findViewById(R.id.session_list_bottom_sheet).getElevation());
        Assert.assertTrue("the dialog must not cover the session list scrim",
            dialogElevation
                < root.findViewById(R.id.session_list_bottom_sheet_scrim).getElevation());
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
        Assert.assertEquals(TOOLBAR_HEIGHT_PIXELS + 5 * TERMINAL_ROW_HEIGHT_PIXELS,
            layoutParams.bottomMargin);
    }

    @Test
    public void movesTheDialogOntoTheTerminalPaneInLandscape() {
        View root = inflateActivityLayout();
        OwnerCallDialogGeometry landscapeGeometry = OwnerCallDialogGeometry.resolve(1080, 1035,
            1389, TOOLBAR_HEIGHT_PIXELS, TERMINAL_ROW_HEIGHT_PIXELS);

        OwnerCallDialogBinder.bind(root, THREE_CALLS, 0, NOW, landscapeGeometry, null);

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)
            root.findViewById(R.id.owner_call_dialog).getLayoutParams();

        Assert.assertEquals(1389, layoutParams.width);
        Assert.assertEquals(1080 / 4, layoutParams.height);
        Assert.assertEquals(1035, layoutParams.leftMargin);
        Assert.assertEquals(TOOLBAR_HEIGHT_PIXELS + 5 * TERMINAL_ROW_HEIGHT_PIXELS,
            layoutParams.bottomMargin);
    }

    @Test
    public void putsTheElapsedTimeOnTheLeftAndEveryControlOnTheRightOfOneHeaderLine() {
        View root = inflateActivityLayout();

        OwnerCallDialogBinder.bind(root, THREE_CALLS, 0, NOW, PORTRAIT_GEOMETRY, null);

        LinearLayout header = root.findViewById(R.id.owner_call_dialog_header);
        TextView relativeTime = root.findViewById(R.id.owner_call_dialog_relative_time);
        TextView position = root.findViewById(R.id.owner_call_dialog_position);

        Assert.assertEquals(LinearLayout.HORIZONTAL, header.getOrientation());
        Assert.assertEquals("6m", relativeTime.getText().toString());
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
    public void closingTheDialogCallsOnDialogCloseRequested() {
        View root = inflateActivityLayout();
        boolean[] closedCalled = {false};

        OwnerCallDialogBinder.bind(root, THREE_CALLS, 2, NOW, PORTRAIT_GEOMETRY,
            new OwnerCallDialogBinder.OwnerCallDialogActions() {
                @Override
                public void onPreviousCallRequested() {
                }

                @Override
                public void onNextCallRequested() {
                }

                @Override
                public void onDialogCloseRequested() {
                    closedCalled[0] = true;
                }

                @Override
                public void onCopyableTextTapped(String text) {
                }

                @Override
                public void onUrlTapped(String url) {
                }
            });
        root.findViewById(R.id.owner_call_dialog_close_button).performClick();

        Assert.assertTrue("close button must invoke onDialogCloseRequested", closedCalled[0]);
    }

    @Test
    public void theDialogTextIsSmallerThanTheDefaultSoMoreOfTheCallFitsOnScreen() {
        View root = inflateActivityLayout();
        DisplayMetrics metrics = root.getResources().getDisplayMetrics();

        assertScaledTextSize(root, R.id.owner_call_dialog_relative_time, 11f, metrics);
        assertScaledTextSize(root, R.id.owner_call_dialog_position, 11f, metrics);
        assertScaledTextSize(root, R.id.owner_call_dialog_body, 12f, metrics);
    }

    private static void assertScaledTextSize(View root, int viewId, float expectedScaledPixels,
                                             DisplayMetrics metrics) {
        Assert.assertEquals(
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, expectedScaledPixels, metrics),
            ((TextView) root.findViewById(viewId)).getTextSize(), 0.01f);
    }

    @Test
    public void keepsALongCallBodyFullyReadableByScrollingInsideTheDialog() {
        View root = inflateActivityLayout();
        OwnerCall longCall = new OwnerCall(SESSION_NAME, "2026-08-14T04:22:28Z",
            repeat("Decide whether the previous addresses may be deleted in bulk.\n", 40));

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
        OwnerCall longCall = new OwnerCall(SESSION_NAME, "2026-08-14T04:22:28Z",
            repeat("Decide whether the previous addresses may be deleted in bulk.\n", 40));
        List<OwnerCall> oneCall = Collections.singletonList(longCall);

        OwnerCallDialogBinder.bind(root, oneCall, 0, NOW, PORTRAIT_GEOMETRY, null);
        layoutDialog(root);
        ScrollView bodyScroll = root.findViewById(R.id.owner_call_dialog_body_scroll);
        bodyScroll.scrollTo(0, 120);
        int readingPosition = bodyScroll.getScrollY();
        Assert.assertTrue("the owner must have scrolled away from the top of the body",
            readingPosition > 0);

        OwnerCallDialogBinder.bind(root, oneCall, 0, NOW + 60_000L, PORTRAIT_GEOMETRY, null);

        Assert.assertEquals(readingPosition, bodyScroll.getScrollY());
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

        Assert.assertEquals("6m" + "1 / 3" + FIRST_CALL.getBody(), renderedTextUnder(dialog));
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

    @Test
    public void rendersAGithubTableBodyAsARealTableWithItsCellTexts() {
        View root = inflateActivityLayout();
        bindSingleBody(root, TABLE_BODY, null);
        View dialog = layoutDialog(root);

        List<TableLayout> tables = tablesUnder(dialog);

        Assert.assertEquals(1, tables.size());
        TableLayout table = tables.get(0);
        Assert.assertEquals(3, table.getChildCount());
        Assert.assertEquals(Arrays.asList("Fruit", "Price"), cellTextsOf(table, 0));
        Assert.assertEquals(Arrays.asList("Apple", "100"), cellTextsOf(table, 1));
        Assert.assertEquals(Arrays.asList("Peach", "250"), cellTextsOf(table, 2));
    }

    @Test
    public void boldsTheHeaderRowAndAppliesTheDelimiterAlignmentToEachColumn() {
        View root = inflateActivityLayout();
        bindSingleBody(root, TABLE_BODY, null);
        View dialog = layoutDialog(root);
        TableLayout table = tablesUnder(dialog).get(0);

        Assert.assertEquals(Typeface.BOLD, cellAt(table, 0, 0).getTypeface().getStyle());
        Assert.assertEquals(Typeface.NORMAL, cellAt(table, 1, 0).getTypeface().getStyle());
        Assert.assertEquals(Gravity.LEFT,
            cellAt(table, 1, 0).getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK);
        Assert.assertEquals(Gravity.RIGHT,
            cellAt(table, 1, 1).getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK);
    }

    @Test
    public void keepsTheTextAroundATableInOrderAndOutsideTheTable() {
        View root = inflateActivityLayout();
        bindSingleBody(root, TABLE_BODY, null);
        layoutDialog(root);
        ViewGroup segments = root.findViewById(R.id.owner_call_dialog_body_segments);

        Assert.assertEquals(3, segments.getChildCount());
        Assert.assertEquals("Intro line", ((TextView) segments.getChildAt(0)).getText().toString());
        Assert.assertTrue(segments.getChildAt(1) instanceof TableLayout);
        Assert.assertEquals("Outro line", ((TextView) segments.getChildAt(2)).getText().toString());
    }

    @Test
    public void keepsABodyWithAPipeLineButNoDelimiterRowAsPlainTextInOneTextView() {
        View root = inflateActivityLayout();
        String body = "compare a | b\nwith nothing else";

        bindSingleBody(root, body, null);
        View dialog = layoutDialog(root);
        ViewGroup segments = root.findViewById(R.id.owner_call_dialog_body_segments);

        Assert.assertTrue(tablesUnder(dialog).isEmpty());
        Assert.assertEquals(1, segments.getChildCount());
        Assert.assertEquals(body, ((TextView) segments.getChildAt(0)).getText().toString());
    }

    @Test
    public void keepsCopyAndUrlTargetsInsideTableCellsTappable() {
        View root = inflateActivityLayout();
        RecordingDialogActions actions = new RecordingDialogActions();
        bindSingleBody(root, TAPPABLE_TABLE_BODY, actions);
        View dialog = layoutDialog(root);
        TableLayout table = tablesUnder(dialog).get(0);

        clickableSpansOf(cellAt(table, 1, 1))[0].onClick(cellAt(table, 1, 1));
        clickableSpansOf(cellAt(table, 2, 1))[0].onClick(cellAt(table, 2, 1));

        Assert.assertEquals(Collections.singletonList("termux-reload-settings"),
            actions.copiedTexts);
        Assert.assertEquals(
            Collections.singletonList("https://github.com/HiromiShikata/termux-app/pull/1925"),
            actions.openedUrls);
    }

    @Test
    public void fitsALongTableCellInsideTheDialogWidthByWrappingItsText() {
        View root = inflateActivityLayout();
        String longCell = repeat("長い説明の文章です。", 30);
        bindSingleBody(root, "| Note | Detail |\n|---|---|\n| first | " + longCell + " |", null);
        View dialog = layoutDialog(root);
        TableLayout table = tablesUnder(dialog).get(0);

        Assert.assertTrue("the table must not be wider than the dialog",
            table.getWidth() <= dialog.getWidth());
        Assert.assertTrue("the long cell must wrap onto several lines",
            cellAt(table, 1, 1).getLineCount() > 1);
    }

    @Test
    public void rendersTheTableBodyIdenticallyWhenTheSameCallIsRebound() {
        View root = inflateActivityLayout();
        bindSingleBody(root, TABLE_BODY, null);
        ViewGroup segments = root.findViewById(R.id.owner_call_dialog_body_segments);
        int childCountBefore = segments.getChildCount();

        OwnerCallDialogBinder.bind(root, Collections.singletonList(
            new OwnerCall(SESSION_NAME, "2026-08-14T04:22:28Z", TABLE_BODY)), 0, NOW + 60_000L,
            PORTRAIT_GEOMETRY, null);

        Assert.assertEquals(childCountBefore, segments.getChildCount());
        Assert.assertEquals(1, tablesUnder(root).size());
    }

    private static void bindSingleBody(View root, String body,
                                       OwnerCallDialogBinder.OwnerCallDialogActions actions) {
        OwnerCallDialogBinder.bind(root, Collections.singletonList(
            new OwnerCall(SESSION_NAME, "2026-08-14T04:22:28Z", body)), 0, NOW,
            PORTRAIT_GEOMETRY, actions);
    }

    private static List<TableLayout> tablesUnder(View view) {
        List<TableLayout> tables = new ArrayList<>();
        if (view instanceof TableLayout) {
            tables.add((TableLayout) view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int childIndex = 0; childIndex < group.getChildCount(); childIndex++) {
                tables.addAll(tablesUnder(group.getChildAt(childIndex)));
            }
        }
        return tables;
    }

    private static TextView cellAt(TableLayout table, int rowIndex, int columnIndex) {
        return (TextView) ((TableRow) table.getChildAt(rowIndex)).getChildAt(columnIndex);
    }

    private static List<String> cellTextsOf(TableLayout table, int rowIndex) {
        List<String> texts = new ArrayList<>();
        TableRow row = (TableRow) table.getChildAt(rowIndex);
        for (int columnIndex = 0; columnIndex < row.getChildCount(); columnIndex++) {
            texts.add(((TextView) row.getChildAt(columnIndex)).getText().toString());
        }
        return texts;
    }

    private static ClickableSpan[] clickableSpansOf(TextView cell) {
        Spanned spanned = (Spanned) cell.getText();
        return spanned.getSpans(0, spanned.length(), ClickableSpan.class);
    }

    private static final class RecordingDialogActions
        implements OwnerCallDialogBinder.OwnerCallDialogActions {

        private final List<String> copiedTexts = new ArrayList<>();
        private final List<String> openedUrls = new ArrayList<>();

        @Override
        public void onCopyableTextTapped(String text) {
            copiedTexts.add(text);
        }

        @Override
        public void onUrlTapped(String url) {
            openedUrls.add(url);
        }

        @Override
        public void onPreviousCallRequested() {
        }

        @Override
        public void onNextCallRequested() {
        }

        @Override
        public void onDialogCloseRequested() {
        }
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
