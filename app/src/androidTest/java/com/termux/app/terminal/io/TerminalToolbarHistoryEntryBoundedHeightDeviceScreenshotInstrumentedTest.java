package com.termux.app.terminal.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.termux.R;
import com.termux.app.terminal.MaxHeightScrollView;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;

@RunWith(AndroidJUnit4.class)
public class TerminalToolbarHistoryEntryBoundedHeightDeviceScreenshotInstrumentedTest {

    private static final int DIALOG_WIDTH = 900;
    private static final int LIGHT_SURFACE_COLOR = 0xFFFFFFFF;

    private static final String LONG_PINNED_ENTRY =
        "for i in $(seq 1 100); do echo \"line $i building the debug apk\"; done "
            + "# a very long pinned prompt that spans far more than three lines and must "
            + "stay bounded to about three lines while remaining scrollable inside the "
            + "history dialog so it no longer fills the whole screen";
    private static final String SHORT_ENTRY = "git status --short";
    private static final String SECOND_SHORT_ENTRY = "./gradlew :app:assembleDebug";

    @Rule
    public GrantPermissionRule writeExternalStoragePermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private Context themedContext() {
        Context appContext = ApplicationProvider.getApplicationContext();
        return new ContextThemeWrapper(appContext, R.style.Theme_TermuxActivity_DayNight_NoActionBar);
    }

    private TerminalToolbarViewPager.SubmittedTextInputHistoryAdapter pinnedLongEntryAdapter(Context context) {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5);
        history.add(SHORT_ENTRY);
        history.add(SECOND_SHORT_ENTRY);
        history.add(LONG_PINNED_ENTRY);
        history.pin(LONG_PINNED_ENTRY);
        return new TerminalToolbarViewPager.SubmittedTextInputHistoryAdapter(context, history, () -> {
        });
    }

    private LinearLayout renderAdapterRows(Context context,
                                           TerminalToolbarViewPager.SubmittedTextInputHistoryAdapter adapter) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        for (int position = 0; position < adapter.getCount(); position++) {
            container.addView(adapter.getView(position, null, container));
        }
        return container;
    }

    @Test
    public void longPinnedEntryStaysBoundedToThreeLinesAndScrollableWhileShortEntriesAreUnchanged() throws Exception {
        Context context = themedContext();
        TerminalToolbarViewPager.SubmittedTextInputHistoryAdapter adapter = pinnedLongEntryAdapter(context);

        assertEquals("pinning must keep the long entry ordered to the top of the list",
            LONG_PINNED_ENTRY, adapter.getItem(0));

        LinearLayout list = renderAdapterRows(context, adapter);
        Bitmap bitmap = renderToBitmap(list);
        saveScreenshot(bitmap, "toolbar-history-long-pinned-bounded.png");

        View longRow = list.getChildAt(0);
        MaxHeightScrollView longScroll =
            longRow.findViewById(R.id.toolbar_text_input_history_entry_scroll);
        TextView longEntry = longRow.findViewById(R.id.toolbar_text_input_history_entry);
        int cap = longEntry.getLineHeight() * HistoryEntryScrollBinder.MAX_VISIBLE_LINES;

        assertEquals("the long pinned entry must be capped to about three lines tall",
            cap, longScroll.getMeasuredHeight());
        assertTrue("the full long text must remain present so the user can scroll to read the rest",
            longScroll.getChildAt(0).getMeasuredHeight() > cap);
        assertTrue("the long entry must be internally scrollable within its bounded height",
            longScroll.canScrollVertically(1));

        View shortRow = list.getChildAt(1);
        MaxHeightScrollView shortScroll =
            shortRow.findViewById(R.id.toolbar_text_input_history_entry_scroll);
        assertTrue("a short entry must keep its natural height below the three-line cap",
            shortScroll.getMeasuredHeight() < cap);
        assertFalse("a short entry must not become scrollable",
            shortScroll.canScrollVertically(1) || shortScroll.canScrollVertically(-1));
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
