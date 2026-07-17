package com.termux.app.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.termux.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;

@RunWith(AndroidJUnit4.class)
public class DialogTextStyleConsistencyDeviceScreenshotInstrumentedTest {

    private static final int DIALOG_WIDTH = 900;
    private static final int LIGHT_SURFACE_COLOR = 0xFFFFFFFF;

    private static final float LEGACY_HISTORY_TEXT_SIZE_SP = 12f;
    private static final float LEGACY_HISTORY_TEXT_PADDING_HORIZONTAL_DP = 20f;
    private static final float LEGACY_HISTORY_TEXT_PADDING_VERTICAL_DP = 16f;

    @Rule
    public GrantPermissionRule writeExternalStoragePermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private Context themedContext() {
        Context appContext = ApplicationProvider.getApplicationContext();
        return new ContextThemeWrapper(appContext, R.style.Theme_TermuxActivity_DayNight_NoActionBar);
    }

    private int spToPx(Context context, float sp) {
        return Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics()));
    }

    private int dpToPx(Context context, float dp) {
        return Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
    }

    @Test
    public void sharedStyleGivesTheHistoryAndBrowserDialogRowsTheSameTextSizesAndSpacing() {
        Context context = themedContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View historyRow = inflater.inflate(R.layout.item_toolbar_text_input_history, null);
        View newTabRow = inflater.inflate(R.layout.item_browser_new_tab_entry, null);
        View browserHistoryRow = inflater.inflate(R.layout.item_browser_history_entry, null);
        View bookmarkRow = inflater.inflate(R.layout.item_browser_bookmark_list_entry, null);

        TextView historyEntry = historyRow.findViewById(R.id.toolbar_text_input_history_entry);
        TextView newTabTitle = newTabRow.findViewById(R.id.browser_new_tab_entry_title);
        TextView newTabUrl = newTabRow.findViewById(R.id.browser_new_tab_entry_url);
        TextView newTabBadge = newTabRow.findViewById(R.id.browser_new_tab_entry_bookmark_badge);
        TextView browserHistoryTitle = browserHistoryRow.findViewById(R.id.browser_history_entry_title);
        TextView browserHistoryUrl = browserHistoryRow.findViewById(R.id.browser_history_entry_url);
        TextView bookmarkEntry = bookmarkRow.findViewById(android.R.id.text1);

        int detailSizePx = spToPx(context, 11f);
        int titleSizePx = spToPx(context, 13f);

        assertEquals("history dialog entry must use the shared detail text size",
            detailSizePx, Math.round(historyEntry.getTextSize()));
        assertEquals("browser new-tab url must use the shared detail text size",
            detailSizePx, Math.round(newTabUrl.getTextSize()));
        assertEquals("browser new-tab bookmark badge must use the shared detail text size",
            detailSizePx, Math.round(newTabBadge.getTextSize()));
        assertEquals("browser history url must use the shared detail text size",
            detailSizePx, Math.round(browserHistoryUrl.getTextSize()));
        assertEquals("bookmark list entry must use the shared detail text size",
            detailSizePx, Math.round(bookmarkEntry.getTextSize()));

        assertEquals("browser new-tab title must use the shared title text size",
            titleSizePx, Math.round(newTabTitle.getTextSize()));
        assertEquals("browser history title must use the shared title text size",
            titleSizePx, Math.round(browserHistoryTitle.getTextSize()));

        assertTrue("the history dialog entry text must now be smaller than the previous size",
            historyEntry.getTextSize() < spToPx(context, LEGACY_HISTORY_TEXT_SIZE_SP));

        int rowPaddingHorizontalPx = dpToPx(context, 12f);
        int rowPaddingVerticalPx = dpToPx(context, 6f);

        assertRowPadding("history dialog row", historyRow,
            rowPaddingHorizontalPx, rowPaddingVerticalPx);
        assertRowPadding("browser new-tab row", newTabRow,
            rowPaddingHorizontalPx, rowPaddingVerticalPx);
        assertRowPadding("browser history row", browserHistoryRow,
            rowPaddingHorizontalPx, rowPaddingVerticalPx);
        assertRowPadding("bookmark list row", bookmarkRow,
            rowPaddingHorizontalPx, rowPaddingVerticalPx);
    }

    private void assertRowPadding(String label, View row, int horizontalPx, int verticalPx) {
        assertEquals(label + " left padding must match the shared row spacing",
            horizontalPx, row.getPaddingLeft());
        assertEquals(label + " right padding must match the shared row spacing",
            horizontalPx, row.getPaddingRight());
        assertEquals(label + " top padding must match the shared row spacing",
            verticalPx, row.getPaddingTop());
        assertEquals(label + " bottom padding must match the shared row spacing",
            verticalPx, row.getPaddingBottom());
    }

    @Test
    public void rendersHistoryBeforeAfterAndCrossDialogConsistencyScreenshots() throws Exception {
        Context context = themedContext();

        View historyAfter = buildHistoryList(context, false);
        View historyBefore = buildHistoryList(context, true);
        View consistencyAfter = buildCrossDialogList(context);

        saveScreenshot(renderToBitmap(historyBefore), "dialog-history-before.png");
        saveScreenshot(renderToBitmap(historyAfter), "dialog-history-after.png");
        saveScreenshot(renderToBitmap(consistencyAfter), "dialog-consistency-after.png");
    }

    private LinearLayout buildHistoryList(Context context, boolean legacySizing) {
        LayoutInflater inflater = LayoutInflater.from(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        String[] entries = {
            "git status --short",
            "./gradlew :app:assembleDebug",
            "cd /data/data/com.termux/files/home && ls -la"
        };
        for (String entry : entries) {
            View row = inflater.inflate(R.layout.item_toolbar_text_input_history, container, false);
            TextView entryView = row.findViewById(R.id.toolbar_text_input_history_entry);
            entryView.setText(entry);
            ImageButton pinButton = row.findViewById(R.id.toolbar_text_input_history_pin_button);
            pinButton.setImageResource(R.drawable.ic_browser_bookmark_star_outline);
            if (legacySizing) {
                row.setPadding(0, 0, 0, 0);
                entryView.setTextSize(TypedValue.COMPLEX_UNIT_SP, LEGACY_HISTORY_TEXT_SIZE_SP);
                int paddingHorizontal = dpToPx(context, LEGACY_HISTORY_TEXT_PADDING_HORIZONTAL_DP);
                int paddingVertical = dpToPx(context, LEGACY_HISTORY_TEXT_PADDING_VERTICAL_DP);
                entryView.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
            }
            container.addView(row);
        }
        return container;
    }

    private LinearLayout buildCrossDialogList(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        View historyRow = inflater.inflate(R.layout.item_toolbar_text_input_history, container, false);
        ((TextView) historyRow.findViewById(R.id.toolbar_text_input_history_entry))
            .setText("history dialog: git commit --amend");
        ((ImageButton) historyRow.findViewById(R.id.toolbar_text_input_history_pin_button))
            .setImageResource(R.drawable.ic_browser_bookmark_star_outline);
        container.addView(historyRow);

        View newTabRow = inflater.inflate(R.layout.item_browser_new_tab_entry, container, false);
        ((TextView) newTabRow.findViewById(R.id.browser_new_tab_entry_title))
            .setText("new-tab dialog: Termux Wiki");
        ((TextView) newTabRow.findViewById(R.id.browser_new_tab_entry_url))
            .setText("https://wiki.termux.com");
        newTabRow.findViewById(R.id.browser_new_tab_entry_bookmark_badge).setVisibility(View.VISIBLE);
        container.addView(newTabRow);

        View browserHistoryRow = inflater.inflate(R.layout.item_browser_history_entry, container, false);
        ((TextView) browserHistoryRow.findViewById(R.id.browser_history_entry_title))
            .setText("edit-url dialog: Example Domain");
        ((TextView) browserHistoryRow.findViewById(R.id.browser_history_entry_url))
            .setText("https://example.com");
        container.addView(browserHistoryRow);

        View bookmarkRow = inflater.inflate(R.layout.item_browser_bookmark_list_entry, container, false);
        ((TextView) bookmarkRow.findViewById(android.R.id.text1))
            .setText("bookmarks dialog: GitHub\nhttps://github.com");
        container.addView(bookmarkRow);

        return container;
    }

    private Bitmap renderToBitmap(View container) {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(DIALOG_WIDTH, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        container.measure(widthSpec, heightSpec);
        int height = Math.max(1, container.getMeasuredHeight());
        container.layout(0, 0, DIALOG_WIDTH, height);
        Bitmap bitmap = Bitmap.createBitmap(DIALOG_WIDTH, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(LIGHT_SURFACE_COLOR);
        container.draw(canvas);
        return bitmap;
    }

    private void saveScreenshot(Bitmap bitmap, String fileName) throws Exception {
        File directory = new File(Environment.getExternalStorageDirectory(),
            "termux-instrumentation-screenshots");
        if (!directory.exists()) {
            assertTrue("shared screenshot directory must be creatable so the rendered PNG survives "
                + "the post-test APK uninstall and can be pulled as a CI artifact", directory.mkdirs());
        }
        File out = new File(directory, fileName);
        try (FileOutputStream stream = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        assertTrue(out.exists() && out.length() > 0);
    }
}
