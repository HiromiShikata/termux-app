package com.termux.app.terminal.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.termux.R;
import com.termux.app.terminal.io.TerminalToolbarViewPager.SubmittedTextInputHistoryAdapter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;

@RunWith(AndroidJUnit4.class)
public class PinnedHistoryEditIconDeviceScreenshotInstrumentedTest {

    private static final int ROW_WIDTH = 1000;
    private static final int SURFACE_COLOR = 0xFF121212;
    private static final String PINNED_ENTRY = "git commit -m \"release\"";
    private static final String UNPINNED_ENTRY = "ls -la";

    @Rule
    public GrantPermissionRule writeExternalStoragePermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void pinnedRowShowsTheEditPencilAndTappingItOpensAPrefilledEditor() throws Exception {
        Context appContext = ApplicationProvider.getApplicationContext();
        Context context =
            new ContextThemeWrapper(appContext, R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        FrameLayout unattachedParent = new FrameLayout(context);

        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5);
        history.add(UNPINNED_ENTRY);
        history.add(PINNED_ENTRY);
        history.pin(PINNED_ENTRY);
        SubmittedTextInputHistoryAdapter adapter =
            new SubmittedTextInputHistoryAdapter(context, history, () -> {});

        View pinnedRow = adapter.getView(0, null, unattachedParent);
        View unpinnedRow = adapter.getView(1, null, unattachedParent);
        assertEquals(PINNED_ENTRY, adapter.getItem(0));
        assertEquals(View.VISIBLE,
            pinnedRow.findViewById(R.id.toolbar_text_input_history_edit_button).getVisibility());
        assertEquals(View.GONE,
            unpinnedRow.findViewById(R.id.toolbar_text_input_history_edit_button).getVisibility());

        View editor = View.inflate(context, R.layout.dialog_toolbar_text_input_history_edit, null);
        EditText editorInput = editor.findViewById(R.id.toolbar_text_input_history_edit_input);
        editorInput.setText(PINNED_ENTRY);

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.addView(pinnedRow, matchWidthWrapHeight());
        column.addView(unpinnedRow, matchWidthWrapHeight());
        column.addView(editor, matchWidthWrapHeight());

        int widthSpec = View.MeasureSpec.makeMeasureSpec(ROW_WIDTH, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        column.measure(widthSpec, heightSpec);
        int columnHeight = Math.max(1, column.getMeasuredHeight());
        column.layout(0, 0, ROW_WIDTH, columnHeight);

        Bitmap bitmap = Bitmap.createBitmap(ROW_WIDTH, columnHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(SURFACE_COLOR);
        column.draw(canvas);

        File out = new File(sharedScreenshotDirectory(), "pinned-history-edit-icon.png");
        try (FileOutputStream stream = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        assertTrue(out.exists() && out.length() > 0);
    }

    private static LinearLayout.LayoutParams matchWidthWrapHeight() {
        return new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static File sharedScreenshotDirectory() {
        File directory = new File(Environment.getExternalStorageDirectory(),
            "termux-instrumentation-screenshots");
        if (!directory.exists()) {
            assertTrue("shared screenshot directory must be creatable so the rendered PNG survives "
                + "the post-test APK uninstall and can be pulled as a CI artifact", directory.mkdirs());
        }
        return directory;
    }
}
